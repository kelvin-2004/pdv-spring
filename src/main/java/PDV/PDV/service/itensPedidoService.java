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

    // Adiciona um produto ao pedido e calcula o subtotal
    public itensPedido adicionarItem(Long pedidoId, Long produtoId, Integer quantidade, String observacao) {

        // 1. Valida se o pedido existe
        pedido p = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado com o ID: " + pedidoId));

        // 2. Valida se o produto existe
        produtos prod = produtoRepo.findById(produtoId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com o ID: " + produtoId));

        // 3. Cria e popula a instância do item
        itensPedido item = new itensPedido();
        item.setPedidos(p);
        item.setProduto(prod);
        item.setQuantidade(quantidade);
        item.setObservacao(observacao);

        // 4. Captura o preço atual e calcula o subtotal (Preço x Quantidade)
        BigDecimal precoUnitario = prod.getPreco(); // Se o preço no produto for Double, use BigDecimal.valueOf(prod.getPreco())
        item.setPrecoUnitario(precoUnitario);
        item.setSubtotal(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));

        return itensPedidoRepo.save(item);
    }

    // Busca todos os itens cadastrados em um pedido
    public List<itensPedido> listarPorPedido(Long pedidoId) {
        return itensPedidoRepo.findByPedidosId(pedidoId);
    }
}