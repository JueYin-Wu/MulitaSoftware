package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "amount")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PaymentStatus status;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    // Número de la operación en Mercado Pago (MER: "referencia externa")
    @Column(name = "external_reference")
    private String externalReference;

    // Detalle que da Mercado Pago (por ejemplo, el motivo de un rechazo: "cc_rejected_insufficient_amount")
    @Column(name = "status_detail")
    private String statusDetail;

    // Cuándo se hizo el último intento de pago
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Devolución (reembolso) al cancelar, según la política de la actividad
    @Column(name = "refunded_amount")
    private BigDecimal refundedAmount;

    @Column(name = "refund_date")
    private LocalDateTime refundDate;

    // Número de la devolución en Mercado Pago
    @Column(name = "refund_reference")
    private String refundReference;

    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    public String getStatusLabel() {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case PENDING -> "Pendiente";
            case APPROVED -> "Aprobado";
            case REJECTED -> "Rechazado";
            case REFUNDED -> refundedAmount != null && amount != null && refundedAmount.compareTo(amount) < 0
                    ? "Devuelto en parte" : "Devuelto";
        };
    }

    // Motivo de un rechazo, en palabras (a partir del detalle que manda Mercado Pago)
    public String getRejectionReason() {
        if (statusDetail == null) {
            return "La tarjeta rechazó el pago";
        }
        return switch (statusDetail) {
            case "cc_rejected_insufficient_amount" -> "La tarjeta no tiene fondos suficientes";
            case "cc_rejected_bad_filled_security_code" -> "El código de seguridad no es correcto";
            case "cc_rejected_bad_filled_date" -> "La fecha de vencimiento no es correcta";
            case "cc_rejected_bad_filled_card_number", "cc_rejected_bad_filled_other" -> "Algún dato de la tarjeta no es correcto";
            case "cc_rejected_call_for_authorize" -> "El banco pide que autorices el pago";
            case "cc_rejected_card_disabled" -> "La tarjeta está deshabilitada";
            case "cc_rejected_duplicated_payment" -> "Ya hiciste un pago igual hace un momento";
            case "cc_rejected_high_risk", "cc_rejected_blacklist" -> "Mercado Pago rechazó el pago por seguridad";
            case "expired" -> "El pago venció sin completarse";
            default -> "La tarjeta rechazó el pago";
        };
    }
}