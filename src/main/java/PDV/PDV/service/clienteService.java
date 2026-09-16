package PDV.PDV.service;

import PDV.PDV.model.clientes;
import PDV.PDV.repository.clienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class clienteService {

    @Autowired
    private clienteRepository clienteRepo;

    public void salvarOuAtualizar(clientes cliente) {
        clienteRepo.save(cliente);
    }

    public Optional<clientes> buscarPorId(Long id) {
        return clienteRepo.findById(id);
    }

    public clientes cadastrarCliente(clientes cliente) {
        if (cliente.getCelular() != null && !cliente.getCelular().isBlank()) {
            Optional<clientes> clienteExistente = clienteRepo.findByCelular(cliente.getCelular());

            if (clienteExistente.isPresent()
                    && !clienteExistente.get().getId().equals(cliente.getId())) {
                throw new RuntimeException("Cliente já cadastrado com este número de celular.");
            }
        }

        return clienteRepo.save(cliente);
    }

    public List<clientes> listarTodos() {
        return clienteRepo.findAll();
    }

    public Page<clientes> pesquisar(String termo, int pagina) {
        String termoNormalizado = termo == null ? "" : termo.trim();
        return clienteRepo.pesquisar(termoNormalizado, PageRequest.of(Math.max(pagina, 0), 5));
    }

    public Optional<clientes> buscarCelular(String celular) {
        return clienteRepo.findByCelular(celular);
    }
}