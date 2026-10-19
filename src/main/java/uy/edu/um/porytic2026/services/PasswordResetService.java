package uy.edu.um.porytic2026.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.porytic2026.entities.PasswordResetToken;
import uy.edu.um.porytic2026.entities.User;
import uy.edu.um.porytic2026.repository.PasswordResetTokenRepository;
import uy.edu.um.porytic2026.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

// Recuperación de contraseña. La contraseña está encriptada, así que no se puede mostrar:
// se le da al usuario un enlace de un solo uso para que elija una nueva.
@Service
public class PasswordResetService {

    private static final int MINUTES_VALID = 30;

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository, UserRepository userRepository,
                                PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Si existe una cuenta con ese correo, crea un token que vence en 30 minutos y lo devuelve.
    // Si no existe, devuelve vacío (la pantalla muestra el mismo mensaje en los dos casos).
    @Transactional
    public Optional<String> createResetToken(String email) {
        if (email == null) {
            return Optional.empty();
        }
        Optional<User> user = userRepository.findByEmail(email.trim().toLowerCase());
        if (user.isEmpty()) {
            return Optional.empty();
        }

        String token = UUID.randomUUID().toString();
        tokenRepository.save(PasswordResetToken.builder()
                .token(token)
                .user(user.get())
                .expiresAt(LocalDateTime.now().plusMinutes(MINUTES_VALID))
                .used(false)
                .build());
        return Optional.of(token);
    }

    // Un enlace sirve si existe, no se usó y no venció
    public boolean isValid(String token) {
        return token != null && tokenRepository.findByToken(token)
                .filter(t -> !t.isUsed() && !t.isExpired())
                .isPresent();
    }

    // Guarda la contraseña nueva (encriptada) y marca el enlace como usado
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .filter(t -> !t.isUsed() && !t.isExpired())
                .orElseThrow(() -> new IllegalArgumentException("El enlace no es válido o ya venció"));

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
}
