package uy.edu.um.porytic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import uy.edu.um.porytic2026.entities.*;
import uy.edu.um.porytic2026.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

// Tests unitarios del inicio de sesión: AuthUserDetailsService le dice a Spring Security
// quién es el usuario, qué rol tiene y si puede entrar.
@ExtendWith(MockitoExtension.class)
class AuthUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private AuthUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new AuthUserDetailsService(userRepository);
    }

    @Test
    void anAdministratorGetsTheAdminRole() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(withCredentials(new Administrator(), "admin")));

        UserDetails details = service.loadUserByUsername("admin");

        assertEquals("ROLE_ADMIN", roleOf(details));
        assertTrue(details.isEnabled());
    }

    @Test
    void aTouristGetsTheTouristRole() {
        when(userRepository.findByUsername("sofia.gomez")).thenReturn(Optional.of(withCredentials(new Tourist(), "sofia.gomez")));

        assertEquals("ROLE_TOURIST", roleOf(service.loadUserByUsername("sofia.gomez")));
    }

    @Test
    void anApprovedProviderCanLogIn() {
        when(userRepository.findByUsername("prestador")).thenReturn(Optional.of(provider(ProviderStatus.APPROVED, false)));

        UserDetails details = service.loadUserByUsername("prestador");

        assertEquals("ROLE_PROVIDER", roleOf(details));
        assertTrue(details.isEnabled());
        assertTrue(details.isAccountNonLocked());
    }

    @Test
    void aProviderWaitingForApprovalCannotLogIn() {
        when(userRepository.findByUsername("prestador")).thenReturn(Optional.of(provider(ProviderStatus.PENDING, false)));

        assertFalse(service.loadUserByUsername("prestador").isEnabled());
    }

    @Test
    void aRejectedProviderCannotLogIn() {
        when(userRepository.findByUsername("prestador")).thenReturn(Optional.of(provider(ProviderStatus.REJECTED, false)));

        assertFalse(service.loadUserByUsername("prestador").isEnabled());
    }

    @Test
    void aSuspendedProviderIsLocked() {
        // La cuenta bloqueada hace que el login muestre el aviso de suspensión (SecurityConfig)
        when(userRepository.findByUsername("prestador")).thenReturn(Optional.of(provider(ProviderStatus.APPROVED, true)));

        assertFalse(service.loadUserByUsername("prestador").isAccountNonLocked());
    }

    @Test
    void youCanAlsoLogInWithTheEmail() {
        Tourist tourist = withCredentials(new Tourist(), "sofia.gomez");
        when(userRepository.findByEmail("sofia@correo.com")).thenReturn(Optional.of(tourist));

        UserDetails details = service.loadUserByUsername(" Sofia@Correo.com ");

        // Spring Security sigue identificando al usuario por su nombre de usuario
        assertEquals("sofia.gomez", details.getUsername());
    }

    @Test
    void anUnknownUserIsRejected() {
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("nadie"));
    }

    // ===================== Métodos auxiliares =====================

    private static <T extends User> T withCredentials(T user, String username) {
        user.setUsername(username);
        user.setPassword("hash");
        return user;
    }

    private static Provider provider(ProviderStatus status, boolean suspended) {
        FisicProvider provider = withCredentials(new FisicProvider(), "prestador");
        provider.setStatus(status);
        provider.setSuspended(suspended);
        return provider;
    }

    private static String roleOf(UserDetails details) {
        return details.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst().orElse(null);
    }
}
