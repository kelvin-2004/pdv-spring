package PDV.PDV.repository;

import PDV.PDV.model.Enum.statusPedido;
import PDV.PDV.model.clientes;
import PDV.PDV.model.pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface pedidoRepository extends JpaRepository<pedido, Long> {

    List<pedido> findByStatusPedido(statusPedido status);

    @Query("SELECT SUM(p.taxaEntrega) FROM pedido p")
    BigDecimal somarTotalTaxasEntrega();

    long countByCliente(clientes cliente);

}