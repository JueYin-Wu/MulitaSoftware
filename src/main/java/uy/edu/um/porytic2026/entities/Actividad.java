package uy.edu.um.porytic2026.entities;

import java.util.List;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "actividades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Actividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre")
    @NotBlank
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "precio")
    @PositiveOrZero
    private Double precio;

    @Column(name = "requisitos")
    private String requisitos;

    @Column(name = "edadMinima")
    private Integer edadMinima;

    @Column(name = "tiempoLimiteReserva")
    private Integer tiempoLimiteReserva; // minutos

    @Column(name = "minutosCancelacion")
    private Integer minutosCancelacion; // null = no cancelable

    @Column(name = "esReembolsable")
    private Boolean esReembolsable;

    @Column(name = "porcentajeReembolso")
    private Integer porcentajeReembolso;

    @Embedded
    private Ubicacion ubicacion;

    @ManyToOne
    @JoinColumn(name = "prestador_id", nullable = false)
    private Prestador prestador;

    @ManyToMany
    @JoinTable(
            name = "actividad_etiqueta",
            joinColumns = @JoinColumn(name = "actividad_id"),
            inverseJoinColumns = @JoinColumn(name = "etiqueta_id")
    )
    private List<Etiqueta> etiquetas;

    @OneToMany(mappedBy = "actividad")
    private List<ActividadEquipamiento> equipamientos;

}