package uy.edu.um.porytic2026.api.dto;

import java.util.List;

// Lo que devuelve y recibe la API REST. Son "records": clases de solo datos que Spring convierte a JSON
// automáticamente (cada campo pasa a ser una clave del JSON). Se agrupan acá para tenerlas a la vista.
public final class ApiDtos {

    private ApiDtos() {
    }

    // ----- Seguridad -----

    // Cuerpo de POST /api/v1/auth/token
    public record TokenRequest(String username, String password) {
    }

    // Respuesta con el token: se manda después en el encabezado "Authorization: Bearer <token>"
    public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {
    }

    // ----- Emprendimientos -----

    public record ProviderResponse(Long id, String name, String type, String description, String address,
                                   int activityCount, Double averageRating, String profileUrl) {
    }

    public record VerificationResponse(Long id, String name, String type, String status, boolean verified,
                                       String taxId, String registeredSince) {
    }

    // ----- Actividades -----

    public record ProviderRef(Long id, String name) {
    }

    public record LocationResponse(String department, String neighborhood, String street, String number,
                                   Double latitude, Double longitude) {
    }

    public record ActivityResponse(Long id, String name, String description, Double price, Integer durationMinutes,
                                   Integer minimumAge, LocationResponse location, ProviderRef provider,
                                   Double averageRating, int reviewCount, String url) {
    }

    // Un horario de una actividad en un día, con los lugares que quedan
    public record SlotResponse(Long availabilityId, String time, Integer capacity, int remainingSpots) {
    }

    public record DailyActivityResponse(ActivityResponse activity, List<SlotResponse> slots) {
    }

    // Puesto en el top 3, con la valoración que obtuvo en ese mes
    public record TopActivityResponse(int position, ActivityResponse activity, double monthAverageRating,
                                      int monthReviewCount) {
    }

    // ----- Errores -----

    public record ApiError(int status, String error, String message, String path, String timestamp) {
    }
}
