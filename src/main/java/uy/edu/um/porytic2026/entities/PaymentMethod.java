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
}