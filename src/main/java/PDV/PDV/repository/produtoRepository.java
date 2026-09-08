package PDV.PDV.repository;

import PDV.PDV.model.Enum.categoriaPedido;
import PDV.PDV.model.produtos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface produtoRepository extends JpaRepository<produtos, Long> {

    // 1. Busca todos os produtos ativos (sem parâmetro)
    List<produtos> findByAtivoTrue();

    // 2. Busca produtos ativos filtrados por uma categoria específica
    List<produtos> findByCategoriaPedidoAndAtivoTrue(categoriaPedido categoria);
}