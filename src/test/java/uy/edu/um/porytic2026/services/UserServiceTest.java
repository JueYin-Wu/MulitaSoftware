package uy.edu.um.porytic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import uy.edu.um.porytic2026.dto.TouristRegistrationForm;
import uy.edu.um.porytic2026.entities.DocumentType;
import uy.edu.um.porytic2026.entities.Tourist;
import uy.edu.um.porytic2026.entities.User;
import uy.edu.um.porytic2026.exceptions.RegistrationException;
import uy.edu.um.porytic2026.repository.TouristRepository;
import uy.edu.um.porytic2026.repository.UserRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Tests unitarios del CU03 - Registro de turista (UserService.registerTourist)
// y de la generación del nombre de usuario, que también usa el registro de prestadores.
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TouristRepository touristRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, touristRepository, passwordEncoder);
    }

    // ===================== CU03 - Registro de turista =====================

    @Test
    void registerTouristSavesTheNormalizedDataAndTheEncryptedPassword() {
        when(passwordEncoder.encode("Turista1234!")).thenReturn("hash-de-la-contraseña");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tourist tourist = userService.registerTourist(validForm());

        assertEquals("Sofía", tourist.getFirstName());
        assertEquals("Gómez", tourist.getLastName());
        assertEquals("sofia.gomez@correo.com", tourist.getEmail());      // en minúsculas y sin espacios
        assertEquals("sofia.gomez", tourist.getUsername());              // sin tildes
        assertEquals("hash-de-la-contraseña", tourist.getPassword());    // nunca en texto plano
        assertEquals("099123456", tourist.getPhone());                   // solo números
        assertEquals(DocumentType.CI, tourist.getDocumentType());
        assertEquals("12345672", tourist.getIdDocument());               // sin puntos ni guion
        assertEquals(LocalDate.of(1999, 5, 10), tourist.getBirthDate());
    }

    @Test
    void anEmailThatAlreadyHasAnAccountIsRejected() {
        when(userRepository.existsByEmail("sofia.gomez@correo.com")).thenReturn(true);

        RegistrationException e = assertThrows(RegistrationException.class,
                () -> userService.registerTourist(validForm()));

        assertEquals("email", e.getField());
        verify(userRepository, never()).save(any());
    }

    @Test
    void aCiWithAWrongCheckDigitIsRejected() {
        TouristRegistrationForm form = validForm();
        form.setIdDocument("1.234.567-3");

        RegistrationException e = assertThrows(RegistrationException.class, () -> userService.registerTourist(form));

        assertEquals("idDocument", e.getField());
        verify(userRepository, never()).save(any());
    }

    @Test
    void aCiAlreadyRegisteredIsRejected() {
        // CU03 4b: "cédula ya registrada"
        when(touristRepository.existsByDocumentTypeAndIdDocument(DocumentType.CI, "12345672")).thenReturn(true);

        RegistrationException e = assertThrows(RegistrationException.class,
                () -> userService.registerTourist(validForm()));

        assertEquals("idDocument", e.getField());
        assertEquals("Cédula ya registrada", e.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void aForeignTouristCanRegisterWithAPassport() {
        TouristRegistrationForm form = validForm();
        form.setDocumentType("PASSPORT");
        form.setIdDocument("ab 1234567");
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tourist tourist = userService.registerTourist(form);

        assertEquals(DocumentType.PASSPORT, tourist.getDocumentType());
        assertEquals("AB1234567", tourist.getIdDocument());
    }

    @Test
    void anUnknownDocumentTypeIsRejected() {
        TouristRegistrationForm form = validForm();
        form.setDocumentType("DNI");

        RegistrationException e = assertThrows(RegistrationException.class, () -> userService.registerTourist(form));

        assertEquals("documentType", e.getField());
    }

    // ===================== Nombre de usuario =====================

    @Test
    void usernameIsNameDotLastNameWithoutAccents() {
        assertEquals("jose.nunez", userService.generateUsername("José", "Núñez"));
    }

    @Test
    void ifTheUsernameIsTakenANumberIsAdded() {
        when(userRepository.existsByUsername("jose.nunez")).thenReturn(true);
        when(userRepository.existsByUsername("jose.nunez1")).thenReturn(true);
        when(userRepository.existsByUsername("jose.nunez2")).thenReturn(false);

        assertEquals("jose.nunez2", userService.generateUsername("José", "Núñez"));
    }

    // ===================== Métodos auxiliares =====================

    private TouristRegistrationForm validForm() {
        TouristRegistrationForm form = new TouristRegistrationForm();
        form.setFirstName(" Sofía ");
        form.setLastName("Gómez");
        form.setEmail(" Sofia.Gomez@Correo.com ");
        form.setPhone("099 123 456");
        form.setDocumentType("CI");
        form.setIdDocument("1.234.567-2");
        form.setBirthDate("1999-05-10");
        form.setPassword("Turista1234!");
        form.setConfirmPassword("Turista1234!");
        return form;
    }
}
