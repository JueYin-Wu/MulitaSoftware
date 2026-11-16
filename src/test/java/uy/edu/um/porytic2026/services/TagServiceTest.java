package uy.edu.um.porytic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.um.porytic2026.entities.Tag;
import uy.edu.um.porytic2026.entities.TagStatus;
import uy.edu.um.porytic2026.repository.ActivityRepository;
import uy.edu.um.porytic2026.repository.TagRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Tests unitarios del ABM de tipos de actividad del administrador (TagService)
// y de la revisión de los tipos que proponen los prestadores.
@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;
    @Mock
    private ActivityRepository activityRepository;

    private TagService tagService;

    @BeforeEach
    void setUp() {
        tagService = new TagService(tagRepository, activityRepository);
    }

    // ===================== Alta =====================

    @Test
    void aTypeCreatedByTheAdminIsApprovedDirectly() {
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tag tag = tagService.create("  Avistaje   de aves ");

        assertEquals("Avistaje de aves", tag.getName()); // sin espacios de más
        assertEquals(TagStatus.APPROVED, tag.getStatus());
    }

    @Test
    void twoTypesCannotHaveTheSameName() {
        when(tagRepository.existsByNameIgnoreCase("Senderismo")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> tagService.create("Senderismo"));
        verify(tagRepository, never()).save(any());
    }

    @Test
    void theNameMustHaveBetweenTwoAndFiftyCharacters() {
        assertThrows(IllegalArgumentException.class, () -> tagService.create("A"));
        assertThrows(IllegalArgumentException.class, () -> tagService.create("x".repeat(TagService.MAX_NAME + 1)));
        assertThrows(IllegalArgumentException.class, () -> tagService.create(null));
        verify(tagRepository, never()).save(any());
    }

    // ===================== Modificación =====================

    @Test
    void renameChangesTheName() {
        Tag tag = tag(1L, "Senderismo", TagStatus.APPROVED);
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagRepository.save(tag)).thenReturn(tag);

        assertEquals("Trekking", tagService.rename(1L, "Trekking").getName());
    }

    @Test
    void renameCannotUseTheNameOfAnotherType() {
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag(1L, "Senderismo", TagStatus.APPROVED)));
        when(tagRepository.existsByNameIgnoreCaseAndIdNot("Aventura", 1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> tagService.rename(1L, "Aventura"));
        verify(tagRepository, never()).save(any());
    }

    // ===================== Baja =====================

    @Test
    void aTypeUsedByActivitiesCannotBeDeleted() {
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag(1L, "Senderismo", TagStatus.APPROVED)));
        when(activityRepository.countByTagsId(1L)).thenReturn(3L);

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> tagService.delete(1L));

        assertTrue(e.getMessage().contains("3 actividades"));
        verify(tagRepository, never()).delete(any());
    }

    @Test
    void anUnusedTypeIsDeleted() {
        Tag tag = tag(1L, "Senderismo", TagStatus.APPROVED);
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(activityRepository.countByTagsId(1L)).thenReturn(0L);

        tagService.delete(1L);

        verify(tagRepository).delete(tag);
    }

    // ===================== Propuestas de los prestadores =====================

    @Test
    void approvingAProposalMakesItAvailable() {
        Tag tag = tag(5L, "Avistaje de aves", TagStatus.PENDING);
        when(tagRepository.findById(5L)).thenReturn(Optional.of(tag));
        when(tagRepository.save(tag)).thenReturn(tag);

        assertEquals(TagStatus.APPROVED, tagService.approve(5L).getStatus());
    }

    @Test
    void rejectingAProposalSavesTheReason() {
        Tag tag = tag(5L, "Fiesta", TagStatus.PENDING);
        when(tagRepository.findById(5L)).thenReturn(Optional.of(tag));
        when(tagRepository.save(tag)).thenReturn(tag);

        Tag rejected = tagService.reject(5L, " No es una actividad turística ");

        assertEquals(TagStatus.REJECTED, rejected.getStatus());
        assertEquals("No es una actividad turística", rejected.getRejectionReason());
    }

    @Test
    void aProposalAlreadyReviewedCannotBeReviewedAgain() {
        when(tagRepository.findById(5L)).thenReturn(Optional.of(tag(5L, "Senderismo", TagStatus.APPROVED)));

        assertThrows(IllegalStateException.class, () -> tagService.reject(5L, "motivo"));
        verify(tagRepository, never()).save(any());
    }

    private static Tag tag(Long id, String name, TagStatus status) {
        return Tag.builder().id(id).name(name).status(status).build();
    }
}
