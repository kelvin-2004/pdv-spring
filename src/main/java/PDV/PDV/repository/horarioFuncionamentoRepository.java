package PDV.PDV.repository;

import PDV.PDV.model.Enum.DiaSemana;
import PDV.PDV.model.horarioFuncionamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface horarioFuncionamentoRepository extends JpaRepository<horarioFuncionamento, Long> {

    Optional<horarioFuncionamento> findByDiaSemana(DiaSemana diaSemana);
}
