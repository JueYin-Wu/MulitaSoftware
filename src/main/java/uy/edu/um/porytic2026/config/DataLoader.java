package uy.edu.um.porytic2026.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import uy.edu.um.porytic2026.entities.*;
import uy.edu.um.porytic2026.repository.*;

import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

// Carga datos de prueba al arrancar la app (solo para desarrollo).
// La base H2 está en memoria, así que se vuelve a cargar cada vez que se reinicia.
@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final EquipmentRepository equipmentRepository;
    private final ActivityEquipmentRepository activityEquipmentRepository;
    private final TagRepository tagRepository;
    private final AvailabilityRepository availabilityRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UserRepository userRepository, ActivityRepository activityRepository,
                      EquipmentRepository equipmentRepository, ActivityEquipmentRepository activityEquipmentRepository,
                      TagRepository tagRepository, AvailabilityRepository availabilityRepository,
                      BookingRepository bookingRepository, ReviewRepository reviewRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.equipmentRepository = equipmentRepository;
        this.activityEquipmentRepository = activityEquipmentRepository;
        this.tagRepository = tagRepository;
        this.availabilityRepository = availabilityRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Foto de portada de cada tipo de actividad (static/images/categories)
    private static final Map<String, String> TAG_IMAGES = Map.of(
            "Naturaleza", "/images/categories/naturaleza.jpg",
            "Actividad acuática", "/images/categories/actividad-acuatica.jpg",
            "Recorrido histórico", "/images/categories/recorrido-historico.jpg",
            "Senderismo", "/images/categories/senderismo.jpg",
            "Cultural", "/images/categories/cultural.jpg",
            "Aventura", "/images/categories/aventura-sierras.jpg");

    @Override
    public void run(String... args) {
        // Se hace en cada arranque: así los tipos que ya estaban guardados en la base también reciben su foto
        assignTagImages();

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

        // Proveedor ya aprobado, dueño de las actividades de ejemplo (usuario: prestador / contraseña: prestador1234)
        FisicProvider martin = FisicProvider.builder()
                .firstName("Martín")
                .lastName("Rodríguez")
                .username("prestador")
                .email("martin@ejemplo.com")
                .password(passwordEncoder.encode("prestador1234"))
                .phone("099123456")
                .businessName("Aventuras del Este")
                .address("Ruta 10 km 160, Rocha")
                .serviceDescription("Excursiones y paseos guiados por el este del país")
                .documentType(DocumentType.CI)
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
                .documentType(DocumentType.CI)
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

        // Turista de prueba, con reservas pasadas y reseñas (usuario: turista / contraseña: turista1234)
        Tourist sofia = Tourist.builder()
                .firstName("Sofía")
                .lastName("Gómez")
                .username("turista")
                .email("sofia@ejemplo.com")
                .password(passwordEncoder.encode("turista1234"))
                .phone("091234567")
                .documentType(DocumentType.CI)
                .idDocument("34567894")
                .build();
        userRepository.save(sofia);

        // Catálogos que administra el admin: etiquetas aprobadas y equipamiento
        Tag naturaleza = saveTag("Naturaleza");
        Tag acuatica = saveTag("Actividad acuática");
        Tag historico = saveTag("Recorrido histórico");
        Tag senderismo = saveTag("Senderismo");
        Tag cultural = saveTag("Cultural");
        Tag aventura = saveTag("Aventura");

        Equipment calzado = saveEquipment("Calzado deportivo", "Zapatillas o botas cómodas con buen agarre");
        Equipment agua = saveEquipment("Agua", "Al menos un litro por persona");
        Equipment protector = saveEquipment("Protector solar", "Factor 30 o más");
        Equipment chaleco = saveEquipment("Chaleco salvavidas", "Obligatorio en el agua");
        Equipment kayak = saveEquipment("Kayak y remos", "Kayak doble con remos");
        Equipment bicicleta = saveEquipment("Bicicleta", "Bicicleta urbana con casco");
        Equipment casco = saveEquipment("Casco", "Casco de protección");

        // Actividades de ejemplo. Las fotos del carrusel están en static/images/activities.
        // Las coordenadas son aproximadas y sirven para ubicar el punto en el mapa.
        Activity ciudadVieja = saveActivity("Recorrido histórico por Ciudad Vieja",
                "Caminata guiada de 2 horas por la Plaza Independencia, el Palacio Salvo y el Mercado del Puerto. "
                        + "El guía cuenta la historia de cada edificio y de la ciudad colonial.",
                900.0, "Ninguno en especial. El recorrido es a pie, unos 3 km.", 0, 60, 120, true, 100,
                "montevideo", new Location("Montevideo", "Ciudad Vieja", "Plaza Independencia", "s/n", -34.9066, -56.2000), martin,
                List.of(historico, cultural));

        Activity sierras = saveActivity("Senderismo en las sierras",
                "Trekking de medio día por cerros y quebradas, con vista panorámica de las sierras.",
                1800.0, "Buena condición física. Dificultad media, unos 8 km de caminata.", 12, 1440, 2880, true, 80,
                "sierras", new Location("Lavalleja", "Minas", "Ruta 8", "km 145", -34.3759, -55.2378), martin,
                List.of(naturaleza, senderismo, aventura));

        Activity kayakRocha = saveActivity("Kayak al atardecer en Laguna de Rocha",
                "Salida en kayak doble de 3 horas para ver la puesta de sol sobre la laguna.",
                1500.0, "Saber nadar. No hace falta experiencia previa en kayak.", 12, 180, 1440, true, 50,
                "atardecer", new Location("Rocha", "La Paloma", "Ruta 10", "km 236", -34.6200, -54.2700), martin,
                List.of(naturaleza, acuatica, aventura));

        Activity punta = saveActivity("Paseo por la península de Punta del Este",
                "Recorrido en bicicleta por la rambla, La Mano y el puerto, con paradas para fotos.",
                1200.0, "Saber andar en bicicleta.", 8, 60, null, false, null,
                "punta-del-este", new Location("Maldonado", "Punta del Este", "Rambla Claudio Williman", "s/n", -34.9578, -54.9385), martin,
                List.of(cultural, aventura));

        Activity casapueblo = saveActivity("Visita guiada a Casapueblo",
                "Visita a la obra de Carlos Páez Vilaró en Punta Ballena, con la ceremonia del sol.",
                1100.0, "Ninguno.", 0, 30, 60, true, 100,
                "casapueblo", new Location("Maldonado", "Punta Ballena", "Ruta 10", "km 116", -34.9067, -55.0466), martin,
                List.of(cultural));

        // Equipamiento: REQUIRED = lo lleva el turista, PROVIDED = lo da el prestador
        saveEquipmentItem(ciudadVieja, calzado, EquipmentRole.REQUIRED);
        saveEquipmentItem(ciudadVieja, agua, EquipmentRole.REQUIRED);
        saveEquipmentItem(sierras, calzado, EquipmentRole.REQUIRED);
        saveEquipmentItem(sierras, agua, EquipmentRole.REQUIRED);
        saveEquipmentItem(sierras, protector, EquipmentRole.REQUIRED);
        saveEquipmentItem(kayakRocha, protector, EquipmentRole.REQUIRED);
        saveEquipmentItem(kayakRocha, kayak, EquipmentRole.PROVIDED);
        saveEquipmentItem(kayakRocha, chaleco, EquipmentRole.PROVIDED);
        saveEquipmentItem(punta, bicicleta, EquipmentRole.PROVIDED);
        saveEquipmentItem(punta, casco, EquipmentRole.PROVIDED);
        saveEquipmentItem(punta, agua, EquipmentRole.REQUIRED);

        // Próximas fechas (disponibilidades) de cada actividad
        for (Activity activity : List.of(ciudadVieja, sierras, kayakRocha, punta, casapueblo)) {
            saveAvailability(activity, 3, LocalTime.of(10, 0), 15);
            saveAvailability(activity, 7, LocalTime.of(17, 30), 12);
            saveAvailability(activity, 14, LocalTime.of(10, 0), 20);
        }

        // Una reserva vigente para que se vea que bajan los cupos (2 de 15 en la primera fecha de Ciudad Vieja)
        Availability nextCiudadVieja = availabilityRepository
                .findByActivityIdAndDateGreaterThanEqualOrderByDateAscTimeAsc(ciudadVieja.getId(), LocalDate.now())
                .get(0);
        bookingRepository.save(Booking.builder()
                .bookingDate(LocalDateTime.now().minusDays(1))
                .participantCount(2)
                .status(BookingStatus.CONFIRMED)
                .tourist(sofia)
                .availability(nextCiudadVieja)
                .build());

        // Reservas pasadas con reseña (la reseña se deja después de haber participado)
        saveReview(sofia, kayakRocha, 10, 5, "Increíble la puesta de sol. Los guías muy atentos y el equipo en perfecto estado.");
        saveReview(sofia, casapueblo, 20, 4, "Muy linda visita, la ceremonia del sol vale la pena. Había bastante gente.");
        saveReview(sofia, ciudadVieja, 30, 5, "Aprendí un montón de historia de Montevideo. Muy recomendable.");
    }

    private Tag saveTag(String name) {
        return tagRepository.save(Tag.builder()
                .name(name)
                .status(TagStatus.APPROVED)
                .proposalDate(LocalDateTime.now().minusDays(30))
                .imageUrl(TAG_IMAGES.get(name))
                .build());
    }

    // A cada tipo de actividad se le pone la foto que le corresponde (si hay una para ese nombre).
    // Se actualiza si no tiene foto o si tiene otra de las fotos de ejemplo (/images/categories/...);
    // si el administrador subió una propia, no se toca.
    private void assignTagImages() {
        for (Tag tag : tagRepository.findAll()) {
            String image = TAG_IMAGES.get(tag.getName());
            String current = tag.getImageUrl();
            boolean isDefault = current == null || current.startsWith("/images/categories/");
            if (image != null && isDefault && !image.equals(current)) {
                tag.setImageUrl(image);
                tagRepository.save(tag);
            }
        }
    }

    private Equipment saveEquipment(String name, String description) {
        return equipmentRepository.save(Equipment.builder().name(name).description(description).build());
    }

    private void saveEquipmentItem(Activity activity, Equipment equipment, EquipmentRole role) {
        activityEquipmentRepository.save(ActivityEquipment.builder()
                .activity(activity)
                .equipment(equipment)
                .role(role)
                .build());
    }

    private Availability saveAvailability(Activity activity, int daysFromToday, LocalTime time, int capacity) {
        return availabilityRepository.save(Availability.builder()
                .activity(activity)
                .date(LocalDate.now().plusDays(daysFromToday))
                .time(time)
                .capacity(capacity)
                .build());
    }

    // Crea una fecha pasada, una reserva confirmada del turista y su reseña
    private void saveReview(Tourist tourist, Activity activity, int daysAgo, int rating, String comment) {
        Availability past = availabilityRepository.save(Availability.builder()
                .activity(activity)
                .date(LocalDate.now().minusDays(daysAgo))
                .time(LocalTime.of(10, 0))
                .capacity(15)
                .build());
        Booking booking = bookingRepository.save(Booking.builder()
                .bookingDate(LocalDateTime.now().minusDays(daysAgo + 5))
                .participantCount(2)
                .status(BookingStatus.CONFIRMED)
                .tourist(tourist)
                .availability(past)
                .build());
        reviewRepository.save(Review.builder()
                .booking(booking)
                .rating(rating)
                .comment(comment)
                .build());
    }

    // gallery = nombre base de las fotos: "sierras" usa sierras-1.jpg, sierras-2.jpg y sierras-3.jpg
    private Activity saveActivity(String name, String description, Double price, String requirements,
                                  Integer minimumAge, Integer bookingDeadline, Integer cancellationMinutes,
                                  Boolean refundable, Integer refundPercentage, String gallery,
                                  Location location, Provider provider, List<Tag> tags) {
        List<String> images = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            images.add("/images/activities/" + gallery + "-" + i + ".jpg");
        }
        return activityRepository.save(Activity.builder()
                .name(name)
                .description(description)
                .price(price)
                .requirements(requirements)
                .minimumAge(minimumAge)
                .bookingDeadline(bookingDeadline)
                .cancellationMinutes(cancellationMinutes)
                .refundable(refundable)
                .refundPercentage(refundPercentage)
                .imageUrl("/images/" + gallery + ".jpg")
                .images(images)
                .location(location)
                .provider(provider)
                .tags(new ArrayList<>(tags))
                .build());
    }
}
