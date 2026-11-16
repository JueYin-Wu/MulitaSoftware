package uy.edu.um.porytic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.um.porytic2026.dto.ProfileForm;
import uy.edu.um.porytic2026.entities.FisicProvider;
import uy.edu.um.porytic2026.entities.Tourist;
import uy.edu.um.porytic2026.exceptions.RegistrationException;
import uy.edu.um.porytic2026.repository.UserRepository;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Tests unitarios de ProfileService ("Editar perfil"). "Hoy" es el 16/11/2026.
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    private ProfileService profileService;
    private Tourist tourist;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 11, 16, 12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        profileService = new ProfileService(userRepository, clock);

        tourist = new Tourist();
        tourist.setId(5L);
        tourist.setFirstName("Sofía");
        tourist.setLastName("Pérez");
        tourist.setEmail("sofia@test.com");
        tourist.setBirthDate(LocalDate.of(1998, 4, 12));
    }

    private ProfileForm touristForm() {
        ProfileForm form = profileService.toForm(tourist);
        form.setPhone("099 123 456");
        return form;
    }

    @Test
    void aTouristCanChangeTheirData() {
        ProfileForm form = touristForm();
        form.setLastName("  Gómez ");
        form.setEmail("Sofia.Nueva@Test.com");
        form.setBirthDate("1999-01-20");
        when(userRepository.findByEmail("sofia.nueva@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        profileService.update(tourist, form);

        assertEquals("Gómez", tourist.getLastName());
        assertEquals("sofia.nueva@test.com", tourist.getEmail());
        assertEquals("099 123 456", tourist.getPhone());
        assertEquals(LocalDate.of(1999, 1, 20), tourist.getBirthDate());
    }

    @Test
    void theEmailOfAnotherAccountCannotBeUsed() {
        Tourist other = new Tourist();
        other.setId(9L);
        ProfileForm form = touristForm();
        form.setEmail("otro@test.com");
        when(userRepository.findByEmail("otro@test.com")).thenReturn(Optional.of(other));

        RegistrationException e = assertThrows(RegistrationException.class, () -> profileService.update(tourist, form));
        assertEquals("email", e.getField());
        assertEquals("sofia@test.com", tourist.getEmail()); // no cambió nada
        verify(userRepository, never()).save(any());
    }

    @Test
    void aBirthDateInTheFutureIsRejected() {
        ProfileForm form = touristForm();
        form.setBirthDate("2030-01-01");

        RegistrationException e = assertThrows(RegistrationException.class, () -> profileService.update(tourist, form));
        assertEquals("birthDate", e.getField());
        verify(userRepository, never()).save(any());
    }

    @Test
    void aProviderMustKeepTheAddressOfTheirBusiness() {
        FisicProvider provider = new FisicProvider();
        provider.setId(7L);
        provider.setFirstName("Martín");
        provider.setLastName("Silva");
        provider.setEmail("martin@test.com");
        provider.setPhone("098 000 000");
        provider.setBusinessName("Kayaks del Este");
        provider.setAddress("Rambla 123, La Paloma");

        ProfileForm form = profileService.toForm(provider);
        form.setAddress("  ");

        RegistrationException e = assertThrows(RegistrationException.class, () -> profileService.update(provider, form));
        assertEquals("address", e.getField());
        assertEquals("Rambla 123, La Paloma", provider.getAddress());
    }

    @Test
    void aProviderCanUpdateTheirBusiness() {
        FisicProvider provider = new FisicProvider();
        provider.setId(7L);
        provider.setFirstName("Martín");
        provider.setLastName("Silva");
        provider.setEmail("martin@test.com");
        provider.setPhone("098 000 000");
        provider.setBusinessName("Kayaks del Este");
        provider.setAddress("Rambla 123, La Paloma");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProfileForm form = profileService.toForm(provider);
        form.setBusinessName("Kayaks La Paloma");
        form.setServiceDescription("Salidas en kayak al atardecer");
        profileService.update(provider, form);

        assertEquals("Kayaks La Paloma", provider.getBusinessName());
        assertEquals("Salidas en kayak al atardecer", provider.getServiceDescription());
    }
}
