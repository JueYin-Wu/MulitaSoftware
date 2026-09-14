package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "personasFisicas")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder

public class PersonaFisica extends Prestador {

    @Column(name = "Cedula", unique = true)
    @NotBlank
    private String cedula;

}
