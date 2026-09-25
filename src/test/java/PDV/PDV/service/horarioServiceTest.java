package PDV.PDV.service;

import PDV.PDV.model.Enum.DiaSemana;
import PDV.PDV.model.horarioFuncionamento;
import PDV.PDV.repository.horarioFuncionamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class horarioServiceTest {

    @Mock
    private horarioFuncionamentoRepository repo;

    @InjectMocks
    private horarioService service;

    private horarioFuncionamento dia(boolean aberto, LocalTime abertura, LocalTime fechamento) {
        horarioFuncionamento h = new horarioFuncionamento();
        h.setDiaSemana(DiaSemana.SEGUNDA);
        h.setAberto(aberto);
        h.setAbertura(abertura);
        h.setFechamento(fechamento);
        return h;
    }

    // ===== estaAbertoNoHorario =====

    @Test
    void abertoDentroDaJanela() {
        horarioFuncionamento h = dia(true, LocalTime.of(7, 0), LocalTime.of(23, 0));
        assertThat(service.estaAbertoNoHorario(h, LocalDateTime.of(2026, 9, 21, 12, 0))).isTrue();
    }

    @Test
    void fechadoAntesDaAbertura() {
        horarioFuncionamento h = dia(true, LocalTime.of(7, 0), LocalTime.of(23, 0));
        assertThat(service.estaAbertoNoHorario(h, LocalDateTime.of(2026, 9, 21, 6, 59))).isFalse();
    }

    @Test
    void fechadoNoHorarioDoFechamento() {
        horarioFuncionamento h = dia(true, LocalTime.of(7, 0), LocalTime.of(23, 0));
        assertThat(service.estaAbertoNoHorario(h, LocalDateTime.of(2026, 9, 21, 23, 0))).isFalse();
    }

    @Test
    void diaFechado() {
        horarioFuncionamento h = dia(false, LocalTime.of(7, 0), LocalTime.of(23, 0));
        assertThat(service.estaAbertoNoHorario(h, LocalDateTime.of(2026, 9, 21, 12, 0))).isFalse();
    }

    @Test
    void horariosNulos() {
        horarioFuncionamento h = dia(true, null, null);
        assertThat(service.estaAbertoNoHorario(h, LocalDateTime.of(2026, 9, 21, 12, 0))).isFalse();
    }

    @Test
    void janelaZerada() {
        horarioFuncionamento h = dia(true, LocalTime.of(18, 0), LocalTime.of(18, 0));
        assertThat(service.estaAbertoNoHorario(h, LocalDateTime.of(2026, 9, 21, 18, 0))).isFalse();
    }

    @Test
    void registroNuloFechado() {
        assertThat(service.estaAbertoNoHorario(null, LocalDateTime.of(2026, 9, 21, 12, 0))).isFalse();
    }

    // ===== virada de madrugada (fechamento < abertura) =====

    @Test
    void viradaAbertoNoInicioDaNoite() {
        horarioFuncionamento h = dia(true, LocalTime.of(18, 0), LocalTime.of(2, 0));
        assertThat(service.estaAbertoNoHorario(h, LocalDateTime.of(2026, 9, 21, 20, 0))).isTrue();
    }

    @Test
    void viradaAbertoDeMadrugada() {
        horarioFuncionamento h = dia(true, LocalTime.of(18, 0), LocalTime.of(2, 0));
        assertThat(service.estaAbertoNoHorario(h, LocalDateTime.of(2026, 9, 21, 1, 0))).isTrue();
    }

    @Test
    void viradaFechadoDuranteODia() {
        horarioFuncionamento h = dia(true, LocalTime.of(18, 0), LocalTime.of(2, 0));
        assertThat(service.estaAbertoNoHorario(h, LocalDateTime.of(2026, 9, 21, 10, 0))).isFalse();
    }

    // ===== salvarTodos (validação) =====

    @Test
    void salvarSemHorarioDeAberturaFalha() {
        when(repo.findByDiaSemana(any())).thenReturn(Optional.empty());
        Map<String, String> form = Map.of("fechamento_SEGUNDA", "18:00");
        assertThatThrownBy(() -> service.salvarTodos(List.of(DiaSemana.SEGUNDA), form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("abertura");
    }

    @Test
    void salvarSemHorarioDeFechamentoFalha() {
        when(repo.findByDiaSemana(any())).thenReturn(Optional.empty());
        Map<String, String> form = Map.of("abertura_SEGUNDA", "07:00");
        assertThatThrownBy(() -> service.salvarTodos(List.of(DiaSemana.SEGUNDA), form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fechamento");
    }

    @Test
    void salvarComAberturaIgualFechamentoFalha() {
        when(repo.findByDiaSemana(any())).thenReturn(Optional.empty());
        Map<String, String> form = Map.of("abertura_SEGUNDA", "18:00", "fechamento_SEGUNDA", "18:00");
        assertThatThrownBy(() -> service.salvarTodos(List.of(DiaSemana.SEGUNDA), form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não podem ser iguais");
    }

    @Test
    void salvarHorarioInvalidoFalha() {
        when(repo.findByDiaSemana(any())).thenReturn(Optional.empty());
        Map<String, String> form = Map.of("abertura_SEGUNDA", "25:99", "fechamento_SEGUNDA", "18:00");
        assertThatThrownBy(() -> service.salvarTodos(List.of(DiaSemana.SEGUNDA), form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inválido");
    }
}
