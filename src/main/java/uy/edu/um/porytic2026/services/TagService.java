package uy.edu.um.porytic2026.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.porytic2026.entities.Tag;
import uy.edu.um.porytic2026.entities.TagStatus;
import uy.edu.um.porytic2026.repository.ActivityRepository;
import uy.edu.um.porytic2026.repository.TagRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Tipos de actividad (etiquetas): alta, consulta, modificación y baja por parte del administrador,
// y revisión de los tipos que propongan los prestadores.
@Service
public class TagService {

    public static final int MAX_NAME = 50;

    private final TagRepository tagRepository;
    private final ActivityRepository activityRepository;

    public TagService(TagRepository tagRepository, ActivityRepository activityRepository) {
        this.tagRepository = tagRepository;
        this.activityRepository = activityRepository;
    }

    public List<Tag> findApproved() {
        return tagRepository.findByStatusOrderByNameAsc(TagStatus.APPROVED);
    }

    public List<Tag> findPending() {
        return tagRepository.findByStatusOrderByProposalDateAsc(TagStatus.PENDING);
    }

    // Cuántas actividades usan cada tipo: id del tipo -> cantidad
    public Map<Long, Long> usageCount(List<Tag> tags) {
        Map<Long, Long> usage = new HashMap<>();
        for (Tag tag : tags) {
            usage.put(tag.getId(), activityRepository.countByTagsId(tag.getId()));
        }
        return usage;
    }

    // Alta: lo que crea el administrador queda aprobado directamente
    @Transactional
    public Tag create(String name) {
        String clean = validName(name);
        if (tagRepository.existsByNameIgnoreCase(clean)) {
            throw new IllegalArgumentException("Ya existe un tipo de actividad llamado \"" + clean + "\"");
        }
        return tagRepository.save(Tag.builder()
                .name(clean)
                .status(TagStatus.APPROVED)
                .proposalDate(LocalDateTime.now())
                .build());
    }

    @Transactional
    public Tag rename(Long id, String name) {
        Tag tag = findById(id);
        String clean = validName(name);
        if (tagRepository.existsByNameIgnoreCaseAndIdNot(clean, id)) {
            throw new IllegalArgumentException("Ya existe un tipo de actividad llamado \"" + clean + "\"");
        }
        tag.setName(clean);
        return tagRepository.save(tag);
    }

    // Baja: no se puede borrar un tipo que tienen actividades, porque esas actividades perderían su categoría
    @Transactional
    public Tag delete(Long id) {
        Tag tag = findById(id);
        long used = activityRepository.countByTagsId(id);
        if (used > 0) {
            throw new IllegalStateException("No se puede eliminar \"" + tag.getName() + "\": lo usan "
                    + used + (used == 1 ? " actividad" : " actividades") + ".");
        }
        tagRepository.delete(tag);
        return tag;
    }

    // Propuesta de un prestador aceptada: pasa a estar disponible para todos
    @Transactional
    public Tag approve(Long id) {
        Tag tag = findPendingById(id);
        tag.setStatus(TagStatus.APPROVED);
        tag.setRejectionReason(null);
        return tagRepository.save(tag);
    }

    @Transactional
    public Tag reject(Long id, String reason) {
        Tag tag = findPendingById(id);
        tag.setStatus(TagStatus.REJECTED);
        tag.setRejectionReason(reason == null || reason.isBlank() ? null : reason.trim());
        return tagRepository.save(tag);
    }

    public Tag findById(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe el tipo de actividad " + id));
    }

    private Tag findPendingById(Long id) {
        Tag tag = findById(id);
        if (tag.getStatus() != TagStatus.PENDING) {
            throw new IllegalStateException("Esa propuesta ya fue revisada");
        }
        return tag;
    }

    // Nombre obligatorio, sin espacios de más, entre 2 y 50 caracteres
    private String validName(String name) {
        String clean = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (clean.length() < 2) {
            throw new IllegalArgumentException("El nombre debe tener al menos 2 caracteres");
        }
        if (clean.length() > MAX_NAME) {
            throw new IllegalArgumentException("El nombre no puede tener más de " + MAX_NAME + " caracteres");
        }
        return clean;
    }
}
