package uy.edu.um.porytic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uy.edu.um.porytic2026.entities.Booking;
import uy.edu.um.porytic2026.entities.BookingStatus;

import java.util.Collection;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Cantidad de lugares ocupados en una fecha (suma de participantes de las reservas que siguen vigentes)
    @Query("SELECT COALESCE(SUM(b.participantCount), 0) FROM Booking b "
            + "WHERE b.availability.id = :availabilityId AND b.status IN :statuses")
    long countParticipants(@Param("availabilityId") Long availabilityId,
                           @Param("statuses") Collection<BookingStatus> statuses);

    // Todas las reservas de un turista
    List<Booking> findByTouristId(Long touristId);

    // ¿El turista ya tiene una reserva vigente para esa fecha? (para no reservar dos veces lo mismo)
    boolean existsByTouristIdAndAvailabilityIdAndStatusIn(Long touristId, Long availabilityId,
                                                          Collection<BookingStatus> statuses);
}
