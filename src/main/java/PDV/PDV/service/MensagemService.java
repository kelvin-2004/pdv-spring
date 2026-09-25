package PDV.PDV.service;

import PDV.PDV.dto.MensagemDTO;
import PDV.PDV.model.Enum.remetenteMensagem;
import PDV.PDV.model.mensagemPedido;
import PDV.PDV.model.pedido;
import PDV.PDV.repository.mensagemPedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MensagemService {

    private final mensagemPedidoRepository repo;

    public MensagemService(mensagemPedidoRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<MensagemDTO> listar(Long pedidoId) {
        return repo.findByPedidoIdOrderByDataHoraAsc(pedidoId).stream()
                .map(this::toDto).toList();
    }

    @Transactional
    public MensagemDTO enviar(pedido p, remetenteMensagem remetente, String texto) {
        mensagemPedido m = new mensagemPedido();
        m.setPedido(p);
        m.setRemetente(remetente);
        m.setTexto(texto);
        m.setDataHora(OffsetDateTime.now());
        m.setLida(false);
        return toDto(repo.save(m));
    }

    @Transactional
    public void marcarLidas(Long pedidoId, remetenteMensagem remetente) {
        repo.marcarLidas(pedidoId, remetente);
    }

    @Transactional(readOnly = true)
    public int naoLidas(Long pedidoId, remetenteMensagem remetente) {
        return (int) repo.countByPedidoIdAndRemetenteAndLidaFalse(pedidoId, remetente);
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> naoLidasPorPedido(remetenteMensagem remetente) {
        Map<Long, Integer> mapa = new LinkedHashMap<>();
        for (Object[] linha : repo.contarNaoLidasPorPedido(remetente)) {
            mapa.put((Long) linha[0], ((Number) linha[1]).intValue());
        }
        return mapa;
    }

    private MensagemDTO toDto(mensagemPedido m) {
        return new MensagemDTO(
                m.getId(),
                m.getRemetente() != null ? m.getRemetente().name() : null,
                m.getTexto(),
                m.getDataHora() != null ? m.getDataHora().toInstant().toEpochMilli() : 0L,
                Boolean.TRUE.equals(m.getLida()));
    }
}
