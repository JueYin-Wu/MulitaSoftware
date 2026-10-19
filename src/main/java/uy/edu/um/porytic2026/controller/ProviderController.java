package uy.edu.um.porytic2026.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uy.edu.um.porytic2026.entities.Provider;
import uy.edu.um.porytic2026.services.FileStorageService;
import uy.edu.um.porytic2026.services.ProviderService;
import uy.edu.um.porytic2026.services.UserService;

// Portal del proveedor. Solo entra el rol PROVIDER (proveedores aprobados).
@Controller
@RequestMapping("/provider")
public class ProviderController {

    private final UserService userService;
    private final ProviderService providerService;
    private final FileStorageService fileStorageService;

    public ProviderController(UserService userService, ProviderService providerService,
                              FileStorageService fileStorageService) {
        this.userService = userService;
        this.providerService = providerService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String home(Authentication authentication, Model model) {
        model.addAttribute("provider", currentProvider(authentication));
        return "provider/home";
    }

    // Sube una foto de perfil nueva (JPG, PNG o WEBP, hasta 2 MB)
    @PostMapping("/profile-image")
    public String uploadProfileImage(@RequestParam("image") MultipartFile image,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        Provider provider = currentProvider(authentication);
        try {
            String url = fileStorageService.storeImage(image, "providers", "provider-" + provider.getId());
            userService.updateProfileImage(provider, url, fileStorageService);
            redirectAttributes.addFlashAttribute("message", "Se actualizó tu foto de perfil.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/provider";
    }

    @PostMapping("/profile-image/delete")
    public String deleteProfileImage(Authentication authentication, RedirectAttributes redirectAttributes) {
        userService.updateProfileImage(currentProvider(authentication), null, fileStorageService);
        redirectAttributes.addFlashAttribute("message", "Se quitó tu foto de perfil.");
        return "redirect:/provider";
    }

    // authentication.getName() es el username de quien inició sesión
    private Provider currentProvider(Authentication authentication) {
        return (Provider) userService.findByUsername(authentication.getName()).orElseThrow();
    }
}
