package uy.edu.um.porytic2026.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uy.edu.um.porytic2026.dto.ReviewForm;
import uy.edu.um.porytic2026.entities.Booking;
import uy.edu.um.porytic2026.entities.Review;
import uy.edu.um.porytic2026.entities.Tourist;
import uy.edu.um.porytic2026.exceptions.BusinessException;
import uy.edu.um.porytic2026.services.ReviewService;
import uy.edu.um.porytic2026.services.UserService;

// Dejar una reseña de una reserva ya realizada. Bajo /tourist/** -> solo el rol TOURIST.
@Controller
@RequestMapping("/tourist/bookings/{id}/review")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;

    public ReviewController(ReviewService reviewService, UserService userService) {
        this.reviewService = reviewService;
        this.userService = userService;
    }

    @GetMapping
    public String form(@PathVariable Long id, Authentication authentication, Model model,
                       RedirectAttributes redirectAttributes) {
        try {
            Booking booking = reviewService.findReviewableBooking(id, currentTourist(authentication));
            addFormData(model, booking, new ReviewForm());
            return "tourist/review-form";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/tourist/bookings";
        }
    }

    @PostMapping
    public String create(@PathVariable Long id, @ModelAttribute ReviewForm form, Authentication authentication,
                         Model model, RedirectAttributes redirectAttributes) {
        Tourist tourist = currentTourist(authentication);
        try {
            Review review = reviewService.create(id, tourist, form);
            redirectAttributes.addFlashAttribute("message", "¡Gracias por tu reseña! Ya aparece en la página de \""
                    + review.getBooking().getAvailability().getActivity().getName() + "\".");
            return "redirect:/tourist/bookings";
        } catch (BusinessException e) {
            if (e.getField() == null) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/tourist/bookings";
            }
            // Error en un campo: se vuelve al formulario con lo que había escrito y el mensaje al lado del campo
            addFormData(model, reviewService.findReviewableBooking(id, tourist), form);
            model.addAttribute(e.getField() + "Error", e.getMessage());
            return "tourist/review-form";
        }
    }

    private void addFormData(Model model, Booking booking, ReviewForm form) {
        model.addAttribute("booking", booking);
        model.addAttribute("activity", booking.getAvailability().getActivity());
        model.addAttribute("form", form);
        model.addAttribute("maxComment", ReviewService.MAX_COMMENT);
        model.addAttribute("minComment", ReviewService.MIN_COMMENT);
    }

    private Tourist currentTourist(Authentication authentication) {
        return (Tourist) userService.findByUsername(authentication.getName()).orElseThrow();
    }
}
