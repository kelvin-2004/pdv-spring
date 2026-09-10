package PDV.PDV.controller;

import PDV.PDV.model.produtos;
import PDV.PDV.repository.produtoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/produtos")
public class produtoController {

    @Autowired
    private produtoRepository produtoRepository;

    // Caminho da pasta externa onde as imagens serão salvas
    private static final String UPLOAD_DIR = "uploads/";

    @GetMapping
    public String listarProdutos(@RequestParam(value = "nome", required = false) String nomeBusca, Model model) {
        List<produtos> listaProdutos;

        if (nomeBusca != null && !nomeBusca.trim().isEmpty()) {
            listaProdutos = produtoRepository.findByNomeContainingIgnoreCase(nomeBusca);
            model.addAttribute("nomeBusca", nomeBusca);
        } else {
            listaProdutos = produtoRepository.findAll();
        }

        model.addAttribute("produtos", listaProdutos);

        if (!model.containsAttribute("novoProduto")) {
            model.addAttribute("novoProduto", new produtos());
        }

        return "produtos/lista";
    }

    @PostMapping("/salvar")
    public String salvarProduto(@ModelAttribute produtos produto,
                                @RequestParam("file") MultipartFile file) {
        try {
            // Processa o upload da imagem apenas se um arquivo foi enviado
            if (file != null && !file.isEmpty()) {
                Path caminhoDiretorio = Paths.get(UPLOAD_DIR);

                // Cria a pasta uploads na raiz do projeto caso não exista
                if (!Files.exists(caminhoDiretorio)) {
                    Files.createDirectories(caminhoDiretorio);
                }

                // Gera um nome único para evitar conflitos de arquivos iguais
                String nomeArquivo = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path caminhoCompleto = caminhoDiretorio.resolve(nomeArquivo);

                // Copia o arquivo para a pasta física
                Files.copy(file.getInputStream(), caminhoCompleto, StandardCopyOption.REPLACE_EXISTING);

                // Salva o caminho relativo que será lido pelo WebConfig e Thymeleaf
                produto.setImgUrl("/uploads/" + nomeArquivo);
            } else if (produto.getId() != null) {
                // Caso seja uma edição e o usuário não tenha enviado uma nova foto, mantém a antiga
                Optional<produtos> produtoExistente = produtoRepository.findById(produto.getId());
                produtoExistente.ifPresent(p -> produto.setImgUrl(p.getImgUrl()));
            }

            produtoRepository.save(produto);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return "redirect:/produtos";
    }

    @GetMapping("/editar/{id}")
    public String editarProduto(@PathVariable("id") Long id, Model model) {
        Optional<produtos> produtoOpt = produtoRepository.findById(id);

        if (produtoOpt.isPresent()) {
            model.addAttribute("novoProduto", produtoOpt.get());
            model.addAttribute("produtos", produtoRepository.findAll());
            return "produtos/lista"; // Retorna para a mesma página preenchendo o formulário
        }

        return "redirect:/produtos";
    }
}