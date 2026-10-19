package uy.edu.um.porytic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.edu.um.porytic2026.entities.ActivityEquipment;

import java.util.List;

@Repository
public interface ActivityEquipmentRepository extends JpaRepository<ActivityEquipment, Long> {

    // Equipamiento de una actividad, con su rol (lo lleva el turista o lo da el prestador)
    List<ActivityEquipment> findByActivityId(Long activityId);

    // En cuántas actividades se usa un equipamiento (para no borrar uno que está en uso)
    long countByEquipmentId(Long equipmentId);
}
