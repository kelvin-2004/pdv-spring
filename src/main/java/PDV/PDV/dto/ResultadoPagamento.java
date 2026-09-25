package PDV.PDV.dto;

import lombok.Data;

@Data
public class ResultadoPagamento {
    private boolean sucesso;
    private String status;
    private String statusDetail;
    private Long paymentId;
    private Long pedidoId;
    private String qrCodeBase64;
    private String copiaECola;
    private Long expiracaoPagamento;
    private boolean pago;
    private String mensagem;
}
