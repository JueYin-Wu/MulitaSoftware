package uy.edu.um.porytic2026.services;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;
import uy.edu.um.porytic2026.entities.Availability;
import uy.edu.um.porytic2026.entities.Booking;
import uy.edu.um.porytic2026.entities.BookingStatus;
import uy.edu.um.porytic2026.entities.Provider;
import uy.edu.um.porytic2026.repository.AvailabilityRepository;
import uy.edu.um.porytic2026.repository.BookingRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

// Reservas de las actividades de un prestador, agrupadas por fecha y horario
// (letra: "el prestador deberá poder consultar las reservas correspondientes a sus actividades y disponer
// de la información necesaria para organizar la prestación del servicio").
@Service
public class ProviderBookingService {

    private final AvailabilityRepository availabilityRepository;
    private final BookingRepository bookingRepository;
    private final Clock clock;

    public ProviderBookingService(AvailabilityRepository availabilityRepository, BookingRepository bookingRepository,
                                  Clock clock) {
        this.availabilityRepository = availabilityRepository;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    private static final DateTimeFormatter DATE_LABEL =
            DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy", Locale.forLanguageTag("es-UY"));

    // Una fecha y horario de una actividad con sus reservas y los totales para organizarse
    @Getter
    @AllArgsConstructor
    public static class DateSchedule {
        private final Availability availability;
        private final List<Booking> bookings;
        private final int confirmedPeople;
        private final int pendingPeople;
        private final int remaining;

        public boolean isEmpty() {
            return bookings.isEmpty();
        }

        // "sábado 21/11/2026"
        public String getDateLabel() {
            return availability.getDate().format(DATE_LABEL);
        }
    }

    // Totales para el resumen de arriba de la página
    @Getter
    @AllArgsConstructor
    public static class Summary {
        private final int dates;
        private final int confirmedPeople;
        private final int pendingBookings;
    }

    // Agenda del prestador: sus fechas (próximas o pasadas), opcionalmente de una sola actividad
    public List<DateSchedule> schedule(Provider provider, boolean past, Long activityId) {
        LocalDate today = LocalDate.now(clock);
        List<Availability> dates = past
                ? availabilityRepository.findByActivityProviderIdAndDateLessThanOrderByDateDescTimeDesc(provider.getId(), today)
                : availabilityRepository.findByActivityProviderIdAndDateGreaterThanEqualOrderByDateAscTimeAsc(provider.getId(), today);

        // Todas las reservas del prestador, agrupadas por fecha (una sola consulta a la base)
        Map<Long, List<Booking>> byDate = bookingRepository.findByAvailabilityActivityProviderId(provider.getId()).stream()
                .collect(Collectors.groupingBy(b -> b.getAvailability().getId()));

        List<DateSchedule> result = new ArrayList<>();
        for (Availability availability : dates) {
            if (activityId != null && !activityId.equals(availability.getActivity().getId())) {
                continue;
            }
            List<Booking> bookings = new ArrayList<>(byDate.getOrDefault(availability.getId(), List.of()));
            // Primero las confirmadas, después las pendientes de pago, al final canceladas y vencidas
            bookings.sort(Comparator.comparingInt((Booking b) -> order(b.getStatus()))
                    .thenComparing(Booking::getBookingDate, Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())));

            int confirmed = people(bookings, BookingStatus.CONFIRMED);
            int pending = people(bookings, BookingStatus.PENDING);
            int capacity = availability.getCapacity() == null ? 0 : availability.getCapacity();
            result.add(new DateSchedule(availability, bookings, confirmed, pending, Math.max(0, capacity - confirmed - pending)));
        }
        return result;
    }

    public Summary summary(List<DateSchedule> schedule) {
        int confirmed = schedule.stream().mapToInt(DateSchedule::getConfirmedPeople).sum();
        int pendingBookings = (int) schedule.stream()
                .flatMap(d -> d.getBookings().stream())
                .filter(b -> b.getStatus() == BookingStatus.PENDING)
                .count();
        return new Summary(schedule.size(), confirmed, pendingBookings);
    }

    private static int people(List<Booking> bookings, BookingStatus status) {
        return bookings.stream()
                .filter(b -> b.getStatus() == status)
                .mapToInt(b -> b.getParticipantCount() == null ? 0 : b.getParticipantCount())
                .sum();
    }

    private static int order(BookingStatus status) {
        if (status == null) {
            return 9;
        }
        return switch (status) {
            case CONFIRMED -> 0;
            case PENDING -> 1;
            case CANCELLED -> 2;
            case EXPIRED -> 3;
        };
    }
}
