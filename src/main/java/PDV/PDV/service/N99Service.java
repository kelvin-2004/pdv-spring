package PDV.PDV.service;

import PDV.PDV.model.Enum.formaPagamento;
import PDV.PDV.model.Enum.origemPedido;
import PDV.PDV.model.Enum.statusPedido;
import PDV.PDV.model.Enum.tipoLogistico;
import PDV.PDV.model.Enum.tipoPedido;
import PDV.PDV.model.itensPedido;
import PDV.PDV.model.pedido;
import PDV.PDV.model.produtos;
import PDV.PDV.repository.itensPedidoRepository;
import PDV.PDV.repository.pedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Integração com a 99Food (99Compras/Didi Food). Recebe pedidos via polling, converte
 * em {@link pedido} com origem NOVENTA_E_NOVE e reflete as mudanças de status do PDV
 * de volta para a 99.
 *
 * <p>Observação sobre a API: os endpoints usam o {@code auth_token} como parâmetro
 * (query para GET, corpo para POST), e a documentação não detalha o schema de resposta
 * do pedido. Por isso este serviço é tolerante (usa {@code path()} e testa vários nomes
 * de campo) e registra o payload bruto em DEBUG, para ajustar no primeiro teste real —
 * mesmo padrão da integração iFood.</p>
 *
 * <p>Valores monetários: a doc de áreas de entrega cita "preços em centavos"; o schema
 * do pedido pode vir em centavos ou em reais. Aqui assumimos reais e deixamos o ajuste
 * para o primeiro teste real (log em DEBUG).</p>
 */
@Service
public class N99Service {

    private static final Logger log = LoggerFactory.getLogger(N99Service.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final N99AuthService authService;
    private final pedidoRepository pedidoRepo;
    private final itensPedidoRepository itensRepo;
    private final configuracaoService configuracaoService;
    private final ImpressaoService impressaoService;
    private final produtoService produtoService;

    public N99Service(ObjectMapper objectMapper, N99AuthService authService,
                      pedidoRepository pedidoRepo, itensPedidoRepository itensRepo,
                      configuracaoService configuracaoService, ImpressaoService impressaoService,
                      produtoService produtoService) {
        this.objectMapper = objectMapper;
        this.authService = authService;
        this.pedidoRepo = pedidoRepo;
        this.itensRepo = itensRepo;
        this.configuracaoService = configuracaoService;
        this.impressaoService = impressaoService;
        this.produtoService = produtoService;
    }

    // ------------------------------------------------------------------
    // Eventos (polling)
    // ------------------------------------------------------------------

    /** Retorna o array de eventos pendentes (ou {@code null} se nada a processar). */
    public JsonNode buscarEventos() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("event_type", List.of("CREATED", "CONFIRMED", "READY", "CONCLUDED", "CANCELLED"));
        JsonNode resp = post("/v1/order/events/list", body);
        if (resp == null) {
            return null;
        }
        // Tolerante a várias formas de resposta: {polling_events: [...]}, {events: [...]},
        // {data: [...]}, {data: {events: [...]}} ou lista direta.
        JsonNode eventos = resp.path("polling_events");
        if (!eventos.isArray()) {
            eventos = resp.path("events");
        }
        if (!eventos.isArray()) {
            eventos = resp.path("data").path("events");
        }
        if (!eventos.isArray() && resp.path("data").isArray()) {
            eventos = resp.path("data");
        }
        if (eventos.isArray()) {
            return eventos;
        }
        return resp.isArray() ? resp : null;
    }

    /** Confirma (ack) a lista de eventos para retirá-los da fila de polling. */
    public void confirmarEventos(JsonNode eventos) {
        if (eventos == null || !eventos.isArray() || eventos.isEmpty()) {
            return;
        }
        List<Map<String, String>> polling = new ArrayList<>();
        for (JsonNode ev : eventos) {
            String eventId = eventIdDoEvento(ev);
            String orderId = orderIdDoEvento(ev);
            if (eventId == null && orderId == null) {
                continue;
            }
            Map<String, String> m = new LinkedHashMap<>();
            m.put("event_id", eventId == null ? "" : eventId);
            m.put("order_id", orderId == null ? "" : orderId);
            polling.add(m);
        }
        if (polling.isEmpty()) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("polling_events", polling);
        try {
            post("/v1/order/events/ack", body);
        } catch (Exception e) {
            log.error("Falha ao confirmar recebimento dos eventos 99: {}", e.getMessage());
        }
    }

