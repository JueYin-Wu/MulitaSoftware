package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

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
}