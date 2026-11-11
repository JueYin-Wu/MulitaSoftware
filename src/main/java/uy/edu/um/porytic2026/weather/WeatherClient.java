package uy.edu.um.porytic2026.weather;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Cliente de la API pública de clima Open-Meteo (gratis y sin clave). Letra: "sería conveniente integrar el
// sistema con alguna API pública de clima para mostrar el pronóstico previsto para la fecha y lugar de la actividad".
// GET /v1/forecast?latitude=..&longitude=..&daily=...&timezone=America/Montevideo&forecast_days=16
@Component
public class WeatherClient {

    // Open-Meteo da pronóstico hasta 16 días hacia adelante
    public static final int MAX_DAYS = 16;

    private final RestClient restClient;
    private final String timezone;

    public WeatherClient(@Value("${app.weather.api-url:https://api.open-meteo.com}") String apiUrl,
                         @Value("${app.weather.timezone:America/Montevideo}") String timezone,
                         @Value("${app.weather.timeout-seconds:4}") int timeoutSeconds) {
        // Tiempo de espera corto: el clima es un extra y no puede hacer lenta la página
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder().baseUrl(apiUrl).requestFactory(requestFactory).build();
        this.timezone = timezone;
    }

    // Pronóstico de los próximos días en esas coordenadas: fecha -> pronóstico.
    // Si la API falla, la excepción sube y WeatherService decide qué hacer (no mostrar el clima).
    @SuppressWarnings("unchecked")
    public Map<LocalDate, DailyForecast> forecast(double latitude, double longitude) {
        Map<String, Object> response = restClient.get()
                .uri(uri -> uri.path("/v1/forecast")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max")
                        .queryParam("timezone", timezone)
                        .queryParam("forecast_days", MAX_DAYS)
                        .build())
                .retrieve()
                .body(Map.class);

        Map<LocalDate, DailyForecast> result = new LinkedHashMap<>();
        if (response == null || !(response.get("daily") instanceof Map<?, ?> daily)) {
            return result;
        }
        List<?> dates = list(daily.get("time"));
        List<?> codes = list(daily.get("weather_code"));
        List<?> max = list(daily.get("temperature_2m_max"));
        List<?> min = list(daily.get("temperature_2m_min"));
        List<?> rain = list(daily.get("precipitation_probability_max"));
        for (int i = 0; i < dates.size(); i++) {
            LocalDate date = LocalDate.parse(String.valueOf(dates.get(i)));
            result.put(date, new DailyForecast(date,
                    number(codes, i) == null ? null : number(codes, i).intValue(),
                    number(max, i) == null ? null : number(max, i).doubleValue(),
                    number(min, i) == null ? null : number(min, i).doubleValue(),
                    number(rain, i) == null ? null : number(rain, i).intValue()));
        }
        return result;
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private static Number number(List<?> values, int index) {
        return index < values.size() && values.get(index) instanceof Number n ? n : null;
    }
}
