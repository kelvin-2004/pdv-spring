package PDV.PDV.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

/**
 * Gerencia o token OAuth2 da iFood (grant client_credentials) com cache local
 * e renovação automática pouco antes da expiração.
 */
@Service
public class IfoodAuthService {

    private static final Logger log = LoggerFactory.getLogger(IfoodAuthService.class);

    @Value("${ifood.client-id:}") private String clientId;
    @Value("${ifood.client-secret:}") private String clientSecret;
    @Value("${ifood.merchant-id:}") private String merchantId;
    @Value("${ifood.base-url:https://merchant-api.ifood.com.br}") private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    private String accessToken;
    private Instant expiraEm = Instant.EPOCH;

    public IfoodAuthService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean configurado() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    public String getMerchantId() {
        return merchantId == null ? "" : merchantId.trim();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public synchronized String obterAccessToken() {
        if (accessToken != null && Instant.now().isBefore(expiraEm.minusSeconds(60))) {
            return accessToken;
        }
        if (!configurado()) {
            throw new IllegalStateException("iFood não configurada (IFOOD_CLIENT_ID / IFOOD_CLIENT_SECRET ausentes).");
        }

        // OAuth2 client_credentials. A iFood usa os nomes camelCase (clientId/clientSecret);
        // enviamos também a variante snake_case por compatibilidade com versões da API.
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("clientId", clientId);
        form.add("clientSecret", clientSecret);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                baseUrl + "/authentication/v1.0/oauth/token", req, String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("Falha ao autenticar na iFood: HTTP " + resp.getStatusCode());
        }

        try {
            JsonNode json = objectMapper.readTree(resp.getBody());
            String token = json.path("access_token").asText(null);
            long expiresIn = json.path("expires_in").asLong(3600);
            if (token == null || token.isBlank()) {
                throw new IllegalStateException("Token da iFood ausente na resposta.");
            }
            this.accessToken = token;
            this.expiraEm = Instant.now().plusSeconds(expiresIn);
            return token;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao interpretar token da iFood.", e);
        }
    }
}
