package uy.edu.um.porytic2026.dto;

import lombok.Getter;
import lombok.Setter;

// Datos que llegan desde el formulario de registro de turista (CU03).
// Las validaciones se hacen en AuthController.
@Getter
@Setter
public class TouristRegistrationForm {

    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String documentType = "CI"; // "CI" (cédula) o "PASSPORT" (pasaporte)
    private String idDocument;
    private String password;
    private String confirmPassword;
}
