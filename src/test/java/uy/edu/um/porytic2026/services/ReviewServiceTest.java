package uy.edu.um.porytic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.um.porytic2026.dto.ReviewForm;
import uy.edu.um.porytic2026.entities.*;
import uy.edu.um.porytic2026.exceptions.BusinessException;
import uy.edu.um.porytic2026.repository.BookingRepository;
import uy.edu.um.porytic2026.repository.ReviewRepository;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Tests unitarios de ReviewService (reseñas). "Ahora" es el 09/11/2026 a las 12:00.
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 9, 12, 0);

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private BookingRepository bookingRepository;

    private ReviewService reviewService;
    private Tourist tourist;
    private Booking booking;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        reviewService = new ReviewService(reviewRepository, bookingRepository, fixedClock);

        tourist = new Tourist();
        tourist.setId(5L);

        Activity activity = new Activity();
        activity.setId(1L);
        activity.setName("Kayak al atardecer");
        activity.setDurationMinutes(120);

        // La actividad fue ayer de 10:00 a 12:00: ya terminó
        Availability availability = Availability.builder()
                .id(50L).activity(activity)
                .date(LocalDate.of(2026, 11, 8)).time(LocalTime.of(10, 0)).capacity(10)
                .build();

        booking = Booking.builder()
                .id(12L).tourist(tourist).availability(availability)
                .participantCount(2).status(BookingStatus.CONFIRMED).bookingDate(NOW.minusDays(5))
                .build();
    }

    private void bookingFound() {
        when(bookingRepository.findById(12L)).thenReturn(Optional.of(booking));
    }

    @Test
    void aFinishedConfirmedBookingCanBeReviewed() {
        bookingFound();
        when(reviewRepository.existsByBookingId(12L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Review review = reviewService.create(12L, tourist, new ReviewForm(5, "  Hermosa experiencia, muy recomendable  "));

        assertEquals(5, review.getRating());
        assertEquals("Hermosa experiencia, muy recomendable", review.getComment()); // sin espacios de más
        assertSame(booking, review.getBooking());
        assertEquals(NOW, review.getCreatedAt());
    }

    @Test
    void anActivityThatHasNotFinishedCannotBeReviewed() {
        // Empezó hoy a las 11:00 y dura 2 horas: termina a las 13:00 (y "ahora" son las 12:00)
        booking.getAvailability().setDate(LocalDate.of(2026, 11, 9));
        booking.getAvailability().setTime(LocalTime.of(11, 0));
        bookingFound();
        when(reviewRepository.existsByBookingId(12L)).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> reviewService.create(12L, tourist, new ReviewForm(5, "Muy lindo todo el recorrido")));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void aBookingCanBeReviewedOnlyOnce() {
        bookingFound();
        when(reviewRepository.existsByBookingId(12L)).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> reviewService.create(12L, tourist, new ReviewForm(4, "Otra reseña de lo mismo")));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void anUnpaidBookingCannotBeReviewed() {
        booking.setStatus(BookingStatus.PENDING);
        bookingFound();
        when(reviewRepository.existsByBookingId(12L)).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> reviewService.create(12L, tourist, new ReviewForm(4, "No la pagué pero opino igual")));
    }

    @Test
    void aTouristCannotReviewAnotherTouristsBooking() {
        Tourist other = new Tourist();
        other.setId(6L);
        bookingFound();

        assertThrows(BusinessException.class,
                () -> reviewService.create(12L, other, new ReviewForm(1, "Reseña de alguien que no fue")));
    }

    @Test
    void theRatingMustBeBetweenOneAndFive() {
        bookingFound();
        when(reviewRepository.existsByBookingId(12L)).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class,
                () -> reviewService.create(12L, tourist, new ReviewForm(6, "Excelente experiencia en general")));
        assertEquals("rating", e.getField());
    }

    @Test
    void theCommentCannotBeTooShort() {
        bookingFound();
        when(reviewRepository.existsByBookingId(12L)).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class,
                () -> reviewService.create(12L, tourist, new ReviewForm(4, "Lindo")));
        assertEquals("comment", e.getField());
    }

    @Test
    void canReviewIsUsedToShowTheButton() {
        when(reviewRepository.existsByBookingId(12L)).thenReturn(false);
        assertTrue(reviewService.canReview(booking));

        booking.setStatus(BookingStatus.CANCELLED);
        assertFalse(reviewService.canReview(booking));
    }
}
