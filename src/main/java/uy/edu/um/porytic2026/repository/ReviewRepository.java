package uy.edu.um.porytic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.edu.um.porytic2026.entities.Review;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Reseñas de una actividad: reseña -> reserva -> disponibilidad -> actividad
    List<Review> findByBookingAvailabilityActivityIdOrderByIdDesc(Long activityId);

    // Reseñas de todas las actividades de un prestador
    List<Review> findByBookingAvailabilityActivityProviderIdOrderByIdDesc(Long providerId);

    // Reseñas que escribió un turista (reseña -> reserva -> turista)
    List<Review> findByBookingTouristIdOrderByIdDesc(Long touristId);

    // Reseñas de las actividades realizadas entre dos fechas (API: top 3 del mes)
    List<Review> findByBookingAvailabilityDateBetween(LocalDate from, LocalDate to);
}
