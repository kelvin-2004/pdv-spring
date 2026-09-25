package PDV.PDV.model;
import PDV.PDV.model.Enum.categoriaPedido;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_produtos")
@Data
public class produtos {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @Column(nullable = false)
    private  String nome;
    private String descricao;
    private String subcategoria;
    @Column(nullable = false)
    private BigDecimal preco;
    @Column(name = "categoria", nullable = false, columnDefinition = "varchar(50)")
    @Enumerated(EnumType.STRING)
    private categoriaPedido CategoriaPedido;
    @Column(name = "disponivel")
    private Boolean ativo = true;
    @Column(name = "caminhoImg")
    private String imgUrl;
    @Column(nullable = false)
    private Integer estoque = 0;



}
