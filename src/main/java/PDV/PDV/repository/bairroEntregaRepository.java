package PDV.PDV.repository;

import PDV.PDV.model.bairroEntrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface bairroEntregaRepository extends JpaRepository<bairroEntrega, Long> {

    List<bairroEntrega> findAllByOrderByNomeAsc();
}
