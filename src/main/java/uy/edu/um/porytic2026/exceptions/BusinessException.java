package uy.edu.um.porytic2026.exceptions;

// Error de una regla de negocio (por ejemplo, un departamento que no existe o un reembolso sin porcentaje).
// Si el error corresponde a un campo del formulario, se guarda su nombre para mostrar el mensaje al lado.
// Si field es null, el mensaje se muestra como error general arriba del formulario.
public class BusinessException extends RuntimeException {

    private final String field;

    public BusinessException(String field, String message) {
        super(message);
        this.field = field;
    }

    public BusinessException(String message) {
        this(null, message);
    }

    public String getField() {
        return field;
    }
}
