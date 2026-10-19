package uy.edu.um.porytic2026.services;

import org.springframework.stereotype.Service;
import uy.edu.um.porytic2026.entities.Activity;
import uy.edu.um.porytic2026.repository.ActivityRepository;

import java.util.Comparator;
import java.util.List;

@Service
public class ActivityService {

    // Los 19 departamentos, para el filtro del buscador
    public static final List<String> DEPARTMENTS = List.of(
            "Artigas", "Canelones", "Cerro Largo", "Colonia", "Durazno", "Flores", "Florida",
            "Lavalleja", "Maldonado", "Montevideo", "Paysandú", "Río Negro", "Rivera", "Rocha",
            "Salto", "San José", "Soriano", "Tacuarembó", "Treinta y Tres");

    private final ActivityRepository activityRepository;

    public ActivityService(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    public List<Activity> findAll() {
        return activityRepository.findAll();
    }

    public List<Activity> findByDepartment(String department) {
        return activityRepository.findByLocationDepartment(department);
    }

    // Búsqueda por texto y/o departamento. Si los dos están vacíos, devuelve todas.
    public List<Activity> search(String text, String department) {
        boolean hasText = text != null && !text.isBlank();
        boolean hasDepartment = department != null && !department.isBlank();

        if (hasText && hasDepartment) {
            return activityRepository.findByNameContainingIgnoreCaseAndLocationDepartment(text.trim(), department);
        }
        if (hasText) {
            return activityRepository.findByNameContainingIgnoreCase(text.trim());
        }
        if (hasDepartment) {
            return activityRepository.findByLocationDepartment(department);
        }
        return activityRepository.findAll();
    }

    // Búsqueda de la página de actividades (RF05): texto, departamento, precio máximo y orden
    public List<Activity> search(String text, String department, Double maxPrice, String sort) {
        List<Activity> result = search(text, department);

        if (maxPrice != null) {
            result = result.stream()
                    .filter(a -> a.getPrice() != null && a.getPrice() <= maxPrice)
                    .toList();
        }

        if ("price_asc".equals(sort)) {
            result = result.stream().sorted(Comparator.comparing(Activity::getPrice)).toList();
        } else if ("price_desc".equals(sort)) {
            result = result.stream().sorted(Comparator.comparing(Activity::getPrice).reversed()).toList();
        } else if ("name".equals(sort)) {
            result = result.stream().sorted(Comparator.comparing(Activity::getName)).toList();
        }
        return result;
    }

    // Actividades que se muestran en la página de inicio (las primeras que se cargaron).
    // Más adelante pueden ser las mejor puntuadas, cuando existan las reseñas.
    public List<Activity> findFeatured(int amount) {
        return activityRepository.findAll().stream().limit(amount).toList();
    }

    public Activity findById(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la actividad " + id));
    }
}
