package PDV.PDV.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResultadoEntrega {
    private boolean sucesso;
    private String mensagem;
    private String cep;
    private String logradouro;
    private String bairro;
    private String cidade;
    private double distanciaKm;
    private boolean dentroDaArea;
    private String bairroAplicado;
    private BigDecimal valorEntrega;
    private String tempoEntrega;
    private boolean taxaPadrao;
}
