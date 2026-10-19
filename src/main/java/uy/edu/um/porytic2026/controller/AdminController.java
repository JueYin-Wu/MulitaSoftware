package uy.edu.um.porytic2026.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uy.edu.um.porytic2026.entities.Provider;
import uy.edu.um.porytic2026.entities.ProviderStatus;
import uy.edu.um.porytic2026.services.ProviderService;

// Portal del administrador. SecurityConfig ya garantiza que solo entra el rol ADMIN.
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ProviderService providerService;

    public AdminController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @GetMapping
    public String index() {
        return "redirect:/admin/providers";
    }

    // Lista de solicitudes. Por defecto muestra las pendientes: /admin/providers?status=APPROVED
    @GetMapping("/providers")
    public String providers(@RequestParam(defaultValue = "PENDING") ProviderStatus status, Model model) {
        model.addAttribute("providers", providerService.findByStatus(status));
        model.addAttribute("status", status);
        model.addAttribute("pendingCount", providerService.countByStatus(ProviderStatus.PENDING));
        model.addAttribute("approvedCount", providerService.countByStatus(ProviderStatus.APPROVED));
        model.addAttribute("rejectedCount", providerService.countByStatus(ProviderStatus.REJECTED));
        return "admin/providers";
    }

    @PostMapping("/providers/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Provider provider = providerService.approve(id);
            redirectAttributes.addFlashAttribute("message",
                    "Se aprobó a " + provider.getBusinessName() + ". Ya puede iniciar sesión.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/providers";
    }

    @PostMapping("/providers/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String reason,
                         RedirectAttributes redirectAttributes) {
        try {
            Provider provider = providerService.reject(id, reason);
            redirectAttributes.addFlashAttribute("message",
                    "Se rechazó la solicitud de " + provider.getBusinessName() + ".");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/providers";
    }
}