    public String orderIdDoEvento(JsonNode ev) {
        String id = textoOuNull(ev.path("order_id"));
        if (id == null) {
            id = textoOuNull(ev.path("orderId"));
        }
        if (id == null) {
            id = textoOuNull(ev.path("order").path("order_id"));
        }
        return id;
    }

    public String eventIdDoEvento(JsonNode ev) {
        String id = textoOuNull(ev.path("event_id"));
        if (id == null) {
            id = textoOuNull(ev.path("eventId"));
        }
        if (id == null) {
            id = textoOuNull(ev.path("id"));
        }
        return id;
    }

    public JsonNode buscarPedido(String orderId) {
        return get("/v1/order/order/detail", "order_id", orderId);
    }

    // ------------------------------------------------------------------
    // Persistência do pedido
    // ------------------------------------------------------------------

    /** Converte o payload de um pedido 99Food em pedido do PDV. Retorna o pedido criado (ou vazio se já existia). */
    @Transactional
    public Optional<pedido> aplicarPedido(JsonNode order) {
        if (order == null || order.isMissingNode() || order.isNull()) {
            return Optional.empty();
        }

        String n99PedidoId = textoOuNull(order.path("order_id"));
        if (n99PedidoId == null) {
            n99PedidoId = textoOuNull(order.path("id"));
        }
        if (n99PedidoId == null || n99PedidoId.isBlank()) {
            log.warn("Pedido 99 sem order_id ignorado: {}", order.toString());
            return Optional.empty();
        }

        String status99 = normalizarStatus(textoOuNull(order.path("status")));
        statusPedido statusPdv = mapearStatus(status99);

        Optional<pedido> existente = pedidoRepo.findByN99PedidoId(n99PedidoId);
        if (existente.isPresent()) {
            pedido p = existente.get();
            if (p.getStatusPedido() != statusPdv) {
                p.setStatusPedido(statusPdv);
                if (statusPdv == statusPedido.PREPARANDO && p.getDataInicioPreparo() == null) {
                    p.setDataInicioPreparo(OffsetDateTime.now());
                }
                pedidoRepo.save(p);
            }
            return Optional.empty();
        }

        pedido novo = new pedido();
        novo.setOrigemPedido(origemPedido.NOVENTA_E_NOVE);
        novo.setN99PedidoId(n99PedidoId);
        novo.setN99Referencia(extrairReferencia(order));
        novo.setStatusPedido(statusPdv);
        novo.setDataHoraPedido(parseData(extrairTimestamp(order)));
        if (statusPdv == statusPedido.PREPARANDO) {
            novo.setDataInicioPreparo(OffsetDateTime.now());
        }

        // Cliente (não tem cadastro no PDV).
        JsonNode customer = order.path("customer");
        JsonNode receiver = order.path("receiver");
        novo.setClienteNomeExterno(textoOuNull(receiver.path("name")) != null
                ? textoOuNull(receiver.path("name"))
                : textoOuNull(customer.path("name")));
        String telefone = textoOuNull(receiver.path("phone"));
        if (telefone == null) {
            telefone = textoOuNull(customer.path("phone"));
        }
        if (telefone == null) {
            telefone = textoOuNull(order.path("customer_phone"));
        }
        novo.setClienteTelefoneExterno(telefone);

        boolean retirada = isRetirada(order);
        novo.setTipoPedido(retirada ? tipoPedido.RETIRADA : tipoPedido.DELIVERY);
        if (retirada) {
            novo.setTipoLogistica(tipoLogistico.PROPRIA);
        } else if (isSelfDelivery(order)) {
            novo.setTipoLogistica(tipoLogistico.PROPRIA);
        } else {
            novo.setTipoLogistica(tipoLogistico.ENTREGA_99);
            novo.setDataPrazoEntrega(extrairPrazoEntrega(order));
        }

        BigDecimal taxa = decimal(order.path("delivery_fee"));
        if (taxa == null) {
            taxa = decimal(order.path("delivery_price"));
        }
        novo.setTaxaEntrega(taxa != null ? taxa : BigDecimal.ZERO);

        BigDecimal total = decimal(order.path("total_price"));
        if (total == null) {
            total = decimal(order.path("order_amount"));
        }
        if (total == null) {
            total = decimal(order.path("pay_amount"));
        }
        if (total == null) {
            total = decimal(order.path("actual_total"));
        }
        novo.setValorTotal(total);
        novo.setFormaPagamento(mapearPagamento(order));
        novo.setPago(true); // a 99 cobra na própria plataforma
        novo.setTempoPreparoMinutos(configuracaoService.obterTempoPreparoPadrao());

        novo.setObservacoes(montarObservacoes(order));

        pedido salvo = pedidoRepo.save(novo);

        List<itensPedido> itens = new ArrayList<>();
        JsonNode arr = order.path("items");
        if (!arr.isArray()) {
            arr = order.path("item_list");
        }
        if (!arr.isArray()) {
            arr = order.path("goods_list");
        }
        if (arr.isArray()) {
            for (JsonNode item : arr) {
                itensPedido i = new itensPedido();
                i.setPedido(salvo);
                String nomeItem = nomeItem(item);
                i.setNome(nomeItem);
                // Tenta vincular o item ao produto do PDV pelo nome; se não casar, fica sem
                // vínculo (nome preservado no campo próprio para exibição na comanda).
                i.setProduto(produtoService.buscarPorNomeNormalizado(nomeItem).orElse(null));
                int qtd = item.path("quantity").asInt(item.path("count").asInt(item.path("num").asInt(1)));
                BigDecimal unit = decimal(item.path("price"));
                if (unit == null) {
                    unit = decimal(item.path("unit_price"));
                }
                if (unit == null) {
                    unit = decimal(item.path("item_price"));
                }
                BigDecimal preco = unit != null ? unit : BigDecimal.ZERO;
                i.setQuantidade(qtd);
                i.setPrecoUnitario(preco);
                i.setSubtotal(preco.multiply(BigDecimal.valueOf(qtd)));
                i.setObservacao(observacaoItem(item));
                itens.add(i);
            }
        }
        itensRepo.saveAll(itens);
        salvo.setItens(itens);
        pedidoRepo.save(salvo);

        imprimirComanda(salvo);
        return Optional.of(salvo);
    }

