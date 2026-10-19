package uy.edu.um.porytic2026.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

// Datos que llegan desde el formulario de registro de proveedor.
// No es una entidad: no se guarda así en la base, el service arma el FisicProvider o JuridicProvider.
@Getter
@Setter
public class ProviderRegistrationForm {

    public static final String FISIC = "FISIC";
    public static final String JURIDIC = "JURIDIC";

    @NotBlank
    @Pattern(regexp = "FISIC|JURIDIC", message = "Elegí un tipo de proveedor")
    private String providerType = FISIC;

    @NotBlank(message = "Ingresá tu nombre")
    @Size(min = 3, max = 30, message = "El nombre debe tener entre 3 y 30 caracteres")
    private String firstName;

    @NotBlank(message = "Ingresá tu apellido")
    private String lastName;

    // CU01 4a: si el correo no cumple el formato se muestra "Correo no válido"
    @NotBlank(message = "Correo no válido")
    @Email(message = "Correo no válido")
    @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Correo no válido")
    private String email;

    @NotBlank(message = "Ingresá una contraseña")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "Repetí la contraseña")
    private String confirmPassword;

    @NotBlank(message = "Ingresá un teléfono de contacto")
    private String phone;

    @NotBlank(message = "Ingresá el nombre del emprendimiento")
    private String businessName;

    @NotBlank(message = "Ingresá la dirección")
    private String address;

    @Size(max = 255, message = "La descripción puede tener hasta 255 caracteres")
    private String serviceDescription;

    // Solo persona física: tipo de documento ("CI" o "PASSPORT") y su número
    private String documentType = "CI";
    private String nationalId;

    // Solo persona jurídica
    private String taxId;
    private String legalRepresentative;

    public boolean isFisic() {
        return FISIC.equals(providerType);
    }

    public boolean isJuridic() {
        return JURIDIC.equals(providerType);
    }
}
