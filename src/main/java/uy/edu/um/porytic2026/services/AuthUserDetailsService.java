package uy.edu.um.porytic2026.services;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import uy.edu.um.porytic2026.entities.Administrator;
import uy.edu.um.porytic2026.entities.Provider;
import uy.edu.um.porytic2026.entities.ProviderStatus;
import uy.edu.um.porytic2026.entities.User;
import uy.edu.um.porytic2026.repository.UserRepository;

// Spring Security llama a esta clase cuando alguien inicia sesión.
// Busca el usuario en nuestra base y le dice a Spring qué rol tiene.
@Service
public class AuthUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AuthUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        String value = login == null ? "" : login.trim();

        // Se puede entrar con el nombre de usuario o con el correo
        User user = userRepository.findByUsername(value)
                .or(() -> userRepository.findByEmail(value.toLowerCase()))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        // El rol sale del tipo de usuario (herencia de User)
        String role;
        boolean enabled = true;
        if (user instanceof Administrator) {
            role = "ADMIN";
        } else if (user instanceof Provider provider) {
            role = "PROVIDER";
            // Un proveedor solo puede entrar si el admin aprobó su solicitud
            enabled = provider.getStatus() == ProviderStatus.APPROVED;
        } else {
            role = "TOURIST";
        }

        // Este "User" es el de Spring Security, no nuestra entidad (por eso va con el paquete completo)
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(role)
                .disabled(!enabled)
                .build();
    }
}
