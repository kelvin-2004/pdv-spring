package PDV.PDV.service;

import PDV.PDV.model.Enum.formaPagamento;
import PDV.PDV.model.Enum.tipoPedido;
import PDV.PDV.model.clientes;
import PDV.PDV.model.itensPedido;
import PDV.PDV.model.pedido;
import PDV.PDV.model.produtos;
import PDV.PDV.repository.pedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImpressaoServiceTest {

    private pedidoRepository pedidoRepo;
    private ImpressaoService impressaoService;

    @BeforeEach
    void setUp() {
        pedidoRepo = mock(pedidoRepository.class);
        impressaoService = new ImpressaoService(pedidoRepo, mock(configuracaoService.class));
    }

    private pedido pedidoCompleto() {
        pedido p = new pedido();
        p.setId(1L);
        p.setNumeroPedidoCliente(2);
        p.setTipoPedido(tipoPedido.DELIVERY);
        p.setFormaPagamento(formaPagamento.DINHEIRO);
        p.setValorTotal(new BigDecimal("45.90"));
        p.setTaxaEntrega(new BigDecimal("6.00"));

        clientes c = new clientes();
        c.setNome("Joao da Silva");
        c.setCelular("11999998888");
        c.setRua("Rua das Flores");
        c.setNumero("123");
        c.setBairro("Centro");
        c.setCep("07953-170");
        p.setCliente(c);

        produtos prod = new produtos();
        prod.setNome("Marmita P");
        prod.setPreco(new BigDecimal("19.95"));

        itensPedido item = new itensPedido();
        item.setProduto(prod);
        item.setQuantidade(2);
        item.setPrecoUnitario(new BigDecimal("19.95"));
        item.setSubtotal(new BigDecimal("39.90"));
        item.setPedido(p);

        List<itensPedido> itens = new ArrayList<>();
        itens.add(item);
        p.setItens(itens);

        return p;
    }

    @Test
    void modoNormalNaoContemComandosEscPos() {
        when(pedidoRepo.findById(1L)).thenReturn(Optional.of(pedidoCompleto()));

        String texto = impressaoService.montarTextoPedido(1L);

        assertThat(texto).contains("MARMITAS SOUSA");
        assertThat(texto).contains("Pedido: #2");
        assertThat(texto).contains("Marmita P");
        assertThat(texto).contains("45,90");
        assertThat(texto).doesNotContain("\u001B");
    }

    @Test
    void modoEscPosDestacaCabecalhoETotal() {
        when(pedidoRepo.findById(1L)).thenReturn(Optional.of(pedidoCompleto()));

        String texto = impressaoService.montarTextoPedido(1L, true);

        // A comanda ganha destaque gráfico (negrito + altura dupla) e mantém o conteúdo.
        assertThat(texto).contains("\u001B!\u0018");
        assertThat(texto).contains("\u001B!\u0008");
        assertThat(texto).contains("MARMITAS SOUSA");
        assertThat(texto).contains("Pedido: #2");
        assertThat(texto).contains("45,90");
    }

    @Test
    void pedidoInexistenteLancaExcecao() {
        when(pedidoRepo.findById(99L)).thenReturn(Optional.empty());

        assertThat(org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> impressaoService.montarTextoPedido(99L)))
                .hasMessageContaining("Pedido não encontrado");
    }
}
