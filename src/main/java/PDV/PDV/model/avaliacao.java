package PDV.PDV.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Data
@Entity
@Table(name = "tb_avaliacoes")
public class avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private produtos produto;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private clientes cliente;

    @Column(nullable = false)
    private Integer nota;

    @Column(length = 1000)
    private String texto;

    @Column(name = "data_hora", nullable = false, updatable = false)
    private OffsetDateTime dataHora = OffsetDateTime.now();

    @Transient
    public String getDataHoraFormatada() {
        if (dataHora == null) {
            return "";
        }
        return dataHora.atZoneSameInstant(ZoneId.of("America/Sao_Paulo"))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}
