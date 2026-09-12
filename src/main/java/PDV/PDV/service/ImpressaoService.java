package PDV.PDV.service;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;

import PDV.PDV.model.pedido;
import PDV.PDV.model.itensPedido;
import PDV.PDV.repository.pedidoRepository;
import org.springframework.stereotype.Service;

@Service
public class ImpressaoService {

    // Nome exato da impressora instalada no Painel de Controle / Sistema
    private final String NOME_IMPRESSORA = "termica";

    private final pedidoRepository pedidoRepo;

    public ImpressaoService(pedidoRepository pedidoRepo) {
        this.pedidoRepo = pedidoRepo;
    }

    public void imprimirPedido(Long id) throws Exception {
        // 1. Busca o pedido pelo ID no banco de dados
        pedido p = pedidoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado com o ID: " + id));

        // 2. Monta o texto do recibo estruturado no padrão exato solicitado
        StringBuilder sb = new StringBuilder();
        sb.append("==================================================\n");
        sb.append("COMANDA DE PEDIDO\n");
        sb.append("==================================================\n");

        if (p.getCliente() != null) {
            sb.append("Cliente: ").append(p.getCliente().getNome() != null ? p.getCliente().getNome() : "").append("\n");
            sb.append("Celular: ").append(p.getCliente().getCelular() != null ? p.getCliente().getCelular() : "").append("\n");

            String rua = p.getCliente().getRua() != null ? p.getCliente().getRua() : "";
            String numero = p.getCliente().getNumero() != null ? ", " + p.getCliente().getNumero() : "";
            sb.append("Endereço: ").append(rua).append(numero).append("\n");

            if (p.getCliente().getBairro() != null && !p.getCliente().getBairro().isEmpty()) {
                sb.append("Bairro: ").append(p.getCliente().getBairro()).append("\n");
            }
        }

        sb.append("\n==================================================\n");
        sb.append("ITENS:\n");

        // Itera sobre os itens do pedido cadastrados no banco
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

        sb.append("==================================================\n\n");

        BigDecimal taxaEntrega = p.getTaxaEntrega() != null ? p.getTaxaEntrega() : BigDecimal.ZERO;
        sb.append("Taxa de Entrega: R$ ").append(String.format("%.2f", taxaEntrega)).append("\n");
        sb.append("==================================================\n");

        BigDecimal valorTotal = p.getValorTotal() != null ? p.getValorTotal() : BigDecimal.ZERO;
        sb.append("TOTAL: R$ ").append(String.format("%.2f", valorTotal)).append("\n");

        // Tratamento da Forma de Pagamento (Convertendo Enum para String com segurança)
        String formaPagamento = "Não informada";
        if (p.getFormaPagamento() != null) {
            formaPagamento = p.getFormaPagamento().name();
        }
        sb.append("Pagamento: ").append(formaPagamento).append("\n");
        sb.append("==================================================\n");
        sb.append("Obrigado pela sua compra!\n");

        // 3. Envia para o método físico de impressão
        imprimirTexto(sb.toString());
    }

    private void imprimirTexto(String textoParaImprimir) throws Exception {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        PrintService impressoraSelecionada = null;

        for (PrintService service : services) {
            if (service.getName().equalsIgnoreCase(NOME_IMPRESSORA)) {
                impressoraSelecionada = service;
                break;
            }
        }

        if (impressoraSelecionada == null) {
            throw new RuntimeException("Impressora não encontrada: " + NOME_IMPRESSORA);
        }

        // Adiciona linhas extras no final para garantir que o papel avance o suficiente para o corte
        String textoFormatado = textoParaImprimir + "\n\n\n\n\n";
        InputStream stream = new ByteArrayInputStream(textoFormatado.getBytes("CP850"));

        DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
        Doc documento = new SimpleDoc(stream, flavor, null);

        DocPrintJob job = impressoraSelecionada.createPrintJob();
        PrintRequestAttributeSet atributos = new HashPrintRequestAttributeSet();
        job.print(documento, atributos);

        stream.close();
    }
}