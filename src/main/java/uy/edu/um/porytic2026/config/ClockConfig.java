package uy.edu.um.porytic2026.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// Reloj que usan los services para saber la fecha y hora actual.
// Se define como bean para que en los tests se pueda usar un reloj fijo
// (así un test no depende del día y la hora en que se ejecuta).
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
