package uy.edu.um.porytic2026.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uy.edu.um.porytic2026.dto.ProfileForm;
import uy.edu.um.porytic2026.entities.Provider;
import uy.edu.um.porytic2026.entities.User;
import uy.edu.um.porytic2026.exceptions.RegistrationException;
import uy.edu.um.porytic2026.services.FileStorageService;
import uy.edu.um.porytic2026.services.ProfileService;
import uy.edu.um.porytic2026.services.UserService;

// "Editar perfil": datos personales, foto y (para prestadores) datos del emprendimiento.
// Solo turistas y prestadores (ver SecurityConfig); cada uno edita únicamente su propio perfil.
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final ProfileService profileService;
    private final FileStorageService fileStorageService;

    public ProfileController(UserService userService, ProfileService profileService,
                             FileStorageService fileStorageService) {
        this.userService = userService;
        this.profileService = profileService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/edit")
    public String edit(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        model.addAttribute("form", profileService.toForm(user));
        addPageData(user, model);
        return "account/profile-edit";
    }

    @PostMapping("/edit")
    public String save(@Valid @ModelAttribute("form") ProfileForm form, BindingResult result,
                       Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        if (!result.hasErrors()) {
            try {
                profileService.update(user, form);
                redirectAttributes.addFlashAttribute("message", "Se guardaron los cambios de tu perfil.");
                return "redirect:" + profileUrl(user);
            } catch (RegistrationException e) {
                result.rejectValue(e.getField(), "invalid", e.getMessage());
            }
        }
        addPageData(user, model);
        return "account/profile-edit";
    }

    // Sube una foto de perfil nueva (JPG, PNG o WEBP, hasta 10 MB)
    @PostMapping("/image")
    public String uploadImage(@RequestParam("image") MultipartFile image, Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        try {
            String folder = user instanceof Provider ? "providers" : "users";
            String prefix = (user instanceof Provider ? "provider-" : "user-") + user.getId();
            String url = fileStorageService.storeImage(image, folder, prefix);
            userService.updateProfileImage(user, url, fileStorageService);
            redirectAttributes.addFlashAttribute("message", "Se actualizó tu foto de perfil.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile/edit";
    }

    @PostMapping("/image/delete")
    public String deleteImage(Authentication authentication, RedirectAttributes redirectAttributes) {
        userService.updateProfileImage(currentUser(authentication), null, fileStorageService);
        redirectAttributes.addFlashAttribute("message", "Se quitó tu foto de perfil.");
        return "redirect:/profile/edit";
    }

    private void addPageData(User user, Model model) {
        model.addAttribute("user", user);
        model.addAttribute("isProvider", user instanceof Provider);
        model.addAttribute("isCompany", user instanceof uy.edu.um.porytic2026.entities.JuridicProvider);
        model.addAttribute("backUrl", profileUrl(user));
    }

    // A dónde vuelve después de guardar: el perfil del turista o el portal del prestador
    private String profileUrl(User user) {
        return user instanceof Provider ? "/provider" : "/tourists/" + user.getId();
    }

    private User currentUser(Authentication authentication) {
        return userService.findByUsername(authentication.getName()).orElseThrow();
    }
}
