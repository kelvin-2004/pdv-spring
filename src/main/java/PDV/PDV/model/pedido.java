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

@Data
@Entity
@Table(name = "tb_pedidos")
public class pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Conecta com o Cliente
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private clientes cliente;

    // Mapeamento bidirecional: a FK "pedido_id" ficará na tabela tb_entregas
    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL)
    private entregas entregas;

    @Enumerated(EnumType.STRING)
    private tipoPedido tipoPedido; // retirada ou delivery

    @Enumerated(EnumType.STRING)
    private tipoLogistico tipoLogistica; // própria ou parceira

    @Enumerated(EnumType.STRING)
    private statusPedido statusPedido; // preparando, despachado, cancelado

    @Enumerated(EnumType.STRING)
    private origemPedido origemPedido; // WhatsApp, iFood, etc.

    @Enumerated(EnumType.STRING)
    private formaPagamento formaPagamento;

    private BigDecimal valorTotal;

    @Column(name = "data_hora_pedido", nullable = false, updatable = false)
    private OffsetDateTime dataHoraPedido = OffsetDateTime.now();
}