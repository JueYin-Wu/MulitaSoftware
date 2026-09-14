package uy.edu.um.porytic2026.entities;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class Ubicacion {

    private String departamento;
    private String barrio;
    private String calle;
    private String nro;
}