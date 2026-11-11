package uy.edu.um.porytic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.um.porytic2026.entities.Location;
import uy.edu.um.porytic2026.weather.DailyForecast;
import uy.edu.um.porytic2026.weather.WeatherClient;

import java.time.*;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

// Tests unitarios de WeatherService (clima). La API de Open-Meteo se reemplaza por un mock.
// "Hoy" es el 09/11/2026.
@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);

    @Mock
    private WeatherClient weatherClient;

    private WeatherService weatherService;
    private Location rocha;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(TODAY.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        weatherService = new WeatherService(weatherClient, fixedClock, 60, true);
        rocha = new Location("Rocha", "La Paloma", "Av. Solari", "s/n", -34.66, -54.16);
    }

    @Test
    void returnsTheForecastOfTheActivityDate() {
        LocalDate date = TODAY.plusDays(3);
        DailyForecast sunny = new DailyForecast(date, 0, 26.4, 15.2, 10);
        when(weatherClient.forecast(-34.66, -54.16)).thenReturn(Map.of(date, sunny));

        DailyForecast forecast = weatherService.forecastFor(rocha, date);

        assertSame(sunny, forecast);
        assertEquals("Despejado", forecast.getDescription());
        assertEquals("26° / 15°", forecast.getTemperatures());
    }

    @Test
    void theForecastIsCachedAndTheApiIsCalledOnce() {
        LocalDate date = TODAY.plusDays(1);
        when(weatherClient.forecast(anyDouble(), anyDouble()))
                .thenReturn(Map.of(date, new DailyForecast(date, 3, 20.0, 12.0, 0)));

        weatherService.forecastFor(rocha, date);
        weatherService.forecastFor(rocha, date);

        verify(weatherClient, times(1)).forecast(anyDouble(), anyDouble());
    }

    @Test
    void ifTheWeatherApiFailsThePageStillWorksWithoutForecast() {
        when(weatherClient.forecast(anyDouble(), anyDouble())).thenThrow(new RuntimeException("Open-Meteo no responde"));

        assertNull(weatherService.forecastFor(rocha, TODAY.plusDays(2)));
        assertNull(weatherService.forecastFor(rocha, TODAY.plusDays(2))); // el fallo se recuerda: no se reintenta enseguida

        verify(weatherClient, times(1)).forecast(anyDouble(), anyDouble());
    }

    @Test
    void datesTooFarAwayOrInThePastHaveNoForecast() {
        assertNull(weatherService.forecastFor(rocha, TODAY.plusDays(30)));
        assertNull(weatherService.forecastFor(rocha, TODAY.minusDays(1)));
        verifyNoInteractions(weatherClient);
    }

    @Test
    void aLocationWithoutCoordinatesHasNoForecast() {
        Location noCoordinates = new Location("Rocha", "La Paloma", "Av. Solari", "s/n");
        assertNull(weatherService.forecastFor(noCoordinates, TODAY.plusDays(1)));
        verifyNoInteractions(weatherClient);
    }

    @Test
    void rainyDaysAreDetected() {
        DailyForecast rain = new DailyForecast(TODAY, 63, 18.0, 13.0, 80);
        assertTrue(rain.isRainy());
        assertEquals("Lluvia", rain.getDescription());
        assertEquals("🌧️", rain.getIcon());
    }
}
