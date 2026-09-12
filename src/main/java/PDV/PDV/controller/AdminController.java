package PDV.PDV.controller;

import PDV.PDV.repository.clienteRepository;
import PDV.PDV.repository.pedidoRepository;
import PDV.PDV.repository.produtoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private pedidoRepository pedidoRepository;

    @Autowired
    private clienteRepository clienteRepository;

    @Autowired
    private produtoRepository produtoRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalClientes", clienteRepository.count());
        model.addAttribute("totalProdutos", produtoRepository.count());
        model.addAttribute("totalPedidos", pedidoRepository.count());
        return "admin/dashboard";
    }

    @GetMapping("/entregas")
    public String entregas(Model model) {
        BigDecimal totalTaxas = pedidoRepository.somarTotalTaxasEntrega();
        if (totalTaxas == null) {
            totalTaxas = BigDecimal.ZERO;
        }
        model.addAttribute("pedidos", pedidoRepository.findAll());
        model.addAttribute("totalTaxas", totalTaxas);
        return "admin/entregas";
    }
}