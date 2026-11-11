package uy.edu.um.porytic2026.payments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

// Cliente de la API REST de Mercado Pago (servicio externo de pagos, letra: "Integración con sistemas externos").
// Solo hace pedidos HTTP y traduce las respuestas; las reglas del negocio están en PaymentService.
//
// Manejo de errores:
//  - Mercado Pago no responde / tarda más de lo permitido / error 5xx o 429 -> PaymentGatewayUnavailableException
//    (se reintenta una vez antes de rendirse)
//  - Error 4xx (credenciales inválidas, datos mal enviados)                  -> PaymentGatewayException
@Component
public class MercadoPagoClient {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoClient.class);
    private static final int MAX_ATTEMPTS = 2;

    private final RestClient restClient;
    private final String accessToken;
    private final String publicKey;

    public MercadoPagoClient(@Value("${app.mercadopago.api-url:https://api.mercadopago.com}") String apiUrl,
                             @Value("${app.mercadopago.access-token:}") String accessToken,
                             @Value("${app.mercadopago.public-key:}") String publicKey,
                             @Value("${app.mercadopago.timeout-seconds:10}") int timeoutSeconds) {
        // Tiempo máximo para conectar y para esperar la respuesta: si Mercado Pago no contesta, no nos quedamos colgados
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .requestFactory(requestFactory)
                .build();
        this.accessToken = accessToken == null ? "" : accessToken.trim();
        this.publicKey = publicKey == null ? "" : publicKey.trim();
    }

    // ¿Se cargó el Access Token? Sin él no se puede cobrar.
    public boolean isConfigured() {
        return !accessToken.isBlank();
    }

    // Clave pública: la usa el formulario de tarjeta en el navegador (no es secreta)
    public String getPublicKey() {
        return publicKey;
    }

    // POST /v1/payments -> cobra con el token de la tarjeta (Checkout API: el turista paga en NUESTRA página).
    // El token lo genera el formulario de Mercado Pago en el navegador: el número de tarjeta nunca pasa por nuestro servidor.
    // La clave de idempotencia es la misma en el reintento, así un reintento nunca cobra dos veces.
    public GatewayPayment createPayment(Map<String, Object> payment) {
        String idempotencyKey = UUID.randomUUID().toString();
        Map<String, Object> response = call(() -> restClient.post()
                .uri("/v1/payments")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payment)
                .retrieve()
                .body(Map.class));
        return toPayment(response);
    }

    // GET /v1/payments/search -> pagos hechos con nuestra referencia, del más nuevo al más viejo
    public List<GatewayPayment> searchPayments(String externalReference) {
        Map<String, Object> response = call(() -> restClient.get()
                .uri(uri -> uri.path("/v1/payments/search")
                        .queryParam("external_reference", externalReference)
                        .queryParam("sort", "date_created")
                        .queryParam("criteria", "desc")
                        .queryParam("limit", 20)
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class));

        Object results = response.get("results");
        if (!(results instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> toPayment((Map<?, ?>) item))
                .toList();
    }

    // POST /v1/payments/{id}/refunds -> devuelve dinero de un pago (todo o una parte).
    // La "clave de idempotencia" es la misma en el reintento: si el primer pedido llegó pero se perdió la
    // respuesta, Mercado Pago reconoce el reintento y NO devuelve el dinero dos veces.
    public GatewayRefund refund(String paymentId, BigDecimal amount) {
        String idempotencyKey = UUID.randomUUID().toString();
        Map<String, Object> response = call(() -> restClient.post()
                .uri("/v1/payments/{id}/refunds", paymentId)
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amount))
                .retrieve()
                .body(Map.class));
        return new GatewayRefund(text(response.get("id")), text(response.get("status")), decimal(response.get("amount")));
    }

    // GET /v1/payments/{id} -> un pago (se usa cuando Mercado Pago nos avisa por webhook)
    public GatewayPayment getPayment(String paymentId) {
        Map<String, Object> response = call(() -> restClient.get()
                .uri("/v1/payments/{id}", paymentId)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class));
        return toPayment(response);
    }

    // ===================== Manejo de errores y reintentos =====================

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, Object> call(Supplier<Map> request) {
        if (!isConfigured()) {
            throw new PaymentGatewayException("Falta configurar el Access Token de Mercado Pago");
        }
        PaymentGatewayUnavailableException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                Map response = request.get();
                if (response == null) {
                    throw new PaymentGatewayException("Mercado Pago devolvió una respuesta vacía");
                }
                return (Map<String, Object>) response;
            } catch (RestClientResponseException e) {
                int code = e.getStatusCode().value();
                if (code >= 500 || code == 429) {
                    log.warn("Mercado Pago respondió {} (intento {} de {})", code, attempt, MAX_ATTEMPTS);
                    lastError = new PaymentGatewayUnavailableException("Mercado Pago no está disponible (" + code + ")", e);
                } else if (code == 401 || code == 403) {
                    throw new PaymentGatewayException("Las credenciales de Mercado Pago no son válidas", e);
                } else {
                    log.warn("Mercado Pago rechazó el pedido ({}): {}", code, e.getResponseBodyAsString());
                    throw new PaymentGatewayException("Mercado Pago rechazó el pedido (" + code + ")", e);
                }
            } catch (ResourceAccessException e) {
                // No hubo conexión, o Mercado Pago tardó más que el tiempo máximo
                log.warn("No se pudo conectar con Mercado Pago (intento {} de {}): {}", attempt, MAX_ATTEMPTS, e.getMessage());
                lastError = new PaymentGatewayUnavailableException("No se pudo conectar con Mercado Pago", e);
            }
        }
        throw lastError;
    }

    // ===================== Lectura de la respuesta =====================

    private GatewayPayment toPayment(Map<?, ?> data) {
        Map<?, ?> card = data.get("card") instanceof Map<?, ?> c ? c : Map.of();
        return new GatewayPayment(
                text(data.get("id")),
                text(data.get("status")),
                text(data.get("status_detail")),
                decimal(data.get("transaction_amount")),
                text(data.get("external_reference")),
                text(data.get("payment_type_id")),
                text(card.get("last_four_digits")),
                integer(card.get("expiration_month")),
                integer(card.get("expiration_year")),
                dateTime(data.get("date_approved")));
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? null : Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal decimal(Object value) {
        try {
            return value == null ? null : new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static OffsetDateTime dateTime(Object value) {
        try {
            return value == null ? null : OffsetDateTime.parse(String.valueOf(value));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
