package uy.edu.um.porytic2026.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// Datos del formulario "Nueva actividad" del portal del prestador.
// No es una entidad: ProviderActivityService arma la Activity a partir de estos datos.
// Las fotos no van acá: llegan aparte como archivos (MultipartFile) en el controller.
@Getter
@Setter
public class ActivityForm {

    @NotBlank(message = "Ingresá el nombre de la actividad")
    @Size(max = 100, message = "El nombre puede tener hasta 100 caracteres")
    private String name;

    @NotBlank(message = "Ingresá una descripción de la experiencia")
    @Size(max = 1000, message = "La descripción puede tener hasta 1000 caracteres")
    private String description;

    @NotNull(message = "Ingresá el precio por persona")
    @PositiveOrZero(message = "El precio no puede ser negativo")
    private Double price;

    @NotNull(message = "Ingresá la duración aproximada")
    @Min(value = 10, message = "La duración mínima es de 10 minutos")
    @Max(value = 1440, message = "La duración máxima es de 24 horas (1440 minutos)")
    private Integer durationMinutes;

    @PositiveOrZero(message = "La edad mínima no puede ser negativa")
    @Max(value = 99, message = "La edad mínima no es válida")
    private Integer minimumAge;

    @Size(max = 500, message = "Los requisitos pueden tener hasta 500 caracteres")
    private String requirements;

    // Ubicación (punto de encuentro)
    @NotBlank(message = "Elegí el departamento")
    private String department;

    @NotBlank(message = "Ingresá la localidad o barrio")
    @Size(max = 100, message = "La localidad puede tener hasta 100 caracteres")
    private String neighborhood;

    @Size(max = 100, message = "La calle puede tener hasta 100 caracteres")
    private String street;

    @Size(max = 20, message = "El número puede tener hasta 20 caracteres")
    private String number;

    // Condiciones de reserva y cancelación, en minutos antes del inicio
    @NotNull(message = "Indicá hasta cuántos minutos antes se puede reservar (0 = hasta la hora de inicio)")
    @PositiveOrZero(message = "Los minutos no pueden ser negativos")
    private Integer bookingDeadline = 0;

    @PositiveOrZero(message = "Los minutos no pueden ser negativos")
    private Integer cancellationMinutes;

    private boolean refundable;

    @Min(value = 0, message = "El porcentaje debe estar entre 0 y 100")
    @Max(value = 100, message = "El porcentaje debe estar entre 0 y 100")
    private Integer refundPercentage;

    // Tipos de actividad elegidos (ids de etiquetas aprobadas por el admin)
    private List<Long> tagIds = new ArrayList<>();

    // Tipo nuevo que propone el prestador si no encuentra uno que le sirva (lo revisa el admin)
    @Size(max = 50, message = "El tipo de actividad puede tener hasta 50 caracteres")
    private String proposedTag;

    // Equipamiento: lo que da el prestador y lo que tiene que llevar el turista
    private List<Long> providedEquipmentIds = new ArrayList<>();
    private List<Long> requiredEquipmentIds = new ArrayList<>();
}
