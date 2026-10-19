package uy.edu.um.porytic2026.exceptions;

// Error de negocio al registrar un usuario (por ejemplo, correo o cédula repetidos).
// Guarda el nombre del campo del formulario para mostrar el mensaje al lado de ese campo.
public class RegistrationException extends RuntimeException {

    private final String field;

    public RegistrationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
