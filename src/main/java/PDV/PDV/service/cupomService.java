package PDV.PDV.service;

import PDV.PDV.model.Enum.publicoCupom;
import PDV.PDV.model.Enum.tipoCupom;
import PDV.PDV.model.clientes;
import PDV.PDV.model.cupom;
import PDV.PDV.repository.cupomRepository;
import PDV.PDV.repository.pedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class cupomService {

    @Autowired
    private cupomRepository cupomRepo;

    @Autowired
    private pedidoRepository pedidoRepo;

    public List<cupom> listar() {
        return cupomRepo.findAllByOrderByIdDesc();
    }

    public cupom criar(String codigo, String descricao, tipoCupom tipo, BigDecimal valor,
                       BigDecimal valorMinimo, publicoCupom publico) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Informe o código do cupom.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Informe o tipo do cupom.");
        }
        String codigoNormalizado = codigo.trim().toUpperCase();
        if (cupomRepo.findByCodigoIgnoreCase(codigoNormalizado).isPresent()) {
            throw new IllegalArgumentException("Já existe um cupom com esse código.");
        }
        cupom c = new cupom();
        c.setCodigo(codigoNormalizado);
        c.setDescricao(descricao);
        c.setTipo(tipo);
        c.setValor(valor);
        c.setValorMinimo(valorMinimo != null ? valorMinimo : BigDecimal.ZERO);
        c.setPublico(publico != null ? publico : publicoCupom.TODOS);
        c.setAtivo(true);
        return cupomRepo.save(c);
    }

    public void remover(Long id) {
        cupomRepo.deleteById(id);
    }

    public cupom buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        return cupomRepo.findByCodigoIgnoreCase(codigo.trim()).orElse(null);
    }

    public BigDecimal calcularDesconto(String codigo, clientes cliente, BigDecimal subtotal, BigDecimal taxa) {
        if (codigo == null || codigo.isBlank()) {
            return BigDecimal.ZERO;
        }
        cupom c = cupomRepo.findByCodigoIgnoreCase(codigo.trim())
                .orElseThrow(() -> new IllegalArgumentException("Cupom inválido."));
        if (!Boolean.TRUE.equals(c.getAtivo())) {
            throw new IllegalArgumentException("Cupom inválido.");
        }

        BigDecimal subtotalSeguro = subtotal != null ? subtotal : BigDecimal.ZERO;
        BigDecimal taxaSegura = taxa != null ? taxa : BigDecimal.ZERO;

        BigDecimal minimo = c.getValorMinimo() != null ? c.getValorMinimo() : BigDecimal.ZERO;
        if (subtotalSeguro.compareTo(minimo) < 0) {
            throw new IllegalArgumentException("Cupom válido para pedidos acima de R$ "
                    + minimo.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ','));
        }

        if (c.getPublico() == publicoCupom.PRIMEIRO_PEDIDO && pedidoRepo.countByCliente(cliente) != 0) {
            throw new IllegalArgumentException("Este cupom é válido apenas para o primeiro pedido.");
        }
        if (c.getPublico() == publicoCupom.CLIENTE_EXISTENTE && pedidoRepo.countByCliente(cliente) < 1) {
            throw new IllegalArgumentException("Este cupom é válido apenas para clientes que já pediram.");
        }

        BigDecimal valor = c.getValor() != null ? c.getValor() : BigDecimal.ZERO;
        return switch (c.getTipo()) {
            case DESCONTO_FIXO -> valor.min(subtotalSeguro);
            case FRETE_GRATIS -> taxaSegura;
            case DESCONTO_ENTREGA -> valor.min(taxaSegura);
        };
    }
}
