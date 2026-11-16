package uy.edu.um.porytic2026.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uy.edu.um.porytic2026.entities.Provider;
import uy.edu.um.porytic2026.services.ProviderService;
import uy.edu.um.porytic2026.services.UserService;
import uy.edu.um.porytic2026.services.ProviderActivityService;

// Portal del proveedor. Solo entra el rol PROVIDER (proveedores aprobados).
@Controller
@RequestMapping("/provider")
public class ProviderController {

    private final UserService userService;
    private final ProviderService providerService;
    private final ProviderActivityService providerActivityService;

    public ProviderController(UserService userService, ProviderService providerService,
                              ProviderActivityService providerActivityService) {
        this.userService = userService;
        this.providerService = providerService;
        this.providerActivityService = providerActivityService;
    }

    @GetMapping
    public String home(Authentication authentication, Model model) {
        Provider provider = currentProvider(authentication);
        model.addAttribute("provider", provider);
        model.addAttribute("activityCount", providerActivityService.findByProvider(provider).size());
        return "provider/home";
    }

    // authentication.getName() es el username de quien inició sesión
    private Provider currentProvider(Authentication authentication) {
        return (Provider) userService.findByUsername(authentication.getName()).orElseThrow();
    }
}
