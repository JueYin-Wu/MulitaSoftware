package uy.edu.um.porytic2026.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uy.edu.um.porytic2026.entities.Availability;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    // Próximas fechas de una actividad (desde hoy), ordenadas por fecha y hora
    List<Availability> findByActivityIdAndDateGreaterThanEqualOrderByDateAscTimeAsc(Long activityId, LocalDate from);

    // Todas las fechas de un día, por hora (API: actividades de un día)
    List<Availability> findByDateOrderByTimeAsc(LocalDate date);

    // Fechas de las actividades de un prestador: próximas (de la más cercana a la más lejana) y pasadas (al revés)
    List<Availability> findByActivityProviderIdAndDateGreaterThanEqualOrderByDateAscTimeAsc(Long providerId, LocalDate from);

    List<Availability> findByActivityProviderIdAndDateLessThanOrderByDateDescTimeDesc(Long providerId, LocalDate before);

    // ¿Ya existe esa fecha y hora para la actividad? (para no repetir)
    boolean existsByActivityIdAndDateAndTime(Long activityId, LocalDate date, java.time.LocalTime time);

    // Cuántas fechas próximas tiene una actividad
    long countByActivityIdAndDateGreaterThanEqual(Long activityId, LocalDate from);

    // Busca la fecha y la bloquea hasta que termine la transacción (SELECT ... FOR UPDATE).
    // Así, si dos turistas reservan los últimos lugares al mismo tiempo, el segundo espera
    // a que el primero termine y ve el cupo actualizado: no se venden más lugares de los que hay.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Availability a WHERE a.id = :id")
    Optional<Availability> findByIdForUpdate(@Param("id") Long id);
}
