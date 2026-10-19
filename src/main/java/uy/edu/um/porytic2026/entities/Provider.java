package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

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
