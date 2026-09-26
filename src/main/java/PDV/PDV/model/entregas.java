package PDV.PDV.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "tb_entregas")
public class entregas {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne
    @JoinColumn(name = "pedido_id", referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private pedido pedido;
    private String rua;
    private String numero;
    private String bairro;
    private String cep;
    private String complemento;



}
