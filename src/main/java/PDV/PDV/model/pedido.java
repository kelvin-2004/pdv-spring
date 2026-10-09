package PDV.PDV.model;

import PDV.PDV.model.Enum.formaPagamento;
import PDV.PDV.model.Enum.origemPedido;
import PDV.PDV.model.Enum.statusPedido;
import PDV.PDV.model.Enum.tipoLogistico;
import PDV.PDV.model.Enum.tipoPedido;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private entregas entregas;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<itensPedido> itens;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(50)")
    private tipoPedido tipoPedido;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(50)")
    private tipoLogistico tipoLogistica;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(50)")
    private statusPedido statusPedido;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(50)")
    private origemPedido origemPedido;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(50)")
    private formaPagamento formaPagamento;


    private BigDecimal valorTotal;

    @Column(name = "data_hora_pedido", nullable = false, updatable = false)
    private OffsetDateTime dataHoraPedido = OffsetDateTime.now();

    private BigDecimal taxaEntrega;

    @Column(precision = 10, scale = 2)
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(name = "cupom_codigo", length = 50)
    private String cupomCodigo;

    @Column(name = "tempo_entrega_minutos")
    private Integer tempoEntregaMinutos;

    @Column(name = "tempo_preparo_minutos")
    private Integer tempoPreparoMinutos;

    @Column(precision = 10, scale = 2)
    private BigDecimal trocoPara;

    @Column(length = 1000)
    private String observacoes;

    @Column(name = "pagamento_mp_id")
    private Long pagamentoMpId;

    @Column(name = "data_expiracao_pagamento")
    private OffsetDateTime dataExpiracaoPagamento;

    @Column(name = "data_inicio_preparo")
    private OffsetDateTime dataInicioPreparo;

    @Column(name = "pago")
    private Boolean pago = false;

    @Column(name = "impresso")
    private Boolean impresso = false;

    // Sinaliza um pedido de reimpressão manual (botão "Imprimir" do gestor). Permite
    // reimprimir pedidos em qualquer status (ex.: cancelado), ao contrário de "impresso"
    // que é usado apenas no fluxo de impressão automática na chegada (status PREPARANDO).
    @Column(name = "reimpressao")
    private Boolean reimpressao = false;

    @Column(name = "pagamento_na_entrega")
    private Boolean pagamentoNaEntrega = false;

    // Integração iFood: id do pedido na plataforma (dedupe no polling) e o número
    // curto exibido ao cliente (displayId, ex.: "#1234").
    @Column(name = "ifood_pedido_id", length = 64)
    private String ifoodPedidoId;

    @Column(name = "ifood_referencia", length = 32)
    private String ifoodReferencia;

    // Pedidos de origem externa (iFood) não têm um clientes cadastrado: guardamos
    // nome/telefone direto do payload para exibição e contato no gestor.
    @Column(name = "cliente_nome_externo", length = 120)
    private String clienteNomeExterno;

    @Column(name = "cliente_telefone_externo", length = 30)
    private String clienteTelefoneExterno;

    // Integração 99Food: id do pedido na plataforma (dedupe no polling) e o número
    // curto exibido ao operador. dataPrazoEntrega guarda o prazo/ETA de entrega da 99
    // (usado para destacar no painel os pedidos com entrega da própria 99).
    @Column(name = "n99_pedido_id", length = 64)
    private String n99PedidoId;

    @Column(name = "n99_referencia", length = 32)
    private String n99Referencia;

    @Column(name = "data_prazo_entrega")
    private OffsetDateTime dataPrazoEntrega;

    public BigDecimal getTrocoPara() {
        return trocoPara;
    }

    public void setTrocoPara(BigDecimal trocoPara) {
        this.trocoPara = trocoPara;
    }

    @Transient
    public OffsetDateTime getDataHoraLocal() {
        if (dataHoraPedido == null) {
            return null;
        }
        ZoneId zonaComercial = ZoneId.of("America/Sao_Paulo");
        return dataHoraPedido.withOffsetSameInstant(
                zonaComercial.getRules().getOffset(dataHoraPedido.toInstant()));
    }

    @Transient
    public String getNomeClienteExibicao() {
        if (cliente != null && cliente.getNome() != null && !cliente.getNome().isBlank()) {
            return cliente.getNome();
        }
        if (clienteNomeExterno != null && !clienteNomeExterno.isBlank()) {
            return clienteNomeExterno;
        }
        return "Cliente";
    }

    @Transient
    public long getExpiracaoPagamentoEpochMillis() {
        if (dataExpiracaoPagamento == null) {
            return 0L;
        }
        return dataExpiracaoPagamento.toInstant().toEpochMilli();
    }

    @Transient
    public long getPrazoPreparoEpochMillis() {
        OffsetDateTime inicio = dataInicioPreparo != null ? dataInicioPreparo : dataHoraPedido;
        if (inicio == null) {
            return 0L;
        }
        int preparo = tempoPreparoMinutos != null ? tempoPreparoMinutos : 30;
        return inicio.plusMinutes(preparo).toInstant().toEpochMilli();
    }

    @Transient
    public long getPrazoEntregaEpochMillis() {
        if (dataPrazoEntrega == null) {
            return 0L;
        }
        return dataPrazoEntrega.toInstant().toEpochMilli();
    }

    @Transient
    public int getProgressoStatus() {
        if (statusPedido == null) {
            return 0;
        }
        return switch (statusPedido) {
            case AGUARDANDO_PAGAMENTO -> 15;
            case PREPARANDO -> 40;
            case AGUARDANDO_ENTREGADOR -> 65;
            case A_CAMINHO -> 85;
            case CONCLUIDO -> 100;
            case CANCELADO -> 0;
        };
    }

    @Transient
    public String getDescricaoStatus() {
        if (statusPedido == null) {
            return "";
        }
        return switch (statusPedido) {
            case AGUARDANDO_PAGAMENTO -> "Aguardando pagamento";
            case PREPARANDO -> "Preparando seu pedido";
            case AGUARDANDO_ENTREGADOR -> this.tipoPedido == PDV.PDV.model.Enum.tipoPedido.RETIRADA
                    ? "Pronto, aguardando retirada" : "Pronto, aguardando entregador";
            case A_CAMINHO -> "Pedido a caminho";
            case CONCLUIDO -> "Pedido concluído";
            case CANCELADO -> "Pedido cancelado";
        };
    }
}