    /** Confirma o recebimento do pedido na 99 (obrigatório em até ~5 min após o pedido). */
    public void confirmarPedido(String orderId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("order_id", orderId);
        try {
            post("/v1/order/order/confirm", body);
        } catch (Exception e) {
            log.error("Falha ao confirmar pedido 99 {}: {}", orderId, e.getMessage());
        }
    }

    /**
     * Garante que a loja aceite confirmação via OpenAPI, para que os pedidos cheguem
     * ao PDV (e não fiquem só no app da 99/DiDi). Idempotente e melhor esforço.
     *
     * <p>{@code order_confirm_method = 1} (B-App &amp; OpenAPI): mantém o app como
     * alternativa e habilita a confirmação pelo sistema integrado.</p>
     *
     * @return {@code true} se a chamada foi aceita (permite marcar como "feito").
     */
    public boolean garantirConfirmMethodOpenapi() {
        if (!authService.configurado()) {
            return false;
        }
        try {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("order_confirm_method", 1);
            post("/v1/shop/shop/setconfirmmethod", b);
            log.info("99: método de confirmação ajustado para B-App & OpenAPI (1).");
            return true;
        } catch (Exception e) {
            log.warn("99: não foi possível ajustar o método de confirmação ({}).", e.getMessage());
            return false;
        }
    }

