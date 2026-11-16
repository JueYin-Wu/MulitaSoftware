package uy.edu.um.porytic2026.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Activa las tareas programadas (@Scheduled), por ejemplo BookingExpirationJob
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
