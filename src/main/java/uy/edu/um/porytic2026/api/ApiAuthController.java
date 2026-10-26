package uy.edu.um.porytic2026.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.porytic2026.api.dto.ApiDtos.TokenRequest;
import uy.edu.um.porytic2026.api.dto.ApiDtos.TokenResponse;
import uy.edu.um.porytic2026.services.JwtTokenService;

// POST /api/v1/auth/token -> recibe usuario y contraseña y devuelve un token JWT (200 OK)
// Usuario o contraseña incorrectos, o cuenta deshabilitada -> 401 Unauthorized
@RestController
@RequestMapping("/api/v1/auth")
public class ApiAuthController {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public ApiAuthController(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder,
                             JwtTokenService jwtTokenService) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> token(@RequestBody TokenRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            throw new IllegalArgumentException("Mandá username y password en el cuerpo (JSON)");
        }
        UserDetails user;
        try {
            user = userDetailsService.loadUserByUsername(request.username().trim());
        } catch (UsernameNotFoundException e) {
            throw new ApiUnauthorizedException("Usuario o contraseña incorrectos");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiUnauthorizedException("Usuario o contraseña incorrectos");
        }
        if (!user.isEnabled()) {
            throw new ApiUnauthorizedException("La cuenta todavía no está habilitada");
        }
        return ResponseEntity.ok(jwtTokenService.createToken(user));
    }
}
