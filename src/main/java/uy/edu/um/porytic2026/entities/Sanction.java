package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sanctions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Sanction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Advertencia (queda como antecedente) o suspensión (el prestador no puede operar hasta que se levante)
    @Enumerated(EnumType.STRING)
    @Column(name = "sanction_type")
    @NotNull
    private SanctionType type;

    @Column(name = "reason", length = 500)
    @NotBlank
    private String reason;

    // Fecha y hora en que el administrador aplicó la sanción (se guarda sola)
    @Column(name = "date")
    private LocalDateTime date;

    // Solo para suspensiones: cuándo se levantó. Mientras sea null, la suspensión está vigente.
    @Column(name = "lifted_date")
    private LocalDateTime liftedDate;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @ManyToOne
    @JoinColumn(name = "administrator_id", nullable = false)
    private Administrator administrator;

    // Suspensión que todavía no se levantó
    public boolean isActiveSuspension() {
        return type == SanctionType.SUSPENSION && liftedDate == null;
    }
}