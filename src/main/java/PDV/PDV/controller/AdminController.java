package PDV.PDV.controller;

import PDV.PDV.dto.ResultadoEntrega;
import PDV.PDV.model.Enum.DiaSemana;
import PDV.PDV.model.Enum.publicoCupom;
import PDV.PDV.model.Enum.statusPedido;
import PDV.PDV.model.Enum.tipoCupom;
import PDV.PDV.repository.clienteRepository;
import PDV.PDV.repository.pedidoRepository;
import PDV.PDV.repository.produtoRepository;
import PDV.PDV.service.ImpressaoService;
import PDV.PDV.service.configuracaoService;
import PDV.PDV.service.cupomService;
import PDV.PDV.service.entregaService;
import PDV.PDV.service.horarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private pedidoRepository pedidoRepository;

    @Autowired
    private clienteRepository clienteRepository;

    @Autowired
    private produtoRepository produtoRepository;

    @Autowired
    private entregaService entregaService;

    @Autowired
    private cupomService cupomService;

    @Autowired
    private horarioService horarioService;

    @Autowired
    private ImpressaoService impressaoService;

    @Autowired
    private configuracaoService configuracaoService;

    @GetMapping("/cupons")
    public String cupons(Model model) {
        model.addAttribute("cupons", cupomService.listar());
        model.addAttribute("tipos", tipoCupom.values());
        model.addAttribute("publicos", publicoCupom.values());
        return "admin/cupons";
    }

    @PostMapping("/cupons")
    public String criarCupom(@RequestParam("codigo") String codigo,
                             @RequestParam(value = "descricao", required = false) String descricao,
                             @RequestParam("tipo") tipoCupom tipo,
                             @RequestParam(value = "valor", required = false) BigDecimal valor,
                             @RequestParam(value = "valorMinimo", required = false) BigDecimal valorMinimo,
                             @RequestParam(value = "publico", required = false) publicoCupom publico,
                             RedirectAttributes redirectAttributes) {
        try {
            cupomService.criar(codigo, descricao, tipo, valor, valorMinimo, publico);
            redirectAttributes.addFlashAttribute("sucesso", "Cupom criado.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro",
                    e.getMessage() != null ? e.getMessage() : "Não foi possível criar o cupom.");
        }
        return "redirect:/admin/cupons";
    }

    @PostMapping("/cupons/{id}/excluir")
    public String excluirCupom(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            cupomService.remover(id);
            redirectAttributes.addFlashAttribute("sucesso", "Cupom removido.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Não foi possível remover o cupom.");
        }
        return "redirect:/admin/cupons";
    }

    @GetMapping("/configuracoes")
    public String configuracoes(Model model) {
        model.addAttribute("horarios", horarioService.listar());
        model.addAttribute("impressoras", impressaoService.listarImpressoras());
        model.addAttribute("impressoraSelecionada", configuracaoService.obterImpressoraNome());
        return "admin/configuracoes";
    }

    @GetMapping("/horarios")
    public String horariosLegado() {
        return "redirect:/admin/configuracoes";
    }

    @PostMapping("/horarios")
    public String salvarHorarios(@RequestParam(value = "aberto", required = false) List<DiaSemana> diasAbertos,
                                 @RequestParam Map<String, String> form,
                                 RedirectAttributes redirectAttributes) {
        try {
            horarioService.salvarTodos(diasAbertos, form);
            redirectAttributes.addFlashAttribute("sucesso", "Horários de funcionamento atualizados.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro",
                    e.getMessage() != null ? e.getMessage() : "Não foi possível salvar os horários.");
        }
        return "redirect:/admin/configuracoes";
    }

    @GetMapping("/impressora")
    public String impressoraLegado() {
        return "redirect:/admin/configuracoes";
    }

    @PostMapping("/impressora")
    public String salvarImpressora(@RequestParam("impressora") String impressora,
                                   RedirectAttributes redirectAttributes) {
        try {
            configuracaoService.atualizarImpressoraNome(impressora);
            redirectAttributes.addFlashAttribute("sucesso", "Impressora configurada.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro",
                    e.getMessage() != null ? e.getMessage() : "Não foi possível salvar a impressora.");
        }
        return "redirect:/admin/configuracoes";
    }

    @PostMapping("/impressora/teste")
    public String testarImpressora(@RequestParam("impressora") String impressora,
                                   RedirectAttributes redirectAttributes) {
        try {
            impressaoService.imprimirTeste(impressora);
            redirectAttributes.addFlashAttribute("sucesso", "Teste enviado para a impressora.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro",
                    e.getMessage() != null ? e.getMessage() : "Não foi possível imprimir o teste.");
        }
        return "redirect:/admin/configuracoes";
    }

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
    public String entregas() {
        return "redirect:/pedidos/entregas";
    }

    @GetMapping("/area-entrega")
    public String areaEntrega(Model model) {
        model.addAttribute("bairros", entregaService.listarBairros());
        model.addAttribute("enderecoOrigem", entregaService.getEnderecoOrigem());
        return "admin/area-entrega";
    }

    @PostMapping("/area-entrega/bairro")
    public String adicionarBairro(@RequestParam("nome") String nome,
                                  @RequestParam("taxaEntrega") String taxaEntrega,
                                  @RequestParam(value = "tempoEntrega", required = false) String tempoEntrega,
                                  RedirectAttributes redirectAttributes) {
        try {
            entregaService.adicionarBairro(nome, taxaEntrega, tempoEntrega);
            redirectAttributes.addFlashAttribute("sucesso", "Bairro adicionado.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro",
                    e.getMessage() != null ? e.getMessage() : "Informe valores válidos.");
        }
        return "redirect:/admin/area-entrega";
    }

    @PostMapping("/area-entrega/bairro/{id}/editar")
    public String editarBairro(@PathVariable("id") Long id,
                               @RequestParam("nome") String nome,
                               @RequestParam("taxaEntrega") String taxaEntrega,
                               @RequestParam(value = "tempoEntrega", required = false) String tempoEntrega,
                               RedirectAttributes redirectAttributes) {
        try {
            entregaService.editarBairro(id, nome, taxaEntrega, tempoEntrega);
            redirectAttributes.addFlashAttribute("sucesso", "Bairro atualizado.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro",
                    e.getMessage() != null ? e.getMessage() : "Não foi possível atualizar o bairro.");
        }
        return "redirect:/admin/area-entrega";
    }

    @PostMapping("/area-entrega/bairro/{id}/excluir")
    public String removerBairro(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            entregaService.removerBairro(id);
            redirectAttributes.addFlashAttribute("sucesso", "Bairro removido.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Não foi possível remover o bairro.");
        }
        return "redirect:/admin/area-entrega";
    }

    @GetMapping("/area-entrega/consultar")
    @ResponseBody
    public ResponseEntity<?> consultarCep(@RequestParam("cep") String cep) {
        try {
            return ResponseEntity.ok(entregaService.consultarEntrega(cep));
        } catch (Exception e) {
            ResultadoEntrega r = new ResultadoEntrega();
            r.setSucesso(false);
            r.setMensagem(e.getMessage() != null ? e.getMessage() : "Erro ao consultar o CEP.");
            return ResponseEntity.badRequest().body(r);
        }
    }
}
