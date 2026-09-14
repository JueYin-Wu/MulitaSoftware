package uy.edu.um.porytic2026.entities;

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

    @Column(name = "description")
    private String description;

    @Column(name = "price")
    @PositiveOrZero
    private Double price;

    @Column(name = "requirements")
    private String requirements;

    @Column(name = "minimum_age")
    private Integer minimumAge;

    @Column(name = "booking_deadline")
    private Integer bookingDeadline; // minutos

    @Column(name = "cancellation_minutes")
    private Integer cancellationMinutes;

    @Column(name = "refundable")
    private Boolean refundable;

    @Column(name = "refund_percentage")
    private Integer refundPercentage;

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
}