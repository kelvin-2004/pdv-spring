package PDV.PDV.controller;

import PDV.PDV.service.MercadoPagoService;
import PDV.PDV.service.pedidoService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import PDV.PDV.service.WebhookSignatureUtil;

@RestController
@RequestMapping("/webhook")
public class MercadoPagoWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    @Value("${mercado.pago.webhook-secret:}")
    private String webhookSecret;

    @Autowired private MercadoPagoService mercadoPagoService;
    @Autowired private pedidoService pedidoService;
    @Autowired private ObjectMapper objectMapper;

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> receberNotificacao(@RequestBody(required = false) String body,
                                                   HttpServletRequest request) {
        try {
            if (body == null || body.isBlank()) {
                return ResponseEntity.ok().build();
            }

            JsonNode notificacao = objectMapper.readTree(body);
            String dataId = extrairDataId(notificacao);

            if (!assinaturaValida(dataId, request)) {
                log.warn("Webhook do Mercado Pago rejeitado: assinatura inválida ou ausente.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String tipo = notificacao.path("type").asText(notificacao.path("topic").asText(null));
            if (!"payment".equals(tipo)) {
                return ResponseEntity.ok().build();
            }

            Long paymentId = notificacao.path("data").path("id").asLong(0);
            if (paymentId <= 0) {
                paymentId = notificacao.path("id").asLong(0);
            }
            if (paymentId <= 0) {
                return ResponseEntity.ok().build();
            }

            JsonNode pagamento = mercadoPagoService.consultarStatus(paymentId);
            if ("approved".equals(pagamento.path("status").asText(null))) {
                log.info("Pagamento aprovado via webhook (paymentId={})", paymentId);
                pedidoService.confirmarPagamento(paymentId);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Falha ao processar webhook do Mercado Pago: {}", e.getMessage(), e);
            return ResponseEntity.ok().build();
        }
    }

    private String extrairDataId(JsonNode notificacao) {
        JsonNode idNode = notificacao.path("data").path("id");
        if (idNode.isMissingNode() || idNode.isNull()) {
            idNode = notificacao.path("id");
        }
        if (idNode.isMissingNode() || idNode.isNull()) {
            return null;
        }
        return idNode.asText();
    }

    private boolean assinaturaValida(String dataId, HttpServletRequest request) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return true;
        }

        String assinatura = request.getHeader("x-signature");
        if (assinatura == null || assinatura.isBlank()) {
            log.warn("Webhook sem header x-signature (secret configurado).");
            return false;
        }

        String ts = null;
        String v1 = null;
        for (String parte : assinatura.split(",")) {
            String[] kv = parte.split("=", 2);
            if (kv.length == 2) {
                if ("ts".equals(kv[0].trim())) ts = kv[1].trim();
                else if ("v1".equals(kv[0].trim())) v1 = kv[1].trim();
            }
        }
        if (ts == null || v1 == null) {
            log.warn("Header x-signature em formato inesperado: {}", assinatura);
            return false;
        }

        if (!WebhookSignatureUtil.timestampValido(ts, System.currentTimeMillis(), 300_000L)) {
            log.warn("Webhook com timestamp inválido ou fora da janela de 5 minutos.");
            return false;
        }

        String requestId = request.getHeader("x-request-id");
        boolean valido = WebhookSignatureUtil.assinaturaConfere(webhookSecret, dataId, requestId, ts, v1);
        if (!valido) {
            log.warn("Assinatura do webhook inválida (manifest gerado: '{}').",
                    WebhookSignatureUtil.montarManifest(dataId, requestId, ts));
        }
        return valido;
    }

}
