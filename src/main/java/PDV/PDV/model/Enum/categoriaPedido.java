package PDV.PDV.model.Enum;

public enum categoriaPedido {
    MARMITA("Marmitas"),
    BEBIDA("Bebidas"),
    SOBREMESA("Sobremesas"),
    EXCECAO("Exceções"),
    COMBO("Combos");

    private final String label;

    categoriaPedido(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
