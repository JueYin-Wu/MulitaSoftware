package uy.edu.um.porytic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.um.porytic2026.dto.CardPaymentRequest;
import uy.edu.um.porytic2026.entities.*;
import uy.edu.um.porytic2026.exceptions.BusinessException;
import uy.edu.um.porytic2026.payments.GatewayPayment;
import uy.edu.um.porytic2026.payments.GatewayRefund;
import uy.edu.um.porytic2026.payments.MercadoPagoClient;
import uy.edu.um.porytic2026.payments.PaymentGatewayUnavailableException;
import uy.edu.um.porytic2026.repository.BookingRepository;
import uy.edu.um.porytic2026.repository.PaymentMethodRepository;
import uy.edu.um.porytic2026.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

// Tests unitarios de PaymentService (pago de reservas con Mercado Pago).
// Mercado Pago se reemplaza por un mock: así se prueban todos los casos (aprobado, rechazado,
// pendiente, servicio caído) sin internet y sin cuenta. "Ahora" es el 09/11/2026 a las 12:00.
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 9, 12, 0);

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private MercadoPagoClient mercadoPago;
    @Mock
    private BookingService bookingService;

    private PaymentService paymentService;
    private Tourist tourist;
    private Booking booking;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        paymentService = new PaymentService(paymentRepository, paymentMethodRepository, bookingRepository,
                mercadoPago, bookingService, fixedClock, "http://localhost:8081");

        tourist = new Tourist();
        tourist.setId(5L);
        tourist.setEmail("turista@test.com");

        Activity activity = new Activity();
        activity.setId(1L);
        activity.setName("Kayak al atardecer");
        activity.setPrice(1000.0);
        activity.setBookingDeadline(60);

        Availability availability = Availability.builder()
                .id(50L)
                .activity(activity)
                .date(LocalDate.of(2026, 11, 20))
                .time(LocalTime.of(10, 0))
                .capacity(10)
                .build();

        // Reserva de 2 personas a $1000 cada una: total $2000, pendiente de pago
        booking = Booking.builder()
                .id(12L)
                .tourist(tourist)
                .availability(availability)
                .participantCount(2)
                .status(BookingStatus.PENDING)
                .bookingDate(NOW)
                .build();
    }

    // Un pago como lo devolvería Mercado Pago
    private GatewayPayment gatewayPayment(String status, String detail, String amount) {
        return new GatewayPayment("999", status, detail, new BigDecimal(amount), "booking-12",
                "account_money", null, null, null, null);
    }

    private Payment savedPayment() {
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        return captor.getValue();
    }

    // ===================== Resultado del pago =====================

    @Test
    void approvedPaymentConfirmsTheBooking() {
        when(bookingRepository.findById(12L)).thenReturn(Optional.of(booking));
        when(mercadoPago.searchPayments("booking-12")).thenReturn(List.of(gatewayPayment("approved", "accredited", "2000")));
        when(paymentRepository.findByBookingId(12L)).thenReturn(Optional.empty());

        Booking result = paymentService.refreshStatus(12L, tourist);

        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
        Payment payment = savedPayment();
        assertEquals(PaymentStatus.APPROVED, payment.getStatus());
        assertEquals("999", payment.getExternalReference());
        assertEquals(0, new BigDecimal("2000").compareTo(payment.getAmount()));
        verify(bookingRepository).save(booking);
    }

    @Test
    void rejectedPaymentKeepsTheBookingPending() {
        when(bookingRepository.findById(12L)).thenReturn(Optional.of(booking));
        when(mercadoPago.searchPayments("booking-12"))
                .thenReturn(List.of(gatewayPayment("rejected", "cc_rejected_insufficient_amount", "2000")));
        when(paymentRepository.findByBookingId(12L)).thenReturn(Optional.empty());

        Booking result = paymentService.refreshStatus(12L, tourist);

        assertEquals(BookingStatus.PENDING, result.getStatus());
        Payment payment = savedPayment();
        assertEquals(PaymentStatus.REJECTED, payment.getStatus());
        assertEquals("La tarjeta no tiene fondos suficientes", payment.getRejectionReason());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void paymentInProcessDoesNotConfirmTheBooking() {
        when(bookingRepository.findById(12L)).thenReturn(Optional.of(booking));
        when(mercadoPago.searchPayments("booking-12")).thenReturn(List.of(gatewayPayment("in_process", null, "2000")));
        when(paymentRepository.findByBookingId(12L)).thenReturn(Optional.empty());

        Booking result = paymentService.refreshStatus(12L, tourist);

        assertEquals(BookingStatus.PENDING, result.getStatus());
        assertEquals(PaymentStatus.PENDING, savedPayment().getStatus());
    }

    @Test
    void approvedPaymentForLessThanTheTotalDoesNotConfirm() {
        when(bookingRepository.findById(12L)).thenReturn(Optional.of(booking));
        when(mercadoPago.searchPayments("booking-12")).thenReturn(List.of(gatewayPayment("approved", "accredited", "1000")));
        when(paymentRepository.findByBookingId(12L)).thenReturn(Optional.empty());

        Booking result = paymentService.refreshStatus(12L, tourist);

        assertEquals(BookingStatus.PENDING, result.getStatus());
        assertNotEquals(PaymentStatus.APPROVED, savedPayment().getStatus());
    }

    @Test
    void withoutPaymentsInMercadoPagoNothingChanges() {
        when(bookingRepository.findById(12L)).thenReturn(Optional.of(booking));
        when(mercadoPago.searchPayments("booking-12")).thenReturn(List.of());

        Booking result = paymentService.refreshStatus(12L, tourist);

        assertEquals(BookingStatus.PENDING, result.getStatus());
        verify(paymentRepository, never()).save(any());
    }

    // ===================== Mercado Pago no disponible =====================

    @Test
    void whenMercadoPagoIsDownTheBookingIsNotConfirmed() {
        when(bookingRepository.findById(12L)).thenReturn(Optional.of(booking));
        when(mercadoPago.searchPayments("booking-12"))
                .thenThrow(new PaymentGatewayUnavailableException("No se pudo conectar con Mercado Pago", null));

        assertThrows(PaymentGatewayUnavailableException.class, () -> paymentService.refreshStatus(12L, tourist));

        assertEquals(BookingStatus.PENDING, booking.getStatus());
        verify(paymentRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
    }

    // ===================== Pagar con tarjeta en nuestra página =====================

    // Lo que manda el formulario de tarjeta: un token, nunca el número de la tarjeta
    private CardPaymentRequest card() {
        CardPaymentRequest card = new CardPaymentRequest();
        card.setToken("tok-123");
        card.setPaymentMethodId("master");
        card.setIssuerId("3");
        card.setInstallments(1);
        card.setIdentificationType("CI");
        card.setIdentificationNumber("12345672");
        return card;
    }

    private GatewayPayment cardResult(String status, String detail) {
        return new GatewayPayment("777", status, detail, new BigDecimal("2000"), "booking-12",
                "credit_card", "0604", 11, 2030, OffsetDateTime.of(NOW, ZoneOffset.UTC));
    }

    @Test
    void anApprovedCardPaymentConfirmsTheBooking() {
        when(bookingRepository.findById(12L)).thenReturn(Optional.of(booking));
        when(mercadoPago.searchPayments("booking-12")).thenReturn(List.of());
        when(mercadoPago.createPayment(anyMap())).thenReturn(cardResult("approved", "accredited"));
        when(paymentRepository.findByBookingId(12L)).thenReturn(Optional.empty());

        Payment payment = paymentService.payWithCard(12L, tourist, card());

        assertEquals(PaymentStatus.APPROVED, payment.getStatus());
        assertEquals("777", payment.getExternalReference());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    void theAmountIsCalculatedByTheServerNotTheBrowser() {
        when(bookingRepository.findById(12L)).thenReturn(Optional.of(booking));
        when(mercadoPago.searchPayments("booking-12")).thenReturn(List.of());
        when(mercadoPago.createPayment(anyMap())).thenReturn(cardResult("approved", "accredited"));
        when(paymentRepository.findByBookingId(12L)).thenReturn(Optional.empty());

        paymentService.payWithCard(12L, tourist, card());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mercadoPago).createPayment(captor.capture());
        Map<String, Object> sent = captor.getValue();
        assertEquals(0, new BigDecimal("2000").compareTo((BigDecimal) sent.get("transaction_amount")));
        assertEquals("tok-123", sent.get("token"));
        assertEquals("booking-12", sent.get("external_reference"));
        assertFalse(sent.containsKey("notification_url")); // con localhost no se piden avisos
    }

    @Test
    void aRejectedCardKeepsTheBookingPendingToRetry() {
        when(bookingRepository.findById(12L)).thenReturn(Optional.of(booking));
        when(mercadoPago.searchPayments("booking-12")).thenReturn(List.of());
        when(mercadoPago.createPayment(anyMap())).thenReturn(cardResult("rejected", "cc_rejected_insufficient_amount"));
        when(paymentRepository.findByBookingId(12L)).thenReturn(Optional.empty());

        Payment payment = paymentService.payWithCard(12L, tourist, card());

        assertEquals(PaymentStatus.REJECTED, payment.getStatus());
        assertEquals(BookingStatus.PENDING, booking.getStatus());
    }

    @Test
    void cardPaymentWhenMercadoPagoIsDownSavesNothing() {
        when(bookingRepository.findById(12L)).thenReturn(Optional.of(booking));
        when(mercadoPago.searchPayments("booking-12")).thenReturn(List.of());
        when(mercadoPago.createPayment(anyMap()))
                .thenThrow(new PaymentGatewayUnavailableException("Mercado Pago no está disponible (503)", null));

        assertThrows(PaymentGatewayUnavailableException.class, () -> paymentService.payWithCard(12L, tourist, card()));

        verify(paymentRepository, never()).save(any());
        assertEquals(BookingStatus.PENDING, booking.getStatus());
    }

    @Test
    void withoutCardTokenNothingIsCharged() {
        when(bookingRepository.findById(12L)).thenReturn(Optional.of(booking));
        when(mercadoPago.searchPayments("booking-12")).thenReturn(List.of());
        CardPaymentRequest empty = card();
        empty.setToken(" ");

        assertThrows(BusinessException.class, () -> paymentService.payWithCard(12L, tourist, empty));
        verify(mercadoPago, never()).createPayment(anyMap());
    }

    @Test
    void aConfirmedBookingCannotBePaidAgain() {
        booking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(12L)).thenReturn(Optional.of(booking));
        when(mercadoPago.searchPayments("booking-12")).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> paymentService.payWithCard(12L, tourist, card()));
        verify(mercadoPago, never()).createPayment(anyMap());
    }

    @Test
    void aTouristCannotPayAnotherTouristsBooking() {
        Tourist other = new Tourist();
        other.setId(6L);
        when(bookingRepository.findById(12L)).thenReturn(Optional.of(booking));

        assertThrows(BusinessException.class, () -> paymentService.payWithCard(12L, other, card()));
        verifyNoInteractions(mercadoPago);
    }

    // ===================== Cancelar con devolución =====================

    // Reserva confirmada con un pago aprobado de $2000; la actividad devuelve el 80 % hasta 24 h antes
    private Payment paidBooking() {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.getAvailability().getActivity().setRefundable(true);
        booking.getAvailability().getActivity().setRefundPercentage(80);
        booking.getAvailability().getActivity().setCancellationMinutes(24 * 60);
        Payment payment = Payment.builder().id(7L).booking(booking).status(PaymentStatus.APPROVED)
                .amount(new BigDecimal("2000")).externalReference("999").build();
        when(bookingRepository.findById(12L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(12L)).thenReturn(Optional.of(payment));
        when(mercadoPago.isConfigured()).thenReturn(true);
        when(mercadoPago.searchPayments("booking-12")).thenReturn(List.of(gatewayPayment("approved", "accredited", "2000")));
        return payment;
    }

    @Test
    void cancellingInTimeRefundsThePercentageOfTheActivity() {
        Payment payment = paidBooking();
        when(bookingService.isRefundableNow(booking)).thenReturn(true);
        when(mercadoPago.refund("999", new BigDecimal("1600.00")))
                .thenReturn(new GatewayRefund("r-1", "approved", new BigDecimal("1600.00")));
        when(bookingService.cancel(12L, tourist)).thenReturn(booking);

        PaymentService.CancellationResult result = paymentService.cancelBooking(12L, tourist);

        assertEquals(0, new BigDecimal("1600").compareTo(result.refundedAmount()));
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertEquals("r-1", payment.getRefundReference());
        assertEquals("Devuelto en parte", payment.getStatusLabel());
        verify(bookingService).cancel(12L, tourist);
    }

    @Test
    void cancellingAfterTheDeadlineDoesNotRefund() {
        Payment payment = paidBooking();
        when(bookingService.isRefundableNow(booking)).thenReturn(false);
        when(bookingService.cancel(12L, tourist)).thenReturn(booking);

        PaymentService.CancellationResult result = paymentService.cancelBooking(12L, tourist);

        assertNull(result.refundedAmount());
        assertTrue(result.wasPaid());
        assertEquals(PaymentStatus.APPROVED, payment.getStatus());
        verify(mercadoPago, never()).refund(any(), any());
    }

    @Test
    void ifTheRefundFailsTheBookingIsNotCancelled() {
        paidBooking();
        when(bookingService.isRefundableNow(booking)).thenReturn(true);
        when(mercadoPago.refund(any(), any()))
                .thenThrow(new PaymentGatewayUnavailableException("No se pudo conectar con Mercado Pago", null));

        assertThrows(PaymentGatewayUnavailableException.class, () -> paymentService.cancelBooking(12L, tourist));

        verify(bookingService, never()).cancel(any(), any());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    void cancellingAnUnpaidBookingDoesNotTouchMercadoPago() {
        when(bookingRepository.findById(12L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(12L)).thenReturn(Optional.empty());
        when(bookingService.cancel(12L, tourist)).thenReturn(booking);

        PaymentService.CancellationResult result = paymentService.cancelBooking(12L, tourist);

        assertNull(result.refundedAmount());
        assertFalse(result.wasPaid());
        verifyNoInteractions(mercadoPago);
    }

    @Test
    void theRefundIsThePercentageRoundedToCents() {
        Payment payment = Payment.builder().amount(new BigDecimal("1999")).build();
        assertEquals(new BigDecimal("1499.25"), PaymentService.refundAmount(payment, 75));
        assertEquals(new BigDecimal("1999.00"), PaymentService.refundAmount(payment, 100));
        assertEquals(BigDecimal.ZERO, PaymentService.refundAmount(payment, 0));
        assertEquals(BigDecimal.ZERO, PaymentService.refundAmount(payment, null));
    }

    // ===================== Conversiones =====================

    @Test
    void mercadoPagoStatusesAreTranslated() {
        assertEquals(PaymentStatus.APPROVED, PaymentService.toStatus("approved"));
        assertEquals(PaymentStatus.REJECTED, PaymentService.toStatus("rejected"));
        assertEquals(PaymentStatus.REJECTED, PaymentService.toStatus("cancelled"));
        assertEquals(PaymentStatus.REFUNDED, PaymentService.toStatus("refunded"));
        assertEquals(PaymentStatus.PENDING, PaymentService.toStatus("in_process"));
        assertEquals(PaymentStatus.PENDING, PaymentService.toStatus(null));
    }

    @Test
    void theBookingIsFoundFromTheExternalReference() {
        assertEquals(12L, PaymentService.bookingIdFrom("booking-12"));
        assertNull(PaymentService.bookingIdFrom("otra-cosa"));
        assertNull(PaymentService.bookingIdFrom(null));
    }
}
