package PDV.PDV.repository;

import PDV.PDV.model.clientes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
@Repository
public interface clienteRepository extends JpaRepository<clientes, Long> {
    Optional<clientes> findByCelular(String celular);
    List<clientes> findByNome(String nome);

        @Query("""
            SELECT c FROM clientes c
            WHERE :termo = ''
               OR LOWER(COALESCE(c.nome, '')) LIKE LOWER(CONCAT('%', :termo, '%'))
               OR LOWER(COALESCE(c.celular, '')) LIKE LOWER(CONCAT('%', :termo, '%'))
               OR LOWER(COALESCE(c.cep, '')) LIKE LOWER(CONCAT('%', :termo, '%'))
               OR LOWER(COALESCE(c.rua, '')) LIKE LOWER(CONCAT('%', :termo, '%'))
               OR LOWER(COALESCE(c.numero, '')) LIKE LOWER(CONCAT('%', :termo, '%'))
               OR LOWER(COALESCE(c.Bairro, '')) LIKE LOWER(CONCAT('%', :termo, '%'))
               OR LOWER(COALESCE(c.Complemento, '')) LIKE LOWER(CONCAT('%', :termo, '%'))
               OR LOWER(COALESCE(c.pontoReferencia, '')) LIKE LOWER(CONCAT('%', :termo, '%'))
            ORDER BY c.nome
            """)
        Page<clientes> pesquisar(@Param("termo") String termo, Pageable pageable);


}
