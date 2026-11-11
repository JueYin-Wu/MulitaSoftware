package uy.edu.um.porytic2026.services;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.porytic2026.entities.Activity;
import uy.edu.um.porytic2026.entities.Availability;
import uy.edu.um.porytic2026.entities.Provider;
import uy.edu.um.porytic2026.exceptions.BusinessException;
import uy.edu.um.porytic2026.repository.ActivityRepository;
import uy.edu.um.porytic2026.repository.AvailabilityRepository;
import uy.edu.um.porytic2026.repository.BookingRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Fechas y cupos de las actividades del prestador (letra: "Una misma actividad podrá ofrecerse en diferentes
// fechas y horarios. El prestador será responsable de organizar estas disponibilidades y de establecer [...]
// la cantidad de personas que puede participar en cada instancia").
@Service
public class ProviderAvailabilityService {

    public static final int MAX_CAPACITY = 500;
    public static final int MAX_REPEAT_WEEKS = 26; // hasta medio año de una vez

    private static final DateTimeFormatter DATE_LABEL =
            DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy", Locale.forLanguageTag("es-UY"));

    private final AvailabilityRepository availabilityRepository;
    private final BookingRepository bookingRepository;
    private final ActivityRepository activityRepository;
    private final Clock clock;

    public ProviderAvailabilityService(AvailabilityRepository availabilityRepository, BookingRepository bookingRepository,
                                       ActivityRepository activityRepository, Clock clock) {
        this.availabilityRepository = availabilityRepository;
        this.bookingRepository = bookingRepository;
        this.activityRepository = activityRepository;
        this.clock = clock;
    }

    // Una fecha con los lugares ya ocupados (para la tabla del prestador)
    @Getter
    @AllArgsConstructor
    public static class DateRow {
        private final Availability availability;
        private final int booked;     // personas con reserva vigente (pagada o pendiente)
        private final boolean hasBookings;

        public String getDateLabel() {
            return availability.getDate().format(DATE_LABEL);
        }

        public int getRemaining() {
            return Math.max(0, (availability.getCapacity() == null ? 0 : availability.getCapacity()) - booked);
        }
    }

    // Próximas fechas de una actividad del prestador
    public List<DateRow> upcoming(Provider provider, Long activityId) {
        Activity activity = findOwnActivity(provider, activityId);
        List<DateRow> rows = new ArrayList<>();
        for (Availability a : availabilityRepository
                .findByActivityIdAndDateGreaterThanEqualOrderByDateAscTimeAsc(activity.getId(), LocalDate.now(clock))) {
            int booked = (int) bookingRepository.countParticipants(a.getId(), BookingService.ACTIVE_STATUSES);
            rows.add(new DateRow(a, booked, bookingRepository.existsByAvailabilityIdAndStatusIn(a.getId(), BookingService.ACTIVE_STATUSES)));
        }
        return rows;
    }

    public long countUpcoming(Long activityId) {
        return availabilityRepository.countByActivityIdAndDateGreaterThanEqual(activityId, LocalDate.now(clock));
    }

    // Agregar una fecha (y opcionalmente repetirla todas las semanas durante N semanas).
    // Devuelve cuántas fechas se crearon; las que ya existían se saltean.
    @Transactional
    public int add(Provider provider, Long activityId, String date, String time, Integer capacity, Integer repeatWeeks) {
        Activity activity = findOwnActivity(provider, activityId);

        LocalDate day = parseDate(date);
        LocalTime hour = parseTime(time);
        checkCapacity(capacity);
        int weeks = repeatWeeks == null ? 1 : repeatWeeks;
        if (weeks < 1 || weeks > MAX_REPEAT_WEEKS) {
            throw new BusinessException("repeatWeeks", "Se puede repetir entre 1 y " + MAX_REPEAT_WEEKS + " semanas");
        }
        if (!LocalDateTime.of(day, hour).isAfter(LocalDateTime.now(clock))) {
            throw new BusinessException("date", "La fecha y hora tienen que ser futuras");
        }

        int created = 0;
        for (int week = 0; week < weeks; week++) {
            LocalDate current = day.plusWeeks(week);
            if (availabilityRepository.existsByActivityIdAndDateAndTime(activity.getId(), current, hour)) {
                continue; // ya estaba
            }
            availabilityRepository.save(Availability.builder()
                    .activity(activity).date(current).time(hour).capacity(capacity).build());
            created++;
        }
        if (created == 0) {
            throw new BusinessException("date", weeks == 1 ? "Esa fecha y hora ya está cargada" : "Todas esas fechas ya estaban cargadas");
        }
        return created;
    }

    // Cambiar el cupo de una fecha: no puede quedar por debajo de las personas que ya reservaron
    @Transactional
    public Availability updateCapacity(Provider provider, Long availabilityId, Integer capacity) {
        Availability availability = findOwnAvailability(provider, availabilityId);
        checkCapacity(capacity);
        int booked = (int) bookingRepository.countParticipants(availabilityId, BookingService.ACTIVE_STATUSES);
        if (capacity < booked) {
            throw new BusinessException("capacity", "Ya hay " + booked + " personas con reserva para esa fecha: el cupo no puede ser menor");
        }
        availability.setCapacity(capacity);
        return availabilityRepository.save(availability);
    }

    // Eliminar una fecha: solo si nadie la reservó (si hay reservas, el prestador tiene que cumplirlas)
    @Transactional
    public Availability delete(Provider provider, Long availabilityId) {
        Availability availability = findOwnAvailability(provider, availabilityId);
        if (bookingRepository.existsByAvailabilityIdAndStatusIn(availabilityId, BookingService.ACTIVE_STATUSES)) {
            throw new BusinessException("Esa fecha tiene reservas, así que no se puede eliminar");
        }
        if (availability.getDate().isBefore(LocalDate.now(clock))) {
            throw new BusinessException("No se pueden eliminar fechas que ya pasaron");
        }
        availabilityRepository.delete(availability);
        return availability;
    }

    // ===================== Auxiliares =====================

    private Activity findOwnActivity(Provider provider, Long activityId) {
        return activityRepository.findById(activityId)
                .filter(a -> a.getProvider() != null && a.getProvider().getId().equals(provider.getId()))
                .orElseThrow(() -> new BusinessException("La actividad no existe o no es tuya"));
    }

    private Availability findOwnAvailability(Provider provider, Long availabilityId) {
        return availabilityRepository.findById(availabilityId)
                .filter(a -> a.getActivity().getProvider().getId().equals(provider.getId()))
                .orElseThrow(() -> new BusinessException("La fecha no existe o no es tuya"));
    }

    private static void checkCapacity(Integer capacity) {
        if (capacity == null || capacity < 1 || capacity > MAX_CAPACITY) {
            throw new BusinessException("capacity", "El cupo tiene que ser entre 1 y " + MAX_CAPACITY + " personas");
        }
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value == null ? "" : value.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException("date", "Elegí una fecha");
        }
    }

    private static LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value == null ? "" : value.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException("time", "Elegí un horario");
        }
    }
}
