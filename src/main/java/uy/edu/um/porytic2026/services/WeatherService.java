package uy.edu.um.porytic2026.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uy.edu.um.porytic2026.entities.Location;
import uy.edu.um.porytic2026.weather.DailyForecast;
import uy.edu.um.porytic2026.weather.WeatherClient;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Pronóstico del clima para la fecha y el lugar de una actividad.
//  - Se guarda en memoria un rato (caché) para no consultar la API en cada visita.
//  - Si la API de clima falla, la página funciona igual: simplemente no se muestra el pronóstico.
//    El fallo también se recuerda unos minutos, para que las páginas no esperen a una API caída en cada visita.
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);
    private static final long FAILURE_CACHE_SECONDS = 5 * 60;

    private final WeatherClient weatherClient;
    private final Clock clock;
    private final long cacheSeconds;
    private final boolean enabled;

    // Pronósticos guardados por lugar ("-34.91,-56.20")
    private final Map<String, CachedForecast> cache = new ConcurrentHashMap<>();

    private record CachedForecast(Map<LocalDate, DailyForecast> days, Instant fetchedAt, boolean failed) {
    }

    public WeatherService(WeatherClient weatherClient, Clock clock,
                          @Value("${app.weather.cache-minutes:60}") long cacheMinutes,
                          @Value("${app.weather.enabled:true}") boolean enabled) {
        this.weatherClient = weatherClient;
        this.clock = clock;
        this.cacheSeconds = cacheMinutes * 60;
        this.enabled = enabled;
    }

    // Pronóstico de un día en un lugar, o null si no hay (fecha muy lejana o pasada, lugar sin coordenadas, API caída)
    public DailyForecast forecastFor(Location location, LocalDate date) {
        if (date == null) {
            return null;
        }
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today) || !date.isBefore(today.plusDays(WeatherClient.MAX_DAYS))) {
            return null;
        }
        return forecastsFor(location).get(date);
    }

    // Todos los días con pronóstico para un lugar (vacío si no se pudo obtener)
    public Map<LocalDate, DailyForecast> forecastsFor(Location location) {
        if (!enabled || location == null || location.getLatitude() == null || location.getLongitude() == null) {
            return Map.of();
        }
        String key = Math.round(location.getLatitude() * 100) / 100.0 + "," + Math.round(location.getLongitude() * 100) / 100.0;
        Instant now = clock.instant();

        CachedForecast cached = cache.get(key);
        if (cached != null) {
            long age = now.getEpochSecond() - cached.fetchedAt().getEpochSecond();
            if (age < (cached.failed() ? FAILURE_CACHE_SECONDS : cacheSeconds)) {
                return cached.days();
            }
        }

        try {
            Map<LocalDate, DailyForecast> days = weatherClient.forecast(location.getLatitude(), location.getLongitude());
            cache.put(key, new CachedForecast(days, now, false));
            return days;
        } catch (RuntimeException e) {
            // Sin conexión, API caída, respuesta rara...: no se muestra el clima, pero nada más se rompe
            log.warn("No se pudo obtener el pronóstico del clima para {}: {}", key, e.getMessage());
            cache.put(key, new CachedForecast(Map.of(), now, true));
            return Map.of();
        }
    }

    // Días con pronóstico (para explicar por qué una fecha lejana todavía no tiene)
    public int getForecastDays() {
        return WeatherClient.MAX_DAYS;
    }
}
