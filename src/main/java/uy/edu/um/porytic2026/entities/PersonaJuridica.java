package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "personasJuridicas")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder

public class PersonaJuridica extends Prestador {

    @Column(name = "RUT", unique = true)
    @NotBlank
    private String rut;

    @Column(name = "Responsable")
    @NotBlank
    private String responsable;

}