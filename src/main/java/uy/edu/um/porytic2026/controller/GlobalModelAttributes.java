package uy.edu.um.porytic2026.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Se ejecuta antes de cada controller y agrega al modelo quién está logueado.
// Así el header puede mostrar "Iniciar sesión" o "Salir" según corresponda.
@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute
    public void addCurrentUser(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);

        String role = null;
        if (loggedIn) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                if (authority.getAuthority().startsWith("ROLE_")) {
                    role = authority.getAuthority().substring(5); // "ROLE_ADMIN" -> "ADMIN"
                }
            }
        }

        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("currentUsername", loggedIn ? auth.getName() : null);
        model.addAttribute("currentRole", role);
    }
}
