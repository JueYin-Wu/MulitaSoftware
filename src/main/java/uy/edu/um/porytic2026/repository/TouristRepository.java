package uy.edu.um.porytic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.edu.um.porytic2026.entities.DocumentType;
import uy.edu.um.porytic2026.entities.Tourist;

@Repository
public interface TouristRepository extends JpaRepository<Tourist, Long> {

    // ¿Ya hay un turista con este documento (del mismo tipo)?
    boolean existsByDocumentTypeAndIdDocument(DocumentType documentType, String idDocument);
}
