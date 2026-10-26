package uy.edu.um.porytic2026.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import uy.edu.um.porytic2026.entities.User;
import uy.edu.um.porytic2026.services.UserService;

// Se ejecuta antes de cada controller y agrega al modelo quién está logueado.
// Así el header puede mostrar "Ingresar" o el botón del usuario (foto + nombre) según corresponda.
// Solo para las páginas del sitio (paquete controller); la API REST no usa el header.
@ControllerAdvice(basePackages = "uy.edu.um.porytic2026.controller")
public class GlobalModelAttributes {

    private final UserService userService;

    public GlobalModelAttributes(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute
    public void addCurrentUser(Model model, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);

        String role = null;
        User currentUser = null;
        if (loggedIn) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                if (authority.getAuthority().startsWith("ROLE_")) {
                    role = authority.getAuthority().substring(5); // "ROLE_ADMIN" -> "ADMIN"
                }
            }
            // Datos del usuario para el botón del header: foto y nombre
            currentUser = userService.findByUsername(auth.getName()).orElse(null);

            // La sesión es de un usuario que ya no existe. Pasa porque la base H2 en memoria se borra al
            // reiniciar, pero el navegador conserva la sesión. Se cierra para que aparezca "Ingresar".
            if (currentUser == null) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                loggedIn = false;
                role = null;
            }
        }

        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("currentUsername", loggedIn ? auth.getName() : null);
        model.addAttribute("currentRole", role);
        model.addAttribute("currentUser", currentUser);
    }
}
