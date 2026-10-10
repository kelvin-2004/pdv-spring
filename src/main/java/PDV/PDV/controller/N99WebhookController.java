package PDV.PDV.controller;

import PDV.PDV.model.Enum.statusPedido;
import PDV.PDV.model.pedido;
import PDV.PDV.service.N99Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

/**
 * Recebe os pedidos da 99Food por <em>push</em> (webhook). A 99 envia a notificação
 * para {@code /webhook/99} quando um pedido é criado/confirmado/etc.; aqui convertemos
 * em {@link pedido} usando o mesmo {@link N99Service} do polling, sem depender do
 * agendador.
 *
 * <p>O endpoint é <em>best effort</em> e idempotente: responde sempre {@code 200 OK}
 * para a 99 parar de contabilizar falha (a plataforma registra 0% de sucesso quando
 * recebe 3xx/4xx) e nunca deixa a exceção subir, já que um push rejeitado é reenviado
 * desnecessariamente.</p>
 */
@RestController
@RequestMapping("/webhook")
public class N99WebhookController {

    private static final Logger log = LoggerFactory.getLogger(N99WebhookController.class);

    private final N99Service n99Service;
    private final ObjectMapper objectMapper;

    public N99WebhookController(N99Service n99Service, ObjectMapper objectMapper) {
        this.n99Service = n99Service;
        this.objectMapper = objectMapper;
    }

    /** Ping/verificação de disponibilidade que a 99 pode disparar antes do push. */
    @GetMapping("/99")
    public ResponseEntity<Void> ping() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/99")
    public ResponseEntity<Void> receber(@RequestBody(required = false) String body) {
        if (body == null || body.isBlank()) {
            return ResponseEntity.ok().build();
        }
        try {
            log.info("Webhook 99 recebido: {}", body);
            processar(objectMapper.readTree(body));
        } catch (Exception e) {
            log.error("Falha ao processar webhook 99: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok().build();
    }

    private void processar(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return;
        }

        // O push pode ser o pedido inteiro, um evento ({event_type, order_id}) ou um
        // envelope ({data: {...}}, {order: {...}}). Extrai primeiro o order_id e depois
        // tenta localizar o nó com os detalhes do pedido.
        String orderId = n99Service.orderIdDoEvento(root);
        JsonNode order = extrairPedido(root);
        if (orderId == null && order != null) {
            orderId = n99Service.orderIdDoEvento(order);
        }
        if (orderId == null || orderId.isBlank()) {
            log.warn("Webhook 99 sem order_id identificável: {}", root);
            return;
        }

        // Se o push não trouxer os detalhes do pedido, busca o pedido completo na API.
        if (order == null || !temDetalhesDePedido(order)) {
            order = n99Service.buscarPedido(orderId);
        }

        Optional<pedido> novo = n99Service.aplicarPedido(order);
        // Confirma o recebimento na 99 para pedido recém-criado e ainda ativo (status
        // numérico no formato real; usamos o status mapeado em vez de comparar texto).
        if (novo.isPresent() && novo.get().getStatusPedido() == statusPedido.PREPARANDO) {
            n99Service.confirmarPedido(orderId);
        }
    }

    /** Localiza o nó com os dados completos do pedido (ou {@code null} se o push só traz o evento). */
    private JsonNode extrairPedido(JsonNode root) {
        if (temDetalhesDePedido(root)) {
            return root;
        }
        for (String chave : new String[] {"order", "order_detail", "order_data"}) {
            JsonNode n = root.path(chave);
            if (n.isObject()) {
                return n;
            }
        }
        JsonNode data = root.path("data");
        if (data.isObject()) {
            if (temDetalhesDePedido(data)) {
                return data;
            }
            JsonNode interno = data.path("order");
            if (interno.isObject()) {
                return interno;
            }
        }
        return null;
    }

    /** Indica se o nó parece ser um pedido completo (e não apenas uma notificação de evento). */
    private boolean temDetalhesDePedido(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull() || !n.isObject()) {
            return false;
        }
        return n.has("order_items") || n.has("items") || n.has("item_list") || n.has("goods_list")
                || n.has("price") || n.has("receive_address") || n.has("receiver")
                || n.has("total_price") || n.has("order_amount") || n.has("pay_amount")
                || n.has("actual_total") || n.has("customer") || n.has("create_time");
    }
}
