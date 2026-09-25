package PDV.PDV.service;

import PDV.PDV.model.itensPedido;
import PDV.PDV.model.pedido;
import PDV.PDV.model.produtos;
import PDV.PDV.repository.itensPedidoRepository;
import PDV.PDV.repository.pedidoRepository;
import PDV.PDV.repository.produtoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class itensPedidoService {

    @Autowired
    private itensPedidoRepository itensPedidoRepo;

    @Autowired
    private pedidoRepository pedidoRepo;

    @Autowired
    private produtoRepository produtoRepo;

    public itensPedido adicionarItem(Long pedidoId, Long produtoId, Integer quantidade, String observacao) {

        pedido p = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado com o ID: " + pedidoId));

        produtos prod = produtoRepo.findById(produtoId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com o ID: " + produtoId));

        itensPedido item = new itensPedido();
        item.setPedido(p);
        item.setProduto(prod);
        item.setQuantidade(quantidade);
        item.setObservacao(observacao);

        BigDecimal precoUnitario = prod.getPreco();
        BigDecimal subtotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade));

        item.setPrecoUnitario(precoUnitario);
        item.setSubtotal(subtotal);

        itensPedido itemSalvo = itensPedidoRepo.save(item);

        BigDecimal valorTotalAtual = p.getValorTotal() != null ? p.getValorTotal() : BigDecimal.ZERO;
        p.setValorTotal(valorTotalAtual.add(subtotal));
        pedidoRepo.save(p);

        return itemSalvo;
    }

    public List<itensPedido> listarPorPedido(Long pedidoId) {
        return itensPedidoRepo.findByPedidoId(pedidoId);
    }}