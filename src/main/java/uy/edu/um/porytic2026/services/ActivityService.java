package uy.edu.um.porytic2026.services;

import org.springframework.stereotype.Service;
import uy.edu.um.porytic2026.dto.AvailabilitySlot;
import uy.edu.um.porytic2026.dto.CategoryCard;
import uy.edu.um.porytic2026.entities.Activity;
import uy.edu.um.porytic2026.entities.ActivityEquipment;
import uy.edu.um.porytic2026.entities.Availability;
import uy.edu.um.porytic2026.entities.BookingStatus;
import uy.edu.um.porytic2026.entities.EquipmentRole;
import uy.edu.um.porytic2026.entities.Review;
import uy.edu.um.porytic2026.entities.Tag;
import uy.edu.um.porytic2026.entities.TagStatus;
import uy.edu.um.porytic2026.repository.ActivityEquipmentRepository;
import uy.edu.um.porytic2026.repository.ActivityRepository;
import uy.edu.um.porytic2026.repository.AvailabilityRepository;
import uy.edu.um.porytic2026.repository.BookingRepository;
import uy.edu.um.porytic2026.repository.ReviewRepository;
import uy.edu.um.porytic2026.repository.TagRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final TagRepository tagRepository;

    public ActivityService(ActivityRepository activityRepository, AvailabilityRepository availabilityRepository,
                           BookingRepository bookingRepository, ReviewRepository reviewRepository,
                           ActivityEquipmentRepository activityEquipmentRepository,
                           TagRepository tagRepository) {
        this.activityRepository = activityRepository;
        this.availabilityRepository = availabilityRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.activityEquipmentRepository = activityEquipmentRepository;
        this.tagRepository = tagRepository;
    }

    // Los tipos de actividad más reservados, para las tarjetas con foto del inicio.
    // Se ordenan por reservas confirmadas; si empatan, por cantidad de actividades.
    // La foto de cada tipo es la de su actividad más reservada (así no hay que cargar imágenes aparte).
    public List<CategoryCard> findTopCategories(int amount) {
        List<Activity> activities = activityRepository.findAll();

        // Reservas confirmadas de cada actividad (se calcula una sola vez)
        Map<Long, Long> bookingsByActivity = new HashMap<>();
        for (Activity activity : activities) {
            bookingsByActivity.put(activity.getId(),
                    bookingRepository.countByAvailabilityActivityIdAndStatus(activity.getId(), BookingStatus.CONFIRMED));
        }

        List<CategoryCard> cards = new ArrayList<>();
        for (Tag tag : tagRepository.findByStatusOrderByNameAsc(TagStatus.APPROVED)) {
            List<Activity> ofTag = activities.stream()
                    .filter(a -> a.getTags() != null && a.getTags().stream().anyMatch(t -> t.getId().equals(tag.getId())))
                    .sorted(Comparator.comparing((Activity a) -> bookingsByActivity.get(a.getId())).reversed())
                    .toList();
            if (ofTag.isEmpty()) {
                continue; // un tipo sin actividades no se muestra
            }
            long bookings = ofTag.stream().mapToLong(a -> bookingsByActivity.get(a.getId())).sum();
            // Foto: la propia del tipo; si no tiene, la de su actividad más reservada
            String image = tag.getImageUrl() != null && !tag.getImageUrl().isBlank() ? tag.getImageUrl()
                    : ofTag.stream().map(Activity::getImageUrl).filter(url -> url != null && !url.isBlank())
                    .findFirst().orElse(null);
            cards.add(new CategoryCard(tag, ofTag.size(), bookings, image));
        }

        return cards.stream()
                .sorted(Comparator.comparingLong(CategoryCard::getBookingCount).reversed()
                        .thenComparing(CategoryCard::getActivityCount, Comparator.reverseOrder()))
                .limit(amount)
                .toList();
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
        return search(text, department, null, null, maxPrice, sort);
    }

    // Búsqueda completa (RF05). La letra pide poder buscar por destino, tipo de actividad, fecha y precio.
    //  - tagId: tipo de actividad (etiqueta)
    //  - date: solo las actividades que tienen alguna fecha disponible ese día
    public List<Activity> search(String text, String department, Long tagId, LocalDate date,
                                 Double maxPrice, String sort) {
        List<Activity> result = search(text, department);

        if (tagId != null) {
            result = result.stream()
                    .filter(a -> a.getTags() != null && a.getTags().stream().anyMatch(t -> tagId.equals(t.getId())))
                    .toList();
        }

        if (date != null) {
            Set<Long> activitiesThatDay = availabilityRepository.findByDateOrderByTimeAsc(date).stream()
                    .map(availability -> availability.getActivity().getId())
                    .collect(Collectors.toSet());
            result = result.stream().filter(a -> activitiesThatDay.contains(a.getId())).toList();
        }

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
        } else if ("rating".equals(sort)) {
            result = sortByRating(result);
        }
        return result;
    }

    // Actividades destacadas del inicio: las mejor valoradas (si empatan, la que tiene más reseñas).
    // Las que todavía no tienen reseñas quedan al final.
    public List<Activity> findFeatured(int amount) {
        return sortByRating(activityRepository.findAll()).stream().limit(amount).toList();
    }

    // Ordena de mejor a peor valoración promedio; desempata la cantidad de reseñas
    private List<Activity> sortByRating(List<Activity> activities) {
        record Rated(Activity activity, double average, int count) {
        }
        return activities.stream()
                .map(activity -> {
                    List<Review> reviews = findReviews(activity.getId());
                    Double average = averageRating(reviews);
                    return new Rated(activity, average == null ? 0 : average, reviews.size());
                })
                .sorted(Comparator.comparingDouble(Rated::average).reversed()
                        .thenComparing(Rated::count, Comparator.reverseOrder()))
                .map(Rated::activity)
                .toList();
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
