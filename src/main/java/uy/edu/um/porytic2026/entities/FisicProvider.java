package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "fisic_providers")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder

public class FisicProvider extends Provider {

    @Column(name = "national_id", unique = true)
    @NotBlank
    private String nationalId;
}
