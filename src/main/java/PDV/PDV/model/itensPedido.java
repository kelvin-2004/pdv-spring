package PDV.PDV.model;

import jakarta.persistence.*;
import lombok.Data;

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
    private pedido Pedidos;
    @ManyToOne
    @JoinColumn(name = "produto_id")
    private produtos produto;

    private Integer quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal subtotal;
    private String observacao;



}
