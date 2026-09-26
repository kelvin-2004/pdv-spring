package PDV.PDV.service;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import PDV.PDV.model.pedido;
import PDV.PDV.model.Enum.formaPagamento;
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
        sb.append("================================\n");
        sb.append("COMANDA DE PEDIDO\n");
        sb.append("================================\n");

        if (p.getCliente() != null) {
            sb.append("Cliente: ").append(p.getCliente().getNome() != null ? p.getCliente().getNome() : "").append("\n");
            sb.append("Celular: ").append(p.getCliente().getCelular() != null ? p.getCliente().getCelular() : "").append("\n");

            // Endereço de entrega: prioriza o endereço informado no checkout (entregas),
            // que fica salvo na entrega do pedido; cai no cadastro do cliente quando não houver.
            String rua = null, numero = null, bairro = null, complemento = null;
            if (p.getEntregas() != null) {
                rua = p.getEntregas().getRua();
                numero = p.getEntregas().getNumero();
                bairro = p.getEntregas().getBairro();
                complemento = p.getEntregas().getComplemento();
            }
            if (rua == null || rua.isBlank()) rua = p.getCliente().getRua();
            if (numero == null || numero.isBlank()) numero = p.getCliente().getNumero();
            if (bairro == null || bairro.isBlank()) bairro = p.getCliente().getBairro();
            if (complemento == null || complemento.isBlank()) complemento = p.getCliente().getComplemento();

            sb.append("Endereço: ").append(rua != null ? rua : "");
            if (numero != null && !numero.isBlank()) {
                sb.append(", ").append(numero);
            }
            sb.append("\n");

            if (bairro != null && !bairro.isBlank()) {
                sb.append("Bairro: ").append(bairro).append("\n");
            }
            if (complemento != null && !complemento.isBlank()) {
                sb.append("Complemento: ").append(complemento).append("\n");
            }
            if (p.getCliente().getPontoReferencia() != null && !p.getCliente().getPontoReferencia().isEmpty()) {
                sb.append("Referência: ").append(p.getCliente().getPontoReferencia()).append("\n");
            }
        }

        sb.append("\n================================\n");
        sb.append("ITENS:\n");

        if (p.getItens() != null && !p.getItens().isEmpty()) {
            for (itensPedido item : p.getItens()) {
                String nomeProduto = (item.getProduto() != null && item.getProduto().getNome() != null)
                        ? item.getProduto().getNome()
                        : "Item";

                BigDecimal valorSubtotal = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;

                sb.append("  ").append(item.getQuantidade()).append("x ").append(nomeProduto)
                        .append(" - R$ ").append(String.format("%.2f", valorSubtotal)).append("\n");

                if (item.getObservacao() != null && !item.getObservacao().trim().isEmpty()) {
                    sb.append("     Obs: ").append(item.getObservacao()).append("\n");
                }
            }
        } else {
            sb.append("  Nenhum item listado.\n");
        }

        sb.append("================================\n\n");

        BigDecimal taxaEntrega = p.getTaxaEntrega() != null ? p.getTaxaEntrega() : BigDecimal.ZERO;
        sb.append("Taxa de Entrega: R$ ").append(String.format("%.2f", taxaEntrega)).append("\n");

        if (p.getTempoEntregaMinutos() != null) {
            sb.append("Tempo de Entrega: ").append(p.getTempoEntregaMinutos()).append(" min\n");
        }
        if (p.getTempoPreparoMinutos() != null) {
            sb.append("Tempo de Preparo: ").append(p.getTempoPreparoMinutos()).append(" min\n");
        }
        sb.append("================================\n");

        BigDecimal valorTotal = p.getValorTotal() != null ? p.getValorTotal() : BigDecimal.ZERO;
        sb.append("TOTAL: R$ ").append(String.format("%.2f", valorTotal)).append("\n");

        String formaPagamentoStr = "Não informada";
        if (p.getFormaPagamento() != null) {
            formaPagamentoStr = p.getFormaPagamento().name();
        }
        sb.append("Pagamento: ").append(formaPagamentoStr).append("\n");

        if (Boolean.TRUE.equals(p.getPagamentoNaEntrega())) {
            sb.append("\n*** COBRAR NA ENTREGA ***\n");
            if (p.getFormaPagamento() == formaPagamento.CARTAO) {
                sb.append(">>> COBRAR VIA MAQUININHA DE CARTÃO <<<\n");
            } else if (p.getFormaPagamento() == formaPagamento.DINHEIRO) {
                sb.append(">>> RECEBER EM DINHEIRO <<<\n");
            }
            sb.append("\n");
        }

        if (p.getFormaPagamento() == formaPagamento.DINHEIRO && p.getTrocoPara() != null) {
            BigDecimal valorRecebido = p.getTrocoPara();
            BigDecimal troco = valorRecebido.subtract(valorTotal);
            if (troco.compareTo(BigDecimal.ZERO) < 0) {
                troco = BigDecimal.ZERO;
            }
            sb.append("Valor Recebido: R$ ").append(String.format("%.2f", valorRecebido)).append("\n");
            sb.append("Troco: R$ ").append(String.format("%.2f", troco)).append("\n");
        }

        if (p.getObservacoes() != null && !p.getObservacoes().trim().isEmpty()) {
            sb.append("Observações: ").append(p.getObservacoes()).append("\n");
        }
        sb.append("================================\n");
        sb.append("Obrigado pela sua compra!\n");

        return sb.toString();
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
