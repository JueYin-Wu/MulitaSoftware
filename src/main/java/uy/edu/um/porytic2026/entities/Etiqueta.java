package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "etiquetas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Etiqueta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre")
    @NotBlank
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoEtiqueta estado;

    @Column(name = "motivoRechazo")
    private String motivoRechazo;

    @ManyToOne
    @JoinColumn(name = "propuesta_por")
    private Prestador propuestaPor;

    @Column(name = "fechaPropuesta")
    private LocalDateTime fechaPropuesta;

    @ManyToMany(mappedBy = "etiquetas")
    private List<Actividad> actividades;
}