package PDV.PDV.service;

import PDV.PDV.model.Enum.formaPagamento;
import PDV.PDV.model.Enum.origemPedido;
import PDV.PDV.model.Enum.statusPedido;
import PDV.PDV.model.Enum.tipoLogistico;
import PDV.PDV.model.Enum.tipoPedido;
import PDV.PDV.model.ifoodVinculo;
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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Integração com a iFood (pedidos). Recebe pedidos via polling, converte em {@link pedido}
 * com origem IFOOD e reflete as mudanças de status do PDV de volta para a iFood.
 *
 * <p>Observação sobre a API: os nomes de campos/endpoints seguem a API Merchant v1 da iFood.
 * Como não há sandbox de fácil validação sem um merchant_id ativo, este serviço é tolerante
 * (usa {@code path()}, que não lança exceção) e registra o payload bruto em DEBUG para que
 * qualquer divergência seja ajustada no primeiro teste real.</p>
 */
@Service
public class IfoodService {

    private static final Logger log = LoggerFactory.getLogger(IfoodService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    // Timeout curto para chamadas síncronas disparadas pelo usuário (ex.: sincronizar
    // disponibilidade ao salvar produto), para nunca travar a UI se a iFood estiver fora do ar.
    private final RestTemplate restTemplateCurto = criarRestTemplateCurto();
    private final ObjectMapper objectMapper;
    private final IfoodAuthService authService;
    private final pedidoRepository pedidoRepo;
    private final itensPedidoRepository itensRepo;
    private final ifoodVinculoService vinculoService;
    private final configuracaoService configuracaoService;
    private final ImpressaoService impressaoService;
    private final produtoService produtoService;

    public IfoodService(ObjectMapper objectMapper, IfoodAuthService authService,
                        pedidoRepository pedidoRepo, itensPedidoRepository itensRepo,
                        ifoodVinculoService vinculoService, configuracaoService configuracaoService,
                        ImpressaoService impressaoService, produtoService produtoService) {
        this.objectMapper = objectMapper;
        this.authService = authService;
        this.pedidoRepo = pedidoRepo;
        this.itensRepo = itensRepo;
        this.vinculoService = vinculoService;
        this.configuracaoService = configuracaoService;
        this.impressaoService = impressaoService;
        this.produtoService = produtoService;
    }

    // ------------------------------------------------------------------
    // Eventos (polling)
    // ------------------------------------------------------------------

    public JsonNode buscarEventos() throws Exception {
        JsonNode resp = get("/v1.0/events:polling");
        if (resp == null) {
            return null;
        }
        // Algumas versões retornam {"events": [...]}; outras, a lista direta.
        if (resp.isObject() && resp.has("events")) {
            return resp.path("events");
        }
        return resp;
    }

    public void confirmarEventos(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        StringBuilder body = new StringBuilder("{\"events\":[");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                body.append(',');
            }
            body.append("{\"id\":\"").append(ids.get(i)).append("\"}");
        }
        body.append("]}");
        try {
            post("/v1.0/events/acknowledgment", body.toString());
        } catch (Exception e) {
            log.error("Falha ao confirmar recebimento dos eventos iFood: {}", e.getMessage());
        }
    }

    public JsonNode buscarPedido(String orderId) throws Exception {
        return get("/v1.0/orders/" + orderId);
    }

    // ------------------------------------------------------------------
    // Persistência do pedido
    // ------------------------------------------------------------------

    /** Converte o payload de um pedido iFood em pedido do PDV. Retorna o pedido criado (ou vazio se já existia). */
    @Transactional
    public Optional<pedido> aplicarPedido(JsonNode order) {
        if (order == null || order.isMissingNode() || order.isNull()) {
            return Optional.empty();
        }

        String ifoodPedidoId = order.path("id").asText(order.path("correlationId").asText(null));
        if (ifoodPedidoId == null || ifoodPedidoId.isBlank()) {
            log.warn("Pedido iFood sem id ignorado: {}", order.toString());
            return Optional.empty();
        }

        String statusIfood = normalizarStatus(order.path("status").asText("PLACED"));
        statusPedido statusPdv = mapearStatus(statusIfood);

        Optional<pedido> existente = pedidoRepo.findByIfoodPedidoId(ifoodPedidoId);
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
        novo.setOrigemPedido(origemPedido.IFOOD);
        novo.setIfoodPedidoId(ifoodPedidoId);
        novo.setIfoodReferencia(textoOuNull(order.path("displayId")));
        if (novo.getIfoodReferencia() == null) {
            novo.setIfoodReferencia(textoOuNull(order.path("reference")));
        }
        novo.setStatusPedido(statusPdv);
        novo.setDataHoraPedido(parseData(order.path("createdAt").asText(null)));
        if (statusPdv == statusPedido.PREPARANDO) {
            novo.setDataInicioPreparo(OffsetDateTime.now());
        }

        // Cliente (não tem cadastro no PDV).
        JsonNode customer = order.path("customer");
        novo.setClienteNomeExterno(textoOuNull(customer.path("name")));
        String telefone = textoOuNull(customer.path("phone").path("number"));
        if (telefone == null) {
            telefone = textoOuNull(customer.path("phone"));
        }
        novo.setClienteTelefoneExterno(telefone);

        boolean retirada = isRetirada(order);
        novo.setTipoPedido(retirada ? tipoPedido.RETIRADA : tipoPedido.DELIVERY);
        novo.setTipoLogistica(retirada ? tipoLogistico.PROPRIA : tipoLogistico.ENTREGA_IFOOD);

        JsonNode total = order.path("total");
        BigDecimal taxa = decimal(total.path("deliveryFee"));
        novo.setTaxaEntrega(taxa);
        BigDecimal orderAmount = decimal(total.path("orderAmount"));
        if (orderAmount == null || orderAmount.signum() == 0) {
            BigDecimal subTotal = decimal(total.path("subTotal"));
            BigDecimal beneficios = decimal(total.path("benefits"));
            orderAmount = (subTotal != null ? subTotal : BigDecimal.ZERO)
                    .add(taxa != null ? taxa : BigDecimal.ZERO)
                    .subtract(beneficios != null ? beneficios : BigDecimal.ZERO);
        }
        novo.setValorTotal(orderAmount);
        novo.setFormaPagamento(mapearPagamento(order.path("payments")));
        novo.setPago(true); // a iFood cobra na própria plataforma
        novo.setTempoPreparoMinutos(configuracaoService.obterTempoPreparoPadrao());

        novo.setObservacoes(montarObservacoes(order));

        pedido salvo = pedidoRepo.save(novo);

        List<itensPedido> itens = new ArrayList<>();
        JsonNode arr = order.path("items");
        if (arr.isArray()) {
            for (JsonNode item : arr) {
                itensPedido i = new itensPedido();
                i.setPedido(salvo);
                String itemId = item.path("id").asText(null);
                String nomeItem = textoOuNull(item.path("name"));
                i.setNome(nomeItem);
                int qtd = item.path("quantity").asInt(1);
                BigDecimal unit = decimal(item.path("unitPrice"));
                if (unit == null) {
                    unit = decimal(item.path("price"));
                }
                produtos produto = itemId != null ? vinculoService.resolverProduto(itemId).orElse(null) : null;
                // Sem vínculo explícito, tenta casar pelo nome do item com o catálogo do PDV.
                if (produto == null) {
                    produto = produtoService.buscarPorNomeNormalizado(nomeItem).orElse(null);
                }
                i.setProduto(produto);
                i.setQuantidade(qtd);
                i.setPrecoUnitario(unit != null ? unit : BigDecimal.ZERO);
                i.setSubtotal((unit != null ? unit : BigDecimal.ZERO).multiply(BigDecimal.valueOf(qtd)));
                i.setObservacao(montarItemObs(item));
                itens.add(i);
            }
        }
        itensRepo.saveAll(itens);
        salvo.setItens(itens);
        pedidoRepo.save(salvo);

        imprimirComanda(salvo);
        return Optional.of(salvo);
    }

    /** Confirma o pedido na iFood (aceite do lojista). Chamado apenas para pedidos recém-chegados em PLACED. */
    public void confirmarPedido(String orderId) {
        try {
            post("/v1.0/orders/" + orderId + "/confirm", "{}");
        } catch (Exception e) {
            log.error("Falha ao confirmar pedido iFood {}: {}", orderId, e.getMessage());
        }
    }

    /** Reflete uma mudança de status feita no PDV de volta para a iFood (melhor esforço). */
    public void refletirStatusNoIfood(pedido p, statusPedido novoStatus) {
        if (p.getOrigemPedido() != origemPedido.IFOOD) {
            return;
        }
        if (p.getIfoodPedidoId() == null || p.getIfoodPedidoId().isBlank() || !authService.configurado()) {
            return;
        }
        String id = p.getIfoodPedidoId();
        try {
            switch (novoStatus) {
                case PREPARANDO -> post("/v1.0/orders/" + id + "/confirm", "{}");
                case AGUARDANDO_ENTREGADOR -> post("/v1.0/orders/" + id + "/readyToPickup", "{}");
                case A_CAMINHO -> post("/v1.0/orders/" + id + "/dispatch", "{}");
                case CONCLUIDO -> post("/v1.0/orders/" + id + "/conclude", "{}");
                case CANCELADO -> post("/v1.0/orders/" + id + "/requestCancellation",
                        "{\"reason\":\"Cancelado pelo lojista\",\"cancellationCode\":\"702\"}");
                default -> { }
            }
        } catch (Exception e) {
            log.error("Falha ao refletir status {} na iFood (pedido {}): {}", novoStatus, id, e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Catálogo (disponibilidade)
    // ------------------------------------------------------------------

    /**
     * Sincroniza a disponibilidade de um produto do PDV com o item vinculado no catálogo
     * da iFood (melhor esforço). Retorna {@code true} se a sincronização foi enviada com
     * sucesso; {@code false} se não havia vínculo/configuração ou se a chamada falhou.
     */
    public boolean sincronizarDisponibilidade(produtos produto) {
        if (produto == null || produto.getId() == null || !authService.configurado()) {
            return false;
        }
        String merchantId = authService.getMerchantId();
        if (merchantId == null || merchantId.isBlank()) {
            return false;
        }
        Optional<ifoodVinculo> vinculo = vinculoService.buscarPorProduto(produto.getId());
        if (vinculo.isEmpty()) {
            return false; // produto não vinculado ao catálogo da iFood
        }
        boolean disponivel = Boolean.TRUE.equals(produto.getAtivo());
        String itemId = vinculo.get().getIfoodItemId();
        String body = "[{\"id\":\"" + itemId + "\",\"available\":" + disponivel + "}]";
        try {
            put("/catalog/v2.0/merchants/" + merchantId + "/availability", body);
            log.info("Disponibilidade do produto {} sincronizada com iFood (item {}): available={}",
                    produto.getNome(), itemId, disponivel);
            return true;
        } catch (Exception e) {
            log.error("Falha ao sincronizar disponibilidade do produto {} com iFood: {}",
                    produto.getId(), e.getMessage());
            return false;
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
            log.error("Falha ao imprimir comanda do pedido iFood {}: {}", p.getId(), e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // HTTP de baixo nível
    // ------------------------------------------------------------------

    private JsonNode get(String path) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authService.obterAccessToken());
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                authService.getBaseUrl() + path, HttpMethod.GET, req, String.class);
        if (resp.getStatusCode().is2xxSuccessful()) {
            String body = resp.getBody();
            if (body == null || body.isBlank()) {
                return null;
            }
            if (log.isDebugEnabled()) {
                log.debug("iFood GET {} -> {}", path, body);
            }
            return objectMapper.readTree(body);
        }
        log.warn("iFood GET {} -> HTTP {}", path, resp.getStatusCode());
        return null;
    }

    private void post(String path, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authService.obterAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                authService.getBaseUrl() + path, HttpMethod.POST, req, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("iFood POST " + path + " -> HTTP " + resp.getStatusCode() + ": " + resp.getBody());
        }
    }

    private static RestTemplate criarRestTemplateCurto() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return new RestTemplate(factory);
    }

    private void put(String path, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authService.obterAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplateCurto.exchange(
                authService.getBaseUrl() + path, HttpMethod.PUT, req, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("iFood PUT " + path + " -> HTTP " + resp.getStatusCode() + ": " + resp.getBody());
        }
    }

    // ------------------------------------------------------------------
    // Mapeamentos
    // ------------------------------------------------------------------

    private static String normalizarStatus(String status) {
        if (status == null) {
            return "PLACED";
        }
        return status.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
    }

    private statusPedido mapearStatus(String statusIfood) {
        return switch (statusIfood) {
            case "CANCELLED", "CANCELED" -> statusPedido.CANCELADO;
            case "CONCLUDED", "DELIVERED" -> statusPedido.CONCLUIDO;
            case "DISPATCHED", "ONTRAVEL", "OUTFORDELIVERY" -> statusPedido.A_CAMINHO;
            case "READYTOPICKUP", "READY" -> statusPedido.AGUARDANDO_ENTREGADOR;
            default -> statusPedido.PREPARANDO; // PLACED, CONFIRMED, PREPARATION_STARTED
        };
    }

    private formaPagamento mapearPagamento(JsonNode payments) {
        if (payments == null || payments.isMissingNode() || payments.isNull()) {
            return formaPagamento.APP;
        }
        JsonNode methods = payments.path("methods");
        if (methods.isArray()) {
            for (JsonNode m : methods) {
                String metodo = m.path("method").asText("").toUpperCase(Locale.ROOT);
                if (metodo.contains("PIX")) {
                    return formaPagamento.PIX;
                }
                if (metodo.contains("CASH") || metodo.contains("DINHEIRO")) {
                    return formaPagamento.DINHEIRO;
                }
            }
        }
        return formaPagamento.APP;
    }

    private boolean isRetirada(JsonNode order) {
        String timing = order.path("orderTiming").asText(order.path("orderType").asText(""));
        if (!timing.isBlank()) {
            String t = timing.toUpperCase(Locale.ROOT);
            return t.contains("PICKUP") || t.contains("TAKEOUT");
        }
        JsonNode addr = order.path("delivery").path("deliveryAddress");
        return addr.isMissingNode() || addr.isNull();
    }

    private String montarObservacoes(JsonNode order) {
        StringBuilder sb = new StringBuilder();
        appendObs(sb, order.path("observations"));
        String endereco = extrairEndereco(order);
        if (endereco != null) {
            sb.append(endereco).append(" | ");
        }
        return sb.length() == 0 ? null : sb.toString().trim();
    }

    private String montarItemObs(JsonNode item) {
        StringBuilder sb = new StringBuilder();
        appendObs(sb, item.path("observations"));
        JsonNode options = item.path("options");
        if (options.isArray()) {
            for (JsonNode opt : options) {
                String nome = opt.path("name").asText(null);
                if (nome == null) {
                    continue;
                }
                String extra = opt.path("addition").asText(null);
                JsonNode sub = opt.path("options");
                String valor = (sub.isArray() && sub.size() > 0) ? sub.get(0).path("name").asText(null) : null;
                sb.append(nome);
                if (extra != null && !extra.isBlank()) {
                    sb.append(" +").append(extra);
                } else if (valor != null) {
                    sb.append(": ").append(valor);
                }
                sb.append("; ");
            }
        }
        return sb.length() == 0 ? null : sb.toString().trim();
    }

    private String extrairEndereco(JsonNode order) {
        JsonNode addr = order.path("delivery").path("deliveryAddress");
        if (addr.isMissingNode() || addr.isNull()) {
            return null;
        }
        String rua = textoOuNull(addr.path("streetName"));
        String num = textoOuNull(addr.path("streetNumber"));
        String bairro = textoOuNull(addr.path("neighborhood"));
        String cidade = textoOuNull(addr.path("city"));
        String ref = textoOuNull(addr.path("reference"));
        StringBuilder sb = new StringBuilder();
        if (rua != null) {
            sb.append(rua);
            if (num != null) {
                sb.append(", ").append(num);
            }
        }
        if (bairro != null) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(bairro);
        }
        if (cidade != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(cidade);
        }
        if (ref != null) {
            if (sb.length() > 0) sb.append(" (ref: ").append(ref).append(")");
            else sb.append("ref: ").append(ref);
        }
        return sb.length() == 0 ? null : "Entrega: " + sb;
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

    private static OffsetDateTime parseData(String s) {
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
}
