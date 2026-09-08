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
    private String celular;


}
