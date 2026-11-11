package uy.edu.um.porytic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.edu.um.porytic2026.entities.Payment;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // El pago de una reserva (hay uno por reserva; si se reintenta, se actualiza el mismo)
    Optional<Payment> findByBookingId(Long bookingId);

    // Pagos hechos con un medio de pago (para desvincularlos si el turista lo quita)
    List<Payment> findByPaymentMethodId(Long paymentMethodId);
}
