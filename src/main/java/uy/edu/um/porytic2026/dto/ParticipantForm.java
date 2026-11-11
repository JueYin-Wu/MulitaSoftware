package uy.edu.um.porytic2026.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Datos de un acompañante tal como llegan del formulario de reserva
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantForm {

    private String firstName;
    private String lastName;
    private String birthDate; // AAAA-MM-DD (lo manda el campo de tipo fecha)
}
