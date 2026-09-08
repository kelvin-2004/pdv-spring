package PDV.PDV.repository;

import PDV.PDV.model.entregas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface entregaRepository extends JpaRepository<entregas, Long> {
    Optional<entregas> findByPedidoId(Long pedidoId);


}
