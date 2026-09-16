package PDV.PDV.controller;

import PDV.PDV.model.Enum.statusPedido;
import PDV.PDV.repository.clienteRepository;
import PDV.PDV.repository.pedidoRepository;
import PDV.PDV.repository.produtoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

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
    public String dashboard(
            @RequestParam(value = "dataInicial", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(value = "dataFinal", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(value = "status", required = false) String status,
            Model model) {
        ZoneId zona = ZoneId.systemDefault();
        LocalDate hoje = LocalDate.now(zona);
        dataInicial = dataInicial != null ? dataInicial : hoje;
        dataFinal = dataFinal != null ? dataFinal : hoje;
        if (dataFinal.isBefore(dataInicial)) {
            LocalDate temp = dataInicial;
            dataInicial = dataFinal;
            dataFinal = temp;
        }

        statusPedido statusFiltro = null;
        if (status != null && !status.isBlank() && !"TODOS".equalsIgnoreCase(status)) {
            try {
                statusFiltro = statusPedido.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                status = "TODOS";
            }
        }
        if (status == null || status.isBlank()) {
            status = "TODOS";
        }

        var inicio = dataInicial.atStartOfDay(zona).toOffsetDateTime();
        var fim = dataFinal.plusDays(1).atStartOfDay(zona).toOffsetDateTime();
        model.addAttribute("totalClientes", clienteRepository.count());
        model.addAttribute("totalProdutos", produtoRepository.count());
        model.addAttribute("totalPedidos", pedidoRepository.contarPedidosPeriodo(inicio, fim, statusFiltro));
        model.addAttribute("totalMarmitas", pedidoRepository.contarMarmitasPeriodo(inicio, fim, statusFiltro));
        model.addAttribute("totalVendas", pedidoRepository.somarVendasPeriodo(inicio, fim, statusFiltro));
        model.addAttribute("faturamentoPratos", pedidoRepository.somarItensPeriodo(inicio, fim, statusFiltro));
        model.addAttribute("vendasPorProduto", pedidoRepository.resumirVendasPorProduto(inicio, fim, statusFiltro));
        model.addAttribute("dataInicial", dataInicial);
        model.addAttribute("dataFinal", dataFinal);
        model.addAttribute("statusSelecionado", status);
        model.addAttribute("statusDisponiveis", statusPedido.values());
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