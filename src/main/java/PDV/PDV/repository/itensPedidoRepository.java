package PDV.PDV.repository;

import PDV.PDV.model.itensPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface itensPedidoRepository extends JpaRepository<itensPedido, Long> {

    // Alinhado com o campo 'pedido' da sua model
    List<itensPedido> findByPedidoId(Long id);
}