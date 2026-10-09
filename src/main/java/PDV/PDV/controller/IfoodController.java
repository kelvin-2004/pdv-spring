package PDV.PDV.controller;

import PDV.PDV.service.IfoodAuthService;
import PDV.PDV.service.ifoodVinculoService;
import PDV.PDV.service.produtoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/ifood")
public class IfoodController {

    @Autowired private ifoodVinculoService vinculoService;
    @Autowired private produtoService produtoService;
    @Autowired private IfoodAuthService ifoodAuthService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("produtos", produtoService.listarAtivos());
        model.addAttribute("vinculos", vinculoService.listar());
        model.addAttribute("configurado", ifoodAuthService.configurado());
        model.addAttribute("merchantId", ifoodAuthService.getMerchantId());
        return "admin/ifood";
    }

    @PostMapping("/vincular")
    public String vincular(@RequestParam String ifoodItemId,
                           @RequestParam(required = false) String ifoodItemNome,
                           @RequestParam Long produtoId,
                           RedirectAttributes ra) {
        try {
            vinculoService.vincular(ifoodItemId, ifoodItemNome, produtoId);
            ra.addFlashAttribute("sucesso", "Vínculo salvo com sucesso.");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/ifood";
    }

    @PostMapping("/{id}/remover")
    public String remover(@PathVariable Long id, RedirectAttributes ra) {
        vinculoService.desvincular(id);
        ra.addFlashAttribute("sucesso", "Vínculo removido.");
        return "redirect:/admin/ifood";
    }
}
