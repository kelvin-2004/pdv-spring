package PDV.PDV.repository;

import PDV.PDV.model.clientes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface clienteRepository extends JpaRepository<clientes, Long> {
    Optional<clientes> findByCelular(String celular);

}
