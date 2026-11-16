package uy.edu.um.porytic2026.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uy.edu.um.porytic2026.entities.Administrator;
import uy.edu.um.porytic2026.entities.Provider;
import uy.edu.um.porytic2026.entities.SanctionType;
import uy.edu.um.porytic2026.exceptions.BusinessException;
import uy.edu.um.porytic2026.services.SanctionService;
import uy.edu.um.porytic2026.services.UserService;

// Sanciones a prestadores desde el panel de administración.
// SecurityConfig ya garantiza que a /admin/** solo entra el rol ADMIN.
@Controller
@RequestMapping("/admin/providers/{providerId}/sanctions")
public class AdminSanctionController {

    private final SanctionService sanctionService;
    private final UserService userService;

    public AdminSanctionController(SanctionService sanctionService, UserService userService) {
        this.sanctionService = sanctionService;
        this.userService = userService;
    }

    // Historial de sanciones del prestador y formulario para aplicar una nueva
    @GetMapping
    public String page(@PathVariable Long providerId, Model model) {
        addPageData(model, providerId);
        return "admin/provider-sanctions";
    }

    @PostMapping
    public String apply(@PathVariable Long providerId,
                        @RequestParam(required = false) SanctionType type,
                        @RequestParam(required = false) String reason,
                        Authentication authentication,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        try {
            Administrator admin = (Administrator) userService.findByUsername(authentication.getName()).orElseThrow();
            var sanction = sanctionService.apply(providerId, admin, type, reason);
            redirectAttributes.addFlashAttribute("message", sanction.getType() == SanctionType.SUSPENSION
                    ? "Se suspendió a " + sanction.getProvider().getBusinessName()
                      + ". Sus actividades ya no se muestran y no puede iniciar sesión."
                    : "Se registró una advertencia para " + sanction.getProvider().getBusinessName() + ".");
            return "redirect:/admin/providers/" + providerId + "/sanctions";
        } catch (BusinessException e) {
            // Se vuelve a mostrar la página con lo que había escrito el admin y el error
            if (type != null) {
                model.addAttribute("selectedType", type);
            }
            model.addAttribute("reason", reason);
            model.addAttribute("formError", e.getMessage());
            addPageData(model, providerId);
            return "admin/provider-sanctions";
        }
    }

    @PostMapping("/lift")
    public String lift(@PathVariable Long providerId, RedirectAttributes redirectAttributes) {
        try {
            sanctionService.liftSuspension(providerId);
            redirectAttributes.addFlashAttribute("message",
                    "Se levantó la suspensión. El prestador ya puede volver a operar.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/providers/" + providerId + "/sanctions";
    }

    private void addPageData(Model model, Long providerId) {
        Provider provider;
        try {
            provider = sanctionService.findProvider(providerId);
        } catch (BusinessException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el prestador");
        }
        model.addAttribute("provider", provider);
        model.addAttribute("sanctions", sanctionService.findByProvider(providerId));
        model.addAttribute("sanctionTypes", SanctionType.values());
        model.addAttribute("maxReasonLength", SanctionService.MAX_REASON_LENGTH);
        if (!model.containsAttribute("selectedType")) {
            model.addAttribute("selectedType", SanctionType.WARNING);
        }
    }
}
