package PDV.PDV.model.Enum;

public enum publicoCupom {
    TODOS("Todos os clientes"),
    PRIMEIRO_PEDIDO("Primeiro pedido"),
    CLIENTE_EXISTENTE("Cliente que já pediu");

    private final String descricao;

    publicoCupom(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
