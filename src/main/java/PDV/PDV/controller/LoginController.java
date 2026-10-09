package PDV.PDV.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/")
    public String home() {
        return "redirect:/site";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
