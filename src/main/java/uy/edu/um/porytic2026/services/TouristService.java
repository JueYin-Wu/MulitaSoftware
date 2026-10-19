package uy.edu.um.porytic2026.services;

import org.springframework.stereotype.Service;
import uy.edu.um.porytic2026.entities.Booking;
import uy.edu.um.porytic2026.entities.BookingStatus;
import uy.edu.um.porytic2026.entities.Review;
import uy.edu.um.porytic2026.entities.Tourist;
import uy.edu.um.porytic2026.repository.BookingRepository;
import uy.edu.um.porytic2026.repository.ReviewRepository;
import uy.edu.um.porytic2026.repository.TouristRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

// Datos para el perfil de un turista: sus reservas y sus reseñas
@Service
public class TouristService {

    private final TouristRepository touristRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;

    public TouristService(TouristRepository touristRepository, BookingRepository bookingRepository,
                          ReviewRepository reviewRepository) {
        this.touristRepository = touristRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
    }

    public Tourist findById(Long id) {
        return touristRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe el turista " + id));
    }

    // Actividades que ya hizo: reservas confirmadas con fecha pasada (de la más reciente a la más vieja)
    public List<Booking> findCompletedBookings(Long touristId) {
        LocalDate today = LocalDate.now();
        return bookingRepository.findByTouristId(touristId).stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED && b.getAvailability().getDate().isBefore(today))
                .sorted(byDate().reversed())
                .toList();
    }

    // Próximas reservas: pendientes o confirmadas, de hoy en adelante (la más cercana primero)
    public List<Booking> findUpcomingBookings(Long touristId) {
        LocalDate today = LocalDate.now();
        return bookingRepository.findByTouristId(touristId).stream()
                .filter(b -> (b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.PENDING)
                        && !b.getAvailability().getDate().isBefore(today))
                .sorted(byDate())
                .toList();
    }

    public List<Review> findReviews(Long touristId) {
        return reviewRepository.findByBookingTouristIdOrderByIdDesc(touristId);
    }

    private Comparator<Booking> byDate() {
        return Comparator.comparing((Booking b) -> b.getAvailability().getDate())
                .thenComparing(b -> b.getAvailability().getTime());
    }
}
