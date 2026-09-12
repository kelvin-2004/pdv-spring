package PDV.PDV.config;

import PDV.PDV.model.Enum.categoriaPedido;
import PDV.PDV.model.Enum.statusPedido;
import PDV.PDV.model.Enum.formaPagamento;
import PDV.PDV.model.clientes;
import PDV.PDV.model.itensPedido;
import PDV.PDV.model.pedido;
import PDV.PDV.model.produtos;
import PDV.PDV.repository.clienteRepository;
import PDV.PDV.repository.itensPedidoRepository;
import PDV.PDV.repository.pedidoRepository;
import PDV.PDV.repository.produtoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(
            clienteRepository clienteRepository,
            produtoRepository produtoRepository,
            pedidoRepository pedidoRepository,
            itensPedidoRepository itensPedidoRepository) {

        return args -> {
            // Evita duplicar se já houver pedidos cadastrados
            if (pedidoRepository.count() > 0) {
                return;
            }

            // 1. Populando Clientes
            clientes c1 = new clientes();
            c1.setNome("Mairla Sampaio Ferracini");
            c1.setCelular("11988887777");
            c1.setRua("Rua das Flores");
            c1.setNumero("123");
            c1.setBairro("Centro");
            c1.setCep("07900-000");
            c1.setComplemento("Casa A");
            c1.setPontoReferencia("Próximo à praça");

            clientes c2 = new clientes();
            c2.setNome("Telma Maria de Jesus Sousa");
            c2.setCelular("11977776666");
            c2.setRua("Av. Principal");
            c2.setNumero("456");
            c2.setBairro("Jardim América");
            c2.setCep("07900-111");
            c2.setComplemento("Apto 202");

            clientes c3 = new clientes();
            c3.setNome("Carlos Alberto Souza");
            c3.setCelular("11966665555");
            c3.setRua("Rua dos Lírios");
            c3.setNumero("789");
            c3.setBairro("Vila Nova");
            c3.setCep("07900-222");

            clienteRepository.saveAll(Arrays.asList(c1, c2, c3));

            // 2. Populando Produtos
            produtos p1 = new produtos();
            p1.setNome("Marmita Executiva de Frango Grelhado");
            p1.setDescricao("Frango grelhado, arroz, feijão, batata frita e salada");
            p1.setPreco(new BigDecimal("22.50"));
            p1.setEstoque(50);
            p1.setCategoriaPedido(categoriaPedido.MARMITA);
            p1.setImgUrl("uploads/default.png");

            produtos p2 = new produtos();
            p2.setNome("Marmita de Carne Moída com Purê");
            p2.setDescricao("Carne moída caseira, purê de batatas, arroz e feijão");
            p2.setPreco(new BigDecimal("25.00"));
            p2.setEstoque(40);
            p2.setCategoriaPedido(categoriaPedido.MARMITA);
            p2.setImgUrl("uploads/default.png");

            produtos p3 = new produtos();
            p3.setNome("Refrigerante Lata 350ml");
            p3.setDescricao("Coca-Cola ou Guaraná Antarctica");
            p3.setPreco(new BigDecimal("6.00"));
            p3.setEstoque(100);
            p3.setCategoriaPedido(categoriaPedido.BEBIDA);
            p3.setImgUrl("uploads/default.png");

            produtoRepository.saveAll(Arrays.asList(p1, p2, p3));

            // 3. Pedido 1 (Status: PREPARANDO)
            pedido ped1 = new pedido();
            ped1.setCliente(c1);
            ped1.setDataHoraPedido(OffsetDateTime.now().minusHours(2));
            ped1.setValorTotal(new BigDecimal("51.00"));
            ped1.setTaxaEntrega(new BigDecimal("5.00"));
            ped1.setFormaPagamento(formaPagamento.PIX);
            ped1.setStatusPedido(statusPedido.PREPARANDO);
            ped1.setNumeroPedidoCliente(1);
            pedidoRepository.save(ped1);

            itensPedido item1 = new itensPedido();
            item1.setPedido(ped1);
            item1.setProduto(p1);
            item1.setQuantidade(2);
            item1.setPrecoUnitario(new BigDecimal("22.50"));
            item1.setSubtotal(new BigDecimal("45.00"));
            item1.setObservacao("Sem cebola no frango");

            itensPedido item2 = new itensPedido();
            item2.setPedido(ped1);
            item2.setProduto(p3);
            item2.setQuantidade(1);
            item2.setPrecoUnitario(new BigDecimal("6.00"));
            item2.setSubtotal(new BigDecimal("6.00"));
            item2.setObservacao("Gelada");
            itensPedidoRepository.saveAll(Arrays.asList(item1, item2));

            // 4. Pedido 2 (Status: AGUARDANDO_ENTREGADOR / Pronto)
            pedido ped2 = new pedido();
            ped2.setCliente(c2);
            ped2.setDataHoraPedido(OffsetDateTime.now().minusMinutes(50));
            ped2.setValorTotal(new BigDecimal("25.00"));
            ped2.setTaxaEntrega(new BigDecimal("0.00"));
            ped2.setFormaPagamento(formaPagamento.DINHEIRO);
            ped2.setTrocoPara(new BigDecimal("50.00"));
            ped2.setStatusPedido(statusPedido.AGUARDANDO_ENTREGADOR);
            ped2.setNumeroPedidoCliente(1);
            pedidoRepository.save(ped2);

            itensPedido item3 = new itensPedido();
            item3.setPedido(ped2);
            item3.setProduto(p2);
            item3.setQuantidade(1);
            item3.setPrecoUnitario(new BigDecimal("25.00"));
            item3.setSubtotal(new BigDecimal("25.00"));
            item3.setObservacao("Caprichar no purê");
            itensPedidoRepository.save(item3);

            // 5. Pedido 3 (Status: A_CAMINHO / Despachado)
            pedido ped3 = new pedido();
            ped3.setCliente(c3);
            ped3.setDataHoraPedido(OffsetDateTime.now().minusMinutes(20));
            ped3.setValorTotal(new BigDecimal("28.50"));
            ped3.setTaxaEntrega(new BigDecimal("6.00"));
            ped3.setFormaPagamento(formaPagamento.CARTAO);
            ped3.setStatusPedido(statusPedido.A_CAMINHO);
            ped3.setNumeroPedidoCliente(2);
            pedidoRepository.save(ped3);

            itensPedido item4 = new itensPedido();
            item4.setPedido(ped3);
            item4.setProduto(p1);
            item4.setQuantidade(1);
            item4.setPrecoUnitario(new BigDecimal("22.50"));
            item4.setSubtotal(new BigDecimal("22.50"));
            itensPedidoRepository.save(item4);

            // 6. Pedido 4 (Status: CONCLUIDO)
            pedido ped4 = new pedido();
            ped4.setCliente(c1);
            ped4.setDataHoraPedido(OffsetDateTime.now().minusHours(4));
            ped4.setValorTotal(new BigDecimal("31.00"));
            ped4.setTaxaEntrega(new BigDecimal("5.00"));
            ped4.setFormaPagamento(formaPagamento.PIX);
            ped4.setStatusPedido(statusPedido.CONCLUIDO);
            ped4.setNumeroPedidoCliente(2);
            pedidoRepository.save(ped4);

            // 7. Pedido 5 (Status: CANCELADO)
            pedido ped5 = new pedido();
            ped5.setCliente(c2);
            ped5.setDataHoraPedido(OffsetDateTime.now().minusHours(5));
            ped5.setValorTotal(new BigDecimal("6.00"));
            ped5.setTaxaEntrega(new BigDecimal("0.00"));
            ped5.setFormaPagamento(formaPagamento.PIX);
            ped5.setStatusPedido(statusPedido.CANCELADO);
            ped5.setNumeroPedidoCliente(2);
            pedidoRepository.save(ped5);
        };
    }
}