package PDV.PDV.controller;

import PDV.PDV.dto.ResultadoEntrega;
import PDV.PDV.dto.ResultadoPagamento;
import PDV.PDV.dto.SecaoSubcategoria;
import PDV.PDV.model.clientes;
import PDV.PDV.model.cupom;
import PDV.PDV.model.entregas;
import PDV.PDV.model.pedido;
import PDV.PDV.model.produtos;
import PDV.PDV.model.Enum.categoriaPedido;
import PDV.PDV.model.Enum.formaPagamento;
import PDV.PDV.model.Enum.origemPedido;
import PDV.PDV.model.Enum.remetenteMensagem;
import PDV.PDV.model.Enum.statusPedido;
import PDV.PDV.model.Enum.tipoLogistico;
import PDV.PDV.model.Enum.tipoPedido;
import PDV.PDV.repository.clienteRepository;
import PDV.PDV.service.GoogleAuthService;
import PDV.PDV.service.MercadoPagoService;
import PDV.PDV.service.MensagemService;
import PDV.PDV.service.cupomService;
import PDV.PDV.service.entregaService;
import PDV.PDV.service.horarioService;
import PDV.PDV.service.pedidoService;
import PDV.PDV.service.produtoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/site")
public class PublicoController {

    private static final String CLIENTE_SESSAO = "clientePublico";

    private static final Logger log = LoggerFactory.getLogger(PublicoController.class);

    private static final List<String> ORDEM_SUBCATEGORIAS = List.of("Carne", "Frango", "Feijoada", "Panqueca");

    @Autowired private produtoService produtoService;
    @Autowired private clienteRepository clienteRepo;
    @Autowired private pedidoService pedidoService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private MercadoPagoService mercadoPagoService;
    @Autowired private GoogleAuthService googleAuthService;
    @Autowired private entregaService entregaService;
    @Autowired private MensagemService mensagemService;
    @Autowired private cupomService cupomService;
    @Autowired private horarioService horarioService;

    @Value("${mercado.pago.public-key:}")
    private String mpPublicKey;

    @Value("${mercado.pago.webhook-url:}")
    private String mpWebhookUrl;

    @GetMapping
    public String catalogo(@RequestParam(value = "busca", required = false, defaultValue = "") String busca,
                           @RequestParam(value = "subcategoria", required = false, defaultValue = "") String subcategoria,
                           Model model, HttpSession session) {
        var produtos = busca.isBlank()
                ? produtoService.listarAtivos()
                : produtoService.buscarAtivosPorNome(busca);
        if (!subcategoria.isBlank()) {
            produtos = produtos.stream()
                    .filter(produto -> subcategoria.equalsIgnoreCase(produto.getSubcategoria()))
                    .toList();
        }
        model.addAttribute("produtos", produtos);

        Map<categoriaPedido, List<SecaoSubcategoria>> agrupados = new LinkedHashMap<>();
        List<categoriaPedido> ordemCategorias = new ArrayList<>(List.of(categoriaPedido.values()));
        ordemCategorias.remove(categoriaPedido.BEBIDA);
        ordemCategorias.add(categoriaPedido.BEBIDA);
        int indiceSecao = 0;
        for (categoriaPedido categoria : ordemCategorias) {
            Map<String, List<produtos>> porSubcategoria = new LinkedHashMap<>();
            for (produtos produto : produtos) {
                if (produto.getCategoriaPedido() == categoria) {
                    String sub = (produto.getSubcategoria() != null && !produto.getSubcategoria().isBlank())
                            ? produto.getSubcategoria().trim()
                            : "Geral";
                    porSubcategoria.computeIfAbsent(sub, k -> new ArrayList<>()).add(produto);
                }
            }
            if (!porSubcategoria.isEmpty()) {
                List<Map.Entry<String, List<produtos>>> entradas = new ArrayList<>(porSubcategoria.entrySet());
                entradas.sort((a, b) -> {
                    int ia = ORDEM_SUBCATEGORIAS.indexOf(a.getKey());
                    int ib = ORDEM_SUBCATEGORIAS.indexOf(b.getKey());
                    if (ia < 0) ia = Integer.MAX_VALUE;
                    if (ib < 0) ib = Integer.MAX_VALUE;
                    if (ia != ib) return Integer.compare(ia, ib);
                    return a.getKey().compareTo(b.getKey());
                });

                boolean ehBebida = categoria == categoriaPedido.BEBIDA;
                List<SecaoSubcategoria> secoes = new ArrayList<>();
                for (Map.Entry<String, List<produtos>> entrada : entradas) {
                    SecaoSubcategoria secao = new SecaoSubcategoria();
                    secao.setNome(entrada.getKey());
                    secao.setProdutos(entrada.getValue());
                    secao.setBebida(ehBebida);
                    secao.setEscuro(!ehBebida && indiceSecao % 2 == 0);
                    secoes.add(secao);
                    indiceSecao++;
                }
                agrupados.put(categoria, secoes);
            }
        }
        model.addAttribute("agrupados", agrupados);

        model.addAttribute("busca", busca);
        model.addAttribute("subcategoria", subcategoria);
        model.addAttribute("subcategorias", produtoService.listarAtivos().stream()
                .map(produto -> produto.getSubcategoria())
                .filter(valor -> valor != null && !valor.isBlank())
                .distinct().sorted().toList());
        model.addAttribute("clientePublico", session.getAttribute(CLIENTE_SESSAO));
        return "publico/catalogo";
    }

