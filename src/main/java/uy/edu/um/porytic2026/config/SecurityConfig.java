package uy.edu.um.porytic2026.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Páginas públicas: cualquiera puede verlas sin iniciar sesión
                        .requestMatchers("/", "/activities", "/login", "/register/**", "/error").permitAll()
                        // Archivos estáticos (estilos, scripts, imágenes)
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        // Consola de la base de datos H2 (solo para desarrollo)
                        .requestMatchers("/h2-console/**").permitAll()
                        // Cada portal solo para su rol
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/provider/**").hasRole("PROVIDER")
                        // Todo lo demás requiere haber iniciado sesión
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")   // nuestra página de login (templates/auth/login.html)
                        .successHandler((request, response, authentication) -> {
                            // Después de entrar, cada rol va a su portal
                            String target = "/";
                            for (GrantedAuthority authority : authentication.getAuthorities()) {
                                if (authority.getAuthority().equals("ROLE_ADMIN")) {
                                    target = "/admin/providers";
                                } else if (authority.getAuthority().equals("ROLE_PROVIDER")) {
                                    target = "/provider";
                                }
                            }
                            response.sendRedirect(request.getContextPath() + target);
                        })
                        .failureHandler((request, response, exception) -> {
                            // DisabledException = proveedor pendiente o rechazado
                            String reason = exception instanceof DisabledException ? "pending" : "error";
                            response.sendRedirect(request.getContextPath() + "/login?" + reason);
                        })
                        .permitAll()
                )
                .logout(logout -> logout.logoutSuccessUrl("/"))
                // La consola H2 necesita estas dos excepciones para funcionar
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    // Sirve para guardar las contraseñas encriptadas, nunca en texto plano
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
