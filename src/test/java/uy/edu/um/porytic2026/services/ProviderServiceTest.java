package uy.edu.um.porytic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import uy.edu.um.porytic2026.dto.ProviderRegistrationForm;
import uy.edu.um.porytic2026.entities.*;
import uy.edu.um.porytic2026.exceptions.RegistrationException;
import uy.edu.um.porytic2026.repository.FisicProviderRepository;
import uy.edu.um.porytic2026.repository.JuridicProviderRepository;
import uy.edu.um.porytic2026.repository.ProviderRepository;
import uy.edu.um.porytic2026.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// Tests unitarios de los casos de uso del prestador:
//  - CU01: registro de prestador (persona física o jurídica), que queda pendiente de revisión
//  - CU02: el administrador aprueba o rechaza la solicitud
@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

    @Mock
    private ProviderRepository providerRepository;
    @Mock
    private FisicProviderRepository fisicProviderRepository;
    @Mock
    private JuridicProviderRepository juridicProviderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private ProviderService providerService;

    @BeforeEach
    void setUp() {
        providerService = new ProviderService(providerRepository, fisicProviderRepository, juridicProviderRepository,
                userRepository, userService, passwordEncoder);
    }

    // ===================== CU01 - Registro de prestador =====================

    @Test
    void aPersonIsRegisteredAsAPendingFisicProvider() {
        when(userService.generateUsername("Martín", "Rodríguez")).thenReturn("martin.rodriguez");
        when(passwordEncoder.encode("Prestador1234!")).thenReturn("hash");
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Provider provider = providerService.register(fisicForm());

        FisicProvider fisic = assertInstanceOf(FisicProvider.class, provider);
        assertEquals(ProviderStatus.PENDING, fisic.getStatus());   // espera la revisión del admin
        assertEquals(DocumentType.CI, fisic.getDocumentType());
        assertEquals("12345672", fisic.getNationalId());
        assertEquals("martin.rodriguez", fisic.getUsername());
        assertEquals("hash", fisic.getPassword());
        assertEquals("martin@aventuras.com", fisic.getEmail());
        assertEquals("Aventuras del Este", fisic.getBusinessName());
        assertNotNull(fisic.getRegistrationDate());
        assertFalse(fisic.isActive());                              // no puede operar hasta que lo aprueben
    }

    @Test
    void aCompanyIsRegisteredAsAPendingJuridicProvider() {
        when(userService.generateUsername(anyString(), anyString())).thenReturn("martin.rodriguez");
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Provider provider = providerService.register(juridicForm());

        JuridicProvider juridic = assertInstanceOf(JuridicProvider.class, provider);
        assertEquals(ProviderStatus.PENDING, juridic.getStatus());
        assertEquals("211234560018", juridic.getTaxId());           // solo números
        assertEquals("Ana Pérez", juridic.getLegalRepresentative());
    }

    @Test
    void anEmailThatAlreadyHasAnAccountIsRejected() {
        when(userRepository.existsByEmail("martin@aventuras.com")).thenReturn(true);

        RegistrationException e = assertThrows(RegistrationException.class,
                () -> providerService.register(fisicForm()));

        assertEquals("email", e.getField());
        verify(providerRepository, never()).save(any());
    }

    @Test
    void aCiAlreadyRegisteredIsRejected() {
        when(fisicProviderRepository.existsByDocumentTypeAndNationalId(DocumentType.CI, "12345672")).thenReturn(true);

        RegistrationException e = assertThrows(RegistrationException.class,
                () -> providerService.register(fisicForm()));

        assertEquals("nationalId", e.getField());
        assertEquals("Cédula ya registrada", e.getMessage());
    }

    @Test
    void anInvalidCiIsRejected() {
        ProviderRegistrationForm form = fisicForm();
        form.setNationalId("1.234.567-9");

        RegistrationException e = assertThrows(RegistrationException.class, () -> providerService.register(form));

        assertEquals("nationalId", e.getField());
    }

    @Test
    void anInvalidRutIsRejected() {
        ProviderRegistrationForm form = juridicForm();
        form.setTaxId("123");

        RegistrationException e = assertThrows(RegistrationException.class, () -> providerService.register(form));

        assertEquals("taxId", e.getField());
        verify(providerRepository, never()).save(any());
    }

    @Test
    void aRutAlreadyRegisteredIsRejected() {
        when(juridicProviderRepository.existsByTaxId("211234560018")).thenReturn(true);

        RegistrationException e = assertThrows(RegistrationException.class,
                () -> providerService.register(juridicForm()));

        assertEquals("taxId", e.getField());
    }

    // ===================== CU02 - Revisión de la solicitud =====================

    @Test
    void approvingAPendingRequestLetsTheProviderOperate() {
        Provider provider = pendingProvider();
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));
        when(providerRepository.save(provider)).thenReturn(provider);

        Provider approved = providerService.approve(10L);

        assertEquals(ProviderStatus.APPROVED, approved.getStatus());
        assertNull(approved.getRejectionReason());
        assertTrue(approved.isActive());
    }

    @Test
    void rejectingARequestSavesTheReason() {
        Provider provider = pendingProvider();
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));
        when(providerRepository.save(provider)).thenReturn(provider);

        Provider rejected = providerService.reject(10L, "  Los datos del RUT no coinciden  ");

        assertEquals(ProviderStatus.REJECTED, rejected.getStatus());
        assertEquals("Los datos del RUT no coinciden", rejected.getRejectionReason());
    }

    @Test
    void theRejectionReasonIsOptional() {
        Provider provider = pendingProvider();
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));
        when(providerRepository.save(provider)).thenReturn(provider);

        assertNull(providerService.reject(10L, "   ").getRejectionReason());
    }

    @Test
    void aRequestAlreadyReviewedCannotBeReviewedAgain() {
        Provider provider = pendingProvider();
        provider.setStatus(ProviderStatus.APPROVED);
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));

        assertThrows(IllegalStateException.class, () -> providerService.reject(10L, "otro motivo"));
        verify(providerRepository, never()).save(any());
    }

    @Test
    void theRequestMustExist() {
        when(providerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> providerService.approve(99L));
    }

    // ===================== Perfil público =====================

    @Test
    void onlyActiveProvidersHaveAPublicProfile() {
        Provider provider = pendingProvider();
        when(providerRepository.findById(10L)).thenReturn(Optional.of(provider));

        // Pendiente: no tiene perfil público
        assertThrows(IllegalArgumentException.class, () -> providerService.findApprovedById(10L));

        // Aprobado: sí
        provider.setStatus(ProviderStatus.APPROVED);
        assertSame(provider, providerService.findApprovedById(10L));

        // Aprobado pero suspendido: no
        provider.setSuspended(true);
        assertThrows(IllegalArgumentException.class, () -> providerService.findApprovedById(10L));
    }

    // ===================== Métodos auxiliares =====================

    private ProviderRegistrationForm baseForm() {
        ProviderRegistrationForm form = new ProviderRegistrationForm();
        form.setFirstName("Martín");
        form.setLastName("Rodríguez");
        form.setEmail(" Martin@Aventuras.com ");
        form.setPhone("099 123 456");
        form.setPassword("Prestador1234!");
        form.setConfirmPassword("Prestador1234!");
        form.setBusinessName(" Aventuras del Este ");
        form.setAddress("Ruta 10 km 236, La Paloma");
        form.setServiceDescription("Salidas en kayak y caminatas guiadas");
        return form;
    }

    private ProviderRegistrationForm fisicForm() {
        ProviderRegistrationForm form = baseForm();
        form.setProviderType(ProviderRegistrationForm.FISIC);
        form.setDocumentType("CI");
        form.setNationalId("1.234.567-2");
        return form;
    }

    private ProviderRegistrationForm juridicForm() {
        ProviderRegistrationForm form = baseForm();
        form.setProviderType(ProviderRegistrationForm.JURIDIC);
        form.setTaxId("21.123456.0018");
        form.setLegalRepresentative(" Ana Pérez ");
        return form;
    }

    private Provider pendingProvider() {
        FisicProvider provider = new FisicProvider();
        provider.setId(10L);
        provider.setBusinessName("Aventuras del Este");
        provider.setStatus(ProviderStatus.PENDING);
        return provider;
    }
}
