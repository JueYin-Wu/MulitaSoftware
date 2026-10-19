package uy.edu.um.porytic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.edu.um.porytic2026.entities.Provider;
import uy.edu.um.porytic2026.entities.ProviderStatus;

import java.util.List;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {

    // Lista de proveedores según su estado, los más antiguos primero
    List<Provider> findByStatusOrderByRegistrationDateAsc(ProviderStatus status);

    long countByStatus(ProviderStatus status);
}
