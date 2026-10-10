package PDV.PDV.service;

import PDV.PDV.model.Enum.categoriaPedido;
import PDV.PDV.model.produtos;
import PDV.PDV.repository.produtoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
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

    /**
     * Busca um produto do PDV pelo nome do item vindo da plataforma (iFood/99), ignorando
     * acentos, caixa e espaços. Usado para vincular automaticamente os itens de pedidos
     * externos a produtos do catálogo — "sincronizar o máximo de produtos identificáveis".
     *
     * <p>Estratégia: primeiro tenta correspondência exata do nome normalizado; se não achar,
     * tenta correspondência por contenção (um nome normalizado contém o outro), com guarda de
     * tamanho para evitar falsos positivos. Retorna vazio se nada casar.</p>
     */
    public Optional<produtos> buscarPorNomeNormalizado(String nomeItem) {
        if (nomeItem == null || nomeItem.isBlank()) {
            return Optional.empty();
        }
        String alvo = normalizar(nomeItem);
        if (alvo.isEmpty()) {
            return Optional.empty();
        }

        List<produtos> ativos = produtoRepo.findByAtivoTrue();
        Optional<produtos> exato = Optional.empty();
        Optional<produtos> parcial = Optional.empty();

        for (produtos p : ativos) {
            String nomeProduto = normalizar(p.getNome());
            if (nomeProduto.isEmpty()) {
                continue;
            }
            if (nomeProduto.equals(alvo)) {
                exato = Optional.of(p);
                break;
            }
            if (parcial.isEmpty() && contemRelevante(nomeProduto, alvo)) {
                parcial = Optional.of(p);
            }
        }

        return exato.isPresent() ? exato : parcial;
    }

    /** Normaliza um nome para comparação: minúsculas, sem acentos, só letras/dígitos. */
    private static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        String semAcentos = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return semAcentos.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim()
                .replaceAll("\\s+", " ");
    }

    /** True se um nome contém o outro (após normalizar), com guarda contra falsos positivos. */
    private static boolean contemRelevante(String a, String b) {
        String maior = a.length() >= b.length() ? a : b;
        String menor = a.length() >= b.length() ? b : a;
        // Evita casar "coca" com qualquer coisa: exige que o termo menor seja minimamente
        // específico (>= 5 caracteres) e esteja contido no maior.
        return menor.length() >= 5 && maior.contains(menor);
    }
}
