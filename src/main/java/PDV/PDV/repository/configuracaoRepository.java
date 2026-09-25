package PDV.PDV.repository;

import PDV.PDV.model.configuracao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface configuracaoRepository extends JpaRepository<configuracao, Long> {
}
