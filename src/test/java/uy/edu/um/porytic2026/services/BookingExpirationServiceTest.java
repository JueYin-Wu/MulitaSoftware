package uy.edu.um.porytic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.um.porytic2026.entities.*;
import uy.edu.um.porytic2026.repository.AvailabilityRepository;
import uy.edu.um.porytic2026.repository.BookingRepository;
import uy.edu.um.porytic2026.repository.PaymentRepository;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Tests unitarios del vencimiento automático de reservas sin pagar.
// Se usa un reloj fijo: "ahora" es el 16/11/2026 a las 12:00; la actividad es el 20/11 a las 10:00.
@ExtendWith(MockitoExtension.class)
class BookingExpirationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 16, 12, 0);

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private AvailabilityRepository availabilityRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentService paymentService;

    private BookingExpirationService expirationService;
    private Booking booking;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        // BookingService real: así se prueba también su regla de "plazo vencido"
        BookingService bookingService = new BookingService(bookingRepository, availabilityRepository, fixedClock);
        expirationService = new BookingExpirationService(bookingRepository, paymentRepository,
                bookingService, paymentService, fixedClock);

        Activity activity = new Activity();
        activity.setId(1L);
        activity.setPrice(1000.0);
        activity.setBookingDeadline(60);
        Availability availability = Availability.builder().id(50L).activity(activity)
                .date(LocalDate.of(2026, 11, 20)).time(LocalTime.of(10, 0)).capacity(10).build();
        Tourist tourist = new Tourist();
        tourist.setId(5L);

        // Reserva de hace 40 minutos, con plazo para pagar que venció hace 10
        booking = Booking.builder().id(7L).tourist(tourist).availability(availability)
                .participantCount(3).status(BookingStatus.PENDING)
                .bookingDate(NOW.minusMinutes(40)).paymentDeadline(NOW.minusMinutes(10)).build();
    }

    @Test
    void anUnpaidBookingPastItsDeadlineExpiresAndFreesItsSpots() {
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(7L)).thenReturn(Optional.empty());

        assertTrue(expirationService.expireIfUnpaid(7L));

        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        // EXPIRED no está en BookingService.ACTIVE_STATUSES: ya no cuenta para el cupo
        assertFalse(BookingService.ACTIVE_STATUSES.contains(BookingStatus.EXPIRED));
        assertFalse(booking.isActive());
        verify(bookingRepository).save(booking);
        verify(paymentService, never()).syncBeforeExpiring(any());
    }

    @Test
    void aBookingStillWithinItsDeadlineDoesNotExpire() {
        booking.setPaymentDeadline(NOW.plusMinutes(5));
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));

        assertFalse(expirationService.expireIfUnpaid(7L));
        assertEquals(BookingStatus.PENDING, booking.getStatus());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void aBookingThatTurnsOutToBePaidIsConfirmedInsteadOfExpired() {
        Payment payment = Payment.builder().id(9L).booking(booking).status(PaymentStatus.PENDING).build();
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(7L)).thenReturn(Optional.of(payment));
        // Mercado Pago responde que el pago estaba aprobado: PaymentService confirma la reserva
        when(paymentService.syncBeforeExpiring(booking)).thenAnswer(invocation -> {
            booking.setStatus(BookingStatus.CONFIRMED);
            return true;
        });

        assertFalse(expirationService.expireIfUnpaid(7L));
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void aPaymentStillInProcessIsWaitedFor() {
        Payment payment = Payment.builder().id(9L).booking(booking).status(PaymentStatus.PENDING).build();
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(7L)).thenReturn(Optional.of(payment));
        when(paymentService.syncBeforeExpiring(booking)).thenReturn(true);

        assertFalse(expirationService.expireIfUnpaid(7L));
        assertEquals(BookingStatus.PENDING, booking.getStatus());
    }

    @Test
    void aRejectedPaymentDoesNotStopTheExpiration() {
        Payment payment = Payment.builder().id(9L).booking(booking).status(PaymentStatus.REJECTED).build();
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(7L)).thenReturn(Optional.of(payment));
        when(paymentService.syncBeforeExpiring(booking)).thenReturn(true);

        assertTrue(expirationService.expireIfUnpaid(7L));
        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
    }

    @Test
    void ifMercadoPagoDoesNotAnswerItTriesAgainLater() {
        Payment payment = Payment.builder().id(9L).booking(booking).status(PaymentStatus.REJECTED).build();
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(7L)).thenReturn(Optional.of(payment));
        when(paymentService.syncBeforeExpiring(booking)).thenReturn(false);

        assertFalse(expirationService.expireIfUnpaid(7L));
        assertEquals(BookingStatus.PENDING, booking.getStatus());
    }

    @Test
    void aCancelledBookingIsLeftAsItIs() {
        booking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));

        assertFalse(expirationService.expireIfUnpaid(7L));
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    void findOverdueIdsReturnsOnlyPendingBookingsPastTheirDeadline() {
        Booking onTime = Booking.builder().id(8L).availability(booking.getAvailability()).participantCount(1)
                .status(BookingStatus.PENDING).bookingDate(NOW.minusMinutes(5)).paymentDeadline(NOW.plusMinutes(25)).build();
        when(bookingRepository.findByStatus(BookingStatus.PENDING)).thenReturn(List.of(booking, onTime));

        assertEquals(List.of(7L), expirationService.findOverdueIds());
    }
}
