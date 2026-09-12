package PDV.PDV.model;

import PDV.PDV.model.Enum.formaPagamento;
import PDV.PDV.model.Enum.origemPedido;
import PDV.PDV.model.Enum.statusPedido;
import PDV.PDV.model.Enum.tipoLogistico;
import PDV.PDV.model.Enum.tipoPedido;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "tb_pedidos")
public class pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_pedido_cliente")
    private Integer numeroPedidoCliente;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private clientes cliente;

    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL)
    private entregas entregas;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<itensPedido> itens;

    @Enumerated(EnumType.STRING)
    private tipoPedido tipoPedido;

    @Enumerated(EnumType.STRING)
    private tipoLogistico tipoLogistica;

    @Enumerated(EnumType.STRING)
    private statusPedido statusPedido;

    @Enumerated(EnumType.STRING)
    private origemPedido origemPedido;

    @Enumerated(EnumType.STRING)
    private formaPagamento formaPagamento;


    private BigDecimal valorTotal;

    @Column(name = "data_hora_pedido", nullable = false, updatable = false)
    private OffsetDateTime dataHoraPedido = OffsetDateTime.now();

    private BigDecimal taxaEntrega;

    @Column(precision = 10, scale = 2)
    private BigDecimal trocoPara;

    public BigDecimal getTrocoPara() {
        return trocoPara;
    }

    public void setTrocoPara(BigDecimal trocoPara) {
        this.trocoPara = trocoPara;
    }
}
