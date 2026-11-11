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
import uy.edu.um.porytic2026.services.ProviderAvailabilityService;
import uy.edu.um.porytic2026.services.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Actividades del prestador: su lista y el alta de una actividad nueva.
// SecurityConfig ya garantiza que a /provider/** solo entra el rol PROVIDER.
@Controller
@RequestMapping("/provider/activities")
public class ProviderActivityController {

    private final UserService userService;
    private final ProviderActivityService providerActivityService;
    private final ProviderAvailabilityService availabilityService;

    public ProviderActivityController(UserService userService, ProviderActivityService providerActivityService,
                                      ProviderAvailabilityService availabilityService) {
        this.userService = userService;
        this.providerActivityService = providerActivityService;
        this.availabilityService = availabilityService;
    }

    // Mis actividades (publicadas y dadas de baja), con cuántas fechas próximas tiene cada una
    @GetMapping
    public String list(Authentication authentication, Model model) {
        List<Activity> activities = providerActivityService.findByProvider(currentProvider(authentication));
        Map<Long, Long> upcomingDates = new HashMap<>();
        for (Activity activity : activities) {
            upcomingDates.put(activity.getId(), availabilityService.countUpcoming(activity.getId()));
        }
        model.addAttribute("activities", activities);
        model.addAttribute("upcomingDates", upcomingDates);
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
                    "Se publicó \"" + activity.getName() + "\". Ahora agregá las fechas y los cupos para que los turistas puedan reservar.");
            return "redirect:/provider/activities/" + activity.getId() + "/dates";
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

    // ===================== Editar =====================

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication authentication, Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            Activity activity = providerActivityService.findOwn(currentProvider(authentication), id);
            model.addAttribute("form", providerActivityService.toForm(activity));
            addEditData(model, activity);
            return "provider/activity-form";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/provider/activities";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") ActivityForm form,
                         BindingResult result,
                         @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Provider provider = currentProvider(authentication);
        Activity current;
        try {
            current = providerActivityService.findOwn(provider, id);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/provider/activities";
        }
        if (result.hasErrors()) {
            addEditData(model, current);
            return "provider/activity-form";
        }
        try {
            Activity activity = providerActivityService.update(provider, id, form, photos);
            redirectAttributes.addFlashAttribute("message", "Se guardaron los cambios de \"" + activity.getName() + "\".");
            return "redirect:/provider/activities";
        } catch (BusinessException e) {
            if (e.getField() != null && !"photos".equals(e.getField())) {
                result.rejectValue(e.getField(), "invalid", e.getMessage());
            } else {
                model.addAttribute(e.getField() == null ? "formError" : "photosError", e.getMessage());
            }
            addEditData(model, current);
            return "provider/activity-form";
        }
    }

    // ===================== Dar de baja / volver a publicar =====================

    @PostMapping("/{id}/unpublish")
    public String unpublish(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            Activity activity = providerActivityService.unpublish(currentProvider(authentication), id);
            redirectAttributes.addFlashAttribute("message", "Se dio de baja \"" + activity.getName()
                    + "\". Ya no aparece en el portal; las reservas que ya estaban hechas siguen valiendo.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/provider/activities";
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            Activity activity = providerActivityService.publish(currentProvider(authentication), id);
            redirectAttributes.addFlashAttribute("message", "Se volvió a publicar \"" + activity.getName() + "\".");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/provider/activities";
    }

    // ===================== Fechas y cupos =====================

    @GetMapping("/{id}/dates")
    public String dates(@PathVariable Long id, Authentication authentication, Model model,
                        RedirectAttributes redirectAttributes) {
        Provider provider = currentProvider(authentication);
        try {
            model.addAttribute("activity", providerActivityService.findOwn(provider, id));
            model.addAttribute("rows", availabilityService.upcoming(provider, id));
            model.addAttribute("today", java.time.LocalDate.now().toString());
            model.addAttribute("maxCapacity", ProviderAvailabilityService.MAX_CAPACITY);
            model.addAttribute("maxWeeks", ProviderAvailabilityService.MAX_REPEAT_WEEKS);
            return "provider/activity-dates";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/provider/activities";
        }
    }

    @PostMapping("/{id}/dates")
    public String addDate(@PathVariable Long id,
                          @RequestParam(required = false) String date,
                          @RequestParam(required = false) String time,
                          @RequestParam(required = false) Integer capacity,
                          @RequestParam(required = false) Integer repeatWeeks,
                          Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            int created = availabilityService.add(currentProvider(authentication), id, date, time, capacity, repeatWeeks);
            redirectAttributes.addFlashAttribute("message", created == 1 ? "Se agregó la fecha." : "Se agregaron " + created + " fechas.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            // se conserva lo que había escrito
            redirectAttributes.addFlashAttribute("newDate", date);
            redirectAttributes.addFlashAttribute("newTime", time);
            redirectAttributes.addFlashAttribute("newCapacity", capacity);
            redirectAttributes.addFlashAttribute("newRepeat", repeatWeeks);
        }
        return "redirect:/provider/activities/" + id + "/dates";
    }

    @PostMapping("/{id}/dates/{dateId}/capacity")
    public String updateCapacity(@PathVariable Long id, @PathVariable Long dateId,
                                 @RequestParam(required = false) Integer capacity,
                                 Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            availabilityService.updateCapacity(currentProvider(authentication), dateId, capacity);
            redirectAttributes.addFlashAttribute("message", "Se cambió el cupo.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/provider/activities/" + id + "/dates";
    }

    @PostMapping("/{id}/dates/{dateId}/delete")
    public String deleteDate(@PathVariable Long id, @PathVariable Long dateId,
                             Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            availabilityService.delete(currentProvider(authentication), dateId);
            redirectAttributes.addFlashAttribute("message", "Se eliminó la fecha.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/provider/activities/" + id + "/dates";
    }

    private void addEditData(Model model, Activity activity) {
        addFormOptions(model);
        model.addAttribute("editing", true);
        model.addAttribute("activity", activity);
    }

    // Listas que necesita el formulario (selects y checkboxes)
    private void addFormOptions(Model model) {
        model.addAttribute("departments", ActivityService.DEPARTMENTS);
        model.addAttribute("tags", providerActivityService.findAvailableTags());
        model.addAttribute("equipmentList", providerActivityService.findAllEquipment());
        model.addAttribute("maxPhotos", ProviderActivityService.MAX_PHOTOS);
        model.addAttribute("editing", false); // addEditData lo cambia a true al editar
    }

    // authentication.getName() es el username de quien inició sesión
    private Provider currentProvider(Authentication authentication) {
        return (Provider) userService.findByUsername(authentication.getName()).orElseThrow();
    }
}