    @GetMapping("/carrinho")
    public String carrinho(HttpSession session, Model model) {
        model.addAttribute("clientePublico", session.getAttribute(CLIENTE_SESSAO));
        List<Map<String, Object>> bebidas = new ArrayList<>();
        for (produtos p : produtoService.listarAtivos()) {
            if (p.getCategoriaPedido() == categoriaPedido.BEBIDA) {
                Map<String, Object> b = new LinkedHashMap<>();
                b.put("id", p.getId());
                b.put("nome", p.getNome());
                b.put("preco", p.getPreco());
                bebidas.add(b);
            }
        }
        model.addAttribute("bebidas", bebidas);
        return "publico/carrinho";
    }

    @GetMapping("/pedidos")
    public String meusPedidos(HttpSession session, Model model) {
        clientes cliente = (clientes) session.getAttribute(CLIENTE_SESSAO);
        if (cliente == null) {
            return "redirect:/site/login";
        }
        model.addAttribute("clientePublico", cliente);
        model.addAttribute("pedidos", pedidoService.listarPorCliente(cliente));
        return "publico/pedidos";
    }

    @PostMapping("/pedidos/{pedidoId}/cancelar")
    public String cancelarPedido(@PathVariable Long pedidoId, HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        clientes cliente = (clientes) session.getAttribute(CLIENTE_SESSAO);
        if (cliente == null) {
            return "redirect:/site/login";
        }
        pedido p = pedidoService.procurarID(pedidoId).orElse(null);
        if (p == null || p.getCliente() == null || !p.getCliente().getId().equals(cliente.getId())) {
            redirectAttributes.addFlashAttribute("erro", "Pedido não encontrado.");
            return "redirect:/site/pedidos";
        }
        if (p.getStatusPedido() != statusPedido.AGUARDANDO_PAGAMENTO
                && p.getStatusPedido() != statusPedido.PREPARANDO) {
            redirectAttributes.addFlashAttribute("erro", "Este pedido não pode mais ser cancelado.");
            return "redirect:/site/pedidos";
        }
        pedidoService.atualizarStatus(p.getId(), statusPedido.CANCELADO);
        redirectAttributes.addFlashAttribute("sucesso", "Pedido cancelado com sucesso.");
        return "redirect:/site/pedidos";
    }

    private clientes clienteLogado(HttpSession session) {
        Object obj = session.getAttribute(CLIENTE_SESSAO);
        return (obj instanceof clientes c) ? c : null;
    }

    private pedido pedidoDoCliente(Long pedidoId, clientes cliente) {
        pedido p = pedidoService.procurarID(pedidoId).orElse(null);
        if (p == null || p.getCliente() == null || !p.getCliente().getId().equals(cliente.getId())) {
            return null;
        }
        return p;
    }

    @GetMapping("/pedidos/{pedidoId}/chat")
    @ResponseBody
    public ResponseEntity<?> chatPedido(@PathVariable Long pedidoId, HttpSession session) {
        clientes cliente = clienteLogado(session);
        if (cliente == null) return naoAutenticado();
        if (pedidoDoCliente(pedidoId, cliente) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", "Pedido não encontrado."));
        }
        return ResponseEntity.ok(mensagemService.listar(pedidoId));
    }

    @PostMapping("/pedidos/{pedidoId}/chat")
    @ResponseBody
    public ResponseEntity<?> enviarMensagemCliente(@PathVariable Long pedidoId,
                                                   @RequestParam(defaultValue = "") String texto,
                                                   HttpSession session) {
        clientes cliente = clienteLogado(session);
        if (cliente == null) return naoAutenticado();
        pedido p = pedidoDoCliente(pedidoId, cliente);
        if (p == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", "Pedido não encontrado."));
        }
        if (texto == null || texto.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Escreva uma mensagem."));
        }
        return ResponseEntity.ok(mensagemService.enviar(p, remetenteMensagem.CLIENTE, texto.trim()));
    }

