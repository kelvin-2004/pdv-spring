package PDV.PDV.repository;

import PDV.PDV.model.cupom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface cupomRepository extends JpaRepository<cupom, Long> {

    Optional<cupom> findByCodigoIgnoreCase(String codigo);

    List<cupom> findAllByOrderByIdDesc();
}
