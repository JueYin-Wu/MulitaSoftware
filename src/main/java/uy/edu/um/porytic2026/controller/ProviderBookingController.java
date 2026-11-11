package uy.edu.um.porytic2026.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uy.edu.um.porytic2026.entities.Provider;
import uy.edu.um.porytic2026.services.ActivityService;
import uy.edu.um.porytic2026.services.ProviderBookingService;
import uy.edu.um.porytic2026.services.UserService;
import uy.edu.um.porytic2026.services.WeatherService;
import uy.edu.um.porytic2026.weather.DailyForecast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Reservas de las actividades del prestador. Bajo /provider/** -> solo el rol PROVIDER, y cada prestador
// ve únicamente las reservas de SUS actividades.
// Ejemplos: /provider/bookings  |  /provider/bookings?when=past  |  /provider/bookings?activity=3
@Controller
@RequestMapping("/provider/bookings")
public class ProviderBookingController {

    private final ProviderBookingService providerBookingService;
    private final ActivityService activityService;
    private final UserService userService;
    private final WeatherService weatherService;

    public ProviderBookingController(ProviderBookingService providerBookingService, ActivityService activityService,
                                     UserService userService, WeatherService weatherService) {
        this.providerBookingService = providerBookingService;
        this.activityService = activityService;
        this.userService = userService;
        this.weatherService = weatherService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "upcoming") String when,
                       @RequestParam(required = false) Long activity,
                       Authentication authentication, Model model) {
        Provider provider = (Provider) userService.findByUsername(authentication.getName()).orElseThrow();
        boolean past = "past".equals(when);

        List<ProviderBookingService.DateSchedule> schedule = providerBookingService.schedule(provider, past, activity);

        // Clima previsto de las próximas salidas (id de la fecha -> pronóstico), útil para organizarse
        Map<Long, DailyForecast> forecasts = new HashMap<>();
        if (!past) {
            for (ProviderBookingService.DateSchedule date : schedule) {
                DailyForecast forecast = weatherService.forecastFor(
                        date.getAvailability().getActivity().getLocation(), date.getAvailability().getDate());
                if (forecast != null) {
                    forecasts.put(date.getAvailability().getId(), forecast);
                }
            }
        }

        model.addAttribute("schedule", schedule);
        model.addAttribute("summary", providerBookingService.summary(schedule));
        model.addAttribute("activities", activityService.findByProvider(provider.getId()));
        model.addAttribute("selectedActivity", activity);
        model.addAttribute("past", past);
        model.addAttribute("forecasts", forecasts);
        return "provider/bookings";
    }
}
