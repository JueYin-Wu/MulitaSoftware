package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.YearMonth;

@Entity
@Table(name = "medios_pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedioDePago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    private TipoTarjeta tipo;

    @Column(name = "ultimosDigitos")
    @Size(min = 4, max = 4)
    private String ultimosDigitos;

    @Column(name = "vencimiento")
    private YearMonth vencimiento;

    @ManyToOne
    @JoinColumn(name = "turista_id", nullable = false)
    private Turista turista;
}