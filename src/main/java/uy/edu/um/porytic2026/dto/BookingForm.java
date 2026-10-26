package uy.edu.um.porytic2026.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// Formulario de reserva: fecha elegida, cantidad de personas y datos de cada acompañante.
// Los acompañantes llegan como companions[0].firstName, companions[1].firstName, etc.
@Getter
@Setter
@NoArgsConstructor
public class BookingForm {

    private Long availabilityId;
    private Integer participantCount = 1;
    private List<ParticipantForm> companions = new ArrayList<>();
}
