package PDV.PDV.repository;

import PDV.PDV.model.avaliacao;
import PDV.PDV.model.clientes;
import PDV.PDV.model.produtos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface avaliacaoRepository extends JpaRepository<avaliacao, Long> {

    List<avaliacao> findByProdutoIdInOrderByDataHoraDesc(List<Long> produtoIds);

    Optional<avaliacao> findByProdutoAndCliente(produtos produto, clientes cliente);
}
