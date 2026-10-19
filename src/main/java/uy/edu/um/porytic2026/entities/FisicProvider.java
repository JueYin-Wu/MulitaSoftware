package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "fisic_providers",
        // No se puede repetir el mismo documento del mismo tipo
        uniqueConstraints = @UniqueConstraint(columnNames = {"document_type", "national_id"}))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder

public class FisicProvider extends Provider {

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Column(name = "national_id")
    @NotBlank
    private String nationalId;

    @Override
    public String getTypeLabel() {
        return "Persona física";
    }

    @Override
    public String getDocument() {
        return (documentType == DocumentType.PASSPORT ? "Pasaporte " : "CI ") + nationalId;
    }
}
