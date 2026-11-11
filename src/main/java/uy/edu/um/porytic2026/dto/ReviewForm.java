package uy.edu.um.porytic2026.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Datos del formulario de reseña: valoración de 1 a 5 estrellas y comentario
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewForm {

    private Integer rating;
    private String comment;
}
