package PDV.PDV.controller;

import PDV.PDV.model.itensPedido;
import PDV.PDV.service.itensPedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/itens-pedido")
public class itensPedidoController {

    @Autowired
    private itensPedidoService itensPedidoService;

    // Exemplo de método auxiliar para buscar itens por ID do pedido
    @GetMapping("/pedido/{pedidoId}")
    public List<itensPedido> listarPorPedido(@PathVariable Long pedidoId) {
        return itensPedidoService.listarPorPedido(pedidoId);
    }
}