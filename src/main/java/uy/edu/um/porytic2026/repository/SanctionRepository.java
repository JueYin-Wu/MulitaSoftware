package uy.edu.um.porytic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.edu.um.porytic2026.entities.Sanction;
import uy.edu.um.porytic2026.entities.SanctionType;

import java.util.List;
import java.util.Optional;

@Repository
public interface SanctionRepository extends JpaRepository<Sanction, Long> {

    // Historial de sanciones de un prestador, la más reciente primero
    List<Sanction> findByProviderIdOrderByDateDesc(Long providerId);

    // Suspensión vigente (todavía no levantada) de un prestador
    Optional<Sanction> findFirstByProviderIdAndTypeAndLiftedDateIsNullOrderByDateDesc(Long providerId, SanctionType type);

    long countByProviderId(Long providerId);
}
