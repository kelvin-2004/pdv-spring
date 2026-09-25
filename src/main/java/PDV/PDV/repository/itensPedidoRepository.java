package PDV.PDV.repository;

import PDV.PDV.model.Enum.statusPedido;
import PDV.PDV.model.clientes;
import PDV.PDV.model.itensPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface itensPedidoRepository extends JpaRepository<itensPedido, Long> {

    List<itensPedido> findByPedidoId(Long id);

    @Query("""
        SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM itensPedido i
        WHERE i.pedido.cliente = :cliente
          AND i.pedido.statusPedido = :status
          AND i.produto.id = :produtoId
    """)
    boolean existePedidoConcluidoComProduto(@Param("cliente") clientes cliente,
                                            @Param("status") statusPedido status,
                                            @Param("produtoId") Long produtoId);

    @Query("""
        SELECT DISTINCT i.produto.id FROM itensPedido i
        WHERE i.pedido.cliente = :cliente
          AND i.pedido.statusPedido = :status
    """)
    List<Long> findProdutoIdsConcluidosPorCliente(@Param("cliente") clientes cliente,
                                                  @Param("status") statusPedido status);
}
