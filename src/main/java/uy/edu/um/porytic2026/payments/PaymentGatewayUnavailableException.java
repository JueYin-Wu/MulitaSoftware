package uy.edu.um.porytic2026.payments;

// Mercado Pago no está disponible: no responde, tarda demasiado o devuelve un error de su lado (5xx).
// En ese caso nunca se confirma nada: la reserva queda como estaba y se puede reintentar más tarde.
public class PaymentGatewayUnavailableException extends PaymentGatewayException {

    public PaymentGatewayUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
