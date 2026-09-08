package PDV.PDV.repository;

import PDV.PDV.model.Enum.statusPedido;
import PDV.PDV.model.pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface pedidoRepository extends JpaRepository<pedido, Long> {
    List<pedido> findByStatusPedido(statusPedido status);

}
