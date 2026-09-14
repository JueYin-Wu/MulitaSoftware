package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "actividad_equipamiento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ActividadEquipamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private RolEquipamiento rol;

    @ManyToOne
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    @ManyToOne
    @JoinColumn(name = "equipamiento_id", nullable = false)
    private Equipamiento equipamiento;
}