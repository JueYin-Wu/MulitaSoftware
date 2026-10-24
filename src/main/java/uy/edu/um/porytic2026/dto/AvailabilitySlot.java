package uy.edu.um.porytic2026.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uy.edu.um.porytic2026.entities.Availability;
import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

// Una fecha disponible de una actividad junto con los lugares que quedan libres
@Getter
@AllArgsConstructor
public class AvailabilitySlot {

    private Availability availability;
    private long remaining;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-UY"));

    public boolean isSoldOut() {
        return remaining <= 0;
    }

    // ¿Todavía se puede reservar? (no pasó el plazo que fijó el prestador antes del inicio)
    public boolean isBookingOpen() {
        return LocalDateTime.now().isBefore(availability.getBookingClosesAt());
    }

    // Fecha en español, por ejemplo "Sábado 25 de octubre"
    public String getDateLabel() {
        String text = availability.getDate().format(DATE_FORMAT);
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}
