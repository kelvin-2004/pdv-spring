    package PDV.PDV.controller;
    import org.springframework.http.ResponseEntity;
    import PDV.PDV.model.clientes;
    import PDV.PDV.service.clienteService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Controller;
    import org.springframework.ui.Model;
    import org.springframework.web.bind.annotation.*;

    import java.util.Optional;


    @Controller
    @RequestMapping("/clientes")

    public class ClienteController {

        @Autowired
        private clienteService ClienteService;

        @GetMapping
        public String listarClientes(Model model) {
            model.addAttribute("clientes", ClienteService.listarTodos());
            model.addAttribute("novoCliente", new clientes());
            return "clientes/lista"; // Aponta para src/main/resources/templates/clientes/lista.html
        }

        @PostMapping("/salvar")
        public String salvarCliente(@ModelAttribute("novoCliente") clientes cliente) {
            ClienteService.cadastrarCliente(cliente);
            return "redirect:/clientes";
        }
        @GetMapping("/buscar")
        @ResponseBody
        public ResponseEntity<clientes> buscarPorCelular(@RequestParam String celular) {
           Optional<clientes> clienteEncontrado = ClienteService.buscarCelular(celular);
            if (clienteEncontrado.isPresent()){
                return ResponseEntity.ok(clienteEncontrado.get());}
                else{
                    return ResponseEntity.notFound().build();

                }
            }

        }
