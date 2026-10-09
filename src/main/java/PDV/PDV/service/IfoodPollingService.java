package PDV.PDV.service;

import PDV.PDV.model.pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Agendador que puxa os pedidos da iFood em intervalos fixos e os repassa ao
 * {@link IfoodService}. Só atua quando o modo é "polling" e as credenciais estão
 * configuradas (em dev, sem IFOOD_* definidas, não faz nada).
 */
@Service
public class IfoodPollingService {

    private static final Logger log = LoggerFactory.getLogger(IfoodPollingService.class);

    @Value("${ifood.modo:polling}")
    private String modo;

    private final IfoodService ifoodService;
    private final IfoodAuthService authService;

    public IfoodPollingService(IfoodService ifoodService, IfoodAuthService authService) {
        this.ifoodService = ifoodService;
        this.authService = authService;
    }

    @Scheduled(fixedDelayString = "${ifood.polling-interval-ms:30000}")
    public void poll() {
        if (!"polling".equalsIgnoreCase(modo) || !authService.configurado()) {
            return;
        }
        try {
            JsonNode eventos = ifoodService.buscarEventos();
            if (eventos == null || !eventos.isArray()) {
                return;
            }

            List<String> ack = new ArrayList<>();
            for (JsonNode ev : eventos) {
                String eventId = ev.path("id").asText(null);
                String orderId = ev.path("correlationId").asText(null);
                if (orderId == null || orderId.isBlank()) {
                    if (eventId != null && !eventId.isBlank()) {
                        ack.add(eventId);
                    }
                    continue;
                }
                try {
                    JsonNode order = ifoodService.buscarPedido(orderId);
                    Optional<pedido> novo = ifoodService.aplicarPedido(order);
                    if (novo.isPresent() && "PLACED".equalsIgnoreCase(
                            order == null ? "" : order.path("status").asText(""))) {
                        ifoodService.confirmarPedido(orderId);
                    }
                    if (eventId != null && !eventId.isBlank()) {
                        ack.add(eventId);
                    }
                } catch (Exception e) {
                    log.error("Falha ao processar evento iFood (orderId={}): {}", orderId, e.getMessage());
                }
            }
            ifoodService.confirmarEventos(ack);
        } catch (Exception e) {
            log.error("Falha no polling da iFood: {}", e.getMessage());
        }
    }
}
