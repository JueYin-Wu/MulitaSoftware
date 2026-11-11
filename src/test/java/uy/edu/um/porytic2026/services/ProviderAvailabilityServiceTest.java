package uy.edu.um.porytic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.um.porytic2026.entities.*;
import uy.edu.um.porytic2026.exceptions.BusinessException;
import uy.edu.um.porytic2026.repository.ActivityRepository;
import uy.edu.um.porytic2026.repository.AvailabilityRepository;
import uy.edu.um.porytic2026.repository.BookingRepository;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Tests unitarios de ProviderAvailabilityService (fechas y cupos). "Ahora" es el 09/11/2026 a las 12:00.
@ExtendWith(MockitoExtension.class)
class ProviderAvailabilityServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);

    @Mock
    private AvailabilityRepository availabilityRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ActivityRepository activityRepository;

    private ProviderAvailabilityService service;
    private Provider provider;
    private Activity kayak;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(TODAY.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new ProviderAvailabilityService(availabilityRepository, bookingRepository, activityRepository, fixedClock);
        provider = new FisicProvider();
        provider.setId(2L);
        kayak = new Activity();
        kayak.setId(1L);
        kayak.setProvider(provider);
    }

    private void activityFound() {
        when(activityRepository.findById(1L)).thenReturn(Optional.of(kayak));
    }

    @Test
    void addsOneDate() {
        activityFound();
        when(availabilityRepository.existsByActivityIdAndDateAndTime(eq(1L), any(), any())).thenReturn(false);

        int created = service.add(provider, 1L, "2026-11-21", "10:00", 12, 1);

        assertEquals(1, created);
        ArgumentCaptor<Availability> captor = ArgumentCaptor.forClass(Availability.class);
        verify(availabilityRepository).save(captor.capture());
        assertEquals(LocalDate.of(2026, 11, 21), captor.getValue().getDate());
        assertEquals(LocalTime.of(10, 0), captor.getValue().getTime());
        assertEquals(12, captor.getValue().getCapacity());
    }

    @Test
    void repeatsEveryWeekAndSkipsTheDatesThatAlreadyExist() {
        activityFound();
        when(availabilityRepository.existsByActivityIdAndDateAndTime(eq(1L), any(), any())).thenReturn(false);
        // la segunda semana ya estaba cargada
        when(availabilityRepository.existsByActivityIdAndDateAndTime(1L, LocalDate.of(2026, 11, 28), LocalTime.of(10, 0)))
                .thenReturn(true);

        int created = service.add(provider, 1L, "2026-11-21", "10:00", 12, 4);

        assertEquals(3, created); // 21/11, 05/12 y 12/12
        verify(availabilityRepository, times(3)).save(any(Availability.class));
    }

    @Test
    void aDateInThePastIsRejected() {
        activityFound();
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.add(provider, 1L, "2026-11-09", "08:00", 10, 1)); // hoy a las 8, ya pasó
        assertEquals("date", e.getField());
        verify(availabilityRepository, never()).save(any());
    }

    @Test
    void theCapacityMustBeValid() {
        activityFound();
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.add(provider, 1L, "2026-11-21", "10:00", 0, 1));
        assertEquals("capacity", e.getField());
    }

    @Test
    void anotherProvidersActivityCannotBeChanged() {
        Provider other = new FisicProvider();
        other.setId(3L);
        activityFound();
        assertThrows(BusinessException.class, () -> service.add(other, 1L, "2026-11-21", "10:00", 10, 1));
    }

    @Test
    void theCapacityCannotBeLowerThanThePeopleWhoAlreadyBooked() {
        Availability date = Availability.builder().id(10L).activity(kayak).date(TODAY.plusDays(5)).time(LocalTime.NOON).capacity(10).build();
        when(availabilityRepository.findById(10L)).thenReturn(Optional.of(date));
        when(bookingRepository.countParticipants(eq(10L), anyCollection())).thenReturn(6L);

        assertThrows(BusinessException.class, () -> service.updateCapacity(provider, 10L, 5));
        assertEquals(10, date.getCapacity());

        when(availabilityRepository.save(date)).thenReturn(date);
        service.updateCapacity(provider, 10L, 6);
        assertEquals(6, date.getCapacity());
    }

    @Test
    void aDateWithBookingsCannotBeDeleted() {
        Availability date = Availability.builder().id(10L).activity(kayak).date(TODAY.plusDays(5)).time(LocalTime.NOON).capacity(10).build();
        when(availabilityRepository.findById(10L)).thenReturn(Optional.of(date));
        when(bookingRepository.existsByAvailabilityIdAndStatusIn(eq(10L), anyCollection())).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.delete(provider, 10L));
        verify(availabilityRepository, never()).delete(any());
    }

    @Test
    void aDateWithoutBookingsCanBeDeleted() {
        Availability date = Availability.builder().id(10L).activity(kayak).date(TODAY.plusDays(5)).time(LocalTime.NOON).capacity(10).build();
        when(availabilityRepository.findById(10L)).thenReturn(Optional.of(date));
        when(bookingRepository.existsByAvailabilityIdAndStatusIn(eq(10L), anyCollection())).thenReturn(false);

        service.delete(provider, 10L);

        verify(availabilityRepository).delete(date);
    }
}
