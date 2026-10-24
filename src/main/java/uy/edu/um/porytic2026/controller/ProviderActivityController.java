package uy.edu.um.porytic2026.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uy.edu.um.porytic2026.dto.ActivityForm;
import uy.edu.um.porytic2026.entities.Activity;
import uy.edu.um.porytic2026.entities.Provider;
import uy.edu.um.porytic2026.exceptions.BusinessException;
import uy.edu.um.porytic2026.services.ActivityService;
import uy.edu.um.porytic2026.services.ProviderActivityService;
import uy.edu.um.porytic2026.services.UserService;

import java.util.List;

// Actividades del prestador: su lista y el alta de una actividad nueva.
// SecurityConfig ya garantiza que a /provider/** solo entra el rol PROVIDER.
@Controller
@RequestMapping("/provider/activities")
public class ProviderActivityController {

    private final UserService userService;
    private final ProviderActivityService providerActivityService;

    public ProviderActivityController(UserService userService, ProviderActivityService providerActivityService) {
        this.userService = userService;
        this.providerActivityService = providerActivityService;
    }

    // Mis actividades
    @GetMapping
    public String list(Authentication authentication, Model model) {
        model.addAttribute("activities", providerActivityService.findByProvider(currentProvider(authentication)));
        return "provider/activities";
    }

    // Formulario vacío
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ActivityForm());
        addFormOptions(model);
        return "provider/activity-form";
    }

    // Publicar la actividad. Las fotos llegan aparte porque son archivos (formulario multipart).
    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") ActivityForm form,
                         BindingResult result,
                         @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            addFormOptions(model);
            return "provider/activity-form";
        }
        try {
            Activity activity = providerActivityService.create(currentProvider(authentication), form, photos);
            redirectAttributes.addFlashAttribute("message",
                    "Se publicó \"" + activity.getName() + "\". Ya aparece en el portal de turistas.");
            return "redirect:/provider/activities";
        } catch (BusinessException e) {
            if (e.getField() != null && !"photos".equals(e.getField())) {
                result.rejectValue(e.getField(), "invalid", e.getMessage());
            } else {
                // Las fotos no son un campo del formulario: el error se muestra aparte
                model.addAttribute(e.getField() == null ? "formError" : "photosError", e.getMessage());
            }
            addFormOptions(model);
            return "provider/activity-form";
        }
    }

    // Listas que necesita el formulario (selects y checkboxes)
    private void addFormOptions(Model model) {
        model.addAttribute("departments", ActivityService.DEPARTMENTS);
        model.addAttribute("tags", providerActivityService.findAvailableTags());
        model.addAttribute("equipmentList", providerActivityService.findAllEquipment());
        model.addAttribute("maxPhotos", ProviderActivityService.MAX_PHOTOS);
    }

    // authentication.getName() es el username de quien inició sesión
    private Provider currentProvider(Authentication authentication) {
        return (Provider) userService.findByUsername(authentication.getName()).orElseThrow();
    }
}
