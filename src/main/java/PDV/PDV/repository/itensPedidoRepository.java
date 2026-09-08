package PDV.PDV.repository;

import PDV.PDV.model.itensPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface itensPedidoRepository extends JpaRepository<itensPedido, Long> {

    // Alinhado com o campo 'Pedidos' da sua model
    List<itensPedido> findByPedidosId(Long id);
}