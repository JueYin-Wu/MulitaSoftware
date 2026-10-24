package uy.edu.um.porytic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.edu.um.porytic2026.entities.Tag;
import uy.edu.um.porytic2026.entities.TagStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    // Tipos de actividad en un estado, por nombre (los aprobados son los que se usan en la plataforma)
    List<Tag> findByStatusOrderByNameAsc(TagStatus status);

    // Propuestas de los prestadores, la más vieja primero
    List<Tag> findByStatusOrderByProposalDateAsc(TagStatus status);

    // Para no repetir nombres ("Senderismo" y "senderismo" cuentan como el mismo)
    boolean existsByNameIgnoreCase(String name);

    // Busca un tipo por nombre sin importar mayúsculas (para no duplicar las propuestas de los prestadores)
    Optional<Tag> findFirstByNameIgnoreCase(String name);

    // Lo mismo, pero sin contar al propio tipo (cuando se edita)
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
