package uy.edu.um.porytic2026.payments;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

// Un pago tal como lo informa Mercado Pago (solo los datos que usamos)
public record GatewayPayment(
        String id,                  // número de operación en Mercado Pago
        String status,              // approved, rejected, pending, in_process, cancelled, refunded...
        String statusDetail,        // detalle, por ejemplo el motivo del rechazo
        BigDecimal amount,          // monto cobrado
        String externalReference,   // nuestra referencia: "booking-<id de la reserva>"
        String paymentTypeId,       // credit_card, debit_card, account_money...
        String lastFourDigits,      // últimos 4 dígitos de la tarjeta (nunca el número completo)
        Integer expirationMonth,
        Integer expirationYear,
        OffsetDateTime dateApproved) {
}
