package PDV.PDV.controller;

import PDV.PDV.dto.ComandaPendente;
import PDV.PDV.model.pedido;
import PDV.PDV.repository.pedidoRepository;
import PDV.PDV.service.ImpressaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/impressao")
public class ImpressaoBridgeController {

    private final pedidoRepository pedidoRepository;
    private final ImpressaoService impressaoService;

    public ImpressaoBridgeController(pedidoRepository pedidoRepository, ImpressaoService impressaoService) {
        this.pedidoRepository = pedidoRepository;
        this.impressaoService = impressaoService;
    }

    @GetMapping("/pendentes")
    public List<ComandaPendente> pendentes() {
        List<pedido> pendentes = pedidoRepository.findPendentesImpressao();
        List<ComandaPendente> resultado = new ArrayList<>();
        for (pedido p : pendentes) {
            String texto = impressaoService.montarTextoPedido(p.getId());
            ComandaPendente c = new ComandaPendente();
            c.setId(p.getId());
            c.setTextoBase64(Base64.getEncoder().encodeToString(texto.getBytes(StandardCharsets.UTF_8)));
            resultado.add(c);
        }
        return resultado;
    }

    @PostMapping("/{id}/concluido")
    public ResponseEntity<?> concluido(@PathVariable("id") Long id) {
        int marcado = pedidoRepository.marcarComoImpresso(id);
        return ResponseEntity.ok(Map.of("id", id, "marcado", marcado));
    }
}
