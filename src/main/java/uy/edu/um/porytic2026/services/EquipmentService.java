package uy.edu.um.porytic2026.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.porytic2026.entities.Equipment;
import uy.edu.um.porytic2026.repository.ActivityEquipmentRepository;
import uy.edu.um.porytic2026.repository.EquipmentRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Tipos de equipamiento: alta, consulta, modificación y baja por parte del administrador
@Service
public class EquipmentService {

    public static final int MAX_NAME = 50;
    public static final int MAX_DESCRIPTION = 255;

    private final EquipmentRepository equipmentRepository;
    private final ActivityEquipmentRepository activityEquipmentRepository;

    public EquipmentService(EquipmentRepository equipmentRepository,
                            ActivityEquipmentRepository activityEquipmentRepository) {
        this.equipmentRepository = equipmentRepository;
        this.activityEquipmentRepository = activityEquipmentRepository;
    }

    public List<Equipment> findAll() {
        return equipmentRepository.findAllByOrderByNameAsc();
    }

    // Cuántas actividades usan cada equipamiento: id -> cantidad
    public Map<Long, Long> usageCount(List<Equipment> items) {
        Map<Long, Long> usage = new HashMap<>();
        for (Equipment equipment : items) {
            usage.put(equipment.getId(), activityEquipmentRepository.countByEquipmentId(equipment.getId()));
        }
        return usage;
    }

    @Transactional
    public Equipment create(String name, String description) {
        String clean = validName(name);
        if (equipmentRepository.existsByNameIgnoreCase(clean)) {
            throw new IllegalArgumentException("Ya existe un equipamiento llamado \"" + clean + "\"");
        }
        return equipmentRepository.save(Equipment.builder()
                .name(clean)
                .description(validDescription(description))
                .build());
    }

    @Transactional
    public Equipment update(Long id, String name, String description) {
        Equipment equipment = findById(id);
        String clean = validName(name);
        if (equipmentRepository.existsByNameIgnoreCaseAndIdNot(clean, id)) {
            throw new IllegalArgumentException("Ya existe un equipamiento llamado \"" + clean + "\"");
        }
        equipment.setName(clean);
        equipment.setDescription(validDescription(description));
        return equipmentRepository.save(equipment);
    }

    // No se borra un equipamiento que piden o dan actividades
    @Transactional
    public Equipment delete(Long id) {
        Equipment equipment = findById(id);
        long used = activityEquipmentRepository.countByEquipmentId(id);
        if (used > 0) {
            throw new IllegalStateException("No se puede eliminar \"" + equipment.getName() + "\": lo usan "
                    + used + (used == 1 ? " actividad" : " actividades") + ".");
        }
        equipmentRepository.delete(equipment);
        return equipment;
    }

    public Equipment findById(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe el equipamiento " + id));
    }

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

    // La descripción es opcional
    private String validDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String clean = description.trim();
        if (clean.length() > MAX_DESCRIPTION) {
            throw new IllegalArgumentException("La descripción no puede tener más de " + MAX_DESCRIPTION + " caracteres");
        }
        return clean;
    }
}
