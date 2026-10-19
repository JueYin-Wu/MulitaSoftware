package uy.edu.um.porytic2026.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uy.edu.um.porytic2026.services.ActivityService;

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
}
