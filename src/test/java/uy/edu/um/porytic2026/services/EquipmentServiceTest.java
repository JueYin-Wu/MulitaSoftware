package uy.edu.um.porytic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.um.porytic2026.entities.Equipment;
import uy.edu.um.porytic2026.repository.ActivityEquipmentRepository;
import uy.edu.um.porytic2026.repository.EquipmentRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Tests unitarios del ABM de equipamiento del administrador (EquipmentService)
@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private ActivityEquipmentRepository activityEquipmentRepository;

    private EquipmentService equipmentService;

    @BeforeEach
    void setUp() {
        equipmentService = new EquipmentService(equipmentRepository, activityEquipmentRepository);
    }

    @Test
    void createSavesTheNameAndTheOptionalDescription() {
        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Equipment withDescription = equipmentService.create(" Chaleco  salvavidas ", " Talles S a XL ");
        Equipment withoutDescription = equipmentService.create("Casco", "   ");

        assertEquals("Chaleco salvavidas", withDescription.getName());
        assertEquals("Talles S a XL", withDescription.getDescription());
        assertNull(withoutDescription.getDescription());
    }

    @Test
    void twoItemsCannotHaveTheSameName() {
        when(equipmentRepository.existsByNameIgnoreCase("Casco")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> equipmentService.create("Casco", null));
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void nameAndDescriptionHaveAMaximumLength() {
        assertThrows(IllegalArgumentException.class, () -> equipmentService.create("C", null));
        assertThrows(IllegalArgumentException.class,
                () -> equipmentService.create("Casco", "x".repeat(EquipmentService.MAX_DESCRIPTION + 1)));
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void updateChangesNameAndDescription() {
        Equipment equipment = Equipment.builder().id(1L).name("Casco").build();
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment));
        when(equipmentRepository.save(equipment)).thenReturn(equipment);

        Equipment updated = equipmentService.update(1L, "Casco de bicicleta", "Se entrega al llegar");

        assertEquals("Casco de bicicleta", updated.getName());
        assertEquals("Se entrega al llegar", updated.getDescription());
    }

    @Test
    void updateCannotUseTheNameOfAnotherItem() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(Equipment.builder().id(1L).name("Casco").build()));
        when(equipmentRepository.existsByNameIgnoreCaseAndIdNot("Agua", 1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> equipmentService.update(1L, "Agua", null));
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void anItemUsedByActivitiesCannotBeDeleted() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(Equipment.builder().id(1L).name("Casco").build()));
        when(activityEquipmentRepository.countByEquipmentId(1L)).thenReturn(1L);

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> equipmentService.delete(1L));

        assertTrue(e.getMessage().contains("1 actividad"));
        verify(equipmentRepository, never()).delete(any());
    }

    @Test
    void anUnusedItemIsDeleted() {
        Equipment equipment = Equipment.builder().id(1L).name("Casco").build();
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment));
        when(activityEquipmentRepository.countByEquipmentId(1L)).thenReturn(0L);

        equipmentService.delete(1L);

        verify(equipmentRepository).delete(equipment);
    }

    @Test
    void anItemThatDoesNotExistIsRejected() {
        when(equipmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> equipmentService.findById(99L));
    }
}
