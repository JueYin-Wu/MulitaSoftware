package uy.edu.um.porytic2026.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.porytic2026.entities.Payment;
import uy.edu.um.porytic2026.entities.PaymentMethod;
import uy.edu.um.porytic2026.entities.Tourist;
import uy.edu.um.porytic2026.exceptions.BusinessException;
import uy.edu.um.porytic2026.repository.PaymentMethodRepository;
import uy.edu.um.porytic2026.repository.PaymentRepository;

import java.util.List;

// Medios de pago guardados del turista (MER: MEDIO DE PAGO: tipo, 4 dígitos y vencimiento).
// Se guardan solos cuando Mercado Pago aprueba un pago con tarjeta (ver PaymentService). Acá el turista los
// consulta y puede quitarlos. Nunca se guarda el número completo de la tarjeta.
@Service
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentRepository paymentRepository;

    public PaymentMethodService(PaymentMethodRepository paymentMethodRepository, PaymentRepository paymentRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentRepository = paymentRepository;
    }

    public List<PaymentMethod> findByTourist(Tourist tourist) {
        return paymentMethodRepository.findByTouristIdOrderByIdDesc(tourist.getId());
    }

    // Quitar una tarjeta guardada. Los pagos ya hechos con ella se conservan (el historial no se borra),
    // solo dejan de apuntar a la tarjeta.
    @Transactional
    public PaymentMethod delete(Long paymentMethodId, Tourist tourist) {
        PaymentMethod method = paymentMethodRepository.findById(paymentMethodId)
                .filter(m -> m.getTourist().getId().equals(tourist.getId()))
                .orElseThrow(() -> new BusinessException("Ese medio de pago no existe o no es tuyo"));
        for (Payment payment : paymentRepository.findByPaymentMethodId(paymentMethodId)) {
            payment.setPaymentMethod(null);
            paymentRepository.save(payment);
        }
        paymentMethodRepository.delete(method);
        return method;
    }
}
