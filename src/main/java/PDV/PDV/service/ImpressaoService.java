package PDV.PDV.service;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import PDV.PDV.model.clientes;
import PDV.PDV.model.pedido;
import PDV.PDV.model.Enum.formaPagamento;
import PDV.PDV.model.Enum.tipoPedido;
import PDV.PDV.model.itensPedido;
import PDV.PDV.repository.pedidoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImpressaoService {

    @Value("${impressora.nome:termica}")
    private String nomeImpressora;

    @Value("${impressora.modo:local}")
    private String modoImpressao;

    // Largura do papel térmico 58mm em colunas (fonte padrão) — usada para centralizar
    // e alinhar à direita o cabeçalho e os totais.
    private static final int LARGURA = 32;
    private static final String SEPARADOR = "=".repeat(LARGURA);
    private static final String SEPARADOR_FINO = "-".repeat(LARGURA);
    private static final DateTimeFormatter FMT_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final pedidoRepository pedidoRepo;
    private final configuracaoService configuracaoService;

    public ImpressaoService(pedidoRepository pedidoRepo, configuracaoService configuracaoService) {
        this.pedidoRepo = pedidoRepo;
        this.configuracaoService = configuracaoService;
    }

    public boolean isModoPonte() {
        return "ponte".equalsIgnoreCase(modoImpressao);
    }

    private String nomeImpressoraEfetivo() {
        String configurada = configuracaoService.obterImpressoraNome();
        if (configurada != null && !configurada.isBlank()) {
            return configurada;
        }
        return nomeImpressora;
    }

    public List<String> listarImpressoras() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        List<String> nomes = new ArrayList<>();
        for (PrintService service : services) {
            nomes.add(service.getName());
        }
        return nomes;
    }

    public void imprimirTeste(String nomeImpressora) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("================================\n");
        sb.append("TESTE DE IMPRESSÃO\n");
        sb.append("Marmitas Sousa\n");
        sb.append("================================\n");
        sb.append("Se você está lendo isto, a impressora\n");
        sb.append("está configurada corretamente.\n");
        imprimirTexto(sb.toString(), nomeImpressora);
    }

    @Transactional
    public void imprimirPedido(Long id) throws Exception {
        if (isModoPonte()) {
            // Em modo ponte, não imprimimos aqui: a ponte (notebook) busca e imprime.
            // Reenfileira o pedido marcando impresso=false para a ponte pegar na próxima consulta.
            pedidoRepo.reverterImpressao(id);
            return;
        }
        imprimirTexto(montarTextoPedido(id), nomeImpressoraEfetivo());
    }

    @Transactional(readOnly = true)
    public String montarTextoPedido(Long id) {
        pedido p = pedidoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado com o ID: " + id));

        StringBuilder sb = new StringBuilder();

        // ===== Cabeçalho =====
        sb.append(SEPARADOR).append('\n');
        sb.append(centralizar("MARMITAS SOUSA")).append('\n');
        sb.append(centralizar("Comanda de Pedido")).append('\n');
        sb.append(SEPARADOR).append('\n');

        Object numero = p.getNumeroPedidoCliente() != null ? p.getNumeroPedidoCliente() : p.getId();
        sb.append("Pedido: #").append(numero).append('\n');
        if (p.getDataHoraLocal() != null) {
            sb.append("Data: ").append(p.getDataHoraLocal().format(FMT_DATA_HORA)).append('\n');
        }
        if (p.getTipoPedido() != null) {
            sb.append("Tipo: ").append(p.getTipoPedido() == tipoPedido.RETIRADA ? "Retirada" : "Entrega").append('\n');
        }

        // ===== Cliente =====
        if (p.getCliente() != null) {
            clientes c = p.getCliente();
            sb.append(SEPARADOR_FINO).append('\n');
            if (tem(c.getNome())) {
                sb.append("Cliente: ").append(c.getNome()).append('\n');
            }
            if (tem(c.getCelular())) {
                sb.append("Celular: ").append(c.getCelular()).append('\n');
            }
        }

        // ===== Endereço (só para entrega) =====
        String rua = null, numeroEndereco = null, bairro = null, cep = null, complemento = null, referencia = null;
        if (p.getEntregas() != null) {
            rua = p.getEntregas().getRua();
            numeroEndereco = p.getEntregas().getNumero();
            bairro = p.getEntregas().getBairro();
            cep = p.getEntregas().getCep();
            complemento = p.getEntregas().getComplemento();
        }
        if (p.getCliente() != null) {
            if (!tem(rua)) rua = p.getCliente().getRua();
            if (!tem(numeroEndereco)) numeroEndereco = p.getCliente().getNumero();
            if (!tem(bairro)) bairro = p.getCliente().getBairro();
            if (!tem(cep)) cep = p.getCliente().getCep();
            if (!tem(complemento)) complemento = p.getCliente().getComplemento();
            referencia = p.getCliente().getPontoReferencia();
        }

        if (p.getTipoPedido() != tipoPedido.RETIRADA) {
            sb.append(SEPARADOR_FINO).append('\n');
            sb.append("Endereco:\n");
            String endereco = tem(rua) ? rua : "";
            if (tem(numeroEndereco)) {
                endereco = endereco.isBlank() ? numeroEndereco : endereco + ", " + numeroEndereco;
            }
            if (!endereco.isBlank()) {
                sb.append("  ").append(endereco).append('\n');
            }
            if (tem(bairro)) {
                sb.append("  Bairro: ").append(bairro).append('\n');
            }
            if (tem(cep)) {
                sb.append("  CEP: ").append(cep).append('\n');
            }
            if (tem(complemento)) {
                sb.append("  Compl.: ").append(complemento).append('\n');
            }
            if (tem(referencia)) {
                sb.append("  Ref.: ").append(referencia).append('\n');
            }
        }

        // ===== Itens =====
        sb.append(SEPARADOR_FINO).append('\n');
        sb.append("ITENS:\n");

        if (p.getItens() != null && !p.getItens().isEmpty()) {
            for (itensPedido item : p.getItens()) {
                String nomeProduto = (item.getProduto() != null && item.getProduto().getNome() != null)
                        ? item.getProduto().getNome()
                        : "Item";
                int quantidade = item.getQuantidade() != null ? item.getQuantidade() : 1;
                BigDecimal valorUnitario = item.getPrecoUnitario() != null ? item.getPrecoUnitario() : BigDecimal.ZERO;
                BigDecimal valorSubtotal = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;

                sb.append("  ").append(quantidade).append("x ").append(nomeProduto).append('\n');
                if (quantidade > 1 && valorUnitario.compareTo(BigDecimal.ZERO) > 0) {
                    sb.append("     ").append(quantidade).append(" x ").append(moeda(valorUnitario))
                            .append(" = ").append(moeda(valorSubtotal)).append('\n');
                } else {
                    sb.append("     ").append(moeda(valorSubtotal)).append('\n');
                }

                if (item.getObservacao() != null && !item.getObservacao().trim().isEmpty()) {
                    sb.append("     Obs: ").append(item.getObservacao()).append('\n');
                }
            }
        } else {
            sb.append("  Nenhum item listado.\n");
        }

        // ===== Totais =====
        sb.append(SEPARADOR_FINO).append('\n');

        BigDecimal taxaEntrega = p.getTaxaEntrega() != null ? p.getTaxaEntrega() : BigDecimal.ZERO;
        BigDecimal desconto = p.getDesconto() != null ? p.getDesconto() : BigDecimal.ZERO;
        BigDecimal valorTotal = p.getValorTotal() != null ? p.getValorTotal() : BigDecimal.ZERO;

        if (taxaEntrega.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(direita("Taxa de Entrega:", moeda(taxaEntrega))).append('\n');
        }
        if (desconto.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(direita("Desconto:", "-" + moeda(desconto))).append('\n');
        }
        sb.append(SEPARADOR).append('\n');
        sb.append(direita("TOTAL:", moeda(valorTotal))).append('\n');

        // ===== Pagamento =====
        sb.append(SEPARADOR_FINO).append('\n');
        sb.append("Pagamento: ").append(descricaoFormaPagamento(p.getFormaPagamento())).append('\n');

        if (Boolean.TRUE.equals(p.getPagamentoNaEntrega())) {
            sb.append('\n').append(centralizar("*** COBRAR NA ENTREGA ***")).append('\n');
            if (p.getFormaPagamento() == formaPagamento.CARTAO) {
                sb.append(centralizar(">>> MAQUININHA DE CARTAO <<<")).append('\n');
            } else if (p.getFormaPagamento() == formaPagamento.DINHEIRO) {
                sb.append(centralizar(">>> RECEBER EM DINHEIRO <<<")).append('\n');
            }
            sb.append('\n');
        } else if (Boolean.TRUE.equals(p.getPago())) {
            sb.append("Status: PAGO\n");
        }

        if (p.getFormaPagamento() == formaPagamento.DINHEIRO && p.getTrocoPara() != null) {
            BigDecimal valorRecebido = p.getTrocoPara();
            BigDecimal troco = valorRecebido.subtract(valorTotal);
            if (troco.compareTo(BigDecimal.ZERO) < 0) {
                troco = BigDecimal.ZERO;
            }
            sb.append("Recebido: ").append(moeda(valorRecebido)).append('\n');
            sb.append("Troco: ").append(moeda(troco)).append('\n');
        }

        if (p.getObservacoes() != null && !p.getObservacoes().trim().isEmpty()) {
            sb.append("Obs. geral: ").append(p.getObservacoes()).append('\n');
        }

        sb.append(SEPARADOR).append('\n');
        sb.append(centralizar("Obrigado pela sua compra!")).append('\n');

        return sb.toString();
    }

    private static String centralizar(String texto) {
        if (texto == null || texto.length() >= LARGURA) {
            return texto == null ? "" : texto;
        }
        int espacos = LARGURA - texto.length();
        int esquerda = espacos / 2;
        return " ".repeat(esquerda) + texto;
    }

    private static String direita(String rotulo, String valor) {
        int espacos = LARGURA - rotulo.length() - valor.length();
        if (espacos < 1) {
            espacos = 1;
        }
        return rotulo + " ".repeat(espacos) + valor;
    }

    private static String moeda(BigDecimal valor) {
        if (valor == null) {
            valor = BigDecimal.ZERO;
        }
        return String.format(Locale.US, "%.2f", valor).replace('.', ',');
    }

    private static boolean tem(String s) {
        return s != null && !s.isBlank();
    }

    private static String descricaoFormaPagamento(formaPagamento f) {
        if (f == null) {
            return "Nao informada";
        }
        return switch (f) {
            case DINHEIRO -> "Dinheiro";
            case CARTAO -> "Cartao";
            case PIX -> "PIX";
            case APP -> "App";
        };
    }

    private void imprimirTexto(String textoParaImprimir, String nomeAlvo) throws Exception {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        PrintService impressoraSelecionada = null;

        for (PrintService service : services) {
            if (service.getName().equalsIgnoreCase(nomeAlvo)) {
                impressoraSelecionada = service;
                break;
            }
        }

        if (impressoraSelecionada == null) {
            StringBuilder disponiveis = new StringBuilder();
            for (PrintService service : services) {
                if (disponiveis.length() > 0) {
                    disponiveis.append(", ");
                }
                disponiveis.append(service.getName());
            }
            String lista = disponiveis.length() > 0 ? disponiveis.toString() : "nenhuma impressora detectada";
            throw new RuntimeException("Impressora não encontrada: \"" + nomeAlvo
                    + "\". Impressoras disponíveis: " + lista
                    + ". Selecione a impressora em Administração → Impressora.");
        }

        // A comanda é convertida para ASCII puro (sem acentos) antes de enviar:
        // impressoras térmicas ESC/POS usam code page de 1 byte (CP437/CP850) e o
        // DocFlavor.AUTOSENSE + driver CUPS não tratam UTF-8/acentos de forma confiável.
        // Assim, pedidos com dados acentuados (cliente logado no site) imprimem igual
        // aos pedidos do PDV.
        String textoFormatado = removerAcentos(textoParaImprimir + "\n\n\n\n\n");
        InputStream stream = new ByteArrayInputStream(textoFormatado.getBytes(StandardCharsets.US_ASCII));

        DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
        Doc documento = new SimpleDoc(stream, flavor, null);

        DocPrintJob job = impressoraSelecionada.createPrintJob();
        PrintRequestAttributeSet atributos = new HashPrintRequestAttributeSet();
        job.print(documento, atributos);

        stream.close();
    }

    private String removerAcentos(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        // Mantém apenas caracteres imprimíveis ASCII (e quebras de linha), substituindo o resto por '?'.
        return semAcentos.replaceAll("[^\\x20-\\x7E\\n\\r\\t]", "?");
    }
}
