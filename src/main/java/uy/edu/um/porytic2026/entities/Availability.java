package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "availabilities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Availability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date")
    @NotNull
    private LocalDate date;

    @Column(name = "time")
    @NotNull
    private LocalTime time;

    @Column(name = "capacity")
    @PositiveOrZero
    private Integer capacity;

    @ManyToOne
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    // Fecha y hora en que empieza la actividad
    public LocalDateTime getStartDateTime() {
        return LocalDateTime.of(date, time);
    }

    // Último momento en que se puede reservar: el inicio menos los minutos que fijó el prestador.
    // Si el prestador puso 0 (o nada), se puede reservar hasta la hora de inicio.
    public LocalDateTime getBookingClosesAt() {
        Integer minutes = activity == null ? null : activity.getBookingDeadline();
        return getStartDateTime().minusMinutes(minutes == null ? 0 : minutes);
    }
}