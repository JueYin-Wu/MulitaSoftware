package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "prestadores")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder

public class Prestador extends Usuario {

    @Column(name = "nombreFantasia")
    @NotBlank
    private String nombreFantasia;

    @Column(name = "direccion")
    private String direccion;

    @Column(name = "descripcionServ")
    private String descripcionServ;
}