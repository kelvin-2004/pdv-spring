package PDV.PDV.config;

import PDV.PDV.model.clientes;
import PDV.PDV.model.pedido;
import PDV.PDV.model.produtos;
import PDV.PDV.model.Enum.categoriaPedido;
import PDV.PDV.model.Enum.formaPagamento;
import PDV.PDV.model.Enum.statusPedido;
import PDV.PDV.repository.clienteRepository;
import PDV.PDV.repository.pedidoRepository;
import PDV.PDV.repository.produtoRepository;
import PDV.PDV.service.pedidoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "app.seed.demo", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final produtoRepository produtoRepo;
    private final clienteRepository clienteRepo;
    private final pedidoRepository pedidoRepo;
    private final pedidoService pedidoService;

    public DemoDataSeeder(produtoRepository produtoRepo,
                          clienteRepository clienteRepo,
                          pedidoRepository pedidoRepo,
                          pedidoService pedidoService) {
        this.produtoRepo = produtoRepo;
        this.clienteRepo = clienteRepo;
        this.pedidoRepo = pedidoRepo;
        this.pedidoService = pedidoService;
    }

    @Override
    public void run(String... args) {
        log.info("[SEED] Iniciando criação de dados fake (produtos/clientes são reaproveitados se já existirem).");

        produtos costela = garantirProduto("Marmita de Costela", "25.00", categoriaPedido.MARMITA);
        produtos frango = garantirProduto("Marmita de Frango Grelhado", "20.00", categoriaPedido.MARMITA);
        produtos carnesol = garantirProduto("Marmita de Carne de Sol", "24.00", categoriaPedido.MARMITA);
        produtos veggie = garantirProduto("Marmita Vegetariana", "18.00", categoriaPedido.MARMITA);
        produtos peixe = garantirProduto("Marmita de Peixe", "22.00", categoriaPedido.MARMITA);
        produtos coca = garantirProduto("Coca-Cola Lata 350ml", "5.00", categoriaPedido.BEBIDA);
        produtos pudim = garantirProduto("Pudim de Leite", "8.00", categoriaPedido.SOBREMESA);

        clientes ana = garantirCliente("Ana Souza", "11988881111", "Rua das Flores", "120", "Centro", "01010-000", "Apto 21", "Portão verde");
        clientes bruno = garantirCliente("Bruno Lima", "11977772222", "Av. Brasil", "45", "Jardim América", "01310-001", null, null);
        clientes carla = garantirCliente("Carla Mendes", "11966663333", "Rua XV de Novembro", "789", "Centro", "01020-002", "Casa 2", "Campainha quebrada");
        clientes diego = garantirCliente("Diego Rocha", "11955554444", "Rua dos Ipês", "55", "Boa Vista", "02030-003", null, "Entrar pelos fundos");
        clientes elisa = garantirCliente("Elisa Castro", "11944445555", "Alameda Santos", "1000", "Pinheiros", "01415-004", "Bloco B, 32", null);

        criarPedido(ana, formaPagamento.PIX, "6.00", 35, 25, "Cliente pediu sem cebola na salada.",
                item(costela, 2, "sem feijão"), item(coca, 1, null));
        criarPedido(bruno, formaPagamento.DINHEIRO, "5.00", 40, 15, null,
                item(frango, 1, null), item(pudim, 2, null));
        criarPedido(carla, formaPagamento.CARTAO, "0.00", 30, 45, "Mandar talheres descartáveis.",
                item(carnesol, 1, "arroz separado"), item(coca, 1, null));
        criarPedido(diego, formaPagamento.PIX, "6.00", 50, 30, null,
                item(peixe, 3, null));

        criarPedido(elisa, formaPagamento.PIX, "6.00", 40, 30, null, statusPedido.AGUARDANDO_ENTREGADOR,
                item(veggie, 2, null), item(coca, 2, null));
        criarPedido(carla, formaPagamento.CARTAO, "5.00", 45, 30, "Troco para R$ 100,00.", statusPedido.AGUARDANDO_ENTREGADOR,
                item(costela, 1, "molho à parte"), item(pudim, 1, null));

        criarPedido(bruno, formaPagamento.DINHEIRO, "5.00", 40, 30, null, statusPedido.A_CAMINHO,
                item(frango, 2, null));
        criarPedido(ana, formaPagamento.PIX, "6.00", 35, 30, null, statusPedido.A_CAMINHO,
                item(carnesol, 1, null), item(coca, 1, null));

        criarPedido(diego, formaPagamento.CARTAO, "5.00", 40, 30, null, statusPedido.CONCLUIDO,
                item(costela, 1, null));
        criarPedido(elisa, formaPagamento.DINHEIRO, "0.00", 30, 30, null, statusPedido.CONCLUIDO,
                item(veggie, 1, null), item(pudim, 1, null));

        criarPedido(carla, formaPagamento.PIX, "5.00", 45, 30, "Cliente desistiu.", statusPedido.CANCELADO,
                item(peixe, 1, null));

        log.info("[SEED] Dados fake criados: {} produtos, {} clientes, {} pedidos.",
                produtoRepo.count(), clienteRepo.count(), pedidoRepo.count());
    }

    private produtos garantirProduto(String nome, String preco, categoriaPedido categoria) {
        return produtoRepo.findByNome(nome).orElseGet(() -> {
            produtos p = new produtos();
            p.setNome(nome);
            p.setPreco(new BigDecimal(preco));
            p.setCategoriaPedido(categoria);
            p.setAtivo(true);
            p.setEstoque(50);
            p.setImgUrl("");
            return produtoRepo.save(p);
        });
    }

    private clientes garantirCliente(String nome, String celular, String rua, String numero,
                                     String bairro, String cep, String complemento, String referencia) {
        return clienteRepo.findByCelular(celular).orElseGet(() -> {
            clientes c = new clientes();
            c.setNome(nome);
            c.setCelular(celular);
            c.setRua(rua);
            c.setNumero(numero);
            c.setBairro(bairro);
            c.setCep(cep);
            c.setComplemento(complemento);
            c.setPontoReferencia(referencia);
            c.setSenha("");
            return clienteRepo.save(c);
        });
    }

    private void criarPedido(clientes c, formaPagamento f, String taxa, int tempoEntrega,
                             int tempoPreparo, String observacoes, ItemSpec... itens) {
        criarPedido(c, f, taxa, tempoEntrega, tempoPreparo, observacoes, null, itens);
    }

    private void criarPedido(clientes c, formaPagamento f, String taxa, int tempoEntrega,
                             int tempoPreparo, String observacoes, statusPedido status, ItemSpec... itens) {
        pedido p = new pedido();
        p.setCliente(c);
        p.setFormaPagamento(f);
        p.setTaxaEntrega(new BigDecimal(taxa));
        p.setTempoEntregaMinutos(tempoEntrega);
        p.setTempoPreparoMinutos(tempoPreparo);
        p.setObservacoes(observacoes);

        pedido salvo = pedidoService.novoPedidoComItens(p, carrinhoJson(itens));

        if (status != null && status != statusPedido.PREPARANDO) {
            pedidoService.atualizarStatus(salvo.getId(), status);
        }
    }

    private ItemSpec item(produtos produto, int quantidade, String observacao) {
        return new ItemSpec(produto, quantidade, observacao);
    }

    private String carrinhoJson(ItemSpec... itens) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < itens.length; i++) {
            if (i > 0) sb.append(",");
            ItemSpec s = itens[i];
            sb.append("{\"id\":").append(s.produto.getId())
              .append(",\"quantidade\":").append(s.quantidade);
            if (s.observacao != null && !s.observacao.isBlank()) {
                sb.append(",\"observacao\":\"").append(jsonEscape(s.observacao)).append("\"");
            }
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String jsonEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static class ItemSpec {
        final produtos produto;
        final int quantidade;
        final String observacao;

        ItemSpec(produtos produto, int quantidade, String observacao) {
            this.produto = produto;
            this.quantidade = quantidade;
            this.observacao = observacao;
        }
    }
}
