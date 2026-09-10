package PDV.PDV.service;

import PDV.PDV.model.clientes;
import PDV.PDV.repository.clienteRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;



@Data
@Service
public class clienteService {
    @Autowired

    private clienteRepository clienteRepo;
    public clientes cadastrarCliente(clientes cliente) {
        if (cliente.getId() == null) {
            Optional<clientes> clienteExistente = clienteRepo.findByCelular(cliente.getCelular());

            // 1. Testa se a busca encontrou um cliente com esse celular
            if (clienteExistente.isPresent()) {
                throw new RuntimeException("Cliente já cadastrado com este número de celular.");
            }
        }

        // 2. Salva e retorna o cliente cadastrado
        return clienteRepo.save(cliente);
    }
    //metodo listar todos os clientes
public List<clientes> listarTodos(){
        return clienteRepo.findAll();

} //metodo por celular

public Optional<clientes> buscarCelular( String celular){
    return clienteRepo.findByCelular(celular);

}}