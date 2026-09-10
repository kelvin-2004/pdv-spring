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

    // 1. Cadastrar ou atualizar um produto no estoque
    public produtos salvarProduto(produtos produto) {
        return produtoRepo.save(produto);
    }

    // 2. Listar TODOS os produtos (ativos e inativos)
    public List<produtos> listarTodos() {
        return produtoRepo.findAll();
    }

    // 3. Listar apenas os produtos ativos
    public List<produtos> listarAtivos() {
        return produtoRepo.findByAtivoTrue();
    }

    // 4. Filtrar produtos por categoria e ativos
    public List<produtos> listarPorCategoria(categoriaPedido categoria) {
        return produtoRepo.findByCategoriaPedidoAndAtivoTrue(categoria);
    }

    // 5. Buscar produto por ID
    public Optional<produtos> buscarPorId(Long id) {
        return produtoRepo.findById(id);
    }

    // 6. Buscar produtos por parte do nome
    public List<produtos> buscarPorNome(String nome) {
        return produtoRepo.findByNomeContainingIgnoreCase(nome);
    }

    // 7. Ativar ou desativar produto
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