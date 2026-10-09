package PDV.PDV.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Vínculo manual entre um item do catálogo da iFood e um produto do PDV.
 * A chave usada para casar é o {@code ifoodItemId} (id estável do item na iFood);
 * {@code ifoodItemNome} é guardado apenas como referência visual na tela.
 */
@Data
@Entity
@Table(name = "tb_ifood_vinculo")
public class ifoodVinculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ifood_item_id", length = 64, nullable = false)
    private String ifoodItemId;

    @Column(name = "ifood_item_nome", length = 160)
    private String ifoodItemNome;

    @ManyToOne
    @JoinColumn(name = "produto_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private produtos produto;
}
