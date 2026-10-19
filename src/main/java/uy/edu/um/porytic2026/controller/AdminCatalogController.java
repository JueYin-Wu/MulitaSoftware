package uy.edu.um.porytic2026.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uy.edu.um.porytic2026.entities.Equipment;
import uy.edu.um.porytic2026.entities.Tag;
import uy.edu.um.porytic2026.services.EquipmentService;
import uy.edu.um.porytic2026.services.TagService;

import java.util.List;

// Datos generales que mantiene el administrador (letra del proyecto, "Administración de la plataforma"):
// tipos de actividad y tipos de equipamiento, con alta, consulta, modificación y baja.
// Solo entra el rol ADMIN (lo controla SecurityConfig con "/admin/**").
@Controller
@RequestMapping("/admin")
public class AdminCatalogController {

    private final TagService tagService;
    private final EquipmentService equipmentService;

    public AdminCatalogController(TagService tagService, EquipmentService equipmentService) {
        this.tagService = tagService;
        this.equipmentService = equipmentService;
    }

    // ---------- Tipos de actividad ----------

    @GetMapping("/tags")
    public String tags(Model model) {
        List<Tag> tags = tagService.findApproved();
        model.addAttribute("tags", tags);
        model.addAttribute("usage", tagService.usageCount(tags));
        model.addAttribute("pendingTags", tagService.findPending());
        model.addAttribute("maxName", TagService.MAX_NAME);
        return "admin/tags";
    }

    @PostMapping("/tags")
    public String createTag(@RequestParam String name, RedirectAttributes ra) {
        try {
            Tag tag = tagService.create(name);
            ra.addFlashAttribute("message", "Se agregó el tipo de actividad \"" + tag.getName() + "\".");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("newName", name); // para no perder lo que escribió
        }
        return "redirect:/admin/tags";
    }

    @PostMapping("/tags/{id}")
    public String updateTag(@PathVariable Long id, @RequestParam String name, RedirectAttributes ra) {
        try {
            Tag tag = tagService.rename(id, name);
            ra.addFlashAttribute("message", "Se guardó \"" + tag.getName() + "\".");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/tags";
    }

    @PostMapping("/tags/{id}/delete")
    public String deleteTag(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Tag tag = tagService.delete(id);
            ra.addFlashAttribute("message", "Se eliminó \"" + tag.getName() + "\".");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/tags";
    }

    @PostMapping("/tags/{id}/approve")
    public String approveTag(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Tag tag = tagService.approve(id);
            ra.addFlashAttribute("message", "Se aprobó la propuesta \"" + tag.getName() + "\".");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/tags";
    }

    @PostMapping("/tags/{id}/reject")
    public String rejectTag(@PathVariable Long id, @RequestParam(required = false) String reason,
                            RedirectAttributes ra) {
        try {
            Tag tag = tagService.reject(id, reason);
            ra.addFlashAttribute("message", "Se rechazó la propuesta \"" + tag.getName() + "\".");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/tags";
    }

    // ---------- Equipamiento ----------

    @GetMapping("/equipment")
    public String equipment(Model model) {
        List<Equipment> items = equipmentService.findAll();
        model.addAttribute("items", items);
        model.addAttribute("usage", equipmentService.usageCount(items));
        model.addAttribute("maxName", EquipmentService.MAX_NAME);
        model.addAttribute("maxDescription", EquipmentService.MAX_DESCRIPTION);
        return "admin/equipment";
    }

    @PostMapping("/equipment")
    public String createEquipment(@RequestParam String name,
                                  @RequestParam(required = false) String description,
                                  RedirectAttributes ra) {
        try {
            Equipment equipment = equipmentService.create(name, description);
            ra.addFlashAttribute("message", "Se agregó el equipamiento \"" + equipment.getName() + "\".");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("newName", name);
            ra.addFlashAttribute("newDescription", description);
        }
        return "redirect:/admin/equipment";
    }

    @PostMapping("/equipment/{id}")
    public String updateEquipment(@PathVariable Long id, @RequestParam String name,
                                  @RequestParam(required = false) String description,
                                  RedirectAttributes ra) {
        try {
            Equipment equipment = equipmentService.update(id, name, description);
            ra.addFlashAttribute("message", "Se guardó \"" + equipment.getName() + "\".");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/equipment";
    }

    @PostMapping("/equipment/{id}/delete")
    public String deleteEquipment(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Equipment equipment = equipmentService.delete(id);
            ra.addFlashAttribute("message", "Se eliminó \"" + equipment.getName() + "\".");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/equipment";
    }
}
