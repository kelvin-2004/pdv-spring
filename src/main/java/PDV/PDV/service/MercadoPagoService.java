package PDV.PDV.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MercadoPagoService {

    private static final String BASE_URL = "https://api.mercadopago.com";

    @Value("${mercado.pago.access-token:}")
    private String accessToken;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public MercadoPagoService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public JsonNode criarPagamentoPix(BigDecimal valor, String descricao, String email, String nome,
                                      String notificationUrl, OffsetDateTime dataExpiracao) throws Exception {
        Map<String, Object> payer = new LinkedHashMap<>();
        payer.put("email", (email == null || email.isBlank()) ? "cliente@marmitassousa.com.br" : email);
        if (nome != null && !nome.isBlank()) {
            payer.put("first_name", nome);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transaction_amount", valor);
        body.put("description", descricao);
        body.put("payment_method_id", "pix");
        body.put("payer", payer);
        if (notificationUrl != null && !notificationUrl.isBlank()) {
            body.put("notification_url", notificationUrl);
        }
        if (dataExpiracao != null) {
            body.put("date_of_expiration", dataExpiracao.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }

        return post("/v1/payments", body);
    }

    public JsonNode criarPagamentoCartao(BigDecimal valor, String descricao, String token,
                                         String paymentMethodId, String email, String nome,
                                         String notificationUrl) throws Exception {
        Map<String, Object> payer = new LinkedHashMap<>();
        payer.put("email", (email == null || email.isBlank()) ? "cliente@marmitassousa.com.br" : email);
        if (nome != null && !nome.isBlank()) {
            payer.put("first_name", nome);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("transaction_amount", valor);
        body.put("installments", 1);
        body.put("description", descricao);
        if (paymentMethodId != null && !paymentMethodId.isBlank()) {
            body.put("payment_method_id", paymentMethodId);
        }
        body.put("payer", payer);
        if (notificationUrl != null && !notificationUrl.isBlank()) {
            body.put("notification_url", notificationUrl);
        }

        return post("/v1/payments", body);
    }

    public JsonNode consultarStatus(Long paymentId) throws Exception {
        return get("/v1/payments/" + paymentId);
    }

    private JsonNode post(String path, Map<String, Object> body) throws Exception {
        String json = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(response.body());
    }

    private JsonNode get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(response.body());
    }
}
