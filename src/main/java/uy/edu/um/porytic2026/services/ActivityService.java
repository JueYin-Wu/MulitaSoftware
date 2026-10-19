package uy.edu.um.porytic2026.services;

import org.springframework.stereotype.Service;
import uy.edu.um.porytic2026.dto.AvailabilitySlot;
import uy.edu.um.porytic2026.entities.Activity;
import uy.edu.um.porytic2026.entities.ActivityEquipment;
import uy.edu.um.porytic2026.entities.Availability;
import uy.edu.um.porytic2026.entities.BookingStatus;
import uy.edu.um.porytic2026.entities.EquipmentRole;
import uy.edu.um.porytic2026.entities.Review;
import uy.edu.um.porytic2026.repository.ActivityEquipmentRepository;
import uy.edu.um.porytic2026.repository.ActivityRepository;
import uy.edu.um.porytic2026.repository.AvailabilityRepository;
import uy.edu.um.porytic2026.repository.BookingRepository;
import uy.edu.um.porytic2026.repository.ReviewRepository;

import java.time.LocalDate;
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
    private final AvailabilityRepository availabilityRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final ActivityEquipmentRepository activityEquipmentRepository;

    public ActivityService(ActivityRepository activityRepository, AvailabilityRepository availabilityRepository,
                           BookingRepository bookingRepository, ReviewRepository reviewRepository,
                           ActivityEquipmentRepository activityEquipmentRepository) {
        this.activityRepository = activityRepository;
        this.availabilityRepository = availabilityRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.activityEquipmentRepository = activityEquipmentRepository;
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

    // ===== Página de detalle de una actividad =====

    // Próximas fechas con los lugares que quedan (cupos menos los participantes de reservas vigentes)
    public List<AvailabilitySlot> findUpcomingSlots(Long activityId) {
        List<Availability> availabilities =
                availabilityRepository.findByActivityIdAndDateGreaterThanEqualOrderByDateAscTimeAsc(activityId, LocalDate.now());
        return availabilities.stream()
                .map(a -> {
                    long taken = bookingRepository.countParticipants(a.getId(),
                            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));
                    int capacity = a.getCapacity() == null ? 0 : a.getCapacity();
                    return new AvailabilitySlot(a, Math.max(0, capacity - taken));
                })
                .toList();
    }

    // Equipamiento según su rol: REQUIRED = lo tiene que llevar el turista, PROVIDED = lo da el prestador
    public List<ActivityEquipment> findEquipment(Long activityId, EquipmentRole role) {
        return activityEquipmentRepository.findByActivityId(activityId).stream()
                .filter(e -> e.getRole() == role)
                .toList();
    }

    public List<Activity> findByProvider(Long providerId) {
        return activityRepository.findByProviderId(providerId);
    }

    public List<Review> findProviderReviews(Long providerId) {
        return reviewRepository.findByBookingAvailabilityActivityProviderIdOrderByIdDesc(providerId);
    }

    public List<Review> findReviews(Long activityId) {
        return reviewRepository.findByBookingAvailabilityActivityIdOrderByIdDesc(activityId);
    }

    // Promedio de las valoraciones (1 a 5), o null si todavía no tiene reseñas
    public Double averageRating(List<Review> reviews) {
        return reviews.stream()
                .filter(r -> r.getRating() != null)
                .mapToInt(Review::getRating)
                .average()
                .stream().boxed().findFirst().orElse(null);
    }

    public Activity findById(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la actividad " + id));
    }
}
