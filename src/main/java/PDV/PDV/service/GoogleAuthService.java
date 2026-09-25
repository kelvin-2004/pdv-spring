package PDV.PDV.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class GoogleAuthService {

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    @Value("${GOOGLE_CLIENT_ID:}")
    private String clientId;

    @Value("${GOOGLE_CLIENT_SECRET:}")
    private String clientSecret;

    @Value("${GOOGLE_REDIRECT_URI:}")
    private String redirectUri;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public GoogleAuthService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildAuthorizationUrl(String state) {
        return AUTH_URL
                + "?client_id=" + enc(clientId)
                + "&redirect_uri=" + enc(redirectUri)
                + "&response_type=code"
                + "&scope=" + enc("openid email profile")
                + "&access_type=online"
                + "&state=" + enc(state);
    }

    public GoogleUsuario trocarCodigo(String code) throws Exception {
        String body = "code=" + enc(code)
                + "&client_id=" + enc(clientId)
                + "&client_secret=" + enc(clientSecret)
                + "&redirect_uri=" + enc(redirectUri)
                + "&grant_type=authorization_code";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalArgumentException("Falha ao autenticar com o Google.");
        }
        JsonNode token = objectMapper.readTree(response.body());
        String accessToken = token.path("access_token").asText(null);
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Token de acesso não recebido.");
        }
        return obterUsuario(accessToken);
    }

    private GoogleUsuario obterUsuario(String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USERINFO_URL))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalArgumentException("Não foi possível obter os dados da conta Google.");
        }
        JsonNode u = objectMapper.readTree(response.body());
        return new GoogleUsuario(
                u.path("sub").asText(null),
                u.path("email").asText(null),
                u.path("name").asText(null));
    }

    private static String enc(String valor) {
        return URLEncoder.encode(valor == null ? "" : valor, StandardCharsets.UTF_8);
    }

    public record GoogleUsuario(String googleId, String email, String nome) {
    }
}
