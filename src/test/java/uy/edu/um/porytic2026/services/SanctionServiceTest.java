package uy.edu.um.porytic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.um.porytic2026.entities.*;
import uy.edu.um.porytic2026.exceptions.BusinessException;
import uy.edu.um.porytic2026.repository.ProviderRepository;
import uy.edu.um.porytic2026.repository.SanctionRepository;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Tests unitarios de SanctionService (advertencias y suspensiones a prestadores).
// Se usa un reloj fijo: "ahora" es el 16/11/2026 a las 10:00.
@ExtendWith(MockitoExtension.class)
class SanctionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 16, 10, 0);

    @Mock
    private SanctionRepository sanctionRepository;

    @Mock
    private ProviderRepository providerRepository;

    private SanctionService sanctionService;

    private Provider provider;
    private Administrator admin;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        sanctionService = new SanctionService(sanctionRepository, providerRepository, fixedClock);

        provider = new FisicProvider();
        provider.setId(10L);
        provider.setBusinessName("Aventuras del Este");
        provider.setStatus(ProviderStatus.APPROVED);

        admin = new Administrator();
        admin.setId(1L);
    }

    // ===================== Aplicar =====================

    @Test
    void warningIsSavedWithDateAndAdminAndDoesNotSuspend() {
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));
        when(sanctionRepository.save(any(Sanction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sanction sanction = sanctionService.apply(10L, admin, SanctionType.WARNING,
                "  Llegó 40 minutos tarde a la salida del sábado  ");

        assertEquals(SanctionType.WARNING, sanction.getType());
        assertEquals("Llegó 40 minutos tarde a la salida del sábado", sanction.getReason());
        assertEquals(NOW, sanction.getDate());
        assertSame(admin, sanction.getAdministrator());
        assertSame(provider, sanction.getProvider());
        assertFalse(provider.isSuspended());
        assertTrue(provider.isActive());
        verify(providerRepository, never()).save(any());
    }

    @Test
    void suspensionMarksTheProviderAsSuspended() {
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));
        when(sanctionRepository.save(any(Sanction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sanction sanction = sanctionService.apply(10L, admin, SanctionType.SUSPENSION,
                "Canceló tres salidas sin avisar a los turistas");

        assertTrue(sanction.isActiveSuspension());
        assertTrue(provider.isSuspended());
        assertFalse(provider.isActive());
        verify(providerRepository).save(provider);
    }

    @Test
    void reasonIsRequired() {
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));

        BusinessException e = assertThrows(BusinessException.class,
                () -> sanctionService.apply(10L, admin, SanctionType.WARNING, "   "));

        assertEquals("reason", e.getField());
        verify(sanctionRepository, never()).save(any());
    }

    @Test
    void reasonTooLongIsRejected() {
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));
        String longReason = "x".repeat(SanctionService.MAX_REASON_LENGTH + 1);

        BusinessException e = assertThrows(BusinessException.class,
                () -> sanctionService.apply(10L, admin, SanctionType.WARNING, longReason));

        assertEquals("reason", e.getField());
    }

    @Test
    void typeIsRequired() {
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));

        BusinessException e = assertThrows(BusinessException.class,
                () -> sanctionService.apply(10L, admin, null, "Motivo suficientemente largo"));

        assertEquals("type", e.getField());
    }

    @Test
    void aProviderThatIsNotApprovedCannotBeSanctioned() {
        provider.setStatus(ProviderStatus.PENDING);
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));

        assertThrows(BusinessException.class,
                () -> sanctionService.apply(10L, admin, SanctionType.WARNING, "Motivo suficientemente largo"));
        verify(sanctionRepository, never()).save(any());
    }

    @Test
    void anAlreadySuspendedProviderCannotBeSuspendedAgain() {
        provider.setSuspended(true);
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));

        BusinessException e = assertThrows(BusinessException.class,
                () -> sanctionService.apply(10L, admin, SanctionType.SUSPENSION, "Otra suspensión por lo mismo"));

        assertEquals("type", e.getField());
        verify(sanctionRepository, never()).save(any());
    }

    @Test
    void aSuspendedProviderCanStillReceiveAWarning() {
        provider.setSuspended(true);
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));
        when(sanctionRepository.save(any(Sanction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sanction sanction = sanctionService.apply(10L, admin, SanctionType.WARNING, "Respondió mal a un turista");

        assertEquals(SanctionType.WARNING, sanction.getType());
        assertTrue(provider.isSuspended());
    }

    @Test
    void unknownProviderIsRejected() {
        when(providerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> sanctionService.apply(99L, admin, SanctionType.WARNING, "Motivo suficientemente largo"));
    }

    // ===================== Levantar la suspensión =====================

    @Test
    void liftSuspensionReactivatesTheProviderAndSavesTheDate() {
        provider.setSuspended(true);
        Sanction suspension = Sanction.builder().id(3L).provider(provider).administrator(admin)
                .type(SanctionType.SUSPENSION).reason("Canceló salidas sin avisar").date(NOW.minusDays(5)).build();
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));
        when(sanctionRepository.findFirstByProviderIdAndTypeAndLiftedDateIsNullOrderByDateDesc(10L, SanctionType.SUSPENSION))
                .thenReturn(Optional.of(suspension));

        sanctionService.liftSuspension(10L);

        assertEquals(NOW, suspension.getLiftedDate());
        assertFalse(suspension.isActiveSuspension());
        assertFalse(provider.isSuspended());
        assertTrue(provider.isActive());
        verify(providerRepository).save(provider);
    }

    @Test
    void liftWhenNotSuspendedIsRejected() {
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));

        assertThrows(BusinessException.class, () -> sanctionService.liftSuspension(10L));
        verify(providerRepository, never()).save(any());
    }

    // ===================== Efecto sobre las actividades =====================

    @Test
    void activitiesOfASuspendedProviderAreNotVisibleToTourists() {
        Activity activity = new Activity();
        activity.setProvider(provider);
        assertTrue(activity.isVisibleToTourists());

        provider.setSuspended(true);
        assertFalse(activity.isVisibleToTourists());
        // Sigue "publicada" para el prestador: cuando se levante la suspensión vuelve a mostrarse sola
        assertTrue(activity.isPublished());
    }
}
