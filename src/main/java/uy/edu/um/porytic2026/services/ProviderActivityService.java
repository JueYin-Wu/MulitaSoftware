package uy.edu.um.porytic2026.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uy.edu.um.porytic2026.dto.ActivityForm;
import uy.edu.um.porytic2026.entities.*;
import uy.edu.um.porytic2026.exceptions.BusinessException;
import uy.edu.um.porytic2026.repository.ActivityEquipmentRepository;
import uy.edu.um.porytic2026.repository.ActivityRepository;
import uy.edu.um.porytic2026.repository.EquipmentRepository;
import uy.edu.um.porytic2026.repository.TagRepository;

import java.time.LocalDateTime;
import java.util.*;

// Actividades desde el portal del prestador: consultar las suyas y publicar una nueva.
@Service
public class ProviderActivityService {

    // Máximo de fotos por actividad (la primera es la de la tarjeta)
    public static final int MAX_PHOTOS = 5;

    private final ActivityRepository activityRepository;
    private final TagRepository tagRepository;
    private final EquipmentRepository equipmentRepository;
    private final ActivityEquipmentRepository activityEquipmentRepository;
    private final FileStorageService fileStorageService;

    public ProviderActivityService(ActivityRepository activityRepository,
                                   TagRepository tagRepository,
                                   EquipmentRepository equipmentRepository,
                                   ActivityEquipmentRepository activityEquipmentRepository,
                                   FileStorageService fileStorageService) {
        this.activityRepository = activityRepository;
        this.tagRepository = tagRepository;
        this.equipmentRepository = equipmentRepository;
        this.activityEquipmentRepository = activityEquipmentRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<Activity> findByProvider(Provider provider) {
        return activityRepository.findByProviderIdOrderByNameAsc(provider.getId());
    }

    // Tipos de actividad que el prestador puede elegir: solo los aprobados por el admin
    public List<Tag> findAvailableTags() {
        return tagRepository.findByStatusOrderByNameAsc(TagStatus.APPROVED);
    }

    public List<Equipment> findAllEquipment() {
        return equipmentRepository.findAllByOrderByNameAsc();
    }

    // Publica una actividad nueva del prestador.
    // Primero se controlan todos los datos y recién después se guardan las fotos en disco,
    // así no quedan archivos sueltos si algún dato está mal.
    @Transactional
    public Activity create(Provider provider, ActivityForm form, List<MultipartFile> photos) {
        validate(form);
        List<MultipartFile> files = selectedFiles(photos);
        if (files.size() > MAX_PHOTOS) {
            throw new BusinessException("photos", "Podés subir hasta " + MAX_PHOTOS + " fotos");
        }

        List<String> imageUrls = storePhotos(provider, files);
        try {
            Activity activity = new Activity();
            activity.setProvider(provider);
            copyFormToActivity(form, activity);
            activity.setTags(resolveTags(form, provider));
            // La primera foto es la de la tarjeta y todas forman el carrusel de la página de la actividad
            activity.setImageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0));
            activity.setImages(new ArrayList<>(imageUrls));

            Activity saved = activityRepository.save(activity);
            saveEquipment(saved, form);
            return saved;
        } catch (RuntimeException e) {
            // Si algo falla al guardar, se borran las fotos que ya se habían subido
            imageUrls.forEach(fileStorageService::delete);
            throw e;
        }
    }

    // ===================== Métodos auxiliares =====================

    // Reglas que no se pueden expresar con las anotaciones del formulario
    private void validate(ActivityForm form) {
        if (!ActivityService.DEPARTMENTS.contains(form.getDepartment())) {
            throw new BusinessException("department", "Elegí un departamento de la lista");
        }
        if (form.isRefundable() && form.getCancellationMinutes() == null) {
            throw new BusinessException("cancellationMinutes",
                    "Indicá hasta cuántos minutos antes se puede cancelar con reembolso");
        }
        if (form.isRefundable() && form.getRefundPercentage() == null) {
            throw new BusinessException("refundPercentage", "Indicá qué porcentaje se devuelve si el turista cancela");
        }
        // Un mismo equipo no puede ser a la vez "lo da el prestador" y "lo trae el turista"
        Set<Long> provided = new HashSet<>(safe(form.getProvidedEquipmentIds()));
        for (Long id : safe(form.getRequiredEquipmentIds())) {
            if (provided.contains(id)) {
                throw new BusinessException("requiredEquipmentIds",
                        "Un mismo equipamiento no puede estar en \"lo da el prestador\" y en \"lo lleva el turista\"");
            }
        }
        String proposed = clean(form.getProposedTag());
        if (proposed != null && proposed.length() < 2) {
            throw new BusinessException("proposedTag", "El tipo de actividad debe tener al menos 2 caracteres");
        }
    }

    private void copyFormToActivity(ActivityForm form, Activity activity) {
        activity.setName(clean(form.getName()));
        activity.setDescription(clean(form.getDescription()));
        activity.setPrice(form.getPrice());
        activity.setDurationMinutes(form.getDurationMinutes());
        activity.setMinimumAge(form.getMinimumAge() == null ? 0 : form.getMinimumAge());
        activity.setRequirements(clean(form.getRequirements()));
        activity.setLocation(new Location(form.getDepartment(), clean(form.getNeighborhood()),
                clean(form.getStreet()), clean(form.getNumber())));
        activity.setBookingDeadline(form.getBookingDeadline());
        activity.setRefundable(form.isRefundable());
        // Si no es reembolsable, el porcentaje y el plazo de cancelación con reembolso no tienen sentido
        activity.setRefundPercentage(form.isRefundable() ? form.getRefundPercentage() : null);
        activity.setCancellationMinutes(form.isRefundable() ? form.getCancellationMinutes() : null);
    }

    // Tipos elegidos (solo aprobados, aunque llegue otro id desde el formulario)
    // más el tipo nuevo que haya propuesto el prestador.
    private List<Tag> resolveTags(ActivityForm form, Provider provider) {
        List<Tag> tags = new ArrayList<>(tagRepository.findAllById(safe(form.getTagIds())).stream()
                .filter(tag -> tag.getStatus() == TagStatus.APPROVED)
                .toList());

        String proposed = clean(form.getProposedTag());
        if (proposed == null) {
            return tags;
        }
        String name = proposed.replaceAll("\\s+", " ");
        Optional<Tag> existing = tagRepository.findFirstByNameIgnoreCase(name);
        Tag tag;
        if (existing.isPresent()) {
            tag = existing.get();
            if (tag.getStatus() == TagStatus.REJECTED) {
                throw new BusinessException("proposedTag",
                        "El tipo \"" + tag.getName() + "\" ya fue propuesto y el administrador lo rechazó");
            }
        } else {
            // Queda pendiente: la actividad lo muestra recién cuando el admin lo aprueba
            tag = tagRepository.save(Tag.builder()
                    .name(name)
                    .status(TagStatus.PENDING)
                    .proposedBy(provider)
                    .proposalDate(LocalDateTime.now())
                    .build());
        }
        if (tags.stream().noneMatch(t -> t.getId() != null && t.getId().equals(tag.getId()))) {
            tags.add(tag);
        }
        return tags;
    }

    private void saveEquipment(Activity activity, ActivityForm form) {
        List<ActivityEquipment> items = new ArrayList<>();
        for (Equipment equipment : equipmentRepository.findAllById(safe(form.getProvidedEquipmentIds()))) {
            items.add(ActivityEquipment.builder()
                    .activity(activity).equipment(equipment).role(EquipmentRole.PROVIDED).build());
        }
        for (Equipment equipment : equipmentRepository.findAllById(safe(form.getRequiredEquipmentIds()))) {
            items.add(ActivityEquipment.builder()
                    .activity(activity).equipment(equipment).role(EquipmentRole.REQUIRED).build());
        }
        activityEquipmentRepository.saveAll(items);
        activity.setEquipmentItems(items);
    }

    // Guarda las fotos en uploads/activities. Si una no es válida (tipo o tamaño),
    // se borran las que ya se habían guardado y se avisa cuál falló.
    private List<String> storePhotos(Provider provider, List<MultipartFile> files) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                urls.add(fileStorageService.storeImage(file, "activities", "activity-p" + provider.getId()));
            } catch (IllegalArgumentException | IllegalStateException e) {
                urls.forEach(fileStorageService::delete);
                throw new BusinessException("photos", "\"" + file.getOriginalFilename() + "\": " + e.getMessage());
            }
        }
        return urls;
    }

    // Si no se elige ningún archivo, el navegador igual manda uno vacío: se descarta
    private List<MultipartFile> selectedFiles(List<MultipartFile> photos) {
        if (photos == null) {
            return List.of();
        }
        return photos.stream().filter(file -> file != null && !file.isEmpty()).toList();
    }

    private static <T> List<T> safe(List<T> list) {
        return list == null ? List.of() : list;
    }

    // Saca espacios de más; un texto vacío se guarda como null
    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
