package uy.edu.um.porytic2026.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.porytic2026.payments.PaymentGatewayException;
import uy.edu.um.porytic2026.payments.PaymentGatewayUnavailableException;
import uy.edu.um.porytic2026.services.PaymentService;

import java.util.Map;

// Avisos automáticos (webhooks) de Mercado Pago: cada vez que cambia un pago, Mercado Pago llama a esta dirección.
// Solo funciona si la página tiene una dirección pública (no con localhost).
// Por seguridad, el aviso no se cree tal cual: con el número de pago se le vuelve a preguntar a la API de Mercado Pago.
//   200 -> aviso recibido   |   503 -> no pudimos consultar a Mercado Pago (Mercado Pago lo vuelve a mandar más tarde)
@RestController
@RequestMapping("/webhooks")
public class MercadoPagoWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    private final PaymentService paymentService;

    public MercadoPagoWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> notification(@RequestParam Map<String, String> params,
                                             @RequestBody(required = false) Map<String, Object> body) {
        // Mercado Pago manda el tipo y el número del pago en la dirección (?type=payment&data.id=123)
        // o en el cuerpo ({"type": "payment", "data": {"id": "123"}}), según la versión del aviso
        String type = params.getOrDefault("type", params.get("topic"));
        String paymentId = params.getOrDefault("data.id", params.get("id"));
        if (body != null) {
            if (type == null && body.get("type") != null) {
                type = String.valueOf(body.get("type"));
            }
            if (paymentId == null && body.get("data") instanceof Map<?, ?> data && data.get("id") != null) {
                paymentId = String.valueOf(data.get("id"));
            }
        }

        if (!"payment".equals(type) || paymentId == null) {
            return ResponseEntity.ok().build(); // otros avisos (órdenes, etc.) no nos interesan
        }
        try {
            paymentService.handleNotification(paymentId);
            return ResponseEntity.ok().build();
        } catch (PaymentGatewayUnavailableException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (PaymentGatewayException e) {
            log.warn("No se pudo procesar el aviso del pago {}: {}", paymentId, e.getMessage());
            return ResponseEntity.ok().build();
        }
    }
}
