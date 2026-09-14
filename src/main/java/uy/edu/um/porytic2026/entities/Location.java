package uy.edu.um.porytic2026.entities;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class Location {

    private String department;
    private String neighborhood;
    private String street;
    private String number;

}