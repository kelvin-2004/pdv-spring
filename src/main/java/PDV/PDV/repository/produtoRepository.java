package PDV.PDV.repository;

import PDV.PDV.model.Enum.categoriaPedido;
import PDV.PDV.model.produtos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface produtoRepository extends JpaRepository<produtos, Long> {

    List<produtos> findByAtivoTrue();

    Optional<produtos> findByNome(String nome);

    List<produtos> findByNomeContainingIgnoreCase(String nome);

    List<produtos> findByNomeContainingIgnoreCaseAndAtivoTrue(String nome);

    List<produtos> findByCategoriaPedidoAndAtivoTrue(categoriaPedido categoria);
}