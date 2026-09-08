package PDV.PDV.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tb_entregas")
public class entregas {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne
    @JoinColumn(name = "pedido_id", referencedColumnName = "id")
    private pedido pedido;
    private String rua;
    private String numero;
    private String bairro;
    private String cep;
    private String complemento;



}
