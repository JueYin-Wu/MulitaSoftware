package uy.edu.um.porytic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import uy.edu.um.porytic2026.dto.ActivityForm;
import uy.edu.um.porytic2026.entities.*;
import uy.edu.um.porytic2026.exceptions.BusinessException;
import uy.edu.um.porytic2026.repository.ActivityEquipmentRepository;
import uy.edu.um.porytic2026.repository.ActivityRepository;
import uy.edu.um.porytic2026.repository.EquipmentRepository;
import uy.edu.um.porytic2026.repository.TagRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Tests unitarios del alta de actividades del prestador (ProviderActivityService.create).
// Los repositorios y el guardado de archivos se reemplazan por mocks: no se usa la base ni el disco.
@ExtendWith(MockitoExtension.class)
class ProviderActivityServiceTest {

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

    @InjectMocks
    private ProviderActivityService providerActivityService;

    private Provider provider;

    @BeforeEach
    void setUp() {
        provider = new Provider();
        provider.setId(10L);
    }

    // ===================== Casos válidos =====================

    @Test
    void createSavesActivityWithItsDataAndLocation() {
        returnSavedActivity();

        Activity created = providerActivityService.create(provider, validForm(), List.of());

        assertEquals(100L, created.getId());
        assertSame(provider, created.getProvider());
        assertEquals("Kayak en la laguna", created.getName());
        assertEquals(1500.0, created.getPrice());
        assertEquals(150, created.getDurationMinutes());
        assertEquals("Rocha", created.getLocation().getDepartment());
        assertEquals("La Paloma", created.getLocation().getNeighborhood());
        assertNull(created.getLocation().getStreet()); // venía con espacios: se guarda vacío
        assertNull(created.getImageUrl());
        assertTrue(created.getImages().isEmpty());
    }

    @Test
    void createKeepsOnlyApprovedTags() {
        ActivityForm form = validForm();
        form.setTagIds(new ArrayList<>(List.of(1L, 2L)));
        Tag approved = Tag.builder().id(1L).name("Senderismo").status(TagStatus.APPROVED).build();
        Tag rejected = Tag.builder().id(2L).name("Otro").status(TagStatus.REJECTED).build();
        when(tagRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(approved, rejected));
        returnSavedActivity();

        Activity created = providerActivityService.create(provider, form, List.of());

        assertEquals(List.of(approved), created.getTags());
    }

    @Test
    void createWithNewProposedTagSavesItAsPending() {
        ActivityForm form = validForm();
        form.setProposedTag("  Avistaje   de aves ");
        when(tagRepository.findFirstByNameIgnoreCase("Avistaje de aves")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            tag.setId(7L);
            return tag;
        });
        returnSavedActivity();

