package uy.edu.um.porytic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.um.porytic2026.entities.*;
import uy.edu.um.porytic2026.exceptions.BusinessException;
import uy.edu.um.porytic2026.repository.*;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Dar de baja y volver a publicar actividades, y que las reservas mantengan su precio
@ExtendWith(MockitoExtension.class)
class ProviderActivityPublicationTest {

    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private ActivityEquipmentRepository activityEquipmentRepository;
    @Mock
    private FileStorageService fileStorageService;

    private ProviderActivityService service;
    private Provider provider;
    private Activity activity;

    @BeforeEach
    void setUp() {
        service = new ProviderActivityService(activityRepository, tagRepository, equipmentRepository,
                activityEquipmentRepository, fileStorageService);
        provider = new FisicProvider();
        provider.setId(2L);
        activity = new Activity();
        activity.setId(1L);
        activity.setName("Kayak");
        activity.setProvider(provider);
        activity.setActive(true);
    }

    @Test
    void unpublishedActivitiesAreHiddenButKept() {
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityRepository.save(activity)).thenReturn(activity);

        service.unpublish(provider, 1L);

        assertFalse(activity.isPublished());
        verify(activityRepository, never()).delete(any()); // no se borra
    }

    @Test
    void anUnpublishedActivityCanBePublishedAgain() {
        activity.setActive(false);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityRepository.save(activity)).thenReturn(activity);

        service.publish(provider, 1L);

        assertTrue(activity.isPublished());
    }

    @Test
    void aProviderCannotUnpublishAnotherProvidersActivity() {
        Provider other = new FisicProvider();
        other.setId(3L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        assertThrows(BusinessException.class, () -> service.unpublish(other, 1L));
        assertTrue(activity.isPublished());
    }

    @Test
    void oldActivitiesWithoutStateArePublished() {
        activity.setActive(null);
        assertTrue(activity.isPublished());
    }

    @Test
    void aBookingKeepsThePriceItHadWhenItWasMade() {
        activity.setPrice(1000.0);
        Availability date = Availability.builder().id(10L).activity(activity).build();
        Booking booking = Booking.builder().availability(date).participantCount(2).unitPrice(1000.0)
                .bookingDate(LocalDateTime.now()).build();

        activity.setPrice(1500.0); // el prestador sube el precio después

        assertEquals(2000.0, booking.getTotalPrice());
    }
}
