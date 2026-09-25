package PDV.PDV.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MensagemDTO {
    private Long id;
    private String remetente;
    private String texto;
    private long dataHoraEpochMillis;
    private boolean lida;
}
