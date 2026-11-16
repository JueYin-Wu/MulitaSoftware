package uy.edu.um.porytic2026.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uy.edu.um.porytic2026.services.BookingExpirationService;

// Tarea automática: cada minuto vence las reservas que no se pagaron a tiempo (y así libera el cupo).
// Se puede apagar con app.bookings.expiration-enabled=false (por ejemplo, para probar algo a mano).
@Component
@ConditionalOnProperty(name = "app.bookings.expiration-enabled", havingValue = "true", matchIfMissing = true)
public class BookingExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(BookingExpirationJob.class);

    private final BookingExpirationService expirationService;

    public BookingExpirationJob(BookingExpirationService expirationService) {
        this.expirationService = expirationService;
    }

    // fixedDelay: espera ese tiempo DESPUÉS de terminar cada vuelta, así nunca se superponen dos ejecuciones
    @Scheduled(initialDelayString = "${app.bookings.expiration-initial-delay-ms:30000}",
               fixedDelayString = "${app.bookings.expiration-check-ms:60000}")
    public void expireUnpaidBookings() {
        int expired = 0;
        for (Long id : expirationService.findOverdueIds()) {
            try {
                // Se llama al service (otro bean) para que cada reserva tenga su propia transacción
                if (expirationService.expireIfUnpaid(id)) {
                    expired++;
                }
            } catch (RuntimeException e) {
                log.error("No se pudo vencer la reserva {}", id, e);
            }
        }
        if (expired > 0) {
            log.info("Se vencieron {} reservas sin pagar", expired);
        }
    }
}
