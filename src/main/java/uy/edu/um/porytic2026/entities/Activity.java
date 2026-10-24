package uy.edu.um.porytic2026.entities;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    @NotBlank
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "price")
    @PositiveOrZero
    private Double price;

    @Column(name = "requirements", length = 500)
    private String requirements;

    // Duración aproximada de la experiencia, en minutos (ej: 150 = 2 h 30 min)
    @Column(name = "duration_minutes")
    @Positive
    private Integer durationMinutes;

    @Column(name = "minimum_age")
    @PositiveOrZero
    private Integer minimumAge;

    @Column(name = "booking_deadline")
    @PositiveOrZero
    private Integer bookingDeadline; // minutos

    @Column(name = "cancellation_minutes")
    @PositiveOrZero
    private Integer cancellationMinutes;

    @Column(name = "refundable")
    private Boolean refundable;

    @Column(name = "refund_percentage")
    @PositiveOrZero
    private Integer refundPercentage;

    // Ruta de la foto que se muestra en la tarjeta, por ejemplo "/images/sierras.jpg"
    @Column(name = "image_url")
    private String imageUrl;

    @Embedded
    private Location location;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @ManyToMany
    @JoinTable(
            name = "activity_tag",
            joinColumns = @JoinColumn(name = "activity_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags;

    @OneToMany(mappedBy = "activity")
    private List<ActivityEquipment> equipmentItems;

    // Fotos que elige el prestador para el carrusel de la página de la actividad, en orden
    @ElementCollection
    @CollectionTable(name = "activity_images", joinColumns = @JoinColumn(name = "activity_id"))
    @OrderColumn(name = "position")
    @Column(name = "url")
    @Builder.Default
    private List<String> images = new ArrayList<>();

    // Imágenes del carrusel. Si el prestador no cargó ninguna, se usa la foto de la tarjeta.
    public List<String> getGallery() {
        if (images != null && !images.isEmpty()) {
            return images;
        }
        return imageUrl != null ? List.of(imageUrl) : List.of();
    }

    // Textos para mostrar los minutos de forma legible ("2 horas", "1 día")
    public String getBookingDeadlineLabel() {
        return formatMinutes(bookingDeadline);
    }

    public String getCancellationLabel() {
        return formatMinutes(cancellationMinutes);
    }

    // Duración para mostrar en pantalla ("2 h 30 min"), o null si no se indicó
    public String getDurationLabel() {
        if (durationMinutes == null || durationMinutes <= 0) {
            return null;
        }
        int hours = durationMinutes / 60;
        int minutes = durationMinutes % 60;
        if (hours == 0) {
            return minutes + " min";
        }
        return minutes == 0 ? hours + " h" : hours + " h " + minutes + " min";
    }

    public static String formatMinutes(Integer minutes) {
        if (minutes == null || minutes <= 0) {
            return null;
        }
        if (minutes % 1440 == 0) {
            int days = minutes / 1440;
            return days + (days == 1 ? " día" : " días");
        }
        if (minutes % 60 == 0) {
            int hours = minutes / 60;
            return hours + (hours == 1 ? " hora" : " horas");
        }
        return minutes + " minutos";
    }
}