package PDV.PDV.controller;

import PDV.PDV.model.Enum.statusPedido;
import PDV.PDV.service.clienteService;
import PDV.PDV.service.produtoService;
import PDV.PDV.service.pedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/pedidos")
public class pedidoController {

    @Autowired
    private clienteService clienteService;

    @Autowired
    private produtoService produtoService;

    @Autowired
    private pedidoService pedidoService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("clientes", clienteService.listarTodos());
        model.addAttribute("produtos", produtoService.listarAtivos());
        return "pedidos/index"; // Aponta para src/main/resources/templates/pedidos/index.html
    }

    @GetMapping("/gerenciar")
    public String gerenciarPedidos(Model model) {
        model.addAttribute("preparando", pedidoService.listarStatus(statusPedido.PREPARANDO));
        model.addAttribute("prontos", pedidoService.listarStatus(statusPedido.AGUARDANDO_ENTREGADOR));
        model.addAttribute("aCaminho", pedidoService.listarStatus(statusPedido.A_CAMINHO));
        model.addAttribute("concluidos", pedidoService.listarStatus(statusPedido.CONCLUIDO));
        model.addAttribute("cancelados", pedidoService.listarStatus(statusPedido.CANCELADO));
        return "pedidos/gerenciar";
    }
}