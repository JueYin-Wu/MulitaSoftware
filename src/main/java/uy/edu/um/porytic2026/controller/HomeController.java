package uy.edu.um.porytic2026.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import uy.edu.um.porytic2026.services.ActivityService;

@Controller
public class HomeController {

    private final ActivityService activityService;

    public HomeController(ActivityService activityService) {
        this.activityService = activityService;
    }

    // Página de inicio: portada, presentación y algunas actividades destacadas
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("activities", activityService.findFeatured(3));
        model.addAttribute("totalActivities", activityService.findAll().size());
        return "tourist/home";
    }
}
