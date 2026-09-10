package PDV.PDV.repository;

import PDV.PDV.model.Enum.categoriaPedido;
import PDV.PDV.model.produtos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface produtoRepository extends JpaRepository<produtos, Long> {

    // Busca todos os produtos ativos
    List<produtos> findByAtivoTrue();

    // Busca exata por nome
    Optional<produtos> findByNome(String nome);

    // Busca por parte do nome (ignorando maiúsculas/minúsculas)
    List<produtos> findByNomeContainingIgnoreCase(String nome);

    // Busca produtos ativos por categoria
    List<produtos> findByCategoriaPedidoAndAtivoTrue(categoriaPedido categoria);
}