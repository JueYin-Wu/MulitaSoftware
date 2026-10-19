package uy.edu.um.porytic2026.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import uy.edu.um.porytic2026.entities.Activity;
import uy.edu.um.porytic2026.entities.EquipmentRole;
import uy.edu.um.porytic2026.entities.Review;
import uy.edu.um.porytic2026.services.ActivityService;

import java.util.List;

// Página dedicada a las actividades (RF04 consultar y RF05 buscar y filtrar)
@Controller
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    // Ejemplo: /activities?q=kayak&department=Rocha&maxPrice=2000&sort=price_asc
    @GetMapping("/activities")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String department,
                       @RequestParam(required = false) Double maxPrice,
                       @RequestParam(required = false) String sort,
                       Model model) {
        model.addAttribute("activities", activityService.search(q, department, maxPrice, sort));
        model.addAttribute("departments", ActivityService.DEPARTMENTS);
        model.addAttribute("q", q);
        model.addAttribute("department", department);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sort", sort);
        model.addAttribute("hasFilters", (q != null && !q.isBlank())
                || (department != null && !department.isBlank()) || maxPrice != null
                || (sort != null && !sort.isBlank()));
        return "tourist/activities";
    }

    // Página de una actividad: fotos, datos del MER, equipamiento, próximas fechas y reseñas
    @GetMapping("/activities/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Activity activity;
        try {
            activity = activityService.findById(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la actividad");
        }

        List<Review> reviews = activityService.findReviews(id);
        model.addAttribute("activity", activity);
        model.addAttribute("slots", activityService.findUpcomingSlots(id));
        model.addAttribute("requiredEquipment", activityService.findEquipment(id, EquipmentRole.REQUIRED));
        model.addAttribute("providedEquipment", activityService.findEquipment(id, EquipmentRole.PROVIDED));
        model.addAttribute("reviews", reviews);
        Double averageRating = activityService.averageRating(reviews);
        model.addAttribute("averageRating", averageRating);
        // Cantidad de estrellas llenas (el promedio redondeado)
        model.addAttribute("filledStars", averageRating == null ? 0 : Math.round(averageRating));
        return "tourist/activity-detail";
    }
}
