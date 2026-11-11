package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    // Precio por persona al momento de reservar. Si después el prestador cambia el precio de la actividad,
    // las reservas ya hechas mantienen el suyo. (Null en reservas viejas: se usa el precio actual.)
    @Column(name = "unit_price")
    private Double unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BookingStatus status;

    @ManyToOne
    @JoinColumn(name = "tourist_id", nullable = false)
    private Tourist tourist;

    @ManyToOne
    @JoinColumn(name = "availability_id", nullable = false)
    private Availability availability;

    // Pago de la reserva (MER: PAGO "corresponde a" RESERVA, 1 a 1). Es null mientras no se intentó pagar.
    @OneToOne(mappedBy = "booking")
    private Payment payment;

    // Reseña que dejó el turista después de hacer la actividad (null si todavía no la dejó)
    @OneToOne(mappedBy = "booking")
    private Review review;

    // Acompañantes (MER: RESERVA "incluye" PARTICIPANTE). El turista que reserva es el primer participante
    // y sus datos ya están en su cuenta, así que acá van solo los demás: participantCount - 1 personas.
    // cascade = ALL: al guardar la reserva se guardan también sus acompañantes.
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    @Builder.Default
    private List<Participant> participants = new ArrayList<>();

    // Precio por persona de esta reserva: el que tenía la actividad cuando se reservó
    public Double getEffectiveUnitPrice() {
        if (unitPrice != null) {
            return unitPrice;
        }
        return availability == null || availability.getActivity() == null ? null : availability.getActivity().getPrice();
    }

    // Precio total: precio por persona por la cantidad de participantes
    public Double getTotalPrice() {
        Double price = getEffectiveUnitPrice();
        if (price == null || participantCount == null) {
            return null;
        }
        return price * participantCount;
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