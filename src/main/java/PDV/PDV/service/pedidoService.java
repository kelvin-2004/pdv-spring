package PDV.PDV.service;

import PDV.PDV.model.Enum.statusPedido;
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

    public pedido novoPedido(pedido novoPedido) {
        novoPedido.setDataHoraPedido(OffsetDateTime.now());
        novoPedido.setStatusPedido(statusPedido.PREPARANDO);

        // Define automaticamente o número do pedido para este cliente
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
        novoPedido.setStatusPedido(statusPedido.PREPARANDO);
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
                if (produtoId <= 0 || quantidade <= 0) {
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
        pedidoSalvo.setValorTotal(subtotal.add(Optional.ofNullable(pedidoSalvo.getTaxaEntrega()).orElse(BigDecimal.ZERO)));
        itensPedidoRepo.saveAll(itens);
        return pedidoRepo.save(pedidoSalvo);
    }

    public pedido atualizarStatus(Long id, statusPedido novoStatus) {
        Optional<pedido> pedidoOptional = pedidoRepo.findById(id);

        if (pedidoOptional.isEmpty()) {
            throw new RuntimeException("Pedido não encontrado com o ID: " + id);
        }

        pedido p = pedidoOptional.get();
        p.setStatusPedido(novoStatus);

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

    public List<pedido> listarTodos() {
        return pedidoRepo.findAll();
    }
}