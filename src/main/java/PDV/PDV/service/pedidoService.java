package PDV.PDV.service;

import PDV.PDV.model.Enum.statusPedido;
import PDV.PDV.model.pedido;
import PDV.PDV.repository.pedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class pedidoService {

    @Autowired
    private pedidoRepository pedidoRepo;

    public pedido novoPedido(pedido novoPedido) {
        novoPedido.setDataHoraPedido(OffsetDateTime.now());
        novoPedido.setStatusPedido(statusPedido.PREPARANDO);

        // Define automaticamente o número do pedido para este cliente
        if (novoPedido.getCliente() != null) {
            long totalPedidosAnteriores = pedidoRepo.countByCliente(novoPedido.getCliente());
            int proximoNumero = (int) totalPedidosAnteriores + 1;
            novoPedido.setNumeroPedidoCliente(proximoNumero);
        }

        return pedidoRepo.save(novoPedido);
    }

    public pedido atualizarStatus(Long id, statusPedido novoStatus) {
        Optional<pedido> pedidoOptional = pedidoRepo.findById(id);

        if (pedidoOptional.isEmpty()) {
            throw new RuntimeException("Pedido não encontrado com o ID: " + id);
        }

        pedido p = pedidoOptional.get();
        p.setStatusPedido(novoStatus);

        return pedidoRepo.save(p);
    }

    public List<pedido> listarStatus(statusPedido status) {
        return pedidoRepo.findByStatusPedido(status);
    }

    public Optional<pedido> procurarID(Long id) {
        return pedidoRepo.findById(id);
    }

    public List<pedido> listarTodos() {
        return pedidoRepo.findAll();
    }
}