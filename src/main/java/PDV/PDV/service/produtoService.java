package PDV.PDV.service;

import PDV.PDV.model.Enum.categoriaPedido;
import PDV.PDV.model.produtos;
import PDV.PDV.repository.produtoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class produtoService {

    @Autowired
    private produtoRepository produtoRepo;

    public produtos salvarProduto(produtos produto) {
        return produtoRepo.save(produto);
    }

    public List<produtos> listarTodos() {
        return produtoRepo.findAll();
    }

    public List<produtos> listarAtivos() {
        return produtoRepo.findByAtivoTrue();
    }

    public List<produtos> listarPorCategoria(categoriaPedido categoria) {
        return produtoRepo.findByCategoriaPedidoAndAtivoTrue(categoria);
    }

    public Optional<produtos> buscarPorId(Long id) {
        return produtoRepo.findById(id);
    }

    public List<produtos> buscarPorNome(String nome) {
        return produtoRepo.findByNomeContainingIgnoreCase(nome);
    }

    public List<produtos> buscarAtivosPorNome(String nome) {
        return produtoRepo.findByNomeContainingIgnoreCaseAndAtivoTrue(nome);
    }

    public produtos alterarStatusAtivo(Long id, boolean status) {
        Optional<produtos> produtoOptional = produtoRepo.findById(id);

        if (produtoOptional.isEmpty()) {
            throw new RuntimeException("Produto não encontrado com o ID: " + id);
        }

        produtos produto = produtoOptional.get();
        produto.setAtivo(status);

        return produtoRepo.save(produto);
    }
}
