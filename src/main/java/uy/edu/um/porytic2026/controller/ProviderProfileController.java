package uy.edu.um.porytic2026.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import uy.edu.um.porytic2026.entities.Provider;
import uy.edu.um.porytic2026.entities.Review;
import uy.edu.um.porytic2026.services.ActivityService;
import uy.edu.um.porytic2026.services.ProviderService;

import java.util.List;

// Página pública de un prestador: se llega haciendo clic en "Ofrecida por ..." en una actividad
@Controller
public class ProviderProfileController {

    private final ProviderService providerService;
    private final ActivityService activityService;

    public ProviderProfileController(ProviderService providerService, ActivityService activityService) {
        this.providerService = providerService;
        this.activityService = activityService;
    }

    @GetMapping("/providers/{id}")
    public String profile(@PathVariable Long id, Model model) {
        Provider provider;
        try {
            provider = providerService.findApprovedById(id); // solo prestadores aprobados
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el prestador");
        }

        List<Review> reviews = activityService.findProviderReviews(id);
        Double averageRating = activityService.averageRating(reviews);
        model.addAttribute("provider", provider);
        model.addAttribute("activities", activityService.findByProvider(id));
        model.addAttribute("reviews", reviews.stream().limit(5).toList()); // las 5 más recientes
        model.addAttribute("reviewCount", reviews.size());
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("filledStars", averageRating == null ? 0 : Math.round(averageRating));
        return "tourist/provider-profile";
    }
}
