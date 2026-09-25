package PDV.PDV.model;

import PDV.PDV.model.Enum.remetenteMensagem;
import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "tb_mensagens_pedido")
public class mensagemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false)
    private pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "remetente", columnDefinition = "varchar(20)", nullable = false)
    private remetenteMensagem remetente;

    @Column(name = "texto", length = 2000, nullable = false)
    private String texto;

    @Column(name = "data_hora", nullable = false)
    private OffsetDateTime dataHora = OffsetDateTime.now();

    @Column(name = "lida")
    private Boolean lida = false;
}
