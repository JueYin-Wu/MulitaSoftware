package uy.edu.um.porytic2026.payments;

// Mercado Pago respondió con un error (por ejemplo, credenciales inválidas o un dato mal enviado)
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
