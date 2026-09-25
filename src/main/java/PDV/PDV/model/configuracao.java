package PDV.PDV.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tb_configuracao")
@Data
public class configuracao {

    @Id
    private Long id;

    @Column(name = "tempo_preparo_padrao_minutos")
    private Integer tempoPreparoPadraoMinutos;

    @Column(name = "tempo_limite_pagamento_minutos")
    private Integer tempoLimitePagamentoMinutos;

    @Column(name = "impressora_nome")
    private String impressoraNome;
}
