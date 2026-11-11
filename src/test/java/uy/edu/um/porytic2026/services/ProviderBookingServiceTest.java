package uy.edu.um.porytic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.um.porytic2026.entities.*;
import uy.edu.um.porytic2026.repository.AvailabilityRepository;
import uy.edu.um.porytic2026.repository.BookingRepository;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Tests unitarios de ProviderBookingService (reservas del prestador). "Hoy" es el 09/11/2026.
@ExtendWith(MockitoExtension.class)
class ProviderBookingServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);

    @Mock
    private AvailabilityRepository availabilityRepository;
    @Mock
    private BookingRepository bookingRepository;

    private ProviderBookingService service;
    private Provider provider;
    private Availability saturday;
    private Availability sunday;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(TODAY.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new ProviderBookingService(availabilityRepository, bookingRepository, fixedClock);

        provider = new FisicProvider();
        provider.setId(2L);
        Activity kayak = new Activity();
        kayak.setId(1L);
        kayak.setName("Kayak");

        saturday = Availability.builder().id(10L).activity(kayak).date(TODAY.plusDays(5)).time(LocalTime.of(10, 0)).capacity(10).build();
        sunday = Availability.builder().id(11L).activity(kayak).date(TODAY.plusDays(6)).time(LocalTime.of(10, 0)).capacity(10).build();
        when(availabilityRepository.findByActivityProviderIdAndDateGreaterThanEqualOrderByDateAscTimeAsc(2L, TODAY))
                .thenReturn(List.of(saturday, sunday));
    }

    private Booking booking(long id, Availability date, int people, BookingStatus status) {
        return Booking.builder().id(id).availability(date).participantCount(people).status(status)
                .bookingDate(TODAY.atTime(9, (int) id)).build();
    }

    @Test
    void bookingsAreGroupedByDateWithTheirTotals() {
        when(bookingRepository.findByAvailabilityActivityProviderId(2L)).thenReturn(List.of(
                booking(1, saturday, 3, BookingStatus.CONFIRMED),
                booking(2, saturday, 2, BookingStatus.PENDING),
                booking(3, saturday, 4, BookingStatus.CANCELLED)));

        var schedule = service.schedule(provider, false, null);

        assertEquals(2, schedule.size());                    // también aparece el domingo, sin reservas
        var sat = schedule.get(0);
        assertEquals(3, sat.getConfirmedPeople());
        assertEquals(2, sat.getPendingPeople());
        assertEquals(5, sat.getRemaining());                    // 10 - 3 confirmadas - 2 pendientes (la cancelada no ocupa)
        assertTrue(schedule.get(1).isEmpty());
    }

    @Test
    void confirmedBookingsComeFirstAndCancelledLast() {
        when(bookingRepository.findByAvailabilityActivityProviderId(2L)).thenReturn(List.of(
                booking(1, saturday, 1, BookingStatus.CANCELLED),
                booking(2, saturday, 1, BookingStatus.PENDING),
                booking(3, saturday, 1, BookingStatus.CONFIRMED)));

        var bookings = service.schedule(provider, false, null).get(0).getBookings();

        assertEquals(BookingStatus.CONFIRMED, bookings.get(0).getStatus());
        assertEquals(BookingStatus.PENDING, bookings.get(1).getStatus());
        assertEquals(BookingStatus.CANCELLED, bookings.get(2).getStatus());
    }

    @Test
    void theSummaryCountsConfirmedPeopleAndUnpaidBookings() {
        when(bookingRepository.findByAvailabilityActivityProviderId(2L)).thenReturn(List.of(
                booking(1, saturday, 3, BookingStatus.CONFIRMED),
                booking(2, sunday, 2, BookingStatus.CONFIRMED),
                booking(3, sunday, 1, BookingStatus.PENDING)));

        var summary = service.summary(service.schedule(provider, false, null));

        assertEquals(2, summary.getDates());
        assertEquals(5, summary.getConfirmedPeople());
        assertEquals(1, summary.getPendingBookings());
    }

    @Test
    void canBeFilteredByActivity() {
        when(bookingRepository.findByAvailabilityActivityProviderId(2L)).thenReturn(List.of());

        assertEquals(2, service.schedule(provider, false, 1L).size());
        assertTrue(service.schedule(provider, false, 99L).isEmpty());
    }
}
