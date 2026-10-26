package uy.edu.um.porytic2026.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.porytic2026.dto.ParticipantForm;
import uy.edu.um.porytic2026.entities.Availability;
import uy.edu.um.porytic2026.entities.Booking;
import uy.edu.um.porytic2026.entities.BookingStatus;
import uy.edu.um.porytic2026.entities.Participant;
import uy.edu.um.porytic2026.entities.Tourist;
import uy.edu.um.porytic2026.exceptions.BusinessException;
import uy.edu.um.porytic2026.repository.AvailabilityRepository;
import uy.edu.um.porytic2026.repository.BookingRepository;
import uy.edu.um.porytic2026.validation.DocumentValidator;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Reservas de los turistas: crear una reserva para una fecha de una actividad, consultarlas y cancelarlas.
@Service
public class BookingService {

    // Estados que ocupan lugar en el cupo. Una reserva cancelada o vencida libera sus lugares.
    public static final List<BookingStatus> ACTIVE_STATUSES = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final BookingRepository bookingRepository;
    private final AvailabilityRepository availabilityRepository;
    private final Clock clock;

    public BookingService(BookingRepository bookingRepository, AvailabilityRepository availabilityRepository,
                          Clock clock) {
        this.bookingRepository = bookingRepository;
        this.availabilityRepository = availabilityRepository;
        this.clock = clock;
    }

    // Lugares que quedan libres en una fecha: cupo menos los participantes de las reservas vigentes
    public int remainingSpots(Availability availability) {
        long taken = bookingRepository.countParticipants(availability.getId(), ACTIVE_STATUSES);
        int capacity = availability.getCapacity() == null ? 0 : availability.getCapacity();
        return (int) Math.max(0, capacity - taken);
    }

