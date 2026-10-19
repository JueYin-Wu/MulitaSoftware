package uy.edu.um.porytic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.edu.um.porytic2026.entities.Availability;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    // Próximas fechas de una actividad (desde hoy), ordenadas por fecha y hora
    List<Availability> findByActivityIdAndDateGreaterThanEqualOrderByDateAscTimeAsc(Long activityId, LocalDate from);
}
