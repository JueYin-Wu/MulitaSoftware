package uy.edu.um.porytic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.edu.um.porytic2026.entities.Activity;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByLocationDepartment(String department);

    List<Activity> findByProviderId(Long providerId);

    // Actividades de un prestador para su portal, ordenadas por nombre
    List<Activity> findByProviderIdOrderByNameAsc(Long providerId);

    // Búsquedas para el buscador de la página de inicio
    List<Activity> findByNameContainingIgnoreCase(String name);

    List<Activity> findByNameContainingIgnoreCaseAndLocationDepartment(String name, String department);

    // En cuántas actividades se usa un tipo de actividad (para no borrar uno que está en uso)
    long countByTagsId(Long tagId);
}
