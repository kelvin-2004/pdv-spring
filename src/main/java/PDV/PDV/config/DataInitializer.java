package PDV.PDV.config;

import PDV.PDV.repository.clienteRepository;
import PDV.PDV.repository.itensPedidoRepository;
import PDV.PDV.repository.pedidoRepository;
import PDV.PDV.repository.produtoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(
            clienteRepository clienteRepository,
            produtoRepository produtoRepository,
            pedidoRepository pedidoRepository,
            itensPedidoRepository itensPedidoRepository) {

        return args -> {
            // Script desativado para não inserir dados fakes.
        };
    }
}