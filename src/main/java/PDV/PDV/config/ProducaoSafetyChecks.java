package PDV.PDV.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile("prod")
public class ProducaoSafetyChecks {

    private static final Logger log = LoggerFactory.getLogger(ProducaoSafetyChecks.class);

    private final String webhookSecret;
    private final String publicKey;
    private final String accessToken;
    private final String webhookUrl;

    public ProducaoSafetyChecks(
            @Value("${mercado.pago.webhook-secret:}") String webhookSecret,
            @Value("${mercado.pago.public-key:}") String publicKey,
            @Value("${mercado.pago.access-token:}") String accessToken,
            @Value("${mercado.pago.webhook-url:}") String webhookUrl) {
        this.webhookSecret = webhookSecret;
        this.publicKey = publicKey;
        this.accessToken = accessToken;
        this.webhookUrl = webhookUrl;
    }

    @PostConstruct
    public void validar() {
        List<String> erros = new ArrayList<>();

        if (isBlank(webhookSecret)) {
            erros.add("MERCADO_PAGO_WEBHOOK_SECRET não está definido — o webhook ficaria aceitando "
                    + "qualquer notificação sem validar a assinatura.");
        }
        if (!publicKey.startsWith("APP_USR-")) {
            erros.add("MERCADO_PAGO_PUBLIC_KEY deve ser uma chave de produção (prefixo APP_USR-...).");
        }
        if (!accessToken.startsWith("APP_USR-")) {
            erros.add("MERCADO_PAGO_ACCESS_TOKEN deve ser um token de produção (prefixo APP_USR-...).");
        }

        if (!erros.isEmpty()) {
            throw new IllegalStateException(
                    "Configuração de produção inválida:\n  - " + String.join("\n  - ", erros));
        }

        if (isBlank(webhookUrl) || !webhookUrl.startsWith("https://")) {
            log.warn("MERCADO_PAGO_WEBHOOK_URL não aponta para uma URL HTTPS pública. "
                    + "As notificações do Mercado Pago podem não chegar (o polling do cliente cobre como fallback).");
        }

        log.info("Checagens de produção OK: credenciais APP_USR- e segredo do webhook configurados.");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
