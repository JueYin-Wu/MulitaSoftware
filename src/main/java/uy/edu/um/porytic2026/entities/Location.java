package uy.edu.um.porytic2026.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    // Coordenadas del punto de encuentro, para ubicarlo en el mapa (opcionales)
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    // Ubicación sin coordenadas: el mapa la busca por la dirección
    public Location(String department, String neighborhood, String street, String number) {
        this(department, neighborhood, street, number, null, null);
    }

    // Lo que se le pasa a Google Maps: las coordenadas si existen, si no la dirección escrita
    public String getMapQuery() {
        if (latitude != null && longitude != null) {
            return latitude + "," + longitude;
        }
        List<String> parts = new ArrayList<>();
        String streetLine = ((street == null ? "" : street) + " "
                + (number == null || number.equalsIgnoreCase("s/n") ? "" : number)).trim();
        if (!streetLine.isEmpty()) {
            parts.add(streetLine);
        }
        if (neighborhood != null && !neighborhood.isBlank()) {
            parts.add(neighborhood);
        }
        if (department != null && !department.isBlank()) {
            parts.add(department);
        }
        parts.add("Uruguay");
        return String.join(", ", parts);
    }
}
