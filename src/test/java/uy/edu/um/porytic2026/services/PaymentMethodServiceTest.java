package uy.edu.um.porytic2026.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.edu.um.porytic2026.entities.*;
import uy.edu.um.porytic2026.exceptions.BusinessException;
import uy.edu.um.porytic2026.repository.PaymentMethodRepository;
import uy.edu.um.porytic2026.repository.PaymentRepository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Tests unitarios de PaymentMethodService (medios de pago guardados)
@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceTest {

    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @Mock
    private PaymentRepository paymentRepository;

    private PaymentMethodService service;
    private Tourist tourist;
    private PaymentMethod card;

    @BeforeEach
    void setUp() {
        service = new PaymentMethodService(paymentMethodRepository, paymentRepository);
        tourist = new Tourist();
        tourist.setId(5L);
        card = PaymentMethod.builder().id(3L).tourist(tourist).type(CardType.CREDIT)
                .lastDigits("0604").expirationDate(YearMonth.of(2030, 11)).build();
    }

    @Test
    void removingACardKeepsThePaymentsButUnlinksThem() {
        Payment payment = Payment.builder().id(1L).paymentMethod(card).status(PaymentStatus.APPROVED).build();
        when(paymentMethodRepository.findById(3L)).thenReturn(Optional.of(card));
        when(paymentRepository.findByPaymentMethodId(3L)).thenReturn(List.of(payment));

        service.delete(3L, tourist);

        assertNull(payment.getPaymentMethod());
        verify(paymentRepository).save(payment);       // el pago se conserva
        verify(paymentMethodRepository).delete(card);  // la tarjeta se borra
    }

    @Test
    void aTouristCannotRemoveAnotherTouristsCard() {
        Tourist other = new Tourist();
        other.setId(6L);
        when(paymentMethodRepository.findById(3L)).thenReturn(Optional.of(card));

        assertThrows(BusinessException.class, () -> service.delete(3L, other));
        verify(paymentMethodRepository, never()).delete(any());
    }

    @Test
    void theCardIsShownMasked() {
        assertEquals("•••• 0604", card.getMaskedNumber());
        assertEquals("11/30", card.getExpirationLabel());
        assertEquals("Crédito", card.getTypeLabel());
        assertFalse(card.isExpired());
    }
}
