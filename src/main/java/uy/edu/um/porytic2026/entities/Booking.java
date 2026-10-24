package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_date")
    @NotNull
    private LocalDateTime bookingDate;

    @Column(name = "participant_count")
    @Positive
    private Integer participantCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BookingStatus status;

    @ManyToOne
    @JoinColumn(name = "tourist_id", nullable = false)
    private Tourist tourist;

    @ManyToOne
    @JoinColumn(name = "availability_id", nullable = false)
    private Availability availability;

    // Precio total: precio por persona de la actividad por la cantidad de participantes
    public Double getTotalPrice() {
        if (availability == null || availability.getActivity() == null
                || availability.getActivity().getPrice() == null || participantCount == null) {
            return null;
        }
        return availability.getActivity().getPrice() * participantCount;
    }

    // Texto del estado para mostrar en pantalla
    public String getStatusLabel() {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case PENDING -> "Pendiente de pago";
            case CONFIRMED -> "Confirmada";
            case CANCELLED -> "Cancelada";
            case EXPIRED -> "Vencida";
        };
    }

    // Reserva que todavía ocupa lugar (no fue cancelada ni venció)
    public boolean isActive() {
        return status == BookingStatus.PENDING || status == BookingStatus.CONFIRMED;
    }
}