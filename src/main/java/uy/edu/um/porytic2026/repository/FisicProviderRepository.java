package uy.edu.um.porytic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.edu.um.porytic2026.entities.DocumentType;
import uy.edu.um.porytic2026.entities.FisicProvider;

@Repository
public interface FisicProviderRepository extends JpaRepository<FisicProvider, Long> {

    // ¿Ya hay un prestador persona física con este documento (del mismo tipo)?
    boolean existsByDocumentTypeAndNationalId(DocumentType documentType, String nationalId);
}
