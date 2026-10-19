package uy.edu.um.porytic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.edu.um.porytic2026.entities.JuridicProvider;

@Repository
public interface JuridicProviderRepository extends JpaRepository<JuridicProvider, Long> {

    boolean existsByTaxId(String taxId);
}
