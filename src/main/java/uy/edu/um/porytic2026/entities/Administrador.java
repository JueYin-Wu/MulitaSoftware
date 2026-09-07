package uy.edu.um.porytic2026.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;
import lombok.*;
@Entity
@Table(name= "administradores")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Administrador extends Usuario{

    //vacío porque no tiene más atributos que los de usurario
}
