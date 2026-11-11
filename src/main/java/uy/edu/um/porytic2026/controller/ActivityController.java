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
import uy.edu.um.porytic2026.services.TagService;
import uy.edu.um.porytic2026.services.WeatherService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

// Página dedicada a las actividades (RF04 consultar y RF05 buscar y filtrar)
@Controller
public class ActivityController {

    private final ActivityService activityService;
    private final TagService tagService;
    private final WeatherService weatherService;

    public ActivityController(ActivityService activityService, TagService tagService, WeatherService weatherService) {
        this.activityService = activityService;
        this.tagService = tagService;
        this.weatherService = weatherService;
    }

    // Ejemplo: /activities?q=kayak&department=Rocha&tag=3&date=2026-11-07&maxPrice=2000&sort=price_asc
    @GetMapping("/activities")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String department,
                       @RequestParam(required = false) Long tag,
                       @RequestParam(required = false) String date,
                       @RequestParam(required = false) Double maxPrice,
                       @RequestParam(required = false) String sort,
                       Model model) {
        // Si la fecha viene mal escrita se ignora (el campo del formulario siempre manda AAAA-MM-DD)
        LocalDate day = null;
        if (date != null && !date.isBlank()) {
            try {
                day = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                day = null;
            }
        }

        model.addAttribute("activities", activityService.search(q, department, tag, day, maxPrice, sort));
        model.addAttribute("departments", ActivityService.DEPARTMENTS);
        model.addAttribute("tags", tagService.findApproved());
        model.addAttribute("today", LocalDate.now().toString());
        model.addAttribute("q", q);
        model.addAttribute("department", department);
        model.addAttribute("tag", tag);
        model.addAttribute("date", day == null ? null : day.toString());
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sort", sort);
        model.addAttribute("hasFilters", (q != null && !q.isBlank())
                || (department != null && !department.isBlank()) || tag != null || day != null
                || maxPrice != null || (sort != null && !sort.isBlank()));
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
        // Si el prestador la dio de baja, la página se sigue viendo (por las reservas y reseñas viejas)
        // pero sin fechas para reservar
        model.addAttribute("slots", activity.isPublished() ? activityService.findUpcomingSlots(id) : java.util.List.of());
        // Pronóstico del clima en el lugar de la actividad (fecha -> pronóstico); vacío si la API no respondió
        model.addAttribute("forecasts", weatherService.forecastsFor(activity.getLocation()));
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