        Activity created = providerActivityService.create(provider, form, List.of());

        ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(captor.capture());
        Tag proposed = captor.getValue();
        assertEquals("Avistaje de aves", proposed.getName());
        assertEquals(TagStatus.PENDING, proposed.getStatus());
        assertSame(provider, proposed.getProposedBy());
        assertNotNull(proposed.getProposalDate());
        assertEquals(1, created.getTags().size());
    }

    @Test
    void createWithProposedTagThatAlreadyExistsReusesIt() {
        ActivityForm form = validForm();
        form.setProposedTag("senderismo");
        Tag existing = Tag.builder().id(1L).name("Senderismo").status(TagStatus.APPROVED).build();
        when(tagRepository.findFirstByNameIgnoreCase("senderismo")).thenReturn(Optional.of(existing));
        returnSavedActivity();

        Activity created = providerActivityService.create(provider, form, List.of());

        assertEquals(List.of(existing), created.getTags());
        verify(tagRepository, never()).save(any());
    }

    @Test
    void createSavesProvidedAndRequiredEquipment() {
        ActivityForm form = validForm();
        form.setProvidedEquipmentIds(new ArrayList<>(List.of(5L)));
        form.setRequiredEquipmentIds(new ArrayList<>(List.of(6L)));
        Equipment lifeJacket = Equipment.builder().id(5L).name("Chaleco salvavidas").build();
        Equipment water = Equipment.builder().id(6L).name("Agua").build();
        when(equipmentRepository.findAllById(List.of(5L))).thenReturn(List.of(lifeJacket));
        when(equipmentRepository.findAllById(List.of(6L))).thenReturn(List.of(water));
        returnSavedActivity();

        providerActivityService.create(provider, form, List.of());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ActivityEquipment>> captor = ArgumentCaptor.forClass(List.class);
        verify(activityEquipmentRepository).saveAll(captor.capture());
        List<ActivityEquipment> items = captor.getValue();
        assertEquals(2, items.size());
        assertEquals(EquipmentRole.PROVIDED, items.get(0).getRole());
        assertSame(lifeJacket, items.get(0).getEquipment());
        assertEquals(EquipmentRole.REQUIRED, items.get(1).getRole());
        assertSame(water, items.get(1).getEquipment());
    }

    @Test
    void createStoresPhotosAndUsesTheFirstOneForTheCard() {
        MultipartFile photo1 = image("uno.jpg");
        MultipartFile photo2 = image("dos.jpg");
        MultipartFile empty = new MockMultipartFile("photos", "", "application/octet-stream", new byte[0]);
        when(fileStorageService.storeImage(photo1, "activities", "activity-p10")).thenReturn("/uploads/activities/a.jpg");
        when(fileStorageService.storeImage(photo2, "activities", "activity-p10")).thenReturn("/uploads/activities/b.jpg");
        returnSavedActivity();

        Activity created = providerActivityService.create(provider, validForm(), List.of(photo1, photo2, empty));

        assertEquals("/uploads/activities/a.jpg", created.getImageUrl());
        assertEquals(List.of("/uploads/activities/a.jpg", "/uploads/activities/b.jpg"), created.getImages());
    }

    @Test
    void createNotRefundableDoesNotKeepRefundData() {
        ActivityForm form = validForm();
        form.setRefundable(false);
        form.setRefundPercentage(50);
        form.setCancellationMinutes(1440);
        returnSavedActivity();

        Activity created = providerActivityService.create(provider, form, List.of());

        assertFalse(created.getRefundable());
        assertNull(created.getRefundPercentage());
        assertNull(created.getCancellationMinutes());
    }

    // ===================== Casos inválidos =====================

    @Test
    void createWithUnknownDepartmentIsRejected() {
        ActivityForm form = validForm();
        form.setDepartment("Buenos Aires");

        BusinessException e = assertThrows(BusinessException.class,
                () -> providerActivityService.create(provider, form, List.of()));

        assertEquals("department", e.getField());
        verify(activityRepository, never()).save(any());
    }

    @Test
    void createRefundableWithoutPercentageIsRejected() {
        ActivityForm form = validForm();
        form.setRefundable(true);
        form.setCancellationMinutes(1440);
        form.setRefundPercentage(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> providerActivityService.create(provider, form, List.of()));

        assertEquals("refundPercentage", e.getField());
    }

    @Test
    void createRefundableWithoutCancellationDeadlineIsRejected() {
        ActivityForm form = validForm();
        form.setRefundable(true);
        form.setRefundPercentage(80);
        form.setCancellationMinutes(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> providerActivityService.create(provider, form, List.of()));

        assertEquals("cancellationMinutes", e.getField());
    }

    @Test
    void createWithSameEquipmentProvidedAndRequiredIsRejected() {
        ActivityForm form = validForm();
        form.setProvidedEquipmentIds(new ArrayList<>(List.of(5L)));
        form.setRequiredEquipmentIds(new ArrayList<>(List.of(5L)));

        BusinessException e = assertThrows(BusinessException.class,
                () -> providerActivityService.create(provider, form, List.of()));

        assertEquals("requiredEquipmentIds", e.getField());
        verify(activityRepository, never()).save(any());
    }

    @Test
    void createWithRejectedProposedTagIsRejected() {
        ActivityForm form = validForm();
        form.setProposedTag("Fiesta");
        Tag rejected = Tag.builder().id(3L).name("Fiesta").status(TagStatus.REJECTED).build();
        when(tagRepository.findFirstByNameIgnoreCase("Fiesta")).thenReturn(Optional.of(rejected));

        BusinessException e = assertThrows(BusinessException.class,
                () -> providerActivityService.create(provider, form, List.of()));

        assertEquals("proposedTag", e.getField());
        verify(activityRepository, never()).save(any());
    }

    @Test
    void createWithTooManyPhotosIsRejectedWithoutStoringAny() {
        List<MultipartFile> photos = new ArrayList<>();
        for (int i = 0; i <= ProviderActivityService.MAX_PHOTOS; i++) {
            photos.add(image("foto" + i + ".jpg"));
        }

        BusinessException e = assertThrows(BusinessException.class,
                () -> providerActivityService.create(provider, validForm(), photos));

        assertEquals("photos", e.getField());
        verify(fileStorageService, never()).storeImage(any(), anyString(), anyString());
    }

    @Test
    void createWithAnInvalidPhotoDeletesThePhotosAlreadyStored() {
        MultipartFile ok = image("bien.jpg");
        MultipartFile bad = new MockMultipartFile("photos", "documento.pdf", "application/pdf", new byte[]{1, 2});
        when(fileStorageService.storeImage(ok, "activities", "activity-p10")).thenReturn("/uploads/activities/ok.jpg");
        when(fileStorageService.storeImage(bad, "activities", "activity-p10"))
                .thenThrow(new IllegalArgumentException("La imagen tiene que ser JPG, PNG o WEBP"));

        BusinessException e = assertThrows(BusinessException.class,
                () -> providerActivityService.create(provider, validForm(), List.of(ok, bad)));

        assertEquals("photos", e.getField());
        assertTrue(e.getMessage().contains("documento.pdf"));
        verify(fileStorageService).delete("/uploads/activities/ok.jpg");
        verify(activityRepository, never()).save(any());
    }

    @Test
    void durationLabelIsFormattedInHoursAndMinutes() {
        Activity activity = new Activity();
        activity.setDurationMinutes(150);
        assertEquals("2 h 30 min", activity.getDurationLabel());
        activity.setDurationMinutes(120);
        assertEquals("2 h", activity.getDurationLabel());
        activity.setDurationMinutes(45);
        assertEquals("45 min", activity.getDurationLabel());
    }

    // ===================== Métodos auxiliares =====================

    private void returnSavedActivity() {
        when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> {
            Activity activity = invocation.getArgument(0);
            activity.setId(100L);
            return activity;
        });
    }

    private MultipartFile image(String name) {
        return new MockMultipartFile("photos", name, "image/jpeg", new byte[]{1, 2, 3});
    }

    private ActivityForm validForm() {
        ActivityForm form = new ActivityForm();
        form.setName("Kayak en la laguna");
        form.setDescription("Salida en kayak doble para ver el atardecer.");
        form.setPrice(1500.0);
        form.setDurationMinutes(150);
        form.setMinimumAge(12);
        form.setDepartment("Rocha");
        form.setNeighborhood("La Paloma");
        form.setStreet("  ");
        form.setBookingDeadline(60);
        form.setRefundable(false);
        return form;
    }
}
