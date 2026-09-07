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
public class Turista extends Usuario{

    @Column(name = "documento")
    private String turistCI;

    @Column(name = "nombre")
    private String turistName;

    @Column(name = "apellido")
    private String turistSurname;


}
