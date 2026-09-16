package PDV.PDV.dto;

import java.math.BigDecimal;

public class ResumoProdutoVenda {
    private final String nome;
    private final Long quantidade;
    private final BigDecimal faturamento;

    public ResumoProdutoVenda(String nome, Long quantidade, BigDecimal faturamento) {
        this.nome = nome;
        this.quantidade = quantidade;
        this.faturamento = faturamento;
    }

    public String getNome() {
        return nome;
    }

    public Long getQuantidade() {
        return quantidade;
    }

    public BigDecimal getFaturamento() {
        return faturamento;
    }
}
