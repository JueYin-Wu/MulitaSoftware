package uy.edu.um.porytic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.um.porytic2026.dto.ParticipantForm;
import uy.edu.um.porytic2026.entities.*;
import uy.edu.um.porytic2026.exceptions.BusinessException;
import uy.edu.um.porytic2026.repository.AvailabilityRepository;
import uy.edu.um.porytic2026.repository.BookingRepository;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Tests unitarios de BookingService (reservas de los turistas).
// Se usa un reloj fijo: "ahora" es el sábado 24/10/2026 a las 12:00.
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 24, 12, 0);

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AvailabilityRepository availabilityRepository;

    private BookingService bookingService;

    private Tourist tourist;
    private Activity activity;
    private Availability availability;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        bookingService = new BookingService(bookingRepository, availabilityRepository, fixedClock);

        tourist = new Tourist();
        tourist.setId(5L);

        activity = new Activity();
        activity.setId(1L);
        activity.setName("Kayak al atardecer");
        activity.setPrice(1000.0);
        activity.setBookingDeadline(60); // se puede reservar hasta 1 hora antes

        // Fecha dentro de dos semanas, con 10 lugares
        availability = Availability.builder()
                .id(50L)
                .activity(activity)
                .date(LocalDate.of(2026, 11, 7))
                .time(LocalTime.of(10, 0))
                .capacity(10)
                .build();
    }

    // ===================== Crear reserva =====================

    @Test
    void createSavesPendingBookingWithTheParticipants() {
        when(availabilityRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(availability));
        when(bookingRepository.countParticipants(eq(50L), anyCollection())).thenReturn(3L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.create(tourist, 50L, 2,
                List.of(new ParticipantForm("Ana", "Pérez")));

        assertEquals(BookingStatus.PENDING, booking.getStatus());
        assertEquals(2, booking.getParticipantCount());
        assertSame(tourist, booking.getTourist());
        assertSame(availability, booking.getAvailability());
        assertEquals(NOW, booking.getBookingDate());
        assertEquals(2000.0, booking.getTotalPrice());

        // Se guarda el acompañante
        assertEquals(1, booking.getParticipants().size());
        Participant companion = booking.getParticipants().get(0);
        assertEquals("Ana", companion.getFirstName());
        assertEquals("Pérez", companion.getLastName());
        assertSame(booking, companion.getBooking());
    }

    @Test
    void createForTwoWithoutCompanionDataIsRejected() {
        when(availabilityRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(availability));
        when(bookingRepository.countParticipants(eq(50L), anyCollection())).thenReturn(0L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> bookingService.create(tourist, 50L, 2, List.of()));
        assertEquals("companions", e.getField());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void companionWithInvalidDataGetsAnErrorPerField() {
        var errors = bookingService.validateCompanions(tourist, 2,
                List.of(new ParticipantForm("Ana3", "")));

        assertTrue(errors.containsKey("companions[0].firstName"));
        assertTrue(errors.containsKey("companions[0].lastName"));
    }

    @Test
    void createWithMoreParticipantsThanRemainingIsRejected() {
        when(availabilityRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(availability));
        when(bookingRepository.countParticipants(eq(50L), anyCollection())).thenReturn(8L); // quedan 2

        BusinessException e = assertThrows(BusinessException.class,
                () -> bookingService.create(tourist, 50L, 3));

        assertEquals("participantCount", e.getField());
        assertTrue(e.getMessage().contains("2 lugares"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createWithZeroParticipantsIsRejected() {
        when(availabilityRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(availability));

        BusinessException e = assertThrows(BusinessException.class,
                () -> bookingService.create(tourist, 50L, 0));

        assertEquals("participantCount", e.getField());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createWhenSoldOutIsRejected() {
        when(availabilityRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(availability));
        when(bookingRepository.countParticipants(eq(50L), anyCollection())).thenReturn(10L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> bookingService.create(tourist, 50L, 1));

        assertNull(e.getField());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createAfterTheBookingDeadlineIsRejected() {
        // Empieza hoy a las 12:30 y se reserva hasta 1 hora antes (11:30): ya cerró
        availability.setDate(NOW.toLocalDate());
        availability.setTime(LocalTime.of(12, 30));
        when(availabilityRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(availability));

        assertThrows(BusinessException.class, () -> bookingService.create(tourist, 50L, 1));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createWithZeroDeadlineIsAllowedUntilTheStartTime() {
        // Con 0 minutos el límite es la hora de inicio: a las 12:00 todavía se puede reservar para las 12:30
        activity.setBookingDeadline(0);
        availability.setDate(NOW.toLocalDate());
        availability.setTime(LocalTime.of(12, 30));
        when(availabilityRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(availability));
        when(bookingRepository.countParticipants(eq(50L), anyCollection())).thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.create(tourist, 50L, 1);

        assertEquals(BookingStatus.PENDING, booking.getStatus());
    }

    @Test
    void createForADateThatAlreadyPassedIsRejected() {
        availability.setDate(LocalDate.of(2026, 10, 20));
        when(availabilityRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(availability));

        assertThrows(BusinessException.class, () -> bookingService.create(tourist, 50L, 1));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createTwiceForTheSameDateIsRejected() {
        when(availabilityRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(availability));
        when(bookingRepository.countParticipants(eq(50L), anyCollection())).thenReturn(0L);
        when(bookingRepository.existsByTouristIdAndAvailabilityIdAndStatusIn(eq(5L), eq(50L), anyCollection()))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> bookingService.create(tourist, 50L, 1));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createForUnknownDateIsRejected() {
        when(availabilityRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> bookingService.create(tourist, 99L, 1));
    }

    @Test
    void remainingSpotsIsNeverNegative() {
        when(bookingRepository.countParticipants(eq(50L), anyCollection())).thenReturn(12L);

        assertEquals(0, bookingService.remainingSpots(availability));
    }

    // ===================== Cancelar =====================

    @Test
    void cancelChangesTheStatusToCancelled() {
        Booking booking = bookingOf(tourist, BookingStatus.PENDING);
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking cancelled = bookingService.cancel(7L, tourist);

        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        assertFalse(cancelled.isActive());
    }

    @Test
    void cancelABookingOfAnotherTouristIsRejected() {
        Tourist other = new Tourist();
        other.setId(99L);
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(bookingOf(other, BookingStatus.PENDING)));

        assertThrows(BusinessException.class, () -> bookingService.cancel(7L, tourist));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelAnAlreadyCancelledBookingIsRejected() {
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(bookingOf(tourist, BookingStatus.CANCELLED)));

        assertThrows(BusinessException.class, () -> bookingService.cancel(7L, tourist));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelAfterTheActivityStartedIsRejected() {
        availability.setDate(LocalDate.of(2026, 10, 20));
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(bookingOf(tourist, BookingStatus.CONFIRMED)));

        assertThrows(BusinessException.class, () -> bookingService.cancel(7L, tourist));
        verify(bookingRepository, never()).save(any());
    }

    // ===================== Reembolso =====================

    @Test
    void refundDependsOnTheCancellationPolicy() {
        activity.setRefundable(true);
        activity.setRefundPercentage(80);
        Booking booking = bookingOf(tourist, BookingStatus.CONFIRMED);

        // Faltan casi 14 días: con un plazo de 24 h todavía corresponde reembolso
        activity.setCancellationMinutes(1440);
        assertTrue(bookingService.isRefundableNow(booking));

        // Con un plazo de 30 días ya no
        activity.setCancellationMinutes(30 * 1440);
        assertFalse(bookingService.isRefundableNow(booking));

        // Si la actividad no es reembolsable, nunca
        activity.setRefundable(false);
        activity.setCancellationMinutes(1440);
        assertFalse(bookingService.isRefundableNow(booking));
    }

    // ===================== Métodos auxiliares =====================

    private Booking bookingOf(Tourist owner, BookingStatus status) {
        return Booking.builder()
                .id(7L)
                .tourist(owner)
                .availability(availability)
                .participantCount(2)
                .status(status)
                .bookingDate(NOW.minusDays(1))
                .build();
    }
}
