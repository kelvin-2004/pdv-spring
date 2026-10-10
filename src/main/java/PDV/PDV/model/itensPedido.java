package PDV.PDV.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "tb_itensPedido")
public class itensPedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pedidos_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private pedido pedido;

    @ManyToOne
    @JoinColumn(name = "produto_id")
    private produtos produto;

    private Integer quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal subtotal;
    private String observacao;

    // Nome do item como veio da plataforma (iFood/99), preservado quando não há vínculo
    // com um produto do PDV — evita que a comanda mostre "Item" genérico.
    private String nome;
}