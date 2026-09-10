package PDV.PDV.controller;

import PDV.PDV.service.itensPedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/itens-pedido")
public class itensPedidoController {

    @Autowired
    private itensPedidoService itensPedidoService;

    // Métodos auxiliares do carrinho / itens do pedido
}