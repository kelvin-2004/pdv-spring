package PDV.PDV.model;

import PDV.PDV.model.Enum.publicoCupom;
import PDV.PDV.model.Enum.tipoCupom;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "tb_cupons")
public class cupom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(length = 255)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(50)")
    private tipoCupom tipo;

    @Column(precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "valor_minimo", precision = 10, scale = 2)
    private BigDecimal valorMinimo = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(50)")
    private publicoCupom publico;

    private Boolean ativo = true;
}