    @PostMapping("/pedidos/{pedidoId}/chat/lidas")
    @ResponseBody
    public ResponseEntity<?> marcarLidasCliente(@PathVariable Long pedidoId, HttpSession session) {
        clientes cliente = clienteLogado(session);
        if (cliente == null) return naoAutenticado();
        if (pedidoDoCliente(pedidoId, cliente) == null) {
            return ResponseEntity.ok().build();
        }
        mensagemService.marcarLidas(pedidoId, remetenteMensagem.LOJA);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pedidos/chat/naolidas")
    @ResponseBody
    public ResponseEntity<Map<Long, Integer>> chatNaoLidasCliente(HttpSession session) {
        clientes cliente = clienteLogado(session);
        if (cliente == null) return ResponseEntity.ok(new LinkedHashMap<>());
        Map<Long, Integer> mapa = new LinkedHashMap<>();
        for (pedido p : pedidoService.listarPorCliente(cliente)) {
            int n = mensagemService.naoLidas(p.getId(), remetenteMensagem.LOJA);
            if (n > 0) mapa.put(p.getId(), n);
        }
        return ResponseEntity.ok(mapa);
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("cliente", new clientes());
        return "publico/login";
    }

    @PostMapping("/login")
    public String autenticar(@RequestParam String celular, @RequestParam String senha,
                             HttpSession session, HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        String celularLimpo = celular.replaceAll("\\D", "");
        clientes cliente = clienteRepo.findByCelular(celularLimpo).orElse(null);
        if (cliente == null || cliente.getSenha() == null || !passwordEncoder.matches(senha, cliente.getSenha())) {
            redirectAttributes.addFlashAttribute("erro", "Celular ou senha inválidos.");
            return "redirect:/site/login";
        }
        session.setAttribute(CLIENTE_SESSAO, cliente);
        request.changeSessionId();
        return "redirect:/site";
    }

    @GetMapping("/cadastro")
    public String cadastro(Model model) {
        model.addAttribute("cliente", new clientes());
        return "publico/cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@ModelAttribute clientes cliente, @RequestParam String senha,
                            RedirectAttributes redirectAttributes) {
        String celular = cliente.getCelular() == null ? "" : cliente.getCelular().replaceAll("\\D", "");
        if (celular.isBlank() || senha == null || senha.length() < 8) {
            redirectAttributes.addFlashAttribute("erro", "Informe um celular e uma senha com pelo menos 8 caracteres.");
            return "redirect:/site/cadastro";
        }
        if (clienteRepo.findByCelular(celular).isPresent()) {
            redirectAttributes.addFlashAttribute("erro", "Este celular já possui cadastro.");
            return "redirect:/site/cadastro";
        }
        cliente.setId(null);
        cliente.setGoogleId(null);
        cliente.setEmail(null);
        cliente.setCelular(celular);
        cliente.setSenha(passwordEncoder.encode(senha));
        clienteRepo.save(cliente);
        redirectAttributes.addFlashAttribute("sucesso", "Cadastro realizado. Agora faça seu login.");
        return "redirect:/site/login";
    }

    @GetMapping("/sair")
    public String sair(HttpSession session) {
        session.invalidate();
        return "redirect:/site";
    }

    @GetMapping("/google")
    public String loginGoogle(HttpSession session) {
        String state = UUID.randomUUID().toString();
        session.setAttribute("googleOauthState", state);
        return "redirect:" + googleAuthService.buildAuthorizationUrl(state);
    }

    @GetMapping("/google/callback")
    public String googleCallback(@RequestParam(value = "code", required = false) String code,
                                 @RequestParam(value = "state", required = false) String state,
                                 @RequestParam(value = "error", required = false) String error,
                                 HttpSession session, HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        if (error != null && !error.isBlank()) {
            redirectAttributes.addFlashAttribute("erro", "Login com Google cancelado.");
            return "redirect:/site/login";
        }
        String stateEsperado = (String) session.getAttribute("googleOauthState");
        if (stateEsperado == null || !stateEsperado.equals(state)) {
            redirectAttributes.addFlashAttribute("erro", "Falha na validação do login com Google.");
            return "redirect:/site/login";
        }
        if (code == null || code.isBlank()) {
            redirectAttributes.addFlashAttribute("erro", "Código de autorização não recebido.");
            return "redirect:/site/login";
        }
        try {
            GoogleAuthService.GoogleUsuario google = googleAuthService.trocarCodigo(code);

            clientes cliente = null;
            if (google.googleId() != null) {
                cliente = clienteRepo.findByGoogleId(google.googleId()).orElse(null);
            }
            if (cliente == null && google.email() != null) {
                cliente = clienteRepo.findByEmail(google.email()).orElse(null);
            }

            if (cliente == null) {
                cliente = new clientes();
                cliente.setNome(google.nome());
                cliente.setEmail(google.email());
                cliente.setGoogleId(google.googleId());
                clienteRepo.save(cliente);
            } else {
                if (cliente.getGoogleId() == null) cliente.setGoogleId(google.googleId());
                if (cliente.getEmail() == null) cliente.setEmail(google.email());
                if (cliente.getNome() == null) cliente.setNome(google.nome());
                clienteRepo.save(cliente);
            }
            session.setAttribute(CLIENTE_SESSAO, cliente);
            request.changeSessionId();
            return "redirect:/site";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro",
                    e.getMessage() != null ? e.getMessage() : "Erro ao entrar com o Google.");
            return "redirect:/site/login";
        }
    }

    @GetMapping("/sobre")
    public String sobre(Model model, HttpSession session) {
        model.addAttribute("clientePublico", session.getAttribute(CLIENTE_SESSAO));
        return "publico/sobre";
    }

    @GetMapping("/contato")
    public String contato(Model model, HttpSession session) {
        model.addAttribute("clientePublico", session.getAttribute(CLIENTE_SESSAO));
        return "publico/contato";
    }

