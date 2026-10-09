package PDV.PDV.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class WebhookSignatureUtil {

    private WebhookSignatureUtil() {
    }

    public static String montarManifest(String dataId, String requestId, String ts) {
        StringBuilder manifest = new StringBuilder();
        if (dataId != null && !dataId.isBlank()) {
            manifest.append("id:").append(dataId).append(';');
        }
        if (requestId != null && !requestId.isBlank()) {
            manifest.append("request-id:").append(requestId).append(';');
        }
        manifest.append("ts:").append(ts).append(';');
        return manifest.toString();
    }

    public static String hmacSha256Hex(String secret, String mensagem) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(mensagem.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao calcular HMAC-SHA256.", e);
        }
    }

    public static boolean assinaturaConfere(String secret, String dataId, String requestId, String ts, String v1) {
        String esperado = hmacSha256Hex(secret, montarManifest(dataId, requestId, ts));
        return MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8),
                v1.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean timestampValido(String ts, long agoraMs, long toleranciaMs) {
        try {
            long timestamp = Long.parseLong(ts);
            // O Mercado Pago envia "ts" em segundos (epoch), mas agoraMs está em milissegundos.
            // Sem a conversão, a comparação sempre falha (diferença ~1e12), rejeitando todos os
            // webhooks com "timestamp inválido ou fora da janela". Se o valor já vier em
            // milissegundos (>= 1e12), usamos como está.
            long timestampMs = timestamp < 1_000_000_000_000L ? timestamp * 1000L : timestamp;
            return Math.abs(agoraMs - timestampMs) <= toleranciaMs;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
