package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tourists",
        // No se puede repetir el mismo documento del mismo tipo
        uniqueConstraints = @UniqueConstraint(columnNames = {"document_type", "id_document"}))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder

public class Tourist extends User {

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Column(name = "id_document")
    @NotBlank
    private String idDocument;
}