    @GetMapping("/perfil")
    public String perfil(HttpSession session, Model model) {
        clientes cliente = (clientes) session.getAttribute(CLIENTE_SESSAO);
        if (cliente == null) {
            return "redirect:/site/login";
        }
        clientes atual = clienteRepo.findById(cliente.getId()).orElse(cliente);
        session.setAttribute(CLIENTE_SESSAO, atual);
        model.addAttribute("clientePublico", atual);
        model.addAttribute("cliente", atual);
        return "publico/perfil";
    }

    @PostMapping("/perfil")
    public String atualizarPerfil(@ModelAttribute clientes form,
                                  @RequestParam(defaultValue = "") String novaSenha,
                                  HttpSession session, RedirectAttributes redirectAttributes) {
        clientes sessao = (clientes) session.getAttribute(CLIENTE_SESSAO);
        if (sessao == null) {
            return "redirect:/site/login";
        }
        clientes cliente = clienteRepo.findById(sessao.getId()).orElse(sessao);

        String celular = form.getCelular() == null ? "" : form.getCelular().replaceAll("\\D", "");
        if (celular.isBlank()) {
            redirectAttributes.addFlashAttribute("erro", "Informe um celular válido.");
            return "redirect:/site/perfil";
        }
        clientes porCelular = clienteRepo.findByCelular(celular).orElse(null);
        if (porCelular != null && !porCelular.getId().equals(cliente.getId())) {
            redirectAttributes.addFlashAttribute("erro", "Este celular já está em uso por outra conta.");
            return "redirect:/site/perfil";
        }

        String email = form.getEmail() == null ? "" : form.getEmail().trim();
        if (!email.isBlank()) {
            clientes porEmail = clienteRepo.findByEmail(email).orElse(null);
            if (porEmail != null && !porEmail.getId().equals(cliente.getId())) {
                redirectAttributes.addFlashAttribute("erro", "Este e-mail já está em uso por outra conta.");
                return "redirect:/site/perfil";
            }
        }

        cliente.setNome(form.getNome());
        cliente.setCelular(celular);
        cliente.setEmail(email.isBlank() ? null : email);
        cliente.setCep(form.getCep());
        cliente.setRua(form.getRua());
        cliente.setBairro(form.getBairro());
        cliente.setNumero(form.getNumero());
        cliente.setComplemento(form.getComplemento());
        cliente.setPontoReferencia(form.getPontoReferencia());

        if (novaSenha != null && !novaSenha.isBlank()) {
            if (novaSenha.length() < 8) {
                redirectAttributes.addFlashAttribute("erro", "A nova senha deve ter pelo menos 8 caracteres.");
                return "redirect:/site/perfil";
            }
            cliente.setSenha(passwordEncoder.encode(novaSenha));
        }

        clienteRepo.save(cliente);
        session.setAttribute(CLIENTE_SESSAO, cliente);
        redirectAttributes.addFlashAttribute("sucesso", "Cadastro atualizado com sucesso.");
        return "redirect:/site/perfil";
    }

    @GetMapping("/pagamento")
    public String paginaPagamento(HttpSession session, Model model) {
        clientes cliente = (clientes) session.getAttribute(CLIENTE_SESSAO);
        if (cliente == null) {
            return "redirect:/site/login";
        }
        model.addAttribute("clientePublico", cliente);
        model.addAttribute("mpPublicKey", mpPublicKey);
        return "publico/pagamento";
    }

    @GetMapping("/entrega/consultar")
    @ResponseBody
    public ResponseEntity<?> consultarEntrega(@RequestParam String cep) {
        try {
            return ResponseEntity.ok(entregaService.consultarEntrega(cep));
        } catch (Exception e) {
            ResultadoEntrega r = new ResultadoEntrega();
            r.setSucesso(false);
            r.setMensagem(e.getMessage() != null ? e.getMessage() : "Não foi possível consultar o CEP.");
            return ResponseEntity.badRequest().body(r);
        }
    }

