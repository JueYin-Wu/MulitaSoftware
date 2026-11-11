package uy.edu.um.porytic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.edu.um.porytic2026.entities.CardType;
import uy.edu.um.porytic2026.entities.PaymentMethod;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    // Para no guardar dos veces la misma tarjeta de un turista
    Optional<PaymentMethod> findFirstByTouristIdAndTypeAndLastDigitsAndExpirationDate(
            Long touristId, CardType type, String lastDigits, YearMonth expirationDate);

    // Medios de pago de un turista, el último usado primero
    List<PaymentMethod> findByTouristIdOrderByIdDesc(Long touristId);
}
