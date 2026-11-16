package uy.edu.um.porytic2026.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.porytic2026.entities.Booking;
import uy.edu.um.porytic2026.entities.BookingStatus;
import uy.edu.um.porytic2026.entities.Payment;
import uy.edu.um.porytic2026.entities.PaymentStatus;
import uy.edu.um.porytic2026.repository.BookingRepository;
import uy.edu.um.porytic2026.repository.PaymentRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Vencimiento automático de las reservas que no se pagaron a tiempo.
// Una reserva PENDING ocupa lugares en el cupo; si el turista no paga antes de su plazo
// (ver BookingService.paymentDeadlineOf), pasa a EXPIRED y esos lugares quedan libres para otros.
// La ejecuta BookingExpirationJob cada minuto.
@Service
public class BookingExpirationService {

    private static final Logger log = LoggerFactory.getLogger(BookingExpirationService.class);

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final Clock clock;

    public BookingExpirationService(BookingRepository bookingRepository, PaymentRepository paymentRepository,
                                    BookingService bookingService, PaymentService paymentService, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.clock = clock;
    }

    // Ids de las reservas sin pagar a las que ya se les pasó el plazo
    public List<Long> findOverdueIds() {
        return bookingRepository.findByStatus(BookingStatus.PENDING).stream()
                .filter(bookingService::isPaymentOverdue)
                .map(Booking::getId)
                .toList();
    }

    // Vence una reserva si sigue sin pagar. Devuelve true si la venció.
    // Cada reserva va en su propia transacción: si una falla, las demás se procesan igual.
    @Transactional
    public boolean expireIfUnpaid(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || !bookingService.isPaymentOverdue(booking)) {
            return false; // ya no existe, ya se pagó o se canceló, o todavía está en plazo
        }
        boolean started = !LocalDateTime.now(clock).isBefore(booking.getAvailability().getStartDateTime());

        Optional<Payment> payment = paymentRepository.findByBookingId(bookingId);
        if (payment.isPresent()) {
            // Intentó pagar: antes de vencerla se le pregunta a Mercado Pago cómo terminó ese pago
            boolean checked = paymentService.syncBeforeExpiring(booking);
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                return false; // el pago estaba aprobado: la reserva quedó confirmada
            }
            if (!checked && !started) {
                return false; // Mercado Pago no respondió: se vuelve a intentar en la próxima vuelta
            }
            // Pago todavía "en proceso" en Mercado Pago: se espera, para no vencer una reserva que se está pagando.
            // (Si la actividad ya empezó, se vence igual.)
            boolean inProcess = paymentRepository.findByBookingId(bookingId)
                    .map(p -> p.getStatus() == PaymentStatus.PENDING)
                    .orElse(false);
            if (inProcess && !started) {
                return false;
            }
        }

        booking.setStatus(BookingStatus.EXPIRED);
        bookingRepository.save(booking);
        log.info("Reserva {} vencida por falta de pago: se liberaron {} lugares de la fecha {}",
                bookingId, booking.getParticipantCount(), booking.getAvailability().getId());
        return true;
    }
}
