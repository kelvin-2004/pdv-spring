package PDV.PDV.config;

import PDV.PDV.model.Enum.categoriaPedido;
import PDV.PDV.model.clientes;
import PDV.PDV.model.produtos;
import PDV.PDV.model.pedido;
import PDV.PDV.model.itensPedido;
import PDV.PDV.repository.clienteRepository;
import PDV.PDV.repository.produtoRepository;
import PDV.PDV.repository.pedidoRepository;
import PDV.PDV.repository.itensPedidoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(produtoRepository produtoRepository,
                                      clienteRepository clienteRepository,
                                      pedidoRepository pedidoRepository,
                                      itensPedidoRepository itensRepository) {
        return args -> {
            // 1. Cadastra produtos apenas se a tabela estiver vazia
            if (produtoRepository.count() == 0) {
                produtos p1 = new produtos();
                p1.setNome("Marmita Executiva de Frango");
                p1.setDescricao("Arroz, feijão, filé de frango grelhado, batata frita e salada");
                p1.setPreco(BigDecimal.valueOf(22.00));
                p1.setCategoriaPedido(categoriaPedido.MARMITA);
                p1.setAtivo(true);
                p1.setImgUrl("/img/logo.png");
                produtoRepository.save(p1);

                produtos p2 = new produtos();
                p2.setNome("Marmita Executiva de Carne");
                p2.setDescricao("Arroz, feijão, bife acebolado, purê de batatas e salada");
                p2.setPreco(BigDecimal.valueOf(25.00));
                p2.setCategoriaPedido(categoriaPedido.MARMITA);
                p2.setAtivo(true);
                p2.setImgUrl("/img/logo.png");
                produtoRepository.save(p2);

                produtos p3 = new produtos();
                p3.setNome("Marmita Vegetariana");
                p3.setDescricao("Arroz integral, feijão, omelete de ervas, legumes a vapor e mix de folhas");
                p3.setPreco(BigDecimal.valueOf(20.00));
                p3.setCategoriaPedido(categoriaPedido.MARMITA);
                p3.setAtivo(true);
                p3.setImgUrl("/img/logo.png");
                produtoRepository.save(p3);

                produtos p4 = new produtos();
                p4.setNome("Coca-Cola Lata 350ml");
                p4.setDescricao("Refrigerante gelado lata 350ml");
                p4.setPreco(BigDecimal.valueOf(6.00));
                p4.setCategoriaPedido(categoriaPedido.BEBIDA);
                p4.setAtivo(true);
                p4.setImgUrl("/img/logo.png");
                produtoRepository.save(p4);

                produtos p5 = new produtos();
                p5.setNome("Suco Natural de Laranja 500ml");
                p5.setDescricao("Suco natural feito na hora da fruta");
                p5.setPreco(BigDecimal.valueOf(8.00));
                p5.setCategoriaPedido(categoriaPedido.BEBIDA);
                p5.setAtivo(true);
                p5.setImgUrl("/img/logo.png");
                produtoRepository.save(p5);

                produtos p6 = new produtos();
                p6.setNome("Pudim de Leite Condensado");
                p6.setDescricao("Fatia generosa de pudim caseiro com calda de caramelo");
                p6.setPreco(BigDecimal.valueOf(7.50));
                p6.setCategoriaPedido(categoriaPedido.SOBREMESA);
                p6.setAtivo(true);
                p6.setImgUrl("/img/logo.png");
                produtoRepository.save(p6);
            }

            // 2. Cadastra clientes apenas se a tabela estiver vazia
            if (clienteRepository.count() == 0) {
                clientes c1 = new clientes();
                c1.setNome("Carlos Silva");
                c1.setCelular("(11) 98888-1111");
                c1.setRua("Rua das Flores, 123");
                c1.setBairro("Centro");
                c1.setPontoReferencia("Cliente prefere sem cebola na salada.");
                clienteRepository.save(c1);

                clientes c2 = new clientes();
                c2.setNome("Ana Paula Souza");
                c2.setCelular("(11) 97777-2222");
                c2.setRua("Av. Principal, 456 - Apto 32");
                c2.setBairro("Jardim América");
                c2.setPontoReferencia("Deixar na portaria se não atender.");
                clienteRepository.save(c2);

                clientes c3 = new clientes();
                c3.setNome("Marcos Oliveira");
                c3.setCelular("(11) 96666-3333");
                c3.setRua("Rua XV de Novembro, 789");
                c3.setBairro("Vila Nova");
                c3.setPontoReferencia("Pagamento via Pix na entrega.");
                clienteRepository.save(c3);
            }

            // 3. Cadastra Pedidos e seus Itens de Exemplo se a tabela de pedidos estiver vazia
            if (pedidoRepository.count() == 0 && clienteRepository.count() > 0 && produtoRepository.count() > 0) {

                clientes clienteExemplo = clienteRepository.findAll().get(0);
                produtos marmita = produtoRepository.findAll().get(0); // Marmita Executiva de Frango (R$ 22,00)
                produtos bebida = produtoRepository.findAll().get(3);  // Coca-Cola (R$ 6,00)

                // Criação do Pedido 1
                pedido pedido1 = new pedido();
                pedido1.setCliente(clienteExemplo);
                // Ajuste caso o nome do seu setter de data seja diferente (ex: setDataPedido)
                // pedido1.setDataPedido(LocalDateTime.now().minusHours(1));
                pedido1.setValorTotal(BigDecimal.valueOf(28.00)); // 22.00 + 6.00

                pedidoRepository.save(pedido1);

                // Salvando os itens vinculados ao Pedido 1
                itensPedido item1 = new itensPedido();
                item1.setPedidos(pedido1);
                item1.setProduto(marmita);
                item1.setQuantidade(1);
                item1.setPrecoUnitario(marmita.getPreco());
                itensRepository.save(item1);

                itensPedido item2 = new itensPedido();
                item2.setPedidos(pedido1);
                item2.setProduto(bebida);
                item2.setQuantidade(1);
                item2.setPrecoUnitario(bebida.getPreco());
                itensRepository.save(item2);
            }
        };
    }
}