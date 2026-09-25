package PDV.PDV.service;

import PDV.PDV.model.Enum.DiaSemana;
import PDV.PDV.model.horarioFuncionamento;
import PDV.PDV.repository.horarioFuncionamentoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class horarioService {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private horarioFuncionamentoRepository repo;

    @PostConstruct
    public void semear() {
        if (repo.count() == 0) {
            for (DiaSemana d : DiaSemana.values()) {
                horarioFuncionamento h = new horarioFuncionamento();
                h.setDiaSemana(d);
                h.setAberto(true);
                h.setAbertura(LocalTime.of(7, 0));
                h.setFechamento(LocalTime.of(23, 0));
                repo.save(h);
            }
        }
    }

    public List<horarioFuncionamento> listar() {
        semear();
        return repo.findAll().stream()
                .sorted((a, b) -> Integer.compare(a.getDiaSemana().ordinal(), b.getDiaSemana().ordinal()))
                .toList();
    }

    public void salvarTodos(List<DiaSemana> diasAbertos, Map<String, String> form) {
        List<DiaSemana> abertos = diasAbertos != null ? diasAbertos : List.of();
        for (DiaSemana d : DiaSemana.values()) {
            horarioFuncionamento h = repo.findByDiaSemana(d).orElseGet(() -> {
                horarioFuncionamento novo = new horarioFuncionamento();
                novo.setDiaSemana(d);
                return novo;
            });
            if (abertos.contains(d)) {
                LocalTime abertura = parseHora(form.getOrDefault("abertura_" + d.name(), null), d, "abertura");
                LocalTime fechamento = parseHora(form.getOrDefault("fechamento_" + d.name(), null), d, "fechamento");
                if (abertura.equals(fechamento)) {
                    throw new IllegalArgumentException("Horário de " + d.getLabel()
                            + ": abertura e fechamento não podem ser iguais.");
                }
                h.setAberto(true);
                h.setAbertura(abertura);
                h.setFechamento(fechamento);
            } else {
                h.setAberto(false);
                h.setAbertura(null);
                h.setFechamento(null);
            }
            repo.save(h);
        }
    }

    private LocalTime parseHora(String valor, DiaSemana d, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Informe o horário de " + campo + " para " + d.getLabel() + ".");
        }
        try {
            return LocalTime.parse(valor.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Horário de " + campo + " para " + d.getLabel() + " é inválido.");
        }
    }

    public boolean estaAbertoAgora() {
        LocalDateTime agora = LocalDateTime.now(ZONA);
        DiaSemana dia = DiaSemana.from(agora.getDayOfWeek());
        horarioFuncionamento h = repo.findByDiaSemana(dia).orElse(null);
        return estaAbertoNoHorario(h, agora);
    }

    public boolean estaAbertoNoHorario(horarioFuncionamento h, LocalDateTime agora) {
        if (h == null || !Boolean.TRUE.equals(h.getAberto())) {
            return false;
        }
        LocalTime abertura = h.getAbertura();
        LocalTime fechamento = h.getFechamento();
        if (abertura == null || fechamento == null) {
            return false;
        }
        if (abertura.equals(fechamento)) {
            return false;
        }
        LocalTime hora = agora.toLocalTime();
        if (fechamento.isAfter(abertura)) {
            return !hora.isBefore(abertura) && hora.isBefore(fechamento);
        }
        return !hora.isBefore(abertura) || hora.isBefore(fechamento);
    }

    public void verificarAberto() {
        if (!estaAbertoAgora()) {
            throw new IllegalArgumentException("A loja está fechada no momento. " + statusAgora());
        }
    }

    public String statusAgora() {
        LocalDateTime agora = LocalDateTime.now(ZONA);
        for (int i = 0; i < 7; i++) {
            DiaSemana d = DiaSemana.from(agora.getDayOfWeek().plus(i));
            horarioFuncionamento h = repo.findByDiaSemana(d).orElse(null);
            if (h == null || !Boolean.TRUE.equals(h.getAberto())
                    || h.getAbertura() == null || h.getFechamento() == null) {
                continue;
            }
            if (i == 0 && estaAbertoNoHorario(h, agora)) {
                return "Aberto até " + formatar(h.getFechamento());
            }
            LocalDateTime abertura = agora.toLocalDate().plusDays(i).atTime(h.getAbertura());
            if (abertura.isAfter(agora)) {
                if (i == 0) {
                    return "Abre hoje às " + formatar(h.getAbertura());
                }
                if (i == 1) {
                    return "Abre amanhã às " + formatar(h.getAbertura());
                }
                return "Abre " + d.getLabel().toLowerCase() + " às " + formatar(h.getAbertura());
            }
        }
        return "Loja fechada";
    }

    private String formatar(LocalTime hora) {
        return hora == null ? "--:--" : hora.format(HORA);
    }
}
