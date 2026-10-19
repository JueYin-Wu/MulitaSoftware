package uy.edu.um.porytic2026.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import uy.edu.um.porytic2026.entities.*;
import uy.edu.um.porytic2026.repository.ActivityRepository;
import uy.edu.um.porytic2026.repository.UserRepository;

import java.time.LocalDateTime;

// Carga datos de prueba al arrancar la app (solo para desarrollo).
// La base H2 está en memoria, así que se vuelve a cargar cada vez que se reinicia.
@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UserRepository userRepository, ActivityRepository activityRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // si ya hay datos, no carga de nuevo
        }

        // Administrador para probar el panel (usuario: admin / contraseña: admin1234)
        Administrator admin = Administrator.builder()
                .firstName("Admin")
                .lastName("Mnatural")
                .username("admin")
                .email("admin@uruguaymnatural.uy")
                .password(passwordEncoder.encode("admin1234"))
                .build();
        userRepository.save(admin);

        // Proveedor ya aprobado, dueño de las actividades de ejemplo
        FisicProvider martin = FisicProvider.builder()
                .firstName("Martín")
                .lastName("Rodríguez")
                .username("martin.rodriguez")
                .email("martin@ejemplo.com")
                .password(passwordEncoder.encode("prestador1234"))
                .phone("099123456")
                .businessName("Aventuras del Este")
                .address("Ruta 10 km 160, Rocha")
                .serviceDescription("Excursiones y paseos guiados por el este del país")
                .nationalId("12345672")
                .status(ProviderStatus.APPROVED)
                .registrationDate(LocalDateTime.now().minusDays(20))
                .build();
        userRepository.save(martin);

        // Dos solicitudes pendientes, para que el panel del admin tenga algo que revisar
        userRepository.save(FisicProvider.builder()
                .firstName("Lucía")
                .lastName("Fernández")
                .username("lucia.fernandez")
                .email("lucia@ejemplo.com")
                .password(passwordEncoder.encode("prestador1234"))
                .phone("098765432")
                .businessName("Cabalgatas La Quebrada")
                .address("Ruta 8 km 306, Treinta y Tres")
                .serviceDescription("Cabalgatas guiadas en la Quebrada de los Cuervos")
                .nationalId("45678905")
                .status(ProviderStatus.PENDING)
                .registrationDate(LocalDateTime.now().minusDays(2))
                .build());

        userRepository.save(JuridicProvider.builder()
                .firstName("Diego")
                .lastName("Pereira")
                .username("diego.pereira")
                .email("contacto@ecoturrocha.com.uy")
                .password(passwordEncoder.encode("prestador1234"))
                .phone("24001234")
                .businessName("EcoTur Rocha S.R.L.")
                .address("Av. Solari 1200, La Paloma")
                .serviceDescription("Avistaje de fauna y paseos en lancha")
                .taxId("214567890018")
                .legalRepresentative("Diego Pereira")
                .status(ProviderStatus.PENDING)
                .registrationDate(LocalDateTime.now().minusHours(5))
                .build());

        // Actividades de ejemplo (las fotos están en static/images)
        saveActivity("Recorrido histórico por Ciudad Vieja",
                "Caminata guiada de 2 horas por la Plaza Independencia, el Palacio Salvo y el Mercado del Puerto.",
                900.0, 0, true, 100, "/images/montevideo.jpg",
                new Location("Montevideo", "Ciudad Vieja", "Plaza Independencia", "s/n"), martin);

        saveActivity("Senderismo en las sierras",
                "Trekking de medio día por cerros y quebradas, con vista panorámica de las sierras.",
                1800.0, 12, true, 80, "/images/sierras.jpg",
                new Location("Lavalleja", "Minas", "Ruta 8", "km 145"), martin);

        saveActivity("Kayak al atardecer en Laguna de Rocha",
                "Salida en kayak doble de 3 horas para ver la puesta de sol sobre la laguna.",
                1500.0, 12, true, 50, "/images/atardecer.jpg",
                new Location("Rocha", "La Paloma", "Ruta 10", "km 236"), martin);

        saveActivity("Paseo por la península de Punta del Este",
                "Recorrido en bicicleta por la rambla, La Mano y el puerto, con paradas para fotos.",
                1200.0, 8, false, null, "/images/punta-del-este.jpg",
                new Location("Maldonado", "Punta del Este", "Rambla Claudio Williman", "s/n"), martin);

        saveActivity("Visita guiada a Casapueblo",
                "Visita a la obra de Carlos Páez Vilaró en Punta Ballena, con la ceremonia del sol.",
                1100.0, 0, true, 100, "/images/casapueblo.jpg",
                new Location("Maldonado", "Punta Ballena", "Ruta 10", "km 116"), martin);
    }

    private void saveActivity(String name, String description, Double price, Integer minimumAge,
                              Boolean refundable, Integer refundPercentage, String imageUrl,
                              Location location, Provider provider) {
        activityRepository.save(Activity.builder()
                .name(name)
                .description(description)
                .price(price)
                .minimumAge(minimumAge)
                .refundable(refundable)
                .refundPercentage(refundPercentage)
                .imageUrl(imageUrl)
                .location(location)
                .provider(provider)
                .build());
    }
}
