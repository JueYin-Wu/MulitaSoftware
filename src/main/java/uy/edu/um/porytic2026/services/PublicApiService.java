package uy.edu.um.porytic2026.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.porytic2026.api.ApiNotFoundException;
import uy.edu.um.porytic2026.api.dto.ApiDtos.*;
import uy.edu.um.porytic2026.entities.*;
import uy.edu.um.porytic2026.repository.*;
import uy.edu.um.porytic2026.validation.DocumentValidator;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

// Lógica de la API REST que expone Uruguay Mnatural a otras plataformas (letra: "Integración con sistemas externos").
// Solo lee datos: nada de lo que hace modifica la base.
@Service
@Transactional(readOnly = true)
public class PublicApiService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final ProviderRepository providerRepository;
    private final JuridicProviderRepository juridicProviderRepository;
    private final ActivityRepository activityRepository;
    private final AvailabilityRepository availabilityRepository;
    private final ReviewRepository reviewRepository;
    private final BookingService bookingService;

    public PublicApiService(ProviderRepository providerRepository,
                            JuridicProviderRepository juridicProviderRepository,
                            ActivityRepository activityRepository,
                            AvailabilityRepository availabilityRepository,
                            ReviewRepository reviewRepository,
                            BookingService bookingService) {
        this.providerRepository = providerRepository;
        this.juridicProviderRepository = juridicProviderRepository;
        this.activityRepository = activityRepository;
        this.availabilityRepository = availabilityRepository;
        this.reviewRepository = reviewRepository;
        this.bookingService = bookingService;
    }

    // 1) Listado de todos los emprendimientos (solo los aprobados: los pendientes o rechazados no son públicos)
    public List<ProviderResponse> listProviders() {
        return providerRepository.findByStatusOrderByRegistrationDateAsc(ProviderStatus.APPROVED).stream()
                .sorted(Comparator.comparing(Provider::getBusinessName, String.CASE_INSENSITIVE_ORDER))
                .map(provider -> {
                    List<Review> reviews = reviewRepository.findByBookingAvailabilityActivityProviderIdOrderByIdDesc(provider.getId());
                    return new ProviderResponse(provider.getId(), provider.getBusinessName(), typeOf(provider),
                            provider.getServiceDescription(), provider.getAddress(),
                            activityRepository.findByProviderId(provider.getId()).size(),
                            average(reviews), "/providers/" + provider.getId());
                })
                .toList();
    }

    // 2) Verificar un emprendimiento por su id: ¿existe y está aprobado?
    public VerificationResponse verify(Long id) {
        Provider provider = providerRepository.findById(id)
                .orElseThrow(() -> new ApiNotFoundException("No existe un emprendimiento con id " + id));
        return toVerification(provider);
    }

    // 2b) Verificar un emprendimiento por su RUT (12 dígitos)
    public VerificationResponse verifyByTaxId(String rut) {
        String digits = DocumentValidator.onlyDigits(rut);
        if (!DocumentValidator.isValidRUT(digits)) {
            throw new IllegalArgumentException("El RUT debe tener 12 dígitos");
        }
        Provider provider = juridicProviderRepository.findByTaxId(digits)
                .orElseThrow(() -> new ApiNotFoundException("No hay un emprendimiento registrado con el RUT " + digits));
        return toVerification(provider);
    }

    // 3) Actividades de un día en un departamento (y opcionalmente una localidad / barrio), con sus horarios
    public List<DailyActivityResponse> activitiesOnDay(String date, String department, String locality) {
        LocalDate day = parseDate(date);
        String departmentName = resolveDepartment(department);
        String localityKey = locality == null || locality.isBlank() ? null : normalize(locality);

        // Se agrupan los horarios por actividad, manteniendo el orden por hora
        Map<Activity, List<SlotResponse>> byActivity = new LinkedHashMap<>();
        for (Availability availability : availabilityRepository.findByDateOrderByTimeAsc(day)) {
            Location location = availability.getActivity().getLocation();
            if (location == null || !departmentName.equals(location.getDepartment())) {
                continue;
            }
            if (localityKey != null && !localityKey.equals(normalize(location.getNeighborhood()))) {
                continue;
            }
            byActivity.computeIfAbsent(availability.getActivity(), a -> new ArrayList<>())
                    .add(new SlotResponse(availability.getId(), availability.getTime().format(TIME),
                            availability.getCapacity(), bookingService.remainingSpots(availability)));
        }

        List<DailyActivityResponse> result = new ArrayList<>();
        byActivity.forEach((activity, slots) -> result.add(new DailyActivityResponse(toActivity(activity), slots)));
        return result;
    }

    // 4) Top 3 de actividades mejor puntuadas en un mes, por departamento.
    //    Se toman las reseñas de las actividades realizadas en ese mes. Desempata la que tiene más reseñas.
    public List<TopActivityResponse> topRated(String department, String month) {
        String departmentName = resolveDepartment(department);
        YearMonth yearMonth = parseMonth(month);

        Map<Activity, List<Integer>> ratings = new HashMap<>();
        for (Review review : reviewRepository.findByBookingAvailabilityDateBetween(yearMonth.atDay(1), yearMonth.atEndOfMonth())) {
            Activity activity = review.getBooking().getAvailability().getActivity();
            if (activity.getLocation() != null && departmentName.equals(activity.getLocation().getDepartment())) {
                ratings.computeIfAbsent(activity, a -> new ArrayList<>()).add(review.getRating());
            }
        }

        List<Map.Entry<Activity, List<Integer>>> ranking = new ArrayList<>(ratings.entrySet());
        ranking.sort(Comparator
                .comparingDouble((Map.Entry<Activity, List<Integer>> e) -> averageOf(e.getValue())).reversed()
                .thenComparing(e -> e.getValue().size(), Comparator.reverseOrder()));

        List<TopActivityResponse> top = new ArrayList<>();
        for (int i = 0; i < Math.min(3, ranking.size()); i++) {
            var entry = ranking.get(i);
            top.add(new TopActivityResponse(i + 1, toActivity(entry.getKey()),
                    round(averageOf(entry.getValue())), entry.getValue().size()));
        }
        return top;
    }

    // 5) Buscar actividades por palabra clave (en el nombre o la descripción)
    public List<ActivityResponse> search(String keyword) {
        if (keyword == null || keyword.trim().length() < 2) {
            throw new IllegalArgumentException("La palabra clave (q) debe tener al menos 2 caracteres");
        }
        String q = keyword.trim();
        return activityRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByNameAsc(q, q)
                .stream().map(this::toActivity).toList();
    }

    // ===================== Conversión a JSON =====================

    private ActivityResponse toActivity(Activity activity) {
        List<Review> reviews = reviewRepository.findByBookingAvailabilityActivityIdOrderByIdDesc(activity.getId());
        Location l = activity.getLocation();
        LocationResponse location = l == null ? null : new LocationResponse(l.getDepartment(), l.getNeighborhood(),
                l.getStreet(), l.getNumber(), l.getLatitude(), l.getLongitude());
        Provider provider = activity.getProvider();
        return new ActivityResponse(activity.getId(), activity.getName(), activity.getDescription(), activity.getPrice(),
                activity.getDurationMinutes(), activity.getMinimumAge(), location,
                provider == null ? null : new ProviderRef(provider.getId(), provider.getBusinessName()),
                average(reviews), reviews.size(), "/activities/" + activity.getId());
    }

    private VerificationResponse toVerification(Provider provider) {
        // El RUT de una empresa es un dato público; la cédula de una persona física no se expone
        String taxId = provider instanceof JuridicProvider juridic ? juridic.getTaxId() : null;
        return new VerificationResponse(provider.getId(), provider.getBusinessName(), typeOf(provider),
                provider.getStatus() == null ? null : provider.getStatus().name(),
                provider.getStatus() == ProviderStatus.APPROVED, taxId,
                provider.getRegistrationDate() == null ? null : provider.getRegistrationDate().toLocalDate().toString());
    }

    private String typeOf(Provider provider) {
        return provider instanceof JuridicProvider ? "JURIDICA" : "FISICA";
    }

    // ===================== Validación de parámetros =====================

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            throw new IllegalArgumentException("Falta el parámetro date (formato AAAA-MM-DD, ej: 2026-11-07)");
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha debe tener formato AAAA-MM-DD (ej: 2026-11-07)");
        }
    }

    // Sin mes, se usa el mes actual
    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("El mes debe tener formato AAAA-MM (ej: 2026-10)");
        }
    }

    // Acepta el departamento en minúscula, sin tildes o con guiones ("rio-negro" -> "Río Negro")
    private String resolveDepartment(String department) {
        if (department == null || department.isBlank()) {
            throw new IllegalArgumentException("Falta el parámetro department (ej: rocha)");
        }
        String key = normalize(department);
        for (String name : ActivityService.DEPARTMENTS) {
            if (normalize(name).equals(key)) {
                return name;
            }
        }
        throw new IllegalArgumentException("No existe el departamento \"" + department + "\"");
    }

    // "Río-Negro" -> "rio negro": minúsculas, sin tildes, guiones como espacios
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String noAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.toLowerCase(Locale.ROOT).replaceAll("[-_]+", " ").replaceAll("\\s+", " ").trim();
    }

    // ===================== Promedios =====================

    private static Double average(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return null;
        }
        return round(reviews.stream().mapToInt(Review::getRating).average().orElse(0));
    }

    private static double averageOf(List<Integer> ratings) {
        return ratings.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    private static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
