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

    // Fecha de nacimiento: para controlar la edad mínima de las actividades
    @Column(name = "birth_date")
    private java.time.LocalDate birthDate;

    // Edad que tiene (o va a tener) en un día, por ejemplo el de la actividad. Null si no cargó su fecha de nacimiento.
    public Integer getAgeAt(java.time.LocalDate day) {
        return birthDate == null || day == null ? null : java.time.Period.between(birthDate, day).getYears();
    }
}