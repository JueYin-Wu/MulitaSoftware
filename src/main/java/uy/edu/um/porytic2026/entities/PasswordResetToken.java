package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Enlace de un solo uso para crear una contraseña nueva cuando el usuario la olvidó.
// Se guarda el token (un código aleatorio), a quién pertenece, cuándo vence y si ya se usó.
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
