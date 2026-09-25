package PDV.PDV.service;

import PDV.PDV.model.configuracao;
import PDV.PDV.repository.configuracaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class configuracaoService {

    private static final long ID_UNICO = 1L;
    private static final int PADRAO_MINUTOS = 30;
    private static final int PADRAO_LIMITE_PAGAMENTO_MINUTOS = 30;

    @Autowired
    private configuracaoRepository repo;

    public configuracao obter() {
        return repo.findById(ID_UNICO).orElseGet(() -> {
            configuracao c = new configuracao();
            c.setId(ID_UNICO);
            c.setTempoPreparoPadraoMinutos(PADRAO_MINUTOS);
            c.setTempoLimitePagamentoMinutos(PADRAO_LIMITE_PAGAMENTO_MINUTOS);
            return repo.save(c);
        });
    }

    public int obterTempoPreparoPadrao() {
        Integer v = obter().getTempoPreparoPadraoMinutos();
        return v != null ? v : PADRAO_MINUTOS;
    }

    public int obterTempoLimitePagamento() {
        Integer v = obter().getTempoLimitePagamentoMinutos();
        return v != null ? v : PADRAO_LIMITE_PAGAMENTO_MINUTOS;
    }

    public void atualizarTempoPreparoPadrao(Integer minutos) {
        if (minutos == null || minutos < 1 || minutos > 180) {
            throw new IllegalArgumentException("O tempo de preparo deve estar entre 1 e 180 minutos.");
        }
        configuracao c = obter();
        c.setTempoPreparoPadraoMinutos(minutos);
        repo.save(c);
    }

    public String obterImpressoraNome() {
        return obter().getImpressoraNome();
    }

    public void atualizarImpressoraNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Selecione uma impressora.");
        }
        configuracao c = obter();
        c.setImpressoraNome(nome.trim());
        repo.save(c);
    }
}
