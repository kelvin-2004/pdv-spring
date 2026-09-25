package PDV.PDV.model;

import PDV.PDV.model.Enum.DiaSemana;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalTime;

@Data
@Entity
@Table(name = "tb_horarios_funcionamento")
public class horarioFuncionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private DiaSemana diaSemana;

    private Boolean aberto = true;

    private LocalTime abertura;

    private LocalTime fechamento;
}
