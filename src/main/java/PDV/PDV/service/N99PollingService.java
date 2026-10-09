package PDV.PDV.service;

import PDV.PDV.model.pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Agendador que puxa os pedidos da 99Food em intervalos fixos e os repassa ao
 * {@link N99Service}. Só atua quando o modo é "polling" e as credenciais estão
 * configuradas (em dev, sem N99_* definidas, não faz nada) — mesmo padrão da iFood.
 */
@Service
public class N99PollingService {

    private static final Logger log = LoggerFactory.getLogger(N99PollingService.class);

    @Value("${n99.modo:polling}")
    private String modo;

    private final N99Service n99Service;
    private final N99AuthService authService;

    private volatile boolean confirmMethodGarantido = false;

    public N99PollingService(N99Service n99Service, N99AuthService authService) {
        this.n99Service = n99Service;
        this.authService = authService;
    }

    @Scheduled(fixedDelayString = "${n99.polling-interval-ms:30000}")
    public void poll() {
        if (!"polling".equalsIgnoreCase(modo)) {
            return;
        }
        // Garante (uma vez) que a loja aceite confirmação via OpenAPI, para os pedidos
        // chegarem ao PDV. Repete enquanto não for aceito (ex.: loja ainda não autorizada).
        if (!confirmMethodGarantido && authService.configurado()) {
            confirmMethodGarantido = n99Service.garantirConfirmMethodOpenapi();
        }
        if (!authService.configurado()) {
            return;
        }
        try {
            JsonNode eventos = n99Service.buscarEventos();
            if (eventos == null || !eventos.isArray()) {
                return;
            }
            for (JsonNode ev : eventos) {
                String orderId = n99Service.orderIdDoEvento(ev);
                if (orderId == null || orderId.isBlank()) {
                    continue;
                }
                try {
                    JsonNode order = n99Service.buscarPedido(orderId);
                    Optional<pedido> novo = n99Service.aplicarPedido(order);
                    String status99 = order == null ? ""
                            : order.path("status").asText("").toUpperCase();
                    if (novo.isPresent() && ("CREATED".equals(status99) || "CONFIRMED".equals(status99))) {
                        n99Service.confirmarPedido(orderId);
                    }
                } catch (Exception e) {
                    log.error("Falha ao processar evento 99 (orderId={}): {}", orderId, e.getMessage());
                }
            }
            n99Service.confirmarEventos(eventos);
        } catch (Exception e) {
            log.error("Falha no polling da 99: {}", e.getMessage());
        }
    }
}
