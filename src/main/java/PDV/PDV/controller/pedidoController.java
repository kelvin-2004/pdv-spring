package PDV.PDV.controller;

import PDV.PDV.model.Enum.statusPedido;
import PDV.PDV.model.clientes;
import PDV.PDV.model.pedido;
import PDV.PDV.repository.clienteRepository;
import PDV.PDV.service.ImpressaoService;
import PDV.PDV.service.clienteService;
import PDV.PDV.service.produtoService;
import PDV.PDV.service.pedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.format.annotation.DateTimeFormat;

@Controller
@RequestMapping("/pedidos")
public class pedidoController {

    @Autowired
    private clienteService clienteService;

    @Autowired
    private clienteRepository clienteRepo;

    @Autowired
    private produtoService produtoService;

    @Autowired
    private pedidoService pedidoService;

    @Autowired
    private ImpressaoService impressaoService; // Injeção correta do serviço de impressão

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("clientes", clienteService.listarTodos());
        model.addAttribute("produtos", produtoService.listarAtivos());
        return "pedidos/index";
    }

    @GetMapping("/gerenciar")
    public String gerenciarPedidos(Model model) {
        model.addAttribute("preparando", pedidoService.listarStatusDoDia(statusPedido.PREPARANDO));
        model.addAttribute("aguardandoEntregador", pedidoService.listarStatusDoDia(statusPedido.AGUARDANDO_ENTREGADOR));
        model.addAttribute("aCaminho", pedidoService.listarStatusDoDia(statusPedido.A_CAMINHO));
        model.addAttribute("concluidos", pedidoService.listarStatusDoDia(statusPedido.CONCLUIDO));
        model.addAttribute("cancelados", pedidoService.listarStatusDoDia(statusPedido.CANCELADO));
        return "pedidos/gerenciar";
    }

    @GetMapping("/entregas")
    public String paginaEntregas(
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

        BigDecimal taxaMinima = BigDecimal.valueOf(5);
        var entregas = pedidoService.listarEntregas(
                taxaMinima,
                dataInicial.atStartOfDay(zona).toOffsetDateTime(),
                dataFinal.plusDays(1).atStartOfDay(zona).toOffsetDateTime(),
                statusFiltro);

        BigDecimal totalTaxas = entregas.stream()
                .map(pedido::getTaxaEntrega)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("entregas", entregas);
        model.addAttribute("totalTaxas", totalTaxas);
        model.addAttribute("dataInicial", dataInicial);
        model.addAttribute("dataFinal", dataFinal);
        model.addAttribute("statusSelecionado", status);
        model.addAttribute("statusDisponiveis", statusPedido.values());
        model.addAttribute("taxaMinima", taxaMinima);
        return "admin/entregas";
    }

    @PostMapping("/etapa2")
    public String receberEtapa1(
            @RequestParam("telefone") String telefone,
            @RequestParam("nomeCliente") String nomeCliente,
            @RequestParam("cep") String cep,
            @RequestParam("rua") String rua,
            @RequestParam("numero") String numero,
            @RequestParam("bairro") String bairro,
            @RequestParam(value = "complemento", required = false) String complemento,
            Model model) {

        String telefoneLimpo = telefone != null ? telefone.replaceAll("\\D", "") : "";

        clientes clienteExistente = clienteRepo.findByCelular(telefoneLimpo).orElse(null);

        if (clienteExistente == null && !telefoneLimpo.isEmpty()) {
            clientes novoCliente = new clientes();
            novoCliente.setCelular(telefoneLimpo);
            novoCliente.setNome(nomeCliente);
            novoCliente.setCep(cep);
            novoCliente.setRua(rua);
            novoCliente.setNumero(numero);
            novoCliente.setBairro(bairro);
            novoCliente.setComplemento(complemento);

            clienteRepo.save(novoCliente);
        }

        model.addAttribute("telefone", telefone);
        model.addAttribute("nomeCliente", nomeCliente);
        model.addAttribute("cep", cep);
        model.addAttribute("rua", rua);
        model.addAttribute("numero", numero);
        model.addAttribute("bairro", bairro);
        model.addAttribute("complemento", complemento);

        model.addAttribute("produtos", produtoService.listarAtivos());
        model.addAttribute("categorias", PDV.PDV.model.Enum.categoriaPedido.values());

        return "pedidos/etapa2";
    }

    @GetMapping("/etapa2")
    public String etapa2(Model model) {
        model.addAttribute("produtos", produtoService.listarAtivos());
        model.addAttribute("categorias", PDV.PDV.model.Enum.categoriaPedido.values());
        return "pedidos/etapa2";
    }

    @PostMapping("/etapa3")
    public String receberEtapa2(
            @RequestParam("nomeCliente") String nomeCliente,
            @RequestParam("telefone") String telefone,
            @RequestParam("cep") String cep,
            @RequestParam("rua") String rua,
            @RequestParam("numero") String numero,
            @RequestParam(value = "bairro", required = false) String bairro,
            @RequestParam(value = "complemento", required = false) String complemento,
            @RequestParam("valorItens") Double valorItens,
            @RequestParam(value = "carrinhoJson", required = false) String carrinhoJson,
            @RequestParam(value = "observacoes", required = false) String observacoes,
            Model model) {

        model.addAttribute("nomeCliente", nomeCliente);
        model.addAttribute("telefone", telefone);
        model.addAttribute("cep", cep);
        model.addAttribute("rua", rua);
        model.addAttribute("numero", numero);
        model.addAttribute("bairro", bairro);
        model.addAttribute("complemento", complemento);
        model.addAttribute("valorItens", valorItens);
        model.addAttribute("carrinhoJson", carrinhoJson);
        model.addAttribute("observacoes", observacoes);

        return "pedidos/etapa3";
    }

    @GetMapping("/etapa3")
    public String etapa3(Model model) {
        return "pedidos/etapa3";
    }

    @PostMapping("/etapa4-preview")
    public String mostrarPreviewEtapa4(
            @RequestParam("nomeCliente") String nomeCliente,
            @RequestParam("telefone") String telefone,
            @RequestParam("cep") String cep,
            @RequestParam("rua") String rua,
            @RequestParam("numero") String numero,
            @RequestParam(value = "bairro", required = false) String bairro,
            @RequestParam(value = "complemento", required = false) String complemento,
            @RequestParam("valorItens") Double valorItens,
            @RequestParam("taxaEntrega") Double taxaEntrega,
            @RequestParam("formaPagamento") String formaPagamento,
            @RequestParam(value = "trocoPara", required = false) String trocoPara,
            @RequestParam(value = "carrinhoJson", required = false) String carrinhoJson,
            @RequestParam(value = "observacoes", required = false) String observacoes,
            Model model) {

        Double totalGeral = valorItens + taxaEntrega;

        StringBuilder sb = new StringBuilder();
        sb.append("*MARMITAS SOUSA - RESUMO DO PEDIDO*\n\n");
        sb.append("Olá, *").append(nomeCliente).append("*! Segue o resumo do seu pedido:\n\n");

        if (carrinhoJson != null && !carrinhoJson.isBlank()) {
            try {
                JsonNode carrinho = objectMapper.readTree(carrinhoJson);
                if (carrinho.isArray()) {
                    sb.append("*Itens do pedido:*\n");
                    for (JsonNode item : carrinho) {
                        String nome = item.hasNonNull("nome") && !item.get("nome").asText().isBlank()
                            ? item.get("nome").asText() : "Produto";
                        int quantidade = item.path("quantidade").asInt(0);
                        double preco = item.path("preco").asDouble(0);
                        sb.append(quantidade).append("x ").append(nome)
                                .append(" - R$ ")
                                .append(String.format("%.2f", preco * quantidade))
                                .append("\n");

                        String observacao = item.hasNonNull("observacao") ? item.get("observacao").asText() : "";
                        if (!observacao.isBlank()) {
                            sb.append("   Obs: ").append(observacao).append("\n");
                        }
                    }
                    sb.append("\n");
                }
            } catch (Exception ignored) {
                // O resumo financeiro continua disponível mesmo se o carrinho estiver inválido.
            }
        }

        sb.append("Subtotal dos Itens: R$ ").append(String.format("%.2f", valorItens)).append("\n");
        sb.append("Taxa de Entrega: R$ ").append(String.format("%.2f", taxaEntrega)).append("\n");
        sb.append("*Total Geral: R$ ").append(String.format("%.2f", totalGeral)).append("*\n\n");

        sb.append("Forma de Pagamento: ").append(formaPagamento).append("\n");
        if ("Dinheiro".equals(formaPagamento) && trocoPara != null && !trocoPara.trim().isEmpty()) {
            sb.append("Troco para: R$ ").append(trocoPara).append("\n");
        }

        sb.append("\n*Endereço de Entrega:*\n");
        sb.append(rua).append(", ").append(numero);
        if (complemento != null && !complemento.trim().isEmpty()) {
            sb.append(" (").append(complemento).append(")");
        }
        if (bairro != null && !bairro.trim().isEmpty()) {
            sb.append(" - Bairro: ").append(bairro);
        }
        sb.append(" - CEP: ").append(cep).append("\n");

        sb.append("\nAgradecemos a preferência! 🍽️");

        model.addAttribute("mensagemWhatsapp", sb.toString());
        model.addAttribute("nomeCliente", nomeCliente);
        model.addAttribute("telefone", telefone);
        model.addAttribute("cep", cep);
        model.addAttribute("rua", rua);
        model.addAttribute("numero", numero);
        model.addAttribute("bairro", bairro);
        model.addAttribute("complemento", complemento);
        model.addAttribute("taxaEntrega", taxaEntrega);
        model.addAttribute("formaPagamento", formaPagamento);
        model.addAttribute("trocoPara", trocoPara);
        model.addAttribute("carrinhoJson", carrinhoJson);
        model.addAttribute("observacoes", observacoes);

        return "pedidos/etapa4-preview";
    }

    @PostMapping("/salvar")
    public String salvarPedido(
            @RequestParam("nomeCliente") String nomeCliente,
            @RequestParam("telefone") String telefone,
            @RequestParam("cep") String cep,
            @RequestParam("rua") String rua,
            @RequestParam("numero") String numero,
            @RequestParam(value = "bairro", required = false) String bairro,
            @RequestParam(value = "complemento", required = false) String complemento,
            @RequestParam("taxaEntrega") double taxaEntrega,
            @RequestParam("formaPagamento") String formaPagamento,
            @RequestParam(value = "trocoPara", required = false) String trocoPara,
            @RequestParam("carrinhoJson") String carrinhoJson,
            @RequestParam(value = "observacoes", required = false) String observacoes,
            @RequestParam(value = "imprimir", defaultValue = "false") boolean imprimir,
            RedirectAttributes redirectAttributes) {

        String telefoneLimpo = telefone != null ? telefone.replaceAll("\\D", "") : "";
        clientes cliente = clienteRepo.findByCelular(telefoneLimpo).orElse(null);

        if (cliente == null && !telefoneLimpo.isEmpty()) {
            cliente = new clientes();
            cliente.setCelular(telefoneLimpo);
            cliente.setNome(nomeCliente);
            cliente.setCep(cep);
            cliente.setRua(rua);
            cliente.setNumero(numero);
            cliente.setBairro(bairro);
            cliente.setComplemento(complemento);
            cliente = clienteRepo.save(cliente);
        }

        pedido novoPedido = new pedido();
        novoPedido.setCliente(cliente);
        novoPedido.setTaxaEntrega(BigDecimal.valueOf(taxaEntrega));
        novoPedido.setObservacoes(observacoes);

        novoPedido.setFormaPagamento(normalizarFormaPagamento(formaPagamento));

        if (trocoPara != null && !trocoPara.trim().isEmpty()) {
            try {
                novoPedido.setTrocoPara(new BigDecimal(trocoPara.replace(",", ".")));
            } catch (Exception ignored) {}
        }

        // Salva o pedido e recupera a instância contendo o ID gerado pelo banco
        pedido pedidoSalvo;
        try {
            pedidoSalvo = pedidoService.novoPedidoComItens(novoPedido, carrinhoJson);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/pedidos";
        }

        // Se a opção de imprimir foi marcada como true, executa o serviço de impressão
        if (imprimir && pedidoSalvo != null && pedidoSalvo.getId() != null) {
            try {
                impressaoService.imprimirPedido(pedidoSalvo.getId());
                redirectAttributes.addFlashAttribute("sucesso", "Pedido salvo e enviado para a impressora térmica com sucesso!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("erro", "Pedido salvo, mas ocorreu um erro ao imprimir: " + e.getMessage());
            }
        } else {
            redirectAttributes.addFlashAttribute("sucesso", "Pedido registrado com sucesso!");
        }

        return "redirect:/pedidos/gerenciar";
    }

    private PDV.PDV.model.Enum.formaPagamento normalizarFormaPagamento(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim().toUpperCase()
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U");
        if (normalizado.startsWith("CARTAO")) {
            return PDV.PDV.model.Enum.formaPagamento.CARTAO;
        }
        return switch (normalizado) {
            case "DINHEIRO" -> PDV.PDV.model.Enum.formaPagamento.DINHEIRO;
            case "PIX" -> PDV.PDV.model.Enum.formaPagamento.PIX;
            case "APP" -> PDV.PDV.model.Enum.formaPagamento.APP;
            default -> throw new IllegalArgumentException("Forma de pagamento inválida");
        };
    }

    @PatchMapping("/{id}/status")
    @ResponseBody
    public ResponseEntity<?> atualizarStatusPedido(@PathVariable Long id, @RequestParam("status") String novoStatusStr) {
        try {
            statusPedido novoStatus = statusPedido.valueOf(novoStatusStr.toUpperCase());
            pedidoService.atualizarStatus(id, novoStatus);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Status inválido");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao atualizar status: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/imprimir")
    public String imprimirPedido(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Chamada correta utilizando a instância do service injetada
            impressaoService.imprimirPedido(id);
            redirectAttributes.addFlashAttribute("sucesso", "Pedido enviado para a impressora com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao imprimir: " + e.getMessage());
        }

        return "redirect:/pedidos/gerenciar";
    }

    @GetMapping("/entregas/concluir/{id}")
    public String concluirEntrega(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            pedidoService.atualizarStatus(id, statusPedido.CONCLUIDO);
            redirectAttributes.addFlashAttribute("sucesso", "Entrega concluída com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Não foi possível concluir a entrega: " + e.getMessage());
        }
        return "redirect:/pedidos/entregas";
    }
}