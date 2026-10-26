package uy.edu.um.porytic2026.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uy.edu.um.porytic2026.dto.BookingForm;
import uy.edu.um.porytic2026.dto.ParticipantForm;
import uy.edu.um.porytic2026.entities.Availability;
import uy.edu.um.porytic2026.entities.Booking;
import uy.edu.um.porytic2026.entities.Tourist;
import uy.edu.um.porytic2026.exceptions.BusinessException;
import uy.edu.um.porytic2026.services.BookingService;
import uy.edu.um.porytic2026.services.UserService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Reservas del turista. SecurityConfig ya garantiza que a /tourist/** solo entra el rol TOURIST.
// Si alguien sin sesión toca "Reservar", Spring Security lo manda al login y después lo trae de vuelta acá.
@Controller
@RequestMapping("/tourist/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserService userService;

    public BookingController(BookingService bookingService, UserService userService) {
        this.bookingService = bookingService;
        this.userService = userService;
    }

    // Formulario de reserva para una fecha: /tourist/bookings/new?availabilityId=12
    @GetMapping("/new")
    public String form(@RequestParam Long availabilityId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Availability availability = bookingService.findBookable(availabilityId);
            BookingForm form = new BookingForm();
            form.setAvailabilityId(availabilityId);
            addFormData(model, availability, form);
            return "tourist/booking-form";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return redirectToActivity(availabilityId);
        }
    }

    // Confirmar la reserva
    @PostMapping
    public String create(@ModelAttribute BookingForm form,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Long availabilityId = form.getAvailabilityId();
        Tourist tourist = currentTourist(authentication);
        try {
            Booking booking = bookingService.create(tourist, availabilityId, form.getParticipantCount(), form.getCompanions());
            redirectAttributes.addFlashAttribute("message", "¡Listo! Reservaste " + booking.getParticipantCount()
                    + (booking.getParticipantCount() == 1 ? " lugar" : " lugares") + " para \""
                    + booking.getAvailability().getActivity().getName() + "\".");
            return "redirect:/tourist/bookings";
        } catch (BusinessException e) {
            // Si el problema es la cantidad o los datos de un acompañante, se vuelve al formulario
            // (con lo que ya había escrito) y cada mensaje aparece al lado de su campo
            if ("participantCount".equals(e.getField()) || "companions".equals(e.getField())) {
                try {
                    Availability availability = bookingService.findBookable(availabilityId);
                    addFormData(model, availability, form);
                    if ("participantCount".equals(e.getField())) {
                        model.addAttribute("participantError", e.getMessage());
                    } else {
                        model.addAttribute("formError", e.getMessage());
                        model.addAttribute("companionErrors",
                                bookingService.validateCompanions(tourist, form.getParticipantCount(), form.getCompanions()));
                    }
                    return "tourist/booking-form";
                } catch (BusinessException other) {
                    redirectAttributes.addFlashAttribute("error", other.getMessage());
                    return redirectToActivity(availabilityId);
                }
            }
            // Por ejemplo: se llenó el cupo o cerraron las reservas mientras completaba el formulario
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return redirectToActivity(availabilityId);
        }
    }

    // Mis reservas
    @GetMapping
    public String list(Authentication authentication, Model model) {
        Tourist tourist = currentTourist(authentication);
        List<Booking> upcoming = bookingService.findUpcoming(tourist);

        // Para cada reserva próxima: ¿se devuelve el dinero si cancela ahora?
        Map<Long, Boolean> refundableNow = new HashMap<>();
        for (Booking booking : upcoming) {
            refundableNow.put(booking.getId(), bookingService.isRefundableNow(booking));
        }

        model.addAttribute("upcoming", upcoming);
        model.addAttribute("history", bookingService.findHistory(tourist));
        model.addAttribute("refundableNow", refundableNow);
        return "tourist/bookings";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingService.cancel(id, currentTourist(authentication));
            redirectAttributes.addFlashAttribute("message",
                    "Se canceló tu reserva para \"" + booking.getAvailability().getActivity().getName() + "\".");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tourist/bookings";
    }

    // ===================== Métodos auxiliares =====================

    private void addFormData(Model model, Availability availability, BookingForm form) {
        int remaining = bookingService.remainingSpots(availability);
        int participantCount = form.getParticipantCount() == null ? 1 : form.getParticipantCount();

        // Se arma un bloque por cada acompañante posible (lugares libres - 1); la página muestra solo
        // los que hacen falta según la cantidad elegida (booking.js) y conserva lo que ya se escribió.
        int maxCompanions = Math.max(0, remaining - 1);
        List<ParticipantForm> companions = new ArrayList<>(form.getCompanions() == null ? List.of() : form.getCompanions());
        while (companions.size() < maxCompanions) {
            companions.add(new ParticipantForm());
        }
        form.setCompanions(new ArrayList<>(companions.subList(0, maxCompanions)));

        model.addAttribute("availability", availability);
        model.addAttribute("activity", availability.getActivity());
        model.addAttribute("remaining", remaining);
        model.addAttribute("participantCount", participantCount);
        model.addAttribute("bookingForm", form);
    }

    // Vuelve a la página de la actividad (ahí se muestra el error), o a la lista si la fecha no existe
    private String redirectToActivity(Long availabilityId) {
        Long activityId = bookingService.findActivityId(availabilityId);
        return activityId == null ? "redirect:/activities" : "redirect:/activities/" + activityId + "#fechas";
    }

    private Tourist currentTourist(Authentication authentication) {
        return (Tourist) userService.findByUsername(authentication.getName()).orElseThrow();
    }
}
