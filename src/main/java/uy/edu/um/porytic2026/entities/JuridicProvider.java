package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "juridic_providers")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder

public class JuridicProvider extends Provider {

    @Column(name = "tax_id", unique = true)
    @NotBlank
    private String taxId;

    @Column(name = "legal_representative")
    @NotBlank
    private String legalRepresentative;
}