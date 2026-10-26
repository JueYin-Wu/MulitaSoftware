package uy.edu.um.porytic2026.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import uy.edu.um.porytic2026.services.ActivityService;
import uy.edu.um.porytic2026.services.TagService;

import java.time.LocalDate;

@Controller
public class HomeController {

    private final ActivityService activityService;
    private final TagService tagService;

    public HomeController(ActivityService activityService, TagService tagService) {
        this.activityService = activityService;
        this.tagService = tagService;
    }

    // Página de inicio: portada, buscador, tipos de actividad, destacadas, cómo funciona e invitaciones
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("activities", activityService.findFeatured(3)); // las mejor valoradas
        model.addAttribute("totalActivities", activityService.findAll().size());
        model.addAttribute("departments", ActivityService.DEPARTMENTS);
        model.addAttribute("tags", tagService.findApproved());
        model.addAttribute("categories", activityService.findTopCategories(5)); // los 5 tipos más reservados
        model.addAttribute("today", LocalDate.now().toString());
        return "tourist/home";
    }
}
