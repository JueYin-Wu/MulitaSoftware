package uy.edu.um.porytic2026.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import uy.edu.um.porytic2026.entities.Provider;
import uy.edu.um.porytic2026.repository.UserRepository;

// Si a un prestador lo suspenden mientras tiene la sesión abierta, el login ya no lo frena
// (solo se controla al iniciar sesión). Este interceptor revisa cada pedido a /provider/**:
// si el prestador está suspendido, le cierra la sesión y lo manda al login con el aviso.
@Component
public class SuspendedProviderInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    public SuspendedProviderInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return true;
        }
        boolean suspended = userRepository.findByUsername(auth.getName())
                .filter(user -> user instanceof Provider provider && provider.isSuspended())
                .isPresent();
        if (!suspended) {
            return true;
        }

        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(request.getContextPath() + "/login?suspended");
        return false;
    }
}
