package PDV.PDV.service;

import PDV.PDV.model.Enum.publicoCupom;
import PDV.PDV.model.Enum.tipoCupom;
import PDV.PDV.model.clientes;
import PDV.PDV.model.cupom;
import PDV.PDV.repository.cupomRepository;
import PDV.PDV.repository.pedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CupomServiceTest {

    @Mock private cupomRepository cupomRepo;
    @Mock private pedidoRepository pedidoRepo;

    @InjectMocks private cupomService cupomService;

    private cupom cupomFixo;
    private cupom cupomFrete;
    private clientes cliente;

    @BeforeEach
    void setUp() {
        cliente = new clientes();

        cupomFixo = new cupom();
        cupomFixo.setCodigo("DESC10");
        cupomFixo.setTipo(tipoCupom.DESCONTO_FIXO);
        cupomFixo.setValor(new BigDecimal("10.00"));
        cupomFixo.setValorMinimo(BigDecimal.ZERO);
        cupomFixo.setPublico(publicoCupom.TODOS);
        cupomFixo.setAtivo(true);

        cupomFrete = new cupom();
        cupomFrete.setCodigo("FRETE");
        cupomFrete.setTipo(tipoCupom.FRETE_GRATIS);
        cupomFrete.setValor(BigDecimal.ZERO);
        cupomFrete.setValorMinimo(BigDecimal.ZERO);
        cupomFrete.setPublico(publicoCupom.TODOS);
        cupomFrete.setAtivo(true);
    }

    @Test
    void codigoEmBrancoRetornaZero() {
        BigDecimal desconto = cupomService.calcularDesconto("", cliente, new BigDecimal("50.00"), BigDecimal.ZERO);
        assertThat(desconto).isEqualByComparingTo("0");
    }

    @Test
    void cupomInexistenteLancaExcecao() {
        when(cupomRepo.findByCodigoIgnoreCase("NAOEXISTE")).thenReturn(Optional.empty());
        assertThatThrownBy(() ->
                cupomService.calcularDesconto("NAOEXISTE", cliente, new BigDecimal("50.00"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cupom inválido.");
    }

    @Test
    void descontoFixoAplicadoNormalmente() {
        when(cupomRepo.findByCodigoIgnoreCase("DESC10")).thenReturn(Optional.of(cupomFixo));
        BigDecimal desconto = cupomService.calcularDesconto("DESC10", cliente, new BigDecimal("50.00"), new BigDecimal("6.00"));
        assertThat(desconto).isEqualByComparingTo("10.00");
    }

    @Test
    void descontoFixoNaoUltrapassaSubtotal() {
        when(cupomRepo.findByCodigoIgnoreCase("DESC10")).thenReturn(Optional.of(cupomFixo));
        BigDecimal desconto = cupomService.calcularDesconto("DESC10", cliente, new BigDecimal("5.00"), BigDecimal.ZERO);
        assertThat(desconto).isEqualByComparingTo("5.00");
    }

    @Test
    void freteGratisRetornaValorDaTaxa() {
        when(cupomRepo.findByCodigoIgnoreCase("FRETE")).thenReturn(Optional.of(cupomFrete));
        BigDecimal desconto = cupomService.calcularDesconto("FRETE", cliente, new BigDecimal("50.00"), new BigDecimal("6.00"));
        assertThat(desconto).isEqualByComparingTo("6.00");
    }

    @Test
    void subtotalAbaixoDoMinimoLancaExcecao() {
        cupomFixo.setValorMinimo(new BigDecimal("30.00"));
        when(cupomRepo.findByCodigoIgnoreCase("DESC10")).thenReturn(Optional.of(cupomFixo));
        assertThatThrownBy(() ->
                cupomService.calcularDesconto("DESC10", cliente, new BigDecimal("20.00"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("acima de R$");
    }

    @Test
    void primeiroPedidoRejeitaClienteComHistorico() {
        cupomFixo.setPublico(publicoCupom.PRIMEIRO_PEDIDO);
        when(cupomRepo.findByCodigoIgnoreCase("DESC10")).thenReturn(Optional.of(cupomFixo));
        when(pedidoRepo.countByCliente(cliente)).thenReturn(1L);
        assertThatThrownBy(() ->
                cupomService.calcularDesconto("DESC10", cliente, new BigDecimal("50.00"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("primeiro pedido");
    }

    @Test
    void primeiroPedidoPermiteClienteSemHistorico() {
        cupomFixo.setPublico(publicoCupom.PRIMEIRO_PEDIDO);
        when(cupomRepo.findByCodigoIgnoreCase("DESC10")).thenReturn(Optional.of(cupomFixo));
        when(pedidoRepo.countByCliente(cliente)).thenReturn(0L);
        BigDecimal desconto = cupomService.calcularDesconto("DESC10", cliente, new BigDecimal("50.00"), BigDecimal.ZERO);
        assertThat(desconto).isEqualByComparingTo("10.00");
    }

    @Test
    void clienteExistenteRejeitaSemHistorico() {
        cupomFixo.setPublico(publicoCupom.CLIENTE_EXISTENTE);
        when(cupomRepo.findByCodigoIgnoreCase("DESC10")).thenReturn(Optional.of(cupomFixo));
        when(pedidoRepo.countByCliente(cliente)).thenReturn(0L);
        assertThatThrownBy(() ->
                cupomService.calcularDesconto("DESC10", cliente, new BigDecimal("50.00"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já pediram");
    }
}