    // Busca una fecha y controla que todavía se pueda reservar (se usa para mostrar el formulario)
    public Availability findBookable(Long availabilityId) {
        Availability availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new BusinessException("La fecha elegida no existe"));
        checkBookingOpen(availability);
        if (remainingSpots(availability) == 0) {
            throw new BusinessException("Ya no quedan lugares para esa fecha");
        }
        return availability;
    }

    // Actividad a la que pertenece una fecha (para volver a su página si algo sale mal), o null si no existe
    public Long findActivityId(Long availabilityId) {
        return availabilityRepository.findById(availabilityId)
                .map(availability -> availability.getActivity().getId())
                .orElse(null);
    }

    // Reserva para una sola persona (el propio turista), sin acompañantes
    @Transactional
    public Booking create(Tourist tourist, Long availabilityId, Integer participantCount) {
        return create(tourist, availabilityId, participantCount, List.of());
    }

    // Crea la reserva. Queda PENDING (pendiente de pago) y ya ocupa los lugares en el cupo.
    // Si van más personas, companions trae los datos de cada acompañante (participantCount - 1).
    @Transactional
    public Booking create(Tourist tourist, Long availabilityId, Integer participantCount,
                          List<ParticipantForm> companions) {
        // Se bloquea la fecha mientras se reserva, para que dos reservas al mismo tiempo no superen el cupo
        Availability availability = availabilityRepository.findByIdForUpdate(availabilityId)
                .orElseThrow(() -> new BusinessException("La fecha elegida no existe"));

        checkBookingOpen(availability);

        if (participantCount == null || participantCount < 1) {
            throw new BusinessException("participantCount", "Tiene que haber al menos 1 participante");
        }
        int remaining = remainingSpots(availability);
        if (remaining == 0) {
            throw new BusinessException("Ya no quedan lugares para esa fecha");
        }
        if (participantCount > remaining) {
            throw new BusinessException("participantCount", remaining == 1
                    ? "Solo queda 1 lugar para esa fecha"
                    : "Solo quedan " + remaining + " lugares para esa fecha");
        }
        if (bookingRepository.existsByTouristIdAndAvailabilityIdAndStatusIn(
                tourist.getId(), availability.getId(), ACTIVE_STATUSES)) {
            throw new BusinessException("Ya tenés una reserva para esa fecha. La podés ver en \"Mis reservas\".");
        }

        // Los datos de los acompañantes son obligatorios y tienen que ser válidos
        if (!validateCompanions(tourist, participantCount, companions).isEmpty()) {
            throw new BusinessException("companions", "Revisá los datos de los acompañantes");
        }

        Booking booking = Booking.builder()
                .tourist(tourist)
                .availability(availability)
                .participantCount(participantCount)
                .status(BookingStatus.PENDING)
                .bookingDate(LocalDateTime.now(clock))
                .build();
        for (int i = 0; i < participantCount - 1; i++) {
            ParticipantForm data = companions.get(i);
            booking.getParticipants().add(Participant.builder()
                    .booking(booking)
                    .firstName(data.getFirstName().trim())
                    .lastName(data.getLastName().trim())
                    .build());
        }
        return bookingRepository.save(booking);
    }

    // Revisa los datos de cada acompañante (nombre y apellido, como en el MER). Devuelve campo -> mensaje
    // de error, por ejemplo "companions[0].firstName" -> "Ingresá el nombre". Si está todo bien, queda vacío.
    public Map<String, String> validateCompanions(Tourist tourist, Integer participantCount,
                                                  List<ParticipantForm> companions) {
        Map<String, String> errors = new LinkedHashMap<>();
        int needed = participantCount == null ? 0 : Math.max(0, participantCount - 1);
        for (int i = 0; i < needed; i++) {
            String field = "companions[" + i + "].";
            ParticipantForm data = companions != null && i < companions.size() && companions.get(i) != null
                    ? companions.get(i) : new ParticipantForm();

            if (!DocumentValidator.isValidName(data.getFirstName())) {
                errors.put(field + "firstName", "Ingresá el nombre (solo letras)");
            }
            if (!DocumentValidator.isValidName(data.getLastName())) {
                errors.put(field + "lastName", "Ingresá el apellido (solo letras)");
            }
        }
        return errors;
    }

    // Reservas del turista: primero las más próximas
    public List<Booking> findByTourist(Tourist tourist) {
        return bookingRepository.findByTouristId(tourist.getId()).stream()
                .sorted(Comparator.comparing((Booking b) -> b.getAvailability().getStartDateTime()))
                .toList();
    }

    // Reservas que todavía no pasaron y siguen vigentes
    public List<Booking> findUpcoming(Tourist tourist) {
        return findByTourist(tourist).stream()
                .filter(b -> b.isActive() && !hasStarted(b.getAvailability()))
                .toList();
    }

    // Historial: las que ya pasaron, las canceladas y las vencidas (la más reciente primero)
    public List<Booking> findHistory(Tourist tourist) {
        return findByTourist(tourist).stream()
                .filter(b -> !b.isActive() || hasStarted(b.getAvailability()))
                .sorted(Comparator.comparing((Booking b) -> b.getAvailability().getStartDateTime()).reversed())
                .toList();
    }

    // El turista cancela su reserva. Solo se puede antes de que empiece la actividad.
    @Transactional
    public Booking cancel(Long bookingId, Tourist tourist) {
        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> b.getTourist().getId().equals(tourist.getId()))
                .orElseThrow(() -> new BusinessException("La reserva no existe o no es tuya"));
        if (!booking.isActive()) {
            throw new BusinessException("Esa reserva ya estaba " + booking.getStatusLabel().toLowerCase());
        }
        if (hasStarted(booking.getAvailability())) {
            throw new BusinessException("No se puede cancelar una actividad que ya empezó o ya pasó");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    // ¿Corresponde reembolso si se cancela ahora? Depende de la política que fijó el prestador.
    // (Se usa para avisarle al turista; el reembolso real se hace cuando esté integrado el pago.)
    public boolean isRefundableNow(Booking booking) {
        var activity = booking.getAvailability().getActivity();
        if (!Boolean.TRUE.equals(activity.getRefundable()) || activity.getCancellationMinutes() == null) {
            return false;
        }
        LocalDateTime limit = booking.getAvailability().getStartDateTime().minusMinutes(activity.getCancellationMinutes());
        return LocalDateTime.now(clock).isBefore(limit);
    }

    // ===================== Métodos auxiliares =====================

    private void checkBookingOpen(Availability availability) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!now.isBefore(availability.getStartDateTime())) {
            throw new BusinessException("Esa fecha ya pasó");
        }
        if (!now.isBefore(availability.getBookingClosesAt())) {
            String deadline = availability.getActivity().getBookingDeadlineLabel();
            throw new BusinessException(deadline == null
                    ? "Las reservas para esa fecha ya cerraron"
                    : "Las reservas cierran " + deadline + " antes del inicio y ese plazo ya pasó");
        }
    }

    private boolean hasStarted(Availability availability) {
        return !LocalDateTime.now(clock).isBefore(availability.getStartDateTime());
    }
}
