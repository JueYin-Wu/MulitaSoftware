package uy.edu.um.porytic2026.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;
import lombok.*;
@Entity
@Table(name= "turistas")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Prestador extends Usuario {

    @Column(name = "documento")
    @Size(max = 12, min = 8) //12 = RUT. 8 = CI
    private long providerRUT; //Tiramos in if para categorizar si es persona física o jurídica

    @Column(name = "nombre fantasía")
    private long providerName;

    @Column(name = "dirección")
    private long providerUbication;

    @Column(name = "descripción")
    private long providerDescription;


}

