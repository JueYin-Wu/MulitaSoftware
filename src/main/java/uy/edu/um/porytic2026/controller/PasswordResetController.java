package uy.edu.um.porytic2026.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uy.edu.um.porytic2026.services.PasswordResetService;
import uy.edu.um.porytic2026.validation.DocumentValidator;

import java.util.Objects;
import java.util.Optional;

// "¿Olvidaste tu contraseña?": pedir el enlace y elegir una contraseña nueva
@Controller
public class PasswordResetController {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetController.class);

    private final PasswordResetService passwordResetService;

    // Mientras no se envíen correos, el enlace se muestra en pantalla (application.properties)
    @Value("${app.reset.show-link:false}")
    private boolean showLink;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestReset(@RequestParam(required = false) String email, Model model) {
        if (email == null || !DocumentValidator.isValidEmail(email)) {
            model.addAttribute("error", "Correo no válido. Ej: nombre@dominio.com");
            model.addAttribute("email", email);
            return "auth/forgot-password";
        }

        Optional<String> token = passwordResetService.createResetToken(email);
        String link = token.map(t -> ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/reset-password").queryParam("token", t).toUriString()).orElse(null);

        if (link != null) {
            // Simula el correo que se le enviaría al usuario
            log.info("[Simulación de correo] Enlace para restablecer la contraseña de {}: {}", email, link);
        }

        model.addAttribute("sent", true);
        if (showLink) {
            model.addAttribute("resetLink", link);
        }
        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("validToken", passwordResetService.isValid(token));
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                @RequestParam(required = false) String password,
                                @RequestParam(required = false) String confirmPassword,
                                Model model) {
        model.addAttribute("token", token);
        if (!passwordResetService.isValid(token)) {
            model.addAttribute("validToken", false);
            return "auth/reset-password";
        }
        model.addAttribute("validToken", true);

        boolean hasErrors = false;
        String passwordProblem = DocumentValidator.passwordProblem(password);
        if (passwordProblem != null) {
            model.addAttribute("passwordError", passwordProblem);
            hasErrors = true;
        }
        if (!Objects.equals(password, confirmPassword)) {
            model.addAttribute("confirmError", "Las contraseñas no coinciden");
            hasErrors = true;
        }
        if (hasErrors) {
            return "auth/reset-password";
        }

        passwordResetService.resetPassword(token, password);
        return "redirect:/login?reset";
    }
}
