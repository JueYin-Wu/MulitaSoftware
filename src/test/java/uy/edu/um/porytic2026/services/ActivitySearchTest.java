package uy.edu.um.porytic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.um.porytic2026.entities.*;
import uy.edu.um.porytic2026.repository.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

// Tests unitarios de RF05 - Búsqueda de actividades (ActivityService.search):
// filtros por texto, destino, tipo de actividad, fecha y precio, y el orden de los resultados.
@ExtendWith(MockitoExtension.class)
class ActivitySearchTest {

    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private AvailabilityRepository availabilityRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ActivityEquipmentRepository activityEquipmentRepository;
    @Mock
    private TagRepository tagRepository;

    private ActivityService activityService;

    private Provider provider;
    private Tag trekking;
    private Tag water;
    private Activity hiking;     // Lavalleja, $1800, senderismo
    private Activity kayak;      // Rocha, $1500, acuática
    private Activity cityTour;   // Montevideo, $900, senderismo

    @BeforeEach
    void setUp() {
        activityService = new ActivityService(activityRepository, availabilityRepository, bookingRepository,
                reviewRepository, activityEquipmentRepository, tagRepository);

        provider = new FisicProvider();
        provider.setId(10L);
        provider.setStatus(ProviderStatus.APPROVED);
        trekking = Tag.builder().id(1L).name("Senderismo").status(TagStatus.APPROVED).build();
        water = Tag.builder().id(2L).name("Actividad acuática").status(TagStatus.APPROVED).build();

        hiking = activity(1L, "Senderismo en las sierras", "Lavalleja", 1800.0, trekking);
        kayak = activity(2L, "Kayak al atardecer", "Rocha", 1500.0, water);
        cityTour = activity(3L, "Recorrido por Ciudad Vieja", "Montevideo", 900.0, trekking);
    }

    // ===================== Filtros =====================

    @Test
    void withoutFiltersAllVisibleActivitiesAreReturned() {
        when(activityRepository.findAll()).thenReturn(List.of(hiking, kayak, cityTour));

        assertEquals(List.of(hiking, kayak, cityTour), activityService.search(null, null, null, null, null, null));
    }

    @Test
    void activitiesThatWereUnpublishedOrWhoseProviderIsSuspendedAreHidden() {
        kayak.setActive(false); // el prestador la dio de baja
        Provider suspended = new FisicProvider();
        suspended.setId(11L);
        suspended.setStatus(ProviderStatus.APPROVED);
        suspended.setSuspended(true);
        cityTour.setProvider(suspended);
        when(activityRepository.findAll()).thenReturn(List.of(hiking, kayak, cityTour));

        assertEquals(List.of(hiking), activityService.search(null, null, null, null, null, null));
    }

    @Test
    void searchByTextUsesTheNameWithoutExtraSpaces() {
        when(activityRepository.findByNameContainingIgnoreCase("kayak")).thenReturn(List.of(kayak));

        assertEquals(List.of(kayak), activityService.search("  kayak ", null, null, null, null, null));
    }

    @Test
    void searchByDestination() {
        when(activityRepository.findByLocationDepartment("Rocha")).thenReturn(List.of(kayak));

        assertEquals(List.of(kayak), activityService.search(null, "Rocha", null, null, null, null));
    }

    @Test
    void searchByTextAndDestinationTogether() {
        when(activityRepository.findByNameContainingIgnoreCaseAndLocationDepartment("sierras", "Lavalleja"))
                .thenReturn(List.of(hiking));

        assertEquals(List.of(hiking), activityService.search("sierras", "Lavalleja", null, null, null, null));
    }

    @Test
    void filterByActivityType() {
        when(activityRepository.findAll()).thenReturn(List.of(hiking, kayak, cityTour));

        assertEquals(List.of(hiking, cityTour), activityService.search(null, null, trekking.getId(), null, null, null));
    }

    @Test
    void filterByDateKeepsOnlyActivitiesWithADateThatDay() {
        LocalDate day = LocalDate.of(2026, 11, 21);
        when(activityRepository.findAll()).thenReturn(List.of(hiking, kayak, cityTour));
        when(availabilityRepository.findByDateOrderByTimeAsc(day)).thenReturn(List.of(
                Availability.builder().id(50L).activity(kayak).date(day).time(LocalTime.of(18, 30)).capacity(8).build()));

        assertEquals(List.of(kayak), activityService.search(null, null, null, day, null, null));
    }

    @Test
    void filterByMaximumPriceIncludesTheExactPrice() {
        when(activityRepository.findAll()).thenReturn(List.of(hiking, kayak, cityTour));

        assertEquals(List.of(kayak, cityTour), activityService.search(null, null, null, null, 1500.0, null));
    }

    @Test
    void severalFiltersAreCombined() {
        when(activityRepository.findAll()).thenReturn(List.of(hiking, kayak, cityTour));

        // Senderismo y hasta $1000: solo el recorrido por Ciudad Vieja
        assertEquals(List.of(cityTour), activityService.search(null, null, trekking.getId(), null, 1000.0, null));
    }

    // ===================== Orden =====================

    @Test
    void sortByPrice() {
        when(activityRepository.findAll()).thenReturn(List.of(hiking, kayak, cityTour));

        assertEquals(List.of(cityTour, kayak, hiking), activityService.search(null, null, null, null, null, "price_asc"));
        assertEquals(List.of(hiking, kayak, cityTour), activityService.search(null, null, null, null, null, "price_desc"));
    }

    @Test
    void sortByName() {
        when(activityRepository.findAll()).thenReturn(List.of(hiking, kayak, cityTour));

        assertEquals(List.of(kayak, cityTour, hiking), activityService.search(null, null, null, null, null, "name"));
    }

    @Test
    void sortByRatingPutsTheBestRatedFirstAndTheOnesWithoutReviewsLast() {
        when(activityRepository.findAll()).thenReturn(List.of(hiking, kayak, cityTour));
        when(reviewRepository.findByBookingAvailabilityActivityIdOrderByIdDesc(1L)).thenReturn(reviews(4, 4)); // 4.0
        when(reviewRepository.findByBookingAvailabilityActivityIdOrderByIdDesc(2L)).thenReturn(reviews(5, 4)); // 4.5
        when(reviewRepository.findByBookingAvailabilityActivityIdOrderByIdDesc(3L)).thenReturn(List.of());    // sin reseñas

        assertEquals(List.of(kayak, hiking, cityTour), activityService.search(null, null, null, null, null, "rating"));
    }

    @Test
    void averageRatingIsNullWithoutReviews() {
        assertNull(activityService.averageRating(List.of()));
        assertEquals(4.5, activityService.averageRating(reviews(5, 4)));
    }

    // ===================== Métodos auxiliares =====================

    private Activity activity(Long id, String name, String department, Double price, Tag tag) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setName(name);
        activity.setPrice(price);
        activity.setLocation(new Location(department, "Centro", null, null));
        activity.setProvider(provider);
        activity.setTags(new ArrayList<>(List.of(tag)));
        return activity;
    }

    private List<Review> reviews(int... ratings) {
        List<Review> list = new ArrayList<>();
        for (int rating : ratings) {
            list.add(Review.builder().rating(rating).build());
        }
        return list;
    }
}
