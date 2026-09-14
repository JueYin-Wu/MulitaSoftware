package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "turistas")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Turista extends Usuario {

    @Column(name = "cedulaPasaporte", unique = true)
    @NotBlank
    private String cedulaPasaporte;
}