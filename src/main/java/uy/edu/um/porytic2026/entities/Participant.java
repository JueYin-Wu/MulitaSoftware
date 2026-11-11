package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name")
    @NotBlank
    private String firstName;

    @Column(name = "last_name")
    @NotBlank
    private String lastName;

    // Fecha de nacimiento del acompañante: para controlar la edad mínima de la actividad
    @Column(name = "birth_date")
    private java.time.LocalDate birthDate;

    // Edad que va a tener el día de la actividad (null si no hay fecha de nacimiento)
    public Integer getAgeAt(java.time.LocalDate day) {
        return birthDate == null || day == null ? null : java.time.Period.between(birthDate, day).getYears();
    }

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
}