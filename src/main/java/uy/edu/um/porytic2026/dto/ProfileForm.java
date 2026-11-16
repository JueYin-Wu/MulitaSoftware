package uy.edu.um.porytic2026.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Datos que se pueden cambiar en "Editar perfil" (turistas y prestadores).
// El usuario, el documento y el RUT no se cambian: identifican a la persona o al emprendimiento.
@Getter
@Setter
@NoArgsConstructor
public class ProfileForm {

    @NotBlank(message = "Ingresá tu nombre")
    @Size(min = 3, max = 30, message = "El nombre debe tener entre 3 y 30 caracteres")
    private String firstName;

    @NotBlank(message = "Ingresá tu apellido")
    @Size(max = 50, message = "El apellido puede tener hasta 50 caracteres")
    private String lastName;

    @NotBlank(message = "Correo no válido")
    @Email(message = "Correo no válido")
    @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Correo no válido")
    private String email;

    @Size(max = 30, message = "El teléfono puede tener hasta 30 caracteres")
    private String phone;

    // Solo turistas
    private String birthDate; // AAAA-MM-DD

    // Solo prestadores
    @Size(max = 100, message = "El nombre puede tener hasta 100 caracteres")
    private String businessName;

    @Size(max = 255, message = "La dirección puede tener hasta 255 caracteres")
    private String address;

    @Size(max = 255, message = "La descripción puede tener hasta 255 caracteres")
    private String serviceDescription;

    // Solo persona jurídica
    @Size(max = 100, message = "El nombre puede tener hasta 100 caracteres")
    private String legalRepresentative;
}
