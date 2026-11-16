package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import lombok.AccessLevel;

@Entity
@Table(name = "providers")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder

public class Provider extends User {

    @Column(name = "business_name")
    @NotBlank
    private String businessName;

    @Column(name = "address")
    private String address;

    @Column(name = "service_description")
    private String serviceDescription;

    // Estado de la solicitud de registro: el admin la aprueba o la rechaza (CU02)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProviderStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "registration_date")
    private LocalDateTime registrationDate;

    // true mientras tenga una suspensión vigente aplicada por un administrador (ver Sanction).
    // Es Boolean y no boolean para que la columna nueva acepte null en los prestadores que ya existían.
    // No tiene getter de Lombok: se consulta con isSuspended(), que trata null como "no suspendido".
    @Column(name = "suspended")
    @Getter(AccessLevel.NONE)
    private Boolean suspended;

    public boolean isSuspended() {
        return Boolean.TRUE.equals(suspended);
    }

    // Puede operar en la plataforma: aprobado por el admin y sin una suspensión vigente
    public boolean isActive() {
        return status == ProviderStatus.APPROVED && !isSuspended();
    }

    // En pantalla un prestador se muestra con el nombre de su emprendimiento
    @Override
    public String getDisplayName() {
        return businessName;
    }

    // Estos dos métodos no son columnas: sirven para mostrar el tipo y el documento en pantalla.
    // Cada subclase (FisicProvider y JuridicProvider) los sobrescribe.
    public String getTypeLabel() {
        return "Proveedor";
    }

    public String getDocument() {
        return "";
    }
}
