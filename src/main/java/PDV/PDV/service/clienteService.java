package PDV.PDV.service;

import PDV.PDV.model.clientes;
import PDV.PDV.repository.clienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
        // Se for um novo cadastro (ID nulo) ou se quisermos validar duplicidade
        if (cliente.getId() == null && cliente.getCelular() != null) {
            Optional<clientes> clienteExistente = clienteRepo.findByCelular(cliente.getCelular());

            if (clienteExistente.isPresent()) {
                throw new RuntimeException("Cliente já cadastrado com este número de celular.");
            }
        }

        return clienteRepo.save(cliente);
    }

    public List<clientes> listarTodos() {
        return clienteRepo.findAll();
    }

    public Optional<clientes> buscarCelular(String celular) {
        return clienteRepo.findByCelular(celular);
    }
}