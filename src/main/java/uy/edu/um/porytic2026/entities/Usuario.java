package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name= "usuarios")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder

public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre")
    @NotEmpty
    @NotNull
    @NotBlank
    @Size(max = 30, min = 3)
    private String name;

    @Column(name = "apellido")
    private String lastName;

    @Column(name = "nombreUsuario")
    private String userName;

    @Column(name = "correo")
    @Email(message = "Formato de correo electrónico no válido") //Mensaje cuando ocurre la validación
    private String userEmail;

    @Column(name = "contraseña")
    private String userPassword;

    @Column(name = "teléfono")
    private String userPhone;

}
