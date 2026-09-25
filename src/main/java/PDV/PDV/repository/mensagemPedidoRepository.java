package PDV.PDV.repository;

import PDV.PDV.model.Enum.remetenteMensagem;
import PDV.PDV.model.mensagemPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface mensagemPedidoRepository extends JpaRepository<mensagemPedido, Long> {

    List<mensagemPedido> findByPedidoIdOrderByDataHoraAsc(Long pedidoId);

    long countByPedidoIdAndRemetenteAndLidaFalse(Long pedidoId, remetenteMensagem remetente);

    @Modifying
    @Query("UPDATE mensagemPedido m SET m.lida = true WHERE m.pedido.id = :pedidoId AND m.remetente = :remetente AND m.lida = false")
    int marcarLidas(@Param("pedidoId") Long pedidoId, @Param("remetente") remetenteMensagem remetente);

    @Query("SELECT m.pedido.id, COUNT(m) FROM mensagemPedido m WHERE m.remetente = :remetente AND m.lida = false GROUP BY m.pedido.id")
    List<Object[]> contarNaoLidasPorPedido(@Param("remetente") remetenteMensagem remetente);
}
