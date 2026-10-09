package PDV.PDV.controller;

import PDV.PDV.model.produtos;
import PDV.PDV.repository.produtoRepository;
import PDV.PDV.service.IfoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @Autowired
    private IfoodService ifoodService;

    private static final String UPLOAD_DIR = "uploads/";

    private static String sanitizarNomeArquivo(String nome) {
        if (nome == null || nome.isBlank()) return "arquivo";
        String base = nome.replace('\\', '/');
        int idx = base.lastIndexOf('/');
        if (idx >= 0) base = base.substring(idx + 1);
        base = base.replaceAll("[^a-zA-Z0-9._-]", "_");
        base = base.replaceAll("^[._-]+", "");
        if (base.length() > 80) {
            String ext = "";
            int dot = base.lastIndexOf('.');
            if (dot > 0) { ext = base.substring(dot); base = base.substring(0, dot); }
            base = base.substring(0, Math.min(base.length(), 60)) + ext;
        }
        return base.isBlank() ? "arquivo" : base;
    }

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
                                @RequestParam(value = "file", required = false) MultipartFile file,
                                RedirectAttributes redirectAttributes) {
        try {
            if (file != null && !file.isEmpty()) {
                Path caminhoDiretorio = Paths.get(UPLOAD_DIR);

                if (!Files.exists(caminhoDiretorio)) {
                    Files.createDirectories(caminhoDiretorio);
                }

                String nomeArquivo = System.currentTimeMillis() + "_" + sanitizarNomeArquivo(file.getOriginalFilename());
                Path caminhoCompleto = caminhoDiretorio.resolve(nomeArquivo);

                Files.copy(file.getInputStream(), caminhoCompleto, StandardCopyOption.REPLACE_EXISTING);
                produto.setImgUrl("/uploads/" + nomeArquivo);
            } else if (produto.getId() != null) {
                Optional<produtos> produtoExistente = produtoRepository.findById(produto.getId());
                produtoExistente.ifPresent(p -> produto.setImgUrl(p.getImgUrl()));
            }

            if (produto.getImgUrl() == null || produto.getImgUrl().isBlank()) {
                produto.setImgUrl("/img/logo.png");
            }

            produtoRepository.save(produto);
            boolean sincronizado = ifoodService.sincronizarDisponibilidade(produto);
            redirectAttributes.addFlashAttribute("sucesso",
                    "Produto salvo com sucesso!" + (sincronizado ? " Disponibilidade sincronizada com o iFood." : ""));

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro",
                    "Não foi possível salvar o produto: " + (e.getMessage() != null ? e.getMessage() : "erro desconhecido"));
        }

        return "redirect:/produtos";
    }

    @GetMapping("/editar/{id}")
    public String editarProduto(@PathVariable("id") Long id, Model model) {
        Optional<produtos> produtoOpt = produtoRepository.findById(id);

        if (produtoOpt.isPresent()) {
            model.addAttribute("novoProduto", produtoOpt.get());
            model.addAttribute("produtos", produtoRepository.findAll());
            return "produtos/lista";
        }

        return "redirect:/produtos";
    }
}