package PDV.PDV.controller;

import PDV.PDV.model.clientes;
import PDV.PDV.model.pedido;
import PDV.PDV.service.horarioService;
import PDV.PDV.service.pedidoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice(assignableTypes = PublicoController.class)
public class PublicoControllerAdvice {

    @Autowired
    private pedidoService pedidoService;

    @Autowired
    private horarioService horarioService;

    @Value("${loja.endereco-linha1:R. Quinze, 292 - Jardim Vassouras}")
    private String lojaLinha1;

    @Value("${loja.endereco-linha2:Francisco Morato - SP, 07953-170}")
    private String lojaLinha2;

    @Value("${loja.maps-url:https://maps.app.goo.gl/JrmmR3oY83w2QjKU7}")
    private String lojaMapsUrl;

    @ModelAttribute("pedidoPendente")
    public pedido pedidoPendente(HttpSession session) {
        Object clienteObj = session.getAttribute("clientePublico");
        if (!(clienteObj instanceof clientes cliente)) {
            return null;
        }
        return pedidoService.pedidoPendentePagamento(cliente);
    }

    @ModelAttribute("loja")
    public Map<String, String> loja() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("linha1", lojaLinha1);
        m.put("linha2", lojaLinha2);
        m.put("maps", lojaMapsUrl);
        return m;
    }

    @ModelAttribute("lojaAberta")
    public boolean lojaAberta() {
        return horarioService.estaAbertoAgora();
    }

    @ModelAttribute("lojaStatus")
    public String lojaStatus() {
        return horarioService.statusAgora();
    }
}
