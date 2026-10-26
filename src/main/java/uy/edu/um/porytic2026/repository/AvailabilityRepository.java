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

    // Busca la fecha y la bloquea hasta que termine la transacción (SELECT ... FOR UPDATE).
    // Así, si dos turistas reservan los últimos lugares al mismo tiempo, el segundo espera
    // a que el primero termine y ve el cupo actualizado: no se venden más lugares de los que hay.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Availability a WHERE a.id = :id")
    Optional<Availability> findByIdForUpdate(@Param("id") Long id);
}
