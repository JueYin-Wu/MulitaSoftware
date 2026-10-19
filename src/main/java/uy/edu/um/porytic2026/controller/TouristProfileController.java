package uy.edu.um.porytic2026.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uy.edu.um.porytic2026.entities.Tourist;
import uy.edu.um.porytic2026.services.FileStorageService;
import uy.edu.um.porytic2026.services.TouristService;
import uy.edu.um.porytic2026.services.UserService;

// Perfil de un turista. Se llega haciendo clic en su nombre en una reseña.
// /tourists/{id} lo ve cualquiera; /tourist/... solo el turista que inició sesión (para cambiar su foto).
@Controller
public class TouristProfileController {

    private final TouristService touristService;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    public TouristProfileController(TouristService touristService, UserService userService,
                                    FileStorageService fileStorageService) {
        this.touristService = touristService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/tourists/{id}")
    public String profile(@PathVariable Long id, Authentication authentication, Model model) {
        Tourist tourist;
        try {
            tourist = touristService.findById(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el turista");
        }

        // ¿Es su propio perfil? Entonces puede cambiar la foto y ver sus próximas reservas
        boolean isOwner = authentication != null && tourist.getUsername().equals(authentication.getName());

        model.addAttribute("tourist", tourist);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("completedBookings", touristService.findCompletedBookings(id));
        model.addAttribute("reviews", touristService.findReviews(id));
        if (isOwner) {
            model.addAttribute("upcomingBookings", touristService.findUpcomingBookings(id));
        }
        return "tourist/tourist-profile";
    }

    // "Mi perfil" del menú: lleva al perfil del turista que inició sesión
    @GetMapping("/tourist")
    public String myProfile(Authentication authentication) {
        return "redirect:/tourists/" + currentTourist(authentication).getId();
    }

    @PostMapping("/tourist/profile-image")
    public String uploadProfileImage(@RequestParam("image") MultipartFile image,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        Tourist tourist = currentTourist(authentication);
        try {
            String url = fileStorageService.storeImage(image, "users", "user-" + tourist.getId());
            userService.updateProfileImage(tourist, url, fileStorageService);
            redirectAttributes.addFlashAttribute("message", "Se actualizó tu foto de perfil.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tourists/" + tourist.getId();
    }

    @PostMapping("/tourist/profile-image/delete")
    public String deleteProfileImage(Authentication authentication, RedirectAttributes redirectAttributes) {
        Tourist tourist = currentTourist(authentication);
        userService.updateProfileImage(tourist, null, fileStorageService);
        redirectAttributes.addFlashAttribute("message", "Se quitó tu foto de perfil.");
        return "redirect:/tourists/" + tourist.getId();
    }

    private Tourist currentTourist(Authentication authentication) {
        return (Tourist) userService.findByUsername(authentication.getName()).orElseThrow();
    }
}
