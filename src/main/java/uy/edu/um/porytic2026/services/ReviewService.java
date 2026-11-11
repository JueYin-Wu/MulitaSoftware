package uy.edu.um.porytic2026.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.porytic2026.dto.ReviewForm;
import uy.edu.um.porytic2026.entities.Booking;
import uy.edu.um.porytic2026.entities.BookingStatus;
import uy.edu.um.porytic2026.entities.Review;
import uy.edu.um.porytic2026.entities.Tourist;
import uy.edu.um.porytic2026.exceptions.BusinessException;
import uy.edu.um.porytic2026.repository.BookingRepository;
import uy.edu.um.porytic2026.repository.ReviewRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Reseñas de los turistas (letra: "Una vez que haya participado de una actividad, podrá compartir su
// experiencia mediante un comentario y una valoración de uno a cinco puntos").
// Reglas: la reserva tiene que ser del turista, estar confirmada (pagada), la actividad ya tiene que
// haber terminado, y se puede dejar una sola reseña por reserva.
@Service
public class ReviewService {

    public static final int MIN_COMMENT = 10;
    public static final int MAX_COMMENT = 1000;

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final Clock clock;

    public ReviewService(ReviewRepository reviewRepository, BookingRepository bookingRepository, Clock clock) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    // ¿Se puede reseñar esta reserva ahora? (se usa para mostrar o no el botón "Dejar reseña")
    public boolean canReview(Booking booking) {
        return booking.getStatus() == BookingStatus.CONFIRMED
                && hasFinished(booking)
                && booking.getReview() == null
                && !reviewRepository.existsByBookingId(booking.getId());
    }

    // Reservas de una lista que se pueden reseñar (ids), para la página "Mis reservas"
    public Set<Long> reviewableIds(List<Booking> bookings) {
        return bookings.stream().filter(this::canReview).map(Booking::getId).collect(Collectors.toSet());
    }

    // La reserva para mostrar el formulario; si no se puede reseñar, explica por qué
    public Booking findReviewableBooking(Long bookingId, Tourist tourist) {
        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> b.getTourist().getId().equals(tourist.getId()))
                .orElseThrow(() -> new BusinessException("La reserva no existe o no es tuya"));

        if (booking.getReview() != null || reviewRepository.existsByBookingId(booking.getId())) {
            throw new BusinessException("Ya dejaste una reseña para esta reserva");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("Solo se pueden reseñar las reservas confirmadas");
        }
        if (!hasFinished(booking)) {
            throw new BusinessException("Vas a poder dejar tu reseña cuando termine la actividad");
        }
        return booking;
    }

    // Guarda la reseña. Los errores de los campos vuelven con el nombre del campo para mostrarlos al lado.
    @Transactional
    public Review create(Long bookingId, Tourist tourist, ReviewForm form) {
        Booking booking = findReviewableBooking(bookingId, tourist);

        Integer rating = form.getRating();
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException("rating", "Elegí una valoración de 1 a 5 estrellas");
        }
        String comment = form.getComment() == null ? "" : form.getComment().trim();
        if (comment.length() < MIN_COMMENT) {
            throw new BusinessException("comment", "Contanos un poco más: el comentario debe tener al menos " + MIN_COMMENT + " caracteres");
        }
        if (comment.length() > MAX_COMMENT) {
            throw new BusinessException("comment", "El comentario no puede tener más de " + MAX_COMMENT + " caracteres");
        }

        return reviewRepository.save(Review.builder()
                .booking(booking)
                .rating(rating)
                .comment(comment)
                .createdAt(LocalDateTime.now(clock))
                .build());
    }

    // La actividad terminó: inicio + duración (si la duración no está cargada, alcanza con que haya empezado)
    private boolean hasFinished(Booking booking) {
        Integer duration = booking.getAvailability().getActivity().getDurationMinutes();
        LocalDateTime end = booking.getAvailability().getStartDateTime().plusMinutes(duration == null ? 0 : duration);
        return !LocalDateTime.now(clock).isBefore(end);
    }
}
