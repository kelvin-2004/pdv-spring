package PDV.PDV.dto;

import PDV.PDV.model.produtos;
import lombok.Data;

import java.util.List;

@Data
public class SecaoSubcategoria {
    private String nome;
    private Boolean escuro;
    private Boolean bebida;
    private List<produtos> produtos;
}
