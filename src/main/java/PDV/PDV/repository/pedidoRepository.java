package PDV.PDV.repository;

import PDV.PDV.model.Enum.statusPedido;
import PDV.PDV.dto.ResumoProdutoVenda;
import PDV.PDV.model.clientes;
import PDV.PDV.model.pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface pedidoRepository extends JpaRepository<pedido, Long> {

    List<pedido> findByStatusPedido(statusPedido status);

        List<pedido> findByStatusPedidoAndDataHoraPedidoGreaterThanEqualAndDataHoraPedidoLessThan(
            statusPedido status, OffsetDateTime inicio, OffsetDateTime fim);

            @Query("""
                SELECT p FROM pedido p
                WHERE p.taxaEntrega >= :taxaMinima
                  AND p.dataHoraPedido >= :inicio
                  AND p.dataHoraPedido < :fim
                  AND (:status IS NOT NULL OR p.statusPedido <> PDV.PDV.model.Enum.statusPedido.CANCELADO)
                  AND (:status IS NOT NULL OR p.statusPedido <> PDV.PDV.model.Enum.statusPedido.CANCELADO)
                  AND (:status IS NOT NULL OR p.statusPedido <> PDV.PDV.model.Enum.statusPedido.CANCELADO)
                  AND (:status IS NOT NULL OR p.statusPedido <> PDV.PDV.model.Enum.statusPedido.CANCELADO)
                  AND (:status IS NULL OR p.statusPedido = :status)
                ORDER BY p.dataHoraPedido DESC
                """)
            List<pedido> buscarEntregas(BigDecimal taxaMinima, OffsetDateTime inicio,
                OffsetDateTime fim, statusPedido status);

    @Query("SELECT SUM(p.taxaEntrega) FROM pedido p")
    BigDecimal somarTotalTaxasEntrega();

    long countByCliente(clientes cliente);

    List<pedido> findByClienteOrderByDataHoraPedidoDesc(clientes cliente);

    List<pedido> findByClienteAndStatusPedidoOrderByDataHoraPedidoDesc(clientes cliente, statusPedido status);

    List<pedido> findByStatusPedidoAndDataExpiracaoPagamentoBefore(statusPedido status, OffsetDateTime agora);

    Optional<pedido> findByPagamentoMpId(Long pagamentoMpId);

    Optional<pedido> findByIfoodPedidoId(String ifoodPedidoId);

    Optional<pedido> findByN99PedidoId(String n99PedidoId);

    @Modifying
    @Transactional
    @Query("UPDATE pedido p SET p.impresso = true, p.reimpressao = false WHERE p.id = :id AND (p.impresso = false OR p.impresso IS NULL OR p.reimpressao = true)")
    int marcarComoImpresso(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE pedido p SET p.impresso = false WHERE p.id = :id")
    int reverterImpressao(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE pedido p SET p.reimpressao = true WHERE p.id = :id")
    int solicitarReimpressao(@Param("id") Long id);

    @Query("""
        SELECT p FROM pedido p
        WHERE p.reimpressao = true
           OR (p.statusPedido = PDV.PDV.model.Enum.statusPedido.PREPARANDO
               AND (p.impresso = false OR p.impresso IS NULL))
        ORDER BY p.dataHoraPedido ASC
        """)
    List<pedido> findPendentesImpressao();

        @Query("""
            SELECT new PDV.PDV.dto.ResumoProdutoVenda(i.produto.nome, SUM(i.quantidade), SUM(i.subtotal))
            FROM itensPedido i
            JOIN i.pedido p
            WHERE p.dataHoraPedido >= :inicio
              AND p.dataHoraPedido < :fim
              AND (:status IS NULL OR p.statusPedido = :status)
            GROUP BY i.produto.id, i.produto.nome
            ORDER BY SUM(i.quantidade) DESC, SUM(i.subtotal) DESC
            """)
        List<ResumoProdutoVenda> resumirVendasPorProduto(
            @Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim,
            @Param("status") statusPedido status);

        @Query("""
            SELECT COALESCE(SUM(p.valorTotal), 0)
            FROM pedido p
            WHERE p.dataHoraPedido >= :inicio
              AND p.dataHoraPedido < :fim
              AND (:status IS NULL OR p.statusPedido = :status)
            """)
        BigDecimal somarVendasPeriodo(@Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim, @Param("status") statusPedido status);

        @Query("""
            SELECT COALESCE(SUM(i.quantidade), 0)
            FROM itensPedido i
            JOIN i.pedido p
            WHERE p.dataHoraPedido >= :inicio
              AND p.dataHoraPedido < :fim
              AND (:status IS NULL OR p.statusPedido = :status)
            """)
        Long contarMarmitasPeriodo(@Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim, @Param("status") statusPedido status);

        @Query("""
            SELECT COUNT(p)
            FROM pedido p
            WHERE p.dataHoraPedido >= :inicio
              AND p.dataHoraPedido < :fim
              AND (:status IS NULL OR p.statusPedido = :status)
            """)
        Long contarPedidosPeriodo(@Param("inicio") OffsetDateTime inicio,
            @Param("fim") OffsetDateTime fim, @Param("status") statusPedido status);

            @Query("""
                SELECT COALESCE(SUM(i.subtotal), 0)
                FROM itensPedido i
                JOIN i.pedido p
                WHERE p.dataHoraPedido >= :inicio
                  AND p.dataHoraPedido < :fim
                  AND (:status IS NOT NULL OR p.statusPedido <> PDV.PDV.model.Enum.statusPedido.CANCELADO)
                  AND (:status IS NULL OR p.statusPedido = :status)
                """)
            BigDecimal somarItensPeriodo(@Param("inicio") OffsetDateTime inicio,
                @Param("fim") OffsetDateTime fim, @Param("status") statusPedido status);

}