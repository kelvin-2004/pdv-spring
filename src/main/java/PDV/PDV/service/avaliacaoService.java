package PDV.PDV.service;

import PDV.PDV.model.Enum.statusPedido;
import PDV.PDV.model.avaliacao;
import PDV.PDV.model.clientes;
import PDV.PDV.model.produtos;
import PDV.PDV.repository.avaliacaoRepository;
import PDV.PDV.repository.itensPedidoRepository;
import PDV.PDV.repository.produtoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class avaliacaoService {

    @Autowired private avaliacaoRepository avaliacaoRepo;
    @Autowired private itensPedidoRepository itensPedidoRepo;
    @Autowired private produtoRepository produtoRepo;

    public Map<Long, List<avaliacao>> listarPorProdutos(List<Long> produtoIds) {
        if (produtoIds == null || produtoIds.isEmpty()) {
            return Map.of();
        }
        return avaliacaoRepo.findByProdutoIdInOrderByDataHoraDesc(produtoIds).stream()
                .collect(Collectors.groupingBy(a -> a.getProduto().getId()));
    }

    public boolean podeAvaliar(clientes cliente, Long produtoId) {
        if (cliente == null || produtoId == null) {
            return false;
        }
        return itensPedidoRepo.existePedidoConcluidoComProduto(cliente, statusPedido.CONCLUIDO, produtoId);
    }

    public Set<Long> produtosAvaliaveis(clientes cliente) {
        if (cliente == null) {
            return Set.of();
        }
        return itensPedidoRepo.findProdutoIdsConcluidosPorCliente(cliente, statusPedido.CONCLUIDO)
                .stream().collect(Collectors.toSet());
    }

    public avaliacao salvar(clientes cliente, Long produtoId, int nota, String texto) {
        if (cliente == null) {
            throw new IllegalArgumentException("Faça login para avaliar.");
        }
        produtos produto = produtoRepo.findById(produtoId)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));
        if (!podeAvaliar(cliente, produtoId)) {
            throw new IllegalArgumentException("Você só pode avaliar após concluir um pedido com este prato.");
        }
        if (nota < 1 || nota > 5) {
            throw new IllegalArgumentException("A nota deve ser entre 1 e 5.");
        }
        avaliacao avaliacao = avaliacaoRepo.findByProdutoAndCliente(produto, cliente)
                .orElseGet(avaliacao::new);
        avaliacao.setProduto(produto);
        avaliacao.setCliente(cliente);
        avaliacao.setNota(nota);
        avaliacao.setTexto(texto);
        avaliacao.setDataHora(OffsetDateTime.now());
        return avaliacaoRepo.save(avaliacao);
    }
}