    /** Reflete uma mudança de status feita no PDV de volta para a 99 (melhor esforço). */
    public void refletirStatusNa99(pedido p, statusPedido novoStatus) {
        if (p.getOrigemPedido() != origemPedido.NOVENTA_E_NOVE) {
            return;
        }
        if (p.getN99PedidoId() == null || p.getN99PedidoId().isBlank() || !authService.configurado()) {
            return;
        }
        String id = p.getN99PedidoId();
        try {
            switch (novoStatus) {
                case PREPARANDO -> {
                    Map<String, Object> b = new LinkedHashMap<>();
                    b.put("order_id", id);
                    post("/v1/order/order/confirm", b);
                }
                case AGUARDANDO_ENTREGADOR ->
                    get("/v1/order/order/ready", "order_id", id);
                case CANCELADO -> {
                    Map<String, Object> b = new LinkedHashMap<>();
                    b.put("order_id", id);
                    b.put("reason_id", 1080); // 1080 = outros motivos
                    b.put("reason", "Cancelado pelo lojista");
                    post("/v1/order/order/cancel", b);
                }
                default -> { }
            }
        } catch (Exception e) {
            log.error("Falha ao refletir status {} na 99 (pedido {}): {}", novoStatus, id, e.getMessage());
        }
    }

    private void imprimirComanda(pedido p) {
        if (impressaoService.isModoPonte()) {
            return;
        }
        try {
            if (pedidoRepo.marcarComoImpresso(p.getId()) == 1) {
                try {
                    impressaoService.imprimirPedido(p.getId());
                } catch (Exception e) {
                    pedidoRepo.reverterImpressao(p.getId());
                }
            }
        } catch (Exception e) {
            log.error("Falha ao imprimir comanda do pedido 99 {}: {}", p.getId(), e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // HTTP de baixo nível
    // ------------------------------------------------------------------

    private JsonNode get(String path, String... queryKv) {
        String token = authService.obterAuthToken();
        StringBuilder url = new StringBuilder(authService.getBaseUrl()).append(path)
                .append("?auth_token=").append(enc(token));
        for (int i = 0; i + 1 < queryKv.length; i += 2) {
            url.append('&').append(enc(queryKv[i])).append('=').append(enc(queryKv[i + 1]));
        }
        ResponseEntity<String> resp = restTemplate.exchange(url.toString(), HttpMethod.GET, HttpEntity.EMPTY, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            log.warn("99 GET {} -> HTTP {}", path, resp.getStatusCode());
            return null;
        }
        String body = resp.getBody();
        if (body == null || body.isBlank()) {
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("99 GET {} -> {}", path, body);
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            log.warn("99 GET {} -> resposta não-JSON: {}", path, e.getMessage());
            return null;
        }
    }

    private JsonNode post(String path, Map<String, Object> body) {
        String token = authService.obterAuthToken();
        Map<String, Object> payload = new LinkedHashMap<>(body);
        payload.put("auth_token", token);
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar corpo da 99.", e);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(json, headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                authService.getBaseUrl() + path, HttpMethod.POST, req, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("99 POST " + path + " -> HTTP " + resp.getStatusCode() + ": " + resp.getBody());
        }
        String respBody = resp.getBody();
        if (respBody == null || respBody.isBlank()) {
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("99 POST {} -> {}", path, respBody);
        }
        try {
            return objectMapper.readTree(respBody);
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Mapeamentos
    // ------------------------------------------------------------------

    private static String normalizarStatus(String status) {
        if (status == null) {
            return "CREATED";
        }
        return status.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
    }

    private statusPedido mapearStatus(String status99) {
        return switch (status99) {
            case "CANCELLED", "CANCELED", "REFUNDED", "REJECTED", "CLOSED" -> statusPedido.CANCELADO;
            case "CONCLUDED", "DELIVERED", "FINISHED" -> statusPedido.CONCLUIDO;
            case "DISPATCHED", "DELIVERING", "ONTHEWAY", "OUTFORDELIVERY", "PICKEDUP" -> statusPedido.A_CAMINHO;
            case "READY" -> statusPedido.AGUARDANDO_ENTREGADOR;
            default -> statusPedido.PREPARANDO; // CREATED, CONFIRMED, PREPARING
        };
    }

    private formaPagamento mapearPagamento(JsonNode order) {
        for (String[] keys : new String[][] {
                {"pay_type"}, {"payment_type"}, {"pay_method"},
                {"payment", "type"}, {"payment", "method"} }) {
            String v = valorEm(order, keys);
            if (v == null) {
                continue;
            }
            String up = v.toUpperCase(Locale.ROOT);
            if (up.contains("PIX")) {
                return formaPagamento.PIX;
            }
            if (up.contains("CARD") || up.contains("CARTAO") || up.contains("CREDIT") || up.contains("DEBIT")) {
                return formaPagamento.CARTAO;
            }
            if (up.contains("CASH") || up.contains("DINHEIRO") || up.contains("MONEY")) {
                return formaPagamento.DINHEIRO;
            }
        }
        return formaPagamento.APP;
    }

    private boolean isRetirada(JsonNode order) {
        String tipo = primeiroTexto(order, "delivery_type", "deliver_type", "order_type");
        if (tipo != null) {
            String t = tipo.toUpperCase(Locale.ROOT);
            return t.contains("PICKUP") || t.contains("TAKEOUT")
                    || t.contains("RETIRADA") || t.contains("SELF_PICKUP");
        }
        return !temEndereco(order);
    }

    private boolean isSelfDelivery(JsonNode order) {
        if (order.path("is_self_delivery").asBoolean(false)) {
            return true;
        }
        if (order.path("self_delivery").asBoolean(false)) {
            return true;
        }
        String tipo = primeiroTexto(order, "delivery_type", "deliver_type", "delivery_mode");
        if (tipo != null) {
            String t = tipo.toUpperCase(Locale.ROOT);
            return t.contains("SELF") || t.contains("PROPRI");
        }
        return false;
    }

    private boolean temEndereco(JsonNode order) {
        return order.hasNonNull("receiver_address") || order.hasNonNull("delivery_address")
                || order.hasNonNull("receiver") || order.hasNonNull("address");
    }

    private OffsetDateTime extrairPrazoEntrega(JsonNode order) {
        String[][] candidatos = {
                {"expected_delivery_time"}, {"promise_delivery_time"}, {"promise_time"},
                {"delivery_time"}, {"expected_time"}, {"latest_ready_time"},
                {"delivery", "expected_time"}, {"delivery", "eta"}, {"promise", "delivery_time"},
                {"eta"}, {"deliver_eta"}, {"promise_deliver_time"},
                {"delivery_eta"}, {"avg_delivery_eta"}, {"promise_delivery_eta"} };
        for (String[] keys : candidatos) {
            JsonNode n = nodeEm(order, keys);
            OffsetDateTime d = parseData(n);
            if (d != null) {
                return d;
            }
        }
        return null;
    }

    private JsonNode extrairTimestamp(JsonNode order) {
        String[][] candidatos = {
                {"create_time"}, {"created_at"}, {"created_time"}, {"order_time"},
                {"created"} };
        for (String[] keys : candidatos) {
            JsonNode n = nodeEm(order, keys);
            if (n != null && !n.isMissingNode() && !n.isNull()) {
                return n;
            }
        }
        return null;
    }

    private String extrairReferencia(JsonNode order) {
        String[] candidatos = {"order_no", "order_number", "display_id", "short_id",
                "serial_number", "order_sequence"};
        for (String k : candidatos) {
            String v = textoOuNull(order.path(k));
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private String montarObservacoes(JsonNode order) {
        StringBuilder sb = new StringBuilder();
        appendObs(sb, order.path("remark"));
        appendObs(sb, order.path("note"));
        appendObs(sb, order.path("customer_note"));
        String endereco = extrairEndereco(order);
        if (endereco != null) {
            sb.append(endereco).append(" | ");
        }
        return sb.length() == 0 ? null : sb.toString().trim();
    }

    private String nomeItem(JsonNode item) {
        String nome = textoOuNull(item.path("item_name"));
        if (nome == null) {
            nome = textoOuNull(item.path("goods_name"));
        }
        if (nome == null) {
            nome = textoOuNull(item.path("name"));
        }
        if (nome == null) {
            nome = textoOuNull(item.path("product_name"));
        }
        return nome;
    }

    /** Observações do item (opções/remark), sem o nome — este agora fica no campo próprio. */
    private String observacaoItem(JsonNode item) {
        StringBuilder sb = new StringBuilder();
        appendObs(sb, item.path("options"));
        String obs = textoOuNull(item.path("remark"));
        if (obs == null) {
            obs = textoOuNull(item.path("note"));
        }
        if (obs != null) {
            sb.append(obs);
        }
        return sb.length() == 0 ? null : sb.toString().trim();
    }

    private String extrairEndereco(JsonNode order) {
        JsonNode addr = order.path("receiver_address");
        if (addr.isMissingNode() || addr.isNull()) {
            addr = order.path("delivery_address");
        }
        if (addr.isMissingNode() || addr.isNull()) {
            addr = order.path("receiver");
        }
        if (addr.isMissingNode() || addr.isNull()) {
            return null;
        }
        String rua = textoOuNull(addr.path("address"));
        if (rua == null) {
            rua = textoOuNull(addr.path("street"));
        }
        if (rua == null) {
            rua = textoOuNull(addr.path("detail_address"));
        }
        String bairro = textoOuNull(addr.path("district"));
        String cidade = textoOuNull(addr.path("city"));
        StringBuilder sb = new StringBuilder();
        if (rua != null) {
            sb.append(rua);
        }
        if (bairro != null) {
            if (sb.length() > 0) {
                sb.append(" - ");
            }
            sb.append(bairro);
        }
        if (cidade != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(cidade);
        }
        return sb.length() == 0 ? null : "Entrega: " + sb;
    }

    // ------------------------------------------------------------------
    // Helpers de parsing tolerante
    // ------------------------------------------------------------------

    private static String valorEm(JsonNode root, String[] keys) {
        JsonNode n = root;
        for (String k : keys) {
            if (n == null || n.isMissingNode() || n.isNull()) {
                return null;
            }
            n = n.path(k);
        }
        return textoOuNull(n);
    }

    private static JsonNode nodeEm(JsonNode root, String[] keys) {
        JsonNode n = root;
        for (String k : keys) {
            if (n == null || n.isMissingNode() || n.isNull()) {
                return null;
            }
            n = n.path(k);
        }
        return (n == null || n.isMissingNode() || n.isNull()) ? null : n;
    }

    private static String primeiroTexto(JsonNode root, String... keys) {
        for (String k : keys) {
            String v = textoOuNull(root.path(k));
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static void appendObs(StringBuilder sb, JsonNode n) {
        String s = textoOuNull(n);
        if (s != null) {
            sb.append(s).append(" | ");
        }
    }

    private static String textoOuNull(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) {
            return null;
        }
        String s = n.asText(null);
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static BigDecimal decimal(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) {
            return null;
        }
        if (n.isNumber()) {
            return n.decimalValue();
        }
        String s = n.asText(null);
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Aceita timestamp em epoch (segundos ou ms) ou string ISO. */
    private static OffsetDateTime parseData(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) {
            return OffsetDateTime.now();
        }
        if (n.isNumber()) {
            long v = n.asLong();
            if (v > 10_000_000_000L) {
                return Instant.ofEpochMilli(v).atOffset(ZoneOffset.UTC);
            }
            return Instant.ofEpochSecond(v).atOffset(ZoneOffset.UTC);
        }
        String s = n.asText(null);
        if (s == null || s.isBlank()) {
            return OffsetDateTime.now();
        }
        try {
            return OffsetDateTime.parse(s);
        } catch (Exception e) {
            try {
                return Instant.parse(s).atOffset(ZoneOffset.UTC);
            } catch (Exception e2) {
                return OffsetDateTime.now();
            }
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
