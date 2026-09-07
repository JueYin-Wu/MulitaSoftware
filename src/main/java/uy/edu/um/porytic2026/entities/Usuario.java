package uy.edu.um.porytic2026.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name= "usuarios")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class Usuario {

    @Id
    private long id;

    @Column(name = "nombre")
    @NotEmpty
    @NotNull
    @NotBlank
    @Size(max = 30, min = 10)
    private String name;

    @Column(name = "apellido")
    private String lastName;

    @Column(name = "nombreUsuario")
    private String userName;

    @Email(message = "Formato de correo electrónico no válido") //Mensaje cuando ocurre la validación
    private long userEmail;


}
