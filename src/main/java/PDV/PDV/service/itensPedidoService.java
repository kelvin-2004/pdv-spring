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

    // Adiciona um produto ao pedido, calcula o subtotal e atualiza o valor total do pedido
    public itensPedido adicionarItem(Long pedidoId, Long produtoId, Integer quantidade, String observacao) {

        // 1. Valida e busca o pedido
        pedido p = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado com o ID: " + pedidoId));

        // 2. Valida e busca o produto
        produtos prod = produtoRepo.findById(produtoId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com o ID: " + produtoId));

        // 3. Instancia e popula o item do pedido
        itensPedido item = new itensPedido();
        item.setPedido(p);
        item.setProduto(prod);
        item.setQuantidade(quantidade);
        item.setObservacao(observacao);

        // 4. Calcula o subtotal do item
        BigDecimal precoUnitario = prod.getPreco();
        BigDecimal subtotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade));

        item.setPrecoUnitario(precoUnitario);
        item.setSubtotal(subtotal);

        // 5. Persiste o item primeiro
        itensPedido itemSalvo = itensPedidoRepo.save(item);

        // 6. Atualiza o valorTotal do pedido acumulando o novo subtotal
        BigDecimal valorTotalAtual = p.getValorTotal() != null ? p.getValorTotal() : BigDecimal.ZERO;
        p.setValorTotal(valorTotalAtual.add(subtotal));
        pedidoRepo.save(p);

        return itemSalvo;
    }

    // Busca todos os itens de um pedido
    public List<itensPedido> listarPorPedido(Long pedidoId) {
        return itensPedidoRepo.findByPedidoId(pedidoId);
    }}