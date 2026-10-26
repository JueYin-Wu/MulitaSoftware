package uy.edu.um.porytic2026.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uy.edu.um.porytic2026.entities.Tag;

// Tarjeta de un tipo de actividad en el inicio: el tipo, cuántas actividades tiene,
// cuántas reservas confirmadas suman y la foto que la representa
@Getter
@AllArgsConstructor
public class CategoryCard {

    private Tag tag;
    private int activityCount;
    private long bookingCount;
    private String imageUrl;
}
