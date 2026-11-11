package uy.edu.um.porytic2026.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.porytic2026.dto.CardPaymentRequest;
import uy.edu.um.porytic2026.entities.*;
import uy.edu.um.porytic2026.exceptions.BusinessException;
import uy.edu.um.porytic2026.payments.GatewayPayment;
import uy.edu.um.porytic2026.payments.GatewayRefund;
import uy.edu.um.porytic2026.payments.MercadoPagoClient;
import uy.edu.um.porytic2026.repository.BookingRepository;
import uy.edu.um.porytic2026.repository.PaymentMethodRepository;
import uy.edu.um.porytic2026.repository.PaymentRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Pago de las reservas con Mercado Pago (Checkout API): el turista paga en NUESTRA página, sin ir a otro sitio.
//
// Flujo:
//  1) payWithCard: el formulario de tarjeta de Mercado Pago (dentro de nuestra página) genera un token;
//     el servidor le pide a la API de Mercado Pago que cobre con ese token y aplica el resultado a la reserva:
//       - aprobado  -> la reserva pasa a CONFIRMED
//       - rechazado -> la reserva sigue PENDING y se puede reintentar
//       - pendiente -> se espera
//  2) refreshStatus / handleNotification: si el pago quedó "en proceso", se le vuelve a PREGUNTAR a
//     Mercado Pago cómo terminó (nunca se confía en lo que diga el navegador).
//  3) Si Mercado Pago no está disponible, no se cambia nada: nunca se confirma una reserva sin pago aprobado.
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    // Referencia que viaja a Mercado Pago para saber a qué reserva corresponde cada pago
    public static final String REFERENCE_PREFIX = "booking-";

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final BookingRepository bookingRepository;
    private final MercadoPagoClient mercadoPago;
    private final BookingService bookingService;
    private final Clock clock;
    private final String publicUrl;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentMethodRepository paymentMethodRepository,
                          BookingRepository bookingRepository,
                          MercadoPagoClient mercadoPago,
                          BookingService bookingService,
                          Clock clock,
                          @Value("${app.public-url:http://localhost:8081}") String publicUrl) {
        this.paymentRepository = paymentRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.bookingRepository = bookingRepository;
        this.mercadoPago = mercadoPago;
        this.bookingService = bookingService;
        this.clock = clock;
        this.publicUrl = publicUrl == null ? "" : publicUrl.replaceAll("/+$", "");
    }

    // Para cobrar hacen falta el Access Token (servidor) y la Public Key (formulario de tarjeta)
    public boolean isConfigured() {
        return mercadoPago.isConfigured() && !mercadoPago.getPublicKey().isBlank();
    }

    public String getPublicKey() {
        return mercadoPago.getPublicKey();
    }

    // La reserva tiene que existir y ser del turista que inició sesión
    public Booking findOwnBooking(Long bookingId, Tourist tourist) {
        return bookingRepository.findById(bookingId)
                .filter(b -> b.getTourist().getId().equals(tourist.getId()))
                .orElseThrow(() -> new BusinessException("La reserva no existe o no es tuya"));
    }

    public Optional<Payment> findPayment(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }

    // 1) Pagar con tarjeta desde nuestra página. Devuelve cómo quedó el pago (aprobado, rechazado o en proceso).
    @Transactional
    public Payment payWithCard(Long bookingId, Tourist tourist, CardPaymentRequest card) {
        Booking booking = findOwnBooking(bookingId, tourist);

        // Por si ya pagó antes (por ejemplo, en otra pestaña): primero se le pregunta a Mercado Pago
        syncWithGateway(booking);

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new BusinessException("Esta reserva ya está pagada y confirmada");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Esta reserva está " + booking.getStatusLabel().toLowerCase() + " y ya no se puede pagar");
        }
        if (!LocalDateTime.now(clock).isBefore(booking.getAvailability().getBookingClosesAt())) {
            throw new BusinessException("Ya cerró el plazo para reservar esa fecha, así que no se puede pagar");
        }
        if (expectedAmount(booking) == null) {
            throw new BusinessException("La actividad no tiene precio, no se puede cobrar");
        }
        if (card == null || isBlank(card.getToken()) || isBlank(card.getPaymentMethodId())) {
            throw new BusinessException("Faltan los datos de la tarjeta");
        }

        GatewayPayment result = mercadoPago.createPayment(buildPayment(booking, tourist, card));
        return apply(booking, List.of(result));
    }

    // 2) Consultar en Mercado Pago cómo está el pago de una reserva y actualizarla
    @Transactional
    public Booking refreshStatus(Long bookingId, Tourist tourist) {
        Booking booking = findOwnBooking(bookingId, tourist);
        syncWithGateway(booking);
        return booking;
    }

    // 2b) Aviso automático de Mercado Pago (webhook): llega el número de un pago y se lo vuelve a pedir
    //     a la API, así nadie puede confirmar una reserva mandando un aviso falso
    @Transactional
    public void handleNotification(String gatewayPaymentId) {
        GatewayPayment payment = mercadoPago.getPayment(gatewayPaymentId);
        Long bookingId = bookingIdFrom(payment.externalReference());
        if (bookingId == null) {
            log.info("Aviso de Mercado Pago de un pago que no es de una reserva: {}", gatewayPaymentId);
            return;
        }
        bookingRepository.findById(bookingId).ifPresent(booking -> apply(booking, List.of(payment)));
    }

    // Resultado de cancelar: la reserva y cuánto se devolvió (null si no correspondía devolver nada)
    public record CancellationResult(Booking booking, BigDecimal refundedAmount, boolean wasPaid) {
    }

    // 3) Cancelar una reserva y, si estaba pagada, devolver el dinero según la política de la actividad:
    //    - la actividad es reembolsable y se cancela antes del plazo (minutos de cancelación antes del inicio)
    //      -> se devuelve el % de reembolso que fijó el prestador
    //    - fuera de plazo, o actividad no reembolsable -> se cancela sin devolución
    //    Primero se pide la devolución a Mercado Pago y recién si sale bien se cancela la reserva:
    //    si Mercado Pago no está disponible, la reserva sigue activa y el turista puede reintentar.
    @Transactional
    public CancellationResult cancelBooking(Long bookingId, Tourist tourist) {
        Booking booking = findOwnBooking(bookingId, tourist);

        // Por si pagó y todavía no nos enteramos: se consulta a Mercado Pago antes de cancelar
        Optional<Payment> existing = paymentRepository.findByBookingId(bookingId);
        if (existing.isPresent() && mercadoPago.isConfigured()) {
            syncWithGateway(booking);
        }

        // Mismas reglas que BookingService.cancel, pero antes de tocar el dinero
        if (!booking.isActive()) {
            throw new BusinessException("Esa reserva ya estaba " + booking.getStatusLabel().toLowerCase());
        }
        if (!LocalDateTime.now(clock).isBefore(booking.getAvailability().getStartDateTime())) {
            throw new BusinessException("No se puede cancelar una actividad que ya empezó o ya pasó");
        }

        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        boolean wasPaid = booking.getStatus() == BookingStatus.CONFIRMED
                && payment != null && payment.getStatus() == PaymentStatus.APPROVED;

        BigDecimal refunded = null;
        if (wasPaid && bookingService.isRefundableNow(booking)) {
            BigDecimal amount = refundAmount(payment, booking.getAvailability().getActivity().getRefundPercentage());
            if (amount.signum() > 0) {
                GatewayRefund refund = mercadoPago.refund(payment.getExternalReference(), amount);
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setRefundedAmount(refund.amount() != null ? refund.amount() : amount);
                payment.setRefundReference(refund.id());
                payment.setRefundDate(LocalDateTime.now(clock));
                paymentRepository.save(payment);
                refunded = payment.getRefundedAmount();
            }
        }

        Booking cancelled = bookingService.cancel(bookingId, tourist);
        return new CancellationResult(cancelled, refunded, wasPaid);
    }

    // Cuánto se devuelve: el porcentaje de lo que se pagó, redondeado a 2 decimales
    static BigDecimal refundAmount(Payment payment, Integer percentage) {
        if (payment.getAmount() == null || percentage == null || percentage <= 0) {
            return BigDecimal.ZERO;
        }
        int pct = Math.min(percentage, 100);
        return payment.getAmount()
                .multiply(BigDecimal.valueOf(pct))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    // ===================== Lógica interna =====================

    private void syncWithGateway(Booking booking) {
        List<GatewayPayment> payments = mercadoPago.searchPayments(REFERENCE_PREFIX + booking.getId());
        apply(booking, payments);
    }

    // Aplica a la reserva el resultado de Mercado Pago. Devuelve el pago guardado (null si no había ninguno)
    private Payment apply(Booking booking, List<GatewayPayment> gatewayPayments) {
        if (gatewayPayments.isEmpty()) {
            return null; // todavía no pagó (o no terminó de pagar)
        }
        // Si alguno de los intentos fue aprobado, ese manda; si no, el más reciente
        GatewayPayment result = gatewayPayments.stream()
                .filter(p -> "approved".equals(p.status()))
                .findFirst()
                .orElse(gatewayPayments.get(0));

        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseGet(() -> Payment.builder().booking(booking).amount(expectedAmount(booking)).build());
        if (payment.getRefundedAmount() != null) {
            return payment; // ya se devolvió (todo o en parte): Mercado Pago lo sigue mostrando "aprobado", no se vuelve atrás
        }
        PaymentStatus status = toStatus(result.status());
        if (payment.getCreatedAt() == null || (result.id() != null && !result.id().equals(payment.getExternalReference()))) {
            payment.setCreatedAt(LocalDateTime.now(clock)); // nuevo intento de pago
        }
        payment.setStatus(status);
        payment.setStatusDetail(result.statusDetail());
        payment.setExternalReference(result.id());

        if (status == PaymentStatus.APPROVED) {
            // Control extra: el monto cobrado tiene que cubrir el total de la reserva
            BigDecimal expected = expectedAmount(booking);
            if (result.amount() != null && expected != null && result.amount().compareTo(expected) < 0) {
                log.warn("Pago {} de la reserva {} por {} cuando el total es {}: no se confirma",
                        result.id(), booking.getId(), result.amount(), expected);
                payment.setStatus(PaymentStatus.PENDING);
                payment.setStatusDetail("amount_mismatch");
                paymentRepository.save(payment);
                return payment;
            }
            payment.setAmount(result.amount() != null ? result.amount() : expected);
            payment.setPaymentDate(result.dateApproved() != null
                    ? result.dateApproved().atZoneSameInstant(clock.getZone()).toLocalDateTime()
                    : LocalDateTime.now(clock));
            payment.setPaymentMethod(rememberPaymentMethod(booking.getTourist(), result));

            if (booking.getStatus() == BookingStatus.PENDING) {
                booking.setStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(booking);
            } else if (booking.getStatus() != BookingStatus.CONFIRMED) {
                // Pagó una reserva que ya estaba cancelada o vencida: hay que devolverle el dinero
                log.warn("Pago aprobado {} de la reserva {} que está {}: corresponde un reembolso",
                        result.id(), booking.getId(), booking.getStatus());
            }
        }
        paymentRepository.save(payment);
        return payment;
    }

    // Estado de Mercado Pago -> estado del pago en nuestro sistema
    static PaymentStatus toStatus(String gatewayStatus) {
        if (gatewayStatus == null) {
            return PaymentStatus.PENDING;
        }
        return switch (gatewayStatus) {
            case "approved" -> PaymentStatus.APPROVED;
            case "rejected", "cancelled" -> PaymentStatus.REJECTED;
            case "refunded", "charged_back" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.PENDING; // pending, in_process, authorized, in_mediation
        };
    }

    // Guarda el medio de pago del turista (MER: MEDIO DE PAGO): tipo, últimos 4 dígitos y vencimiento.
    // Nunca se guarda el número completo de la tarjeta: Mercado Pago ni siquiera nos lo manda.
    private PaymentMethod rememberPaymentMethod(Tourist tourist, GatewayPayment result) {
        CardType type = switch (result.paymentTypeId() == null ? "" : result.paymentTypeId()) {
            case "credit_card" -> CardType.CREDIT;
            case "debit_card", "prepaid_card" -> CardType.DEBIT;
            default -> null; // dinero en cuenta, efectivo, etc.: no es una tarjeta
        };
        if (type == null || result.lastFourDigits() == null) {
            return null;
        }
        YearMonth expiration = result.expirationMonth() != null && result.expirationYear() != null
                ? YearMonth.of(result.expirationYear(), result.expirationMonth()) : null;
        return paymentMethodRepository
                .findFirstByTouristIdAndTypeAndLastDigitsAndExpirationDate(tourist.getId(), type, result.lastFourDigits(), expiration)
                .orElseGet(() -> paymentMethodRepository.save(PaymentMethod.builder()
                        .tourist(tourist)
                        .type(type)
                        .lastDigits(result.lastFourDigits())
                        .expirationDate(expiration)
                        .build()));
    }

    // Lo que se manda a la API de Mercado Pago para cobrar (POST /v1/payments).
    // El monto lo calcula el servidor con el precio de cuando se reservó: nunca se usa el que mande el navegador.
    private Map<String, Object> buildPayment(Booking booking, Tourist tourist, CardPaymentRequest card) {
        Activity activity = booking.getAvailability().getActivity();
        int people = booking.getParticipantCount();

        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("transaction_amount", expectedAmount(booking));
        payment.put("token", card.getToken());
        payment.put("description", activity.getName() + " - " + booking.getAvailability().getDate()
                + " " + booking.getAvailability().getTime() + " (" + people + (people == 1 ? " persona)" : " personas)"));
        int installments = card.getInstallments() == null ? 1 : Math.max(1, Math.min(12, card.getInstallments()));
        payment.put("installments", installments);
        payment.put("payment_method_id", card.getPaymentMethodId());
        if (!isBlank(card.getIssuerId())) {
            payment.put("issuer_id", card.getIssuerId());
        }

        Map<String, Object> payer = new LinkedHashMap<>();
        payer.put("email", tourist.getEmail());
        if (!isBlank(card.getIdentificationNumber())) {
            payer.put("identification", Map.of(
                    "type", isBlank(card.getIdentificationType()) ? "CI" : card.getIdentificationType(),
                    "number", card.getIdentificationNumber()));
        }
        payment.put("payer", payer);

        payment.put("external_reference", REFERENCE_PREFIX + booking.getId()); // para saber de qué reserva es
        payment.put("statement_descriptor", "MNATURAL");
        // Avisos automáticos (webhook): Mercado Pago no acepta localhost, solo con una dirección pública
        if (hasPublicUrl()) {
            payment.put("notification_url", publicUrl + "/webhooks/mercadopago");
        }
        return payment;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean hasPublicUrl() {
        return publicUrl.startsWith("https://") && !publicUrl.contains("localhost") && !publicUrl.contains("127.0.0.1");
    }

    private static BigDecimal expectedAmount(Booking booking) {
        Double total = booking.getTotalPrice();
        return total == null ? null : BigDecimal.valueOf(total);
    }

    // "booking-12" -> 12
    static Long bookingIdFrom(String externalReference) {
        if (externalReference == null || !externalReference.startsWith(REFERENCE_PREFIX)) {
            return null;
        }
        try {
            return Long.valueOf(externalReference.substring(REFERENCE_PREFIX.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
