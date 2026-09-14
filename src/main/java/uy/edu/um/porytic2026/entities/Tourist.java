package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tourists")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder

public class Tourist extends User {

    @Column(name = "id_document", unique = true)
    @NotBlank
    private String idDocument;
}