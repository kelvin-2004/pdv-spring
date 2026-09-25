package PDV.PDV.model.Enum;

public enum tipoCupom {
    DESCONTO_FIXO("Desconto fixo (R$)"),
    FRETE_GRATIS("Frete grátis"),
    DESCONTO_ENTREGA("Desconto na entrega (R$)");

    private final String descricao;

    tipoCupom(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
