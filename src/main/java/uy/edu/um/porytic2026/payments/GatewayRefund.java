package uy.edu.um.porytic2026.payments;

import java.math.BigDecimal;

// Una devolución tal como la informa Mercado Pago
public record GatewayRefund(String id, String status, BigDecimal amount) {
}
