package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.YearMonth;

@Entity
@Table(name = "payment_methods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private CardType type;

    @Column(name = "last_digits")
    @Size(min = 4, max = 4)
    private String lastDigits;

    @Column(name = "expiration_date")
    private YearMonth expirationDate;

    @ManyToOne
    @JoinColumn(name = "tourist_id", nullable = false)
    private Tourist tourist;

    // "Crédito" / "Débito"
    public String getTypeLabel() {
        return type == CardType.DEBIT ? "Débito" : "Crédito";
    }

    // "•••• 0604": nunca se muestra (ni se guarda) el número completo
    public String getMaskedNumber() {
        return "•••• " + lastDigits;
    }

    // "11/30"
    public String getExpirationLabel() {
        return expirationDate == null ? "-" : String.format("%02d/%02d", expirationDate.getMonthValue(), expirationDate.getYear() % 100);
    }

    // La tarjeta ya venció (después del último día del mes de vencimiento)
    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(java.time.YearMonth.now());
    }
}