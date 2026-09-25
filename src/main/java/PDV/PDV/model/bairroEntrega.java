package PDV.PDV.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "tb_bairros_entrega")
public class bairroEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, unique = true)
    private String nome;

    @Column(name = "taxa_entrega", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxaEntrega;

    @Column(name = "tempo_entrega")
    private String tempoEntrega;
}