    @PostMapping("/cupom/validar")
    @ResponseBody
    public ResponseEntity<?> validarCupom(@RequestParam String codigo,
                                          @RequestParam String carrinhoJson,
                                          @RequestParam(defaultValue = "DELIVERY") String tipoPedido,
                                          @RequestParam(defaultValue = "") String cep,
                                          HttpSession session) {
        clientes cliente = clienteLogado(session);
        try {
            BigDecimal subtotal = pedidoService.calcularTotal(carrinhoJson);
            BigDecimal taxa = BigDecimal.ZERO;
            if (!"RETIRADA".equalsIgnoreCase(tipoPedido) && cep != null && !cep.isBlank()) {
                taxa = taxaEntregaParaValidacao(cep);
            }
            // Visitantes não logados podem validar um cupom para ver o desconto no carrinho;
            // o desconto é recalculado no fechamento do pedido com o cliente autenticado.
            BigDecimal desconto = cupomService.calcularDesconto(codigo, cliente, subtotal, taxa);
            cupom c = cupomService.buscarPorCodigo(codigo);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("sucesso", true);
            resp.put("codigo", c != null ? c.getCodigo() : codigo.trim());
            resp.put("tipo", c != null ? c.getTipo().name() : null);
            resp.put("desconto", desconto);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("sucesso", false);
            resp.put("mensagem", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    private BigDecimal taxaEntregaParaValidacao(String cep) {
        if (cep == null || cep.isBlank()) {
            return BigDecimal.ZERO;
        }
        ResultadoEntrega r;
        try {
            r = entregaService.consultarEntrega(cep);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
        if (r == null || !r.isDentroDaArea()) {
            return BigDecimal.ZERO;
        }
        return r.getValorEntrega() != null ? r.getValorEntrega() : BigDecimal.ZERO;
    }

    @PostMapping("/pagamento/pix")
    @ResponseBody
    public ResponseEntity<?> pagarComPix(@RequestParam String carrinhoJson,
                                         @RequestParam(defaultValue = "") String observacoes,
                                         @RequestParam(defaultValue = "DELIVERY") String tipoPedido,
                                         @RequestParam(defaultValue = "") String cep,
                                         @RequestParam(defaultValue = "") String rua,
                                         @RequestParam(defaultValue = "") String numero,
                                         @RequestParam(defaultValue = "") String bairro,
                                         @RequestParam(defaultValue = "") String complemento,
                                         @RequestParam(defaultValue = "") String cupom,
                                         HttpSession session) {
        clientes cliente = (clientes) session.getAttribute(CLIENTE_SESSAO);
        if (cliente == null) {
            return naoAutenticado();
        }
        try {
            horarioService.verificarAberto();
            BigDecimal subtotal = pedidoService.calcularTotal(carrinhoJson);

            pedido novo = new pedido();
            novo.setCliente(cliente);
            novo.setFormaPagamento(formaPagamento.PIX);
            novo.setStatusPedido(statusPedido.AGUARDANDO_PAGAMENTO);
            novo.setObservacoes(observacoes);
            BigDecimal taxa = definirEntrega(novo, tipoPedido, cep, rua, numero, bairro, complemento);
            BigDecimal desconto = cupomService.calcularDesconto(cupom, cliente, subtotal, taxa);
            novo.setDesconto(desconto);
            if (cupom != null && !cupom.isBlank()) {
                novo.setCupomCodigo(cupom.trim().toUpperCase());
            }
            BigDecimal total = subtotal.add(taxa).subtract(desconto);

            OffsetDateTime expiracao = OffsetDateTime.now().plusMinutes(
                    pedidoService.obterTempoLimitePagamento());
            novo.setDataExpiracaoPagamento(expiracao);

            JsonNode pagamento = mercadoPagoService.criarPagamentoPix(total, "Pedido - Marmitas Sousa",
                    "", cliente.getNome(), mpWebhookUrl, expiracao);

            Long paymentId = pagamento.path("id").asLong(0);
            novo.setPagamentoMpId(paymentId);
            pedido salvo = pedidoService.novoPedidoComItens(novo, carrinhoJson);

            ResultadoPagamento r = new ResultadoPagamento();
            r.setSucesso(true);
            r.setStatus(pagamento.path("status").asText("pending"));
            r.setPaymentId(paymentId);
            r.setPedidoId(salvo.getId());
            r.setExpiracaoPagamento(expiracao.toInstant().toEpochMilli());
            JsonNode dadosPix = pagamento.path("point_of_interaction").path("transaction_data");
            r.setQrCodeBase64(dadosPix.path("qr_code_base64").asText(null));
            r.setCopiaECola(dadosPix.path("qr_code").asText(null));
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException e) {
            return erroPagamento(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao criar pagamento Pix", e);
            return erroPagamento("Não foi possível processar o pagamento. Tente novamente.");
        }
    }

    @PostMapping("/pagamento/cartao")
    @ResponseBody
    public ResponseEntity<?> pagarComCartao(@RequestParam String carrinhoJson,
                                            @RequestParam(defaultValue = "") String observacoes,
                                            @RequestParam String cardToken,
                                            @RequestParam(defaultValue = "") String paymentMethodId,
                                            @RequestParam String email,
                                            @RequestParam(defaultValue = "DELIVERY") String tipoPedido,
                                            @RequestParam(defaultValue = "") String cep,
                                            @RequestParam(defaultValue = "") String rua,
                                            @RequestParam(defaultValue = "") String numero,
                                            @RequestParam(defaultValue = "") String bairro,
                                            @RequestParam(defaultValue = "") String complemento,
                                            @RequestParam(defaultValue = "") String cupom,
                                            HttpSession session) {
        clientes cliente = (clientes) session.getAttribute(CLIENTE_SESSAO);
        if (cliente == null) {
            return naoAutenticado();
        }
        try {
            horarioService.verificarAberto();
            BigDecimal subtotal = pedidoService.calcularTotal(carrinhoJson);

            pedido novo = new pedido();
            novo.setCliente(cliente);
            novo.setFormaPagamento(formaPagamento.CARTAO);
            novo.setObservacoes(observacoes);
            BigDecimal taxa = definirEntrega(novo, tipoPedido, cep, rua, numero, bairro, complemento);
            BigDecimal desconto = cupomService.calcularDesconto(cupom, cliente, subtotal, taxa);
            novo.setDesconto(desconto);
            if (cupom != null && !cupom.isBlank()) {
                novo.setCupomCodigo(cupom.trim().toUpperCase());
            }
            BigDecimal total = subtotal.add(taxa).subtract(desconto);

            JsonNode pagamento = mercadoPagoService.criarPagamentoCartao(total, "Pedido - Marmitas Sousa",
                    cardToken, paymentMethodId, email, cliente.getNome(), mpWebhookUrl);

            String status = pagamento.path("status").asText(null);
            Long paymentId = pagamento.path("id").asLong(0);

            ResultadoPagamento r = new ResultadoPagamento();
            r.setSucesso(true);
            r.setStatus(status);
            r.setStatusDetail(pagamento.path("status_detail").asText(null));
            r.setPaymentId(paymentId);

            if ("rejected".equals(status)) {
                return ResponseEntity.ok(r);
            }

            novo.setPagamentoMpId(paymentId);
            novo.setStatusPedido(statusPedido.AGUARDANDO_PAGAMENTO);
            novo.setDataExpiracaoPagamento(OffsetDateTime.now().plusMinutes(
                    pedidoService.obterTempoLimitePagamento()));
            pedido salvo = pedidoService.novoPedidoComItens(novo, carrinhoJson);
            r.setPedidoId(salvo.getId());

            if ("approved".equals(status)) {
                pedidoService.confirmarPagamento(paymentId);
            }
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException e) {
            return erroPagamento(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao processar pagamento com cartão", e);
            return erroPagamento("Não foi possível processar o pagamento. Tente novamente.");
        }
    }

    @PostMapping("/pedido/entrega")
    @ResponseBody
    public ResponseEntity<?> pedidoNaEntrega(@RequestParam String carrinhoJson,
                                             @RequestParam(defaultValue = "") String observacoes,
                                             @RequestParam(defaultValue = "DELIVERY") String tipoPedido,
                                             @RequestParam(defaultValue = "") String cep,
                                             @RequestParam(defaultValue = "") String rua,
                                             @RequestParam(defaultValue = "") String numero,
                                             @RequestParam(defaultValue = "") String bairro,
                                             @RequestParam(defaultValue = "") String complemento,
                                             @RequestParam(defaultValue = "DINHEIRO") String forma,
                                             @RequestParam(defaultValue = "") String trocoPara,
                                             @RequestParam(defaultValue = "") String tipoCartao,
                                             @RequestParam(defaultValue = "false") boolean precisaTroco,
                                             @RequestParam(defaultValue = "") String cupom,
                                             HttpSession session) {
        clientes cliente = (clientes) session.getAttribute(CLIENTE_SESSAO);
        if (cliente == null) {
            return naoAutenticado();
        }
        try {
            horarioService.verificarAberto();
            BigDecimal subtotal = pedidoService.calcularTotal(carrinhoJson);

            pedido novo = new pedido();
            novo.setCliente(cliente);
            formaPagamento formaPag = "CARTAO".equalsIgnoreCase(forma)
                    ? formaPagamento.CARTAO : formaPagamento.DINHEIRO;
            novo.setFormaPagamento(formaPag);
            novo.setStatusPedido(statusPedido.PREPARANDO);
            novo.setPagamentoNaEntrega(true);
            novo.setObservacoes(observacoes);
            BigDecimal taxa = definirEntrega(novo, tipoPedido, cep, rua, numero, bairro, complemento);
            BigDecimal desconto = cupomService.calcularDesconto(cupom, cliente, subtotal, taxa);
            novo.setDesconto(desconto);
            if (cupom != null && !cupom.isBlank()) {
                novo.setCupomCodigo(cupom.trim().toUpperCase());
            }
            BigDecimal total = subtotal.add(taxa).subtract(desconto);

            if (formaPag == formaPagamento.DINHEIRO) {
                if (precisaTroco) {
                    String trocoStr = trocoPara == null ? "" : trocoPara.trim()
                            .replaceAll("[^0-9,.]", "").replace(",", ".");
                    if (trocoStr.isBlank()) {
                        throw new IllegalArgumentException(
                                "Informe o valor que você vai pagar para calcularmos o troco.");
                    }
                    BigDecimal troco;
                    try {
                        troco = new BigDecimal(trocoStr);
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Informe um valor de troco válido.");
                    }
                    if (troco.compareTo(total) < 0) {
                        throw new IllegalArgumentException(
                                "O valor para troco deve ser maior ou igual ao total do pedido.");
                    }
                    novo.setTrocoPara(troco);
                }
            } else {
                String tipo = "credito".equalsIgnoreCase(tipoCartao) ? "crédito" : "débito";
                String nota = "Pagamento na entrega: cartão " + tipo + ".";
                novo.setObservacoes(observacoes.isBlank() ? nota : nota + " " + observacoes);
            }

            pedido salvo = pedidoService.novoPedidoComItens(novo, carrinhoJson);
            pedidoService.imprimirAoPreparar(salvo.getId());

            ResultadoPagamento r = new ResultadoPagamento();
            r.setSucesso(true);
            r.setPedidoId(salvo.getId());
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException e) {
            return erroPagamento(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao registrar pedido na entrega", e);
            return erroPagamento("Não foi possível finalizar o pedido. Tente novamente.");
        }
    }

    @GetMapping("/pagamento/status")
    @ResponseBody
    public ResponseEntity<?> statusPagamento(@RequestParam Long paymentId, HttpSession session) {
        clientes cliente = (clientes) session.getAttribute(CLIENTE_SESSAO);
        if (cliente == null) {
            return naoAutenticado();
        }
        pedido pedido = pedidoService.buscarPorPagamentoMp(paymentId).orElse(null);
        if (pedido == null || pedido.getCliente() == null || !pedido.getCliente().getId().equals(cliente.getId())) {
            ResultadoPagamento r = new ResultadoPagamento();
            r.setSucesso(false);
            r.setMensagem("Pagamento não encontrado.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(r);
        }
        try {
            JsonNode pagamento = mercadoPagoService.consultarStatus(paymentId);
            String status = pagamento.path("status").asText(null);
            ResultadoPagamento r = new ResultadoPagamento();
            r.setSucesso(true);
            r.setStatus(status);
            r.setStatusDetail(pagamento.path("status_detail").asText(null));
            if ("approved".equals(status)) {
                pedidoService.confirmarPagamento(paymentId);
                r.setPago(true);
            }
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            log.error("Erro ao consultar status do pagamento " + paymentId, e);
            return erroPagamento("Não foi possível consultar o status do pagamento.");
        }
    }

    @GetMapping("/pedidos/{pedidoId}/pagar")
    public String paginaPagarPedido(@PathVariable Long pedidoId, HttpSession session, Model model) {
        clientes cliente = (clientes) session.getAttribute(CLIENTE_SESSAO);
        if (cliente == null) {
            return "redirect:/site/login";
        }
        pedido p = pedidoService.carregarPedidoCompleto(pedidoId);
        if (p == null || p.getCliente() == null || !p.getCliente().getId().equals(cliente.getId())
                || Boolean.TRUE.equals(p.getPago())
                || p.getStatusPedido() != statusPedido.AGUARDANDO_PAGAMENTO) {
            return "redirect:/site/pedidos";
        }
        if (p.getDataExpiracaoPagamento() != null
                && p.getDataExpiracaoPagamento().isBefore(OffsetDateTime.now())) {
            pedidoService.atualizarStatus(p.getId(), statusPedido.CANCELADO);
            return "redirect:/site/pedidos";
        }
        model.addAttribute("pedido", p);
        model.addAttribute("clientePublico", cliente);
        model.addAttribute("mpPublicKey", mpPublicKey);
        model.addAttribute("infoEntrega", descricaoEntrega(p));
        return "publico/pagar-pedido";
    }

    private String descricaoEntrega(pedido p) {
        if (p.getTipoPedido() == tipoPedido.RETIRADA) {
            return "Retirar no estabelecimento";
        }
        entregas e = p.getEntregas();
        if (e == null) {
            return "Entrega";
        }
        StringBuilder sb = new StringBuilder("Entrega");
        if (e.getRua() != null && !e.getRua().isBlank()) {
            sb.append(" — ").append(e.getRua());
            if (e.getNumero() != null && !e.getNumero().isBlank()) sb.append(", ").append(e.getNumero());
            if (e.getBairro() != null && !e.getBairro().isBlank()) sb.append(" · ").append(e.getBairro());
        }
        return sb.toString();
    }

    @PostMapping("/pedidos/{pedidoId}/pagar/pix")
    @ResponseBody
    public ResponseEntity<?> pagarPedidoPix(@PathVariable Long pedidoId, HttpSession session) {
        clientes cliente = (clientes) session.getAttribute(CLIENTE_SESSAO);
        if (cliente == null) {
            return naoAutenticado();
        }
        try {
            pedido p = pedidoPagavel(pedidoId, cliente);
            BigDecimal total = p.getValorTotal() != null ? p.getValorTotal() : BigDecimal.ZERO;
            OffsetDateTime expiracao = OffsetDateTime.now().plusMinutes(
                    pedidoService.obterTempoLimitePagamento());
            JsonNode pagamento = mercadoPagoService.criarPagamentoPix(total, descricaoPedido(p),
                    "", cliente.getNome(), mpWebhookUrl, expiracao);

            Long paymentId = pagamento.path("id").asLong(0);
            pedidoService.vincularPagamento(p.getId(), paymentId, formaPagamento.PIX);
            pedidoService.atualizarExpiracaoPagamento(p.getId(), expiracao);

            ResultadoPagamento r = new ResultadoPagamento();
            r.setSucesso(true);
            r.setStatus(pagamento.path("status").asText("pending"));
            r.setPaymentId(paymentId);
            r.setPedidoId(p.getId());
            r.setExpiracaoPagamento(expiracao.toInstant().toEpochMilli());
            JsonNode dadosPix = pagamento.path("point_of_interaction").path("transaction_data");
            r.setQrCodeBase64(dadosPix.path("qr_code_base64").asText(null));
            r.setCopiaECola(dadosPix.path("qr_code").asText(null));
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException e) {
            return erroPagamento(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao criar pagamento Pix para o pedido " + pedidoId, e);
            return erroPagamento("Não foi possível processar o pagamento. Tente novamente.");
        }
    }

    @PostMapping("/pedidos/{pedidoId}/pagar/cartao")
    @ResponseBody
    public ResponseEntity<?> pagarPedidoCartao(@PathVariable Long pedidoId,
                                               @RequestParam String cardToken,
                                               @RequestParam(defaultValue = "") String paymentMethodId,
                                               @RequestParam String email,
                                               HttpSession session) {
        clientes cliente = (clientes) session.getAttribute(CLIENTE_SESSAO);
        if (cliente == null) {
            return naoAutenticado();
        }
        try {
            pedido p = pedidoPagavel(pedidoId, cliente);
            BigDecimal total = p.getValorTotal() != null ? p.getValorTotal() : BigDecimal.ZERO;
            JsonNode pagamento = mercadoPagoService.criarPagamentoCartao(total, descricaoPedido(p),
                    cardToken, paymentMethodId, email, cliente.getNome(), mpWebhookUrl);

            ResultadoPagamento r = new ResultadoPagamento();
            r.setSucesso(true);
            r.setStatus(pagamento.path("status").asText(null));
            r.setStatusDetail(pagamento.path("status_detail").asText(null));
            r.setPaymentId(pagamento.path("id").asLong(0));

            if ("approved".equals(r.getStatus())) {
                pedidoService.vincularPagamento(p.getId(), pagamento.path("id").asLong(0), formaPagamento.CARTAO);
                pedidoService.confirmarPagamento(pagamento.path("id").asLong(0));
            }
            return ResponseEntity.ok(r);
        } catch (IllegalArgumentException e) {
            return erroPagamento(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao processar pagamento com cartão para o pedido " + pedidoId, e);
            return erroPagamento("Não foi possível processar o pagamento. Tente novamente.");
        }
    }

    private BigDecimal definirEntrega(pedido novo, String tipoPedidoStr, String cep,
                                      String rua, String numero, String bairro, String complemento) {
        novo.setOrigemPedido(origemPedido.SITE);
        if ("RETIRADA".equalsIgnoreCase(tipoPedidoStr)) {
            novo.setTipoPedido(tipoPedido.RETIRADA);
            novo.setTaxaEntrega(BigDecimal.ZERO);
            return BigDecimal.ZERO;
        }

        novo.setTipoPedido(tipoPedido.DELIVERY);
        novo.setTipoLogistica(tipoLogistico.PROPRIA);

        ResultadoEntrega r;
        try {
            r = entregaService.consultarEntrega(cep);
        } catch (Exception e) {
            throw new IllegalArgumentException("Não foi possível calcular a entrega para o CEP informado.");
        }
        if (r == null || !r.isDentroDaArea()) {
            throw new IllegalArgumentException(r != null && r.getMensagem() != null
                    ? r.getMensagem() : "Não atendemos essa região.");
        }

        BigDecimal taxa = r.getValorEntrega() != null ? r.getValorEntrega() : BigDecimal.ZERO;
        novo.setTaxaEntrega(taxa);

        entregas entrega = new entregas();
        entrega.setPedido(novo);
        entrega.setCep(cep);
        entrega.setRua(rua);
        entrega.setNumero(numero);
        entrega.setBairro(bairro);
        entrega.setComplemento(complemento);
        novo.setEntregas(entrega);
        return taxa;
    }

    private pedido pedidoPagavel(Long pedidoId, clientes cliente) {
        pedido p = pedidoService.procurarID(pedidoId).orElse(null);
        if (p == null || p.getCliente() == null || !p.getCliente().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("Pedido não encontrado.");
        }
        if (Boolean.TRUE.equals(p.getPago()) || p.getStatusPedido() != statusPedido.AGUARDANDO_PAGAMENTO) {
            throw new IllegalArgumentException("Este pedido não está mais aguardando pagamento.");
        }
        if (p.getDataExpiracaoPagamento() != null
                && p.getDataExpiracaoPagamento().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("O prazo para pagamento deste pedido expirou.");
        }
        return p;
    }

    private String descricaoPedido(pedido p) {
        Object numero = p.getNumeroPedidoCliente() != null ? p.getNumeroPedidoCliente() : p.getId();
        return "Pedido #" + numero + " - Marmitas Sousa";
    }

    private ResponseEntity<?> erroPagamento(String mensagem) {
        ResultadoPagamento r = new ResultadoPagamento();
        r.setSucesso(false);
        r.setMensagem(mensagem != null ? mensagem : "Erro ao processar o pagamento.");
        return ResponseEntity.badRequest().body(r);
    }

    private ResponseEntity<?> naoAutenticado() {
        ResultadoPagamento r = new ResultadoPagamento();
        r.setSucesso(false);
        r.setMensagem("Faça login para continuar.");
        return ResponseEntity.status(401).body(r);
    }
}