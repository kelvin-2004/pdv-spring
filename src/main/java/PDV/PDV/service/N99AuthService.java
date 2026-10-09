package PDV.PDV.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * Gerencia o {@code auth_token} da API 99Food (99Compras/Didi Food). A autenticação
 * usa {@code GET /v1/auth/authtoken/get} com {@code app_id}/{@code app_secret}/
 * {@code app_shop_id} como query params (diferente do OAuth2 client_credentials da
 * iFood). Como a documentação não informa a expiração exata do token, usamos um TTL
 * conservador (50 min) e renovamos via {@code /refresh} antes de obter um novo.
 */
@Service
public class N99AuthService {

    private static final Logger log = LoggerFactory.getLogger(N99AuthService.class);

    @Value("${n99.app-id:}") private String appId;
    @Value("${n99.secret:}") private String appSecret;
    @Value("${n99.shop-id:}") private String shopId;
    @Value("${n99.base-url:}") private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    private String authToken;
    private Instant expiraEm = Instant.EPOCH;

    public N99AuthService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean configurado() {
        return !blank(appId) && !blank(appSecret) && !blank(shopId) && !blank(baseUrl);
    }

    public String getShopId() {
        return shopId == null ? "" : shopId.trim();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public synchronized String obterAuthToken() {
        if (authToken != null && Instant.now().isBefore(expiraEm)) {
            return authToken;
        }
        if (!configurado()) {
            throw new IllegalStateException(
                    "99 não configurada (N99_APP_ID / N99_SECRET / N99_SHOP_ID / N99_BASE_URL ausentes).");
        }

        // Sem token ou expirado: a doc orienta chamar refresh primeiro (melhor esforço).
        try {
            get("/v1/auth/authtoken/refresh");
        } catch (Exception e) {
            log.debug("Refresh do token 99 indisponível (segue para o get): {}", e.getMessage());
        }

        JsonNode json = get("/v1/auth/authtoken/get");
        String token = extrairToken(json);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("auth_token da 99 ausente na resposta.");
        }
        this.authToken = token;
        this.expiraEm = Instant.now().plus(Duration.ofMinutes(50));
        return token;
    }

    private String extrairToken(JsonNode json) {
        if (json == null || json.isMissingNode() || json.isNull()) {
            return null;
        }
        String t = json.path("auth_token").asText(null);
        if (t == null || t.isBlank()) {
            t = json.path("data").path("auth_token").asText(null);
        }
        if (t == null || t.isBlank()) {
            t = json.path("access_token").asText(null);
        }
        return t;
    }

    private JsonNode get(String path) {
        String url = baseUrl + path
                + "?app_id=" + enc(appId)
                + "&app_secret=" + enc(appSecret)
                + "&app_shop_id=" + enc(shopId);
        ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("99 GET " + path + " -> HTTP " + resp.getStatusCode());
        }
        if (log.isDebugEnabled()) {
            log.debug("99 GET {} -> {}", path, resp.getBody());
        }
        try {
            return objectMapper.readTree(resp.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao interpretar resposta da 99.", e);
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
