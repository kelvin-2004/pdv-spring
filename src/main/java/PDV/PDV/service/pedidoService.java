package PDV.PDV.service;

import PDV.PDV.model.Enum.formaPagamento;
import PDV.PDV.model.Enum.statusPedido;
import PDV.PDV.model.clientes;
import PDV.PDV.model.itensPedido;
import PDV.PDV.model.pedido;
import PDV.PDV.model.produtos;
import PDV.PDV.repository.itensPedidoRepository;
import PDV.PDV.repository.pedidoRepository;
import PDV.PDV.repository.produtoRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class pedidoService {

    @Autowired
    private pedidoRepository pedidoRepo;

    @Autowired
    private produtoRepository produtoRepo;

    @Autowired
    private itensPedidoRepository itensPedidoRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ImpressaoService impressaoService;

    @Autowired
    private configuracaoService configuracaoService;

    public pedido novoPedido(pedido novoPedido) {
        novoPedido.setDataHoraPedido(OffsetDateTime.now());
        novoPedido.setStatusPedido(statusPedido.PREPARANDO);
        novoPedido.setDataInicioPreparo(OffsetDateTime.now());
        if (novoPedido.getTempoPreparoMinutos() == null) {
            novoPedido.setTempoPreparoMinutos(configuracaoService.obterTempoPreparoPadrao());
        }

        if (novoPedido.getCliente() != null) {
            long totalPedidosAnteriores = pedidoRepo.countByCliente(novoPedido.getCliente());
            int proximoNumero = (int) totalPedidosAnteriores + 1;
            novoPedido.setNumeroPedidoCliente(proximoNumero);
        }

        return pedidoRepo.save(novoPedido);
    }

    @Transactional
    public pedido novoPedidoComItens(pedido novoPedido, String carrinhoJson) {
        if (carrinhoJson == null || carrinhoJson.isBlank()) {
            throw new IllegalArgumentException("O carrinho do pedido não pode estar vazio");
        }

        novoPedido.setDataHoraPedido(OffsetDateTime.now());
        if (novoPedido.getStatusPedido() == null) {
            novoPedido.setStatusPedido(statusPedido.PREPARANDO);
        }
        if (novoPedido.getStatusPedido() == statusPedido.PREPARANDO
                && novoPedido.getDataInicioPreparo() == null) {
            novoPedido.setDataInicioPreparo(OffsetDateTime.now());
        }
        if (novoPedido.getTempoPreparoMinutos() == null) {
            novoPedido.setTempoPreparoMinutos(configuracaoService.obterTempoPreparoPadrao());
        }
        if (novoPedido.getCliente() != null) {
            long totalPedidosAnteriores = pedidoRepo.countByCliente(novoPedido.getCliente());
            novoPedido.setNumeroPedidoCliente((int) totalPedidosAnteriores + 1);
        }

        pedido pedidoSalvo = pedidoRepo.save(novoPedido);
        BigDecimal subtotal = BigDecimal.ZERO;
        List<itensPedido> itens = new ArrayList<>();

        try {
            JsonNode carrinho = objectMapper.readTree(carrinhoJson);
            if (!carrinho.isArray() || carrinho.isEmpty()) {
                throw new IllegalArgumentException("O carrinho do pedido não pode estar vazio");
            }
            for (JsonNode itemNode : carrinho) {
                long produtoId = itemNode.path("id").asLong(0);
                int quantidade = itemNode.path("quantidade").asInt(0);
                if (produtoId <= 0 || quantidade <= 0 || quantidade > 99) {
                    throw new IllegalArgumentException("Item de pedido inválido");
                }
                produtos produto = produtoRepo.findById(produtoId)
                        .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + produtoId));
                BigDecimal subtotalItem = produto.getPreco().multiply(BigDecimal.valueOf(quantidade));

                itensPedido item = new itensPedido();
                item.setPedido(pedidoSalvo);
                item.setProduto(produto);
                item.setQuantidade(quantidade);
                item.setPrecoUnitario(produto.getPreco());
                item.setSubtotal(subtotalItem);
                item.setObservacao(itemNode.hasNonNull("observacao")
                    ? itemNode.get("observacao").asText()
                    : null);
                itens.add(item);
                subtotal = subtotal.add(subtotalItem);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Não foi possível processar os itens do pedido", e);
        }

        pedidoSalvo.setItens(itens);
        BigDecimal desconto = Optional.ofNullable(pedidoSalvo.getDesconto()).orElse(BigDecimal.ZERO);
        pedidoSalvo.setValorTotal(subtotal.add(Optional.ofNullable(pedidoSalvo.getTaxaEntrega()).orElse(BigDecimal.ZERO)).subtract(desconto));
        itensPedidoRepo.saveAll(itens);
        return pedidoRepo.save(pedidoSalvo);
    }

    @Transactional
    public pedido editarPedido(Long id, formaPagamento novaForma, String carrinhoJson) {
        if (carrinhoJson == null || carrinhoJson.isBlank()) {
            throw new IllegalArgumentException("O pedido precisa ter pelo menos um item");
        }
        pedido p = pedidoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado com o ID: " + id));
        validarPedidoEditavel(p);

        List<itensPedido> novosItens = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        try {
            JsonNode carrinho = objectMapper.readTree(carrinhoJson);
            if (!carrinho.isArray() || carrinho.isEmpty()) {
                throw new IllegalArgumentException("O pedido precisa ter pelo menos um item");
            }
            for (JsonNode itemNode : carrinho) {
                long produtoId = itemNode.path("id").asLong(0);
                int quantidade = itemNode.path("quantidade").asInt(0);
                if (produtoId <= 0 || quantidade <= 0 || quantidade > 99) {
                    throw new IllegalArgumentException("Item de pedido inválido");
                }
                produtos produto = produtoRepo.findById(produtoId)
                        .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + produtoId));
                BigDecimal subtotalItem = produto.getPreco().multiply(BigDecimal.valueOf(quantidade));

                itensPedido item = new itensPedido();
                item.setPedido(p);
                item.setProduto(produto);
                item.setQuantidade(quantidade);
                item.setPrecoUnitario(produto.getPreco());
                item.setSubtotal(subtotalItem);
                item.setObservacao(itemNode.hasNonNull("observacao")
                        ? itemNode.get("observacao").asText()
                        : null);
                novosItens.add(item);
                subtotal = subtotal.add(subtotalItem);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Não foi possível processar os itens do pedido", e);
        }

        List<itensPedido> itensAtuais = p.getItens();
        if (itensAtuais == null) {
            itensAtuais = new ArrayList<>();
            p.setItens(itensAtuais);
        }
        itensAtuais.clear();
        itensAtuais.addAll(novosItens);

        p.setFormaPagamento(novaForma);
        BigDecimal desconto = Optional.ofNullable(p.getDesconto()).orElse(BigDecimal.ZERO);
        p.setValorTotal(subtotal.add(Optional.ofNullable(p.getTaxaEntrega()).orElse(BigDecimal.ZERO)).subtract(desconto));
        return pedidoRepo.save(p);
    }

    @Transactional(readOnly = true)
    public pedido carregarParaEdicao(Long id) {
        pedido p = pedidoRepo.findById(id).orElse(null);
        if (p != null && p.getItens() != null) {
            p.getItens().size();
        }
        return p;
    }

    public BigDecimal calcularTotal(String carrinhoJson) {
        if (carrinhoJson == null || carrinhoJson.isBlank()) {
            throw new IllegalArgumentException("O carrinho do pedido não pode estar vazio");
        }

        BigDecimal total = BigDecimal.ZERO;
        try {
            JsonNode carrinho = objectMapper.readTree(carrinhoJson);
            if (!carrinho.isArray() || carrinho.isEmpty()) {
                throw new IllegalArgumentException("O carrinho do pedido não pode estar vazio");
            }
            for (JsonNode itemNode : carrinho) {
                long produtoId = itemNode.path("id").asLong(0);
                int quantidade = itemNode.path("quantidade").asInt(0);
                if (produtoId <= 0 || quantidade <= 0 || quantidade > 99) {
                    throw new IllegalArgumentException("Item de pedido inválido");
                }
                produtos produto = produtoRepo.findById(produtoId)
                        .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + produtoId));
                total = total.add(produto.getPreco().multiply(BigDecimal.valueOf(quantidade)));
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Não foi possível processar os itens do pedido", e);
        }
        return total;
    }

    @Transactional
    public void confirmarPagamento(Long paymentId) {
        pedidoRepo.findByPagamentoMpId(paymentId).ifPresent(p -> {
            boolean mudou = false;
            if (!Boolean.TRUE.equals(p.getPago())) {
                p.setPago(true);
                mudou = true;
            }
            if (p.getStatusPedido() == statusPedido.AGUARDANDO_PAGAMENTO) {
                p.setStatusPedido(statusPedido.PREPARANDO);
                if (p.getDataInicioPreparo() == null) {
                    p.setDataInicioPreparo(OffsetDateTime.now());
                }
                mudou = true;
            }
            if (mudou) {
                pedidoRepo.save(p);
            }
            imprimirAutomaticamente(p);
        });
    }

    private void imprimirAutomaticamente(pedido p) {
        if (Boolean.TRUE.equals(p.getImpresso()) || p.getStatusPedido() != statusPedido.PREPARANDO) {
            return;
        }
        if (impressaoService.isModoPonte()) {
            return;
        }
        if (pedidoRepo.marcarComoImpresso(p.getId()) == 0) {
            return;
        }
        try {
            impressaoService.imprimirPedido(p.getId());
        } catch (Exception e) {
            pedidoRepo.reverterImpressao(p.getId());
        }
    }

    @Transactional
    public pedido atualizarTempoPreparo(Long id, Integer minutos) {
        if (minutos == null || minutos < 1 || minutos > 180) {
            throw new IllegalArgumentException("O tempo de preparo deve estar entre 1 e 180 minutos.");
        }
        pedido p = pedidoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado com o ID: " + id));
        validarPedidoEditavel(p);
        p.setTempoPreparoMinutos(minutos);
        return pedidoRepo.save(p);
    }

    private void validarPedidoEditavel(pedido p) {
        if (p.getStatusPedido() == statusPedido.CONCLUIDO || p.getStatusPedido() == statusPedido.CANCELADO) {
            throw new IllegalArgumentException("Não é possível alterar um pedido concluído ou cancelado.");
        }
    }

    public pedido atualizarStatus(Long id, statusPedido novoStatus) {
        Optional<pedido> pedidoOptional = pedidoRepo.findById(id);

        if (pedidoOptional.isEmpty()) {
            throw new RuntimeException("Pedido não encontrado com o ID: " + id);
        }

        pedido p = pedidoOptional.get();
        p.setStatusPedido(novoStatus);

        if (novoStatus == statusPedido.PREPARANDO && p.getDataInicioPreparo() == null) {
            p.setDataInicioPreparo(OffsetDateTime.now());
        }

        return pedidoRepo.save(p);
    }

    public List<pedido> listarStatus(statusPedido status) {
        return pedidoRepo.findByStatusPedido(status);
    }

    public List<pedido> listarStatusDoDia(statusPedido status) {
        ZoneId zona = ZoneId.systemDefault();
        OffsetDateTime inicio = LocalDate.now(zona).atStartOfDay(zona).toOffsetDateTime();
        OffsetDateTime fim = LocalDate.now(zona).plusDays(1).atStartOfDay(zona).toOffsetDateTime();
        return pedidoRepo.findByStatusPedidoAndDataHoraPedidoGreaterThanEqualAndDataHoraPedidoLessThan(
                status, inicio, fim);
    }

    public List<pedido> listarEntregas(BigDecimal taxaMinima, OffsetDateTime inicio,
            OffsetDateTime fim, statusPedido status) {
        return pedidoRepo.buscarEntregas(taxaMinima, inicio, fim, status);
    }

    public Optional<pedido> procurarID(Long id) {
        return pedidoRepo.findById(id);
    }

    public int obterTempoLimitePagamento() {
        return configuracaoService.obterTempoLimitePagamento();
    }

    public Optional<pedido> buscarPorPagamentoMp(Long paymentId) {
        return pedidoRepo.findByPagamentoMpId(paymentId);
    }

    @Transactional
    public pedido vincularPagamento(Long pedidoId, Long paymentId, formaPagamento forma) {
        pedido p = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));
        p.setPagamentoMpId(paymentId);
        if (forma != null) {
            p.setFormaPagamento(forma);
        }
        return pedidoRepo.save(p);
    }

    @Transactional
    public pedido atualizarExpiracaoPagamento(Long pedidoId, OffsetDateTime expiracao) {
        pedido p = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));
        p.setDataExpiracaoPagamento(expiracao);
        return pedidoRepo.save(p);
    }

    public List<pedido> listarTodos() {
        return pedidoRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<pedido> listarPorCliente(clientes cliente) {
        List<pedido> pedidos = pedidoRepo.findByClienteOrderByDataHoraPedidoDesc(cliente);
        pedidos.forEach(p -> {
            if (p.getItens() != null) {
                p.getItens().size();
            }
        });
        return pedidos;
    }

    @Transactional(readOnly = true)
    public pedido carregarPedidoCompleto(Long id) {
        pedido p = pedidoRepo.findById(id).orElse(null);
        if (p != null) {
            if (p.getItens() != null) {
                p.getItens().size();
            }
            p.getEntregas();
        }
        return p;
    }

    @Transactional
    public void imprimirAoPreparar(Long pedidoId) {
        pedidoRepo.findById(pedidoId).ifPresent(this::imprimirAutomaticamente);
    }

    @Transactional
    public void cancelarPagamentosExpirados() {
        List<pedido> expirados = pedidoRepo.findByStatusPedidoAndDataExpiracaoPagamentoBefore(
                statusPedido.AGUARDANDO_PAGAMENTO, OffsetDateTime.now());
        for (pedido p : expirados) {
            if (p.getDataExpiracaoPagamento() != null) {
                p.setStatusPedido(statusPedido.CANCELADO);
            }
        }
        if (!expirados.isEmpty()) {
            pedidoRepo.saveAll(expirados);
        }
    }

    @Transactional
    public pedido pedidoPendentePagamento(clientes cliente) {
        cancelarPagamentosExpirados();
        List<pedido> pendentes = pedidoRepo.findByClienteAndStatusPedidoOrderByDataHoraPedidoDesc(
                cliente, statusPedido.AGUARDANDO_PAGAMENTO);
        for (pedido p : pendentes) {
            if (p.getItens() != null) {
                p.getItens().size();
            }
        }
        return pendentes.isEmpty() ? null : pendentes.get(0);
    }
}
