package uy.edu.um.porytic2026.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uy.edu.um.porytic2026.services.UserService;

// Portal del proveedor. Solo entra el rol PROVIDER (proveedores aprobados).
@Controller
@RequestMapping("/provider")
public class ProviderController {

    private final UserService userService;

    public ProviderController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String home(Authentication authentication, Model model) {
        // authentication.getName() es el username de quien inició sesión
        model.addAttribute("provider", userService.findByUsername(authentication.getName()).orElseThrow());
        return "provider/home";
    }
}
