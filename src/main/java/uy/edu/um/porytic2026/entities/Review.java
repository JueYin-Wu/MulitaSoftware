package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comment")
    private String comment;

    @Column(name = "rating")
    @Min(1)
    @Max(5)
    private Integer rating;

    // Una reseña por reserva (unique = true): no se puede reseñar dos veces la misma reserva
    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    // Cuándo se escribió la reseña
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
}