package PDV.PDV.repository;

import PDV.PDV.model.ifoodVinculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ifoodVinculoRepository extends JpaRepository<ifoodVinculo, Long> {

    Optional<ifoodVinculo> findByIfoodItemId(String ifoodItemId);

    Optional<ifoodVinculo> findByProdutoId(Long produtoId);

    List<ifoodVinculo> findAllByOrderByIdAsc();
}
