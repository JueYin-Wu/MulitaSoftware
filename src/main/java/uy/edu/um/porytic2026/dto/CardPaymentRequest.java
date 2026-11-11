package uy.edu.um.porytic2026.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Lo que manda el formulario de tarjeta de Mercado Pago (Card Payment Brick) desde nuestra página.
// No trae el número de tarjeta: trae un TOKEN de un solo uso que generó Mercado Pago en el navegador.
@Getter
@Setter
@NoArgsConstructor
public class CardPaymentRequest {

    private String token;
    private String paymentMethodId;      // visa, master, oca...
    private String issuerId;             // banco emisor
    private Integer installments;        // cuotas
    private String identificationType;   // CI
    private String identificationNumber;
}
