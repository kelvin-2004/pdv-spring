package PDV.PDV.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tb_clientes")
@Data
public class clientes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    @Column(unique = true)
    private String celular;
    private String cep;
    private String rua;
    private String Bairro;
    private String numero;
    private String Complemento;
    private String pontoReferencia;


}
