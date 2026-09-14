package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "activity_equipment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ActivityEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private EquipmentRole role;

    @ManyToOne
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;
}