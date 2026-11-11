package uy.edu.um.porytic2026.weather;

import java.time.LocalDate;

// Pronóstico de un día en un lugar (datos de Open-Meteo).
// weatherCode es el código WMO (estándar de la Organización Meteorológica Mundial) que describe el tiempo.
public record DailyForecast(LocalDate date, Integer weatherCode, Double maxTemperature, Double minTemperature,
                            Integer rainProbability) {

    // Ícono para mostrar en pantalla
    public String getIcon() {
        if (weatherCode == null) {
            return "🌡️";
        }
        return switch (weatherCode) {
            case 0 -> "☀️";
            case 1 -> "🌤️";
            case 2 -> "⛅";
            case 3 -> "☁️";
            case 45, 48 -> "🌫️";
            case 51, 53, 55, 80, 81, 82 -> "🌦️";
            case 56, 57, 61, 63, 65, 66, 67 -> "🌧️";
            case 71, 73, 75, 77, 85, 86 -> "❄️";
            case 95, 96, 99 -> "⛈️";
            default -> "🌡️";
        };
    }

    // Descripción en castellano
    public String getDescription() {
        if (weatherCode == null) {
            return "Sin datos";
        }
        return switch (weatherCode) {
            case 0 -> "Despejado";
            case 1 -> "Mayormente despejado";
            case 2 -> "Parcialmente nublado";
            case 3 -> "Nublado";
            case 45, 48 -> "Niebla";
            case 51, 53, 55 -> "Llovizna";
            case 56, 57 -> "Llovizna helada";
            case 61 -> "Lluvia débil";
            case 63 -> "Lluvia";
            case 65 -> "Lluvia fuerte";
            case 66, 67 -> "Lluvia helada";
            case 71, 73, 75, 77 -> "Nieve";
            case 80, 81, 82 -> "Chaparrones";
            case 85, 86 -> "Chaparrones de nieve";
            case 95 -> "Tormenta";
            case 96, 99 -> "Tormenta con granizo";
            default -> "Sin datos";
        };
    }

    // "24° / 15°"
    public String getTemperatures() {
        return round(maxTemperature) + "° / " + round(minTemperature) + "°";
    }

    // ¿Conviene avisar que puede llover? (probabilidad de 50 % o más)
    public boolean isRainy() {
        return rainProbability != null && rainProbability >= 50;
    }

    private static String round(Double value) {
        return value == null ? "-" : String.valueOf(Math.round(value));
    }
}
