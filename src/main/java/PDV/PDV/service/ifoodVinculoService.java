package PDV.PDV.service;

import PDV.PDV.model.ifoodVinculo;
import PDV.PDV.model.produtos;
import PDV.PDV.repository.ifoodVinculoRepository;
import PDV.PDV.repository.produtoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ifoodVinculoService {

    @Autowired
    private ifoodVinculoRepository repo;

    @Autowired
    private produtoRepository produtoRepo;

    @Transactional(readOnly = true)
    public List<ifoodVinculo> listar() {
        return repo.findAllByOrderByIdAsc();
    }

    @Transactional
    public ifoodVinculo vincular(String ifoodItemId, String ifoodItemNome, Long produtoId) {
        if (ifoodItemId == null || ifoodItemId.isBlank()) {
            throw new IllegalArgumentException("Informe o identificador do item na iFood.");
        }
        produtos produto = produtoRepo.findById(produtoId)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));

        ifoodVinculo v = repo.findByIfoodItemId(ifoodItemId.trim()).orElseGet(ifoodVinculo::new);
        v.setIfoodItemId(ifoodItemId.trim());
        v.setIfoodItemNome(ifoodItemNome == null || ifoodItemNome.isBlank() ? null : ifoodItemNome.trim());
        v.setProduto(produto);
        return repo.save(v);
    }

    @Transactional
    public void desvincular(Long id) {
        repo.deleteById(id);
    }

    /** Resolve um item iFood (pelo id) para o produto do PDV vinculado, se houver. */
    @Transactional(readOnly = true)
    public Optional<produtos> resolverProduto(String ifoodItemId) {
        if (ifoodItemId == null || ifoodItemId.isBlank()) {
            return Optional.empty();
        }
        return repo.findByIfoodItemId(ifoodItemId).map(ifoodVinculo::getProduto);
    }

    /** Busca o vínculo de um produto do PDV (para sincronizar disponibilidade com a iFood). */
    @Transactional(readOnly = true)
    public Optional<ifoodVinculo> buscarPorProduto(Long produtoId) {
        if (produtoId == null) {
            return Optional.empty();
        }
        return repo.findByProdutoId(produtoId);
    }
}
