package uy.edu.um.porytic2026.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uy.edu.um.porytic2026.dto.CardPaymentRequest;
import uy.edu.um.porytic2026.entities.Booking;
import uy.edu.um.porytic2026.entities.Payment;
import uy.edu.um.porytic2026.entities.Tourist;
import uy.edu.um.porytic2026.exceptions.BusinessException;
import uy.edu.um.porytic2026.payments.PaymentGatewayException;
import uy.edu.um.porytic2026.payments.PaymentGatewayUnavailableException;
import uy.edu.um.porytic2026.services.PaymentService;
import uy.edu.um.porytic2026.services.UserService;
import uy.edu.um.porytic2026.services.WeatherService;

import java.util.LinkedHashMap;
import java.util.Map;

// Pago de una reserva con tarjeta, DENTRO de nuestra página (Mercado Pago Checkout API + Card Payment Brick).
// Está bajo /tourist/** así que solo entra el rol TOURIST,
// y además cada método controla que la reserva sea del turista que inició sesión.
@Controller
@RequestMapping("/tourist/bookings/{id}")
public class PaymentController {

    private static final String UNAVAILABLE_MESSAGE = "El servicio de pagos de Mercado Pago no está disponible en este momento. "
            + "Tu reserva sigue guardada: probá de nuevo en unos minutos.";

    private final PaymentService paymentService;
    private final UserService userService;
    private final WeatherService weatherService;

    public PaymentController(PaymentService paymentService, UserService userService, WeatherService weatherService) {
        this.paymentService = paymentService;
        this.userService = userService;
        this.weatherService = weatherService;
    }

    // Página de pago: resumen de la reserva, estado del pago y formulario de tarjeta
    @GetMapping("/pay")
    public String page(@PathVariable Long id, Authentication authentication, Model model,
                       RedirectAttributes redirectAttributes) {
        Tourist tourist = currentTourist(authentication);
        Booking booking;
        try {
            booking = paymentService.findOwnBooking(id, tourist);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/tourist/bookings";
        }

        // Si ya había un intento de pago, se consulta a Mercado Pago cómo salió
        if (paymentService.isConfigured() && paymentService.findPayment(id).isPresent()) {
            try {
                booking = paymentService.refreshStatus(id, tourist);
            } catch (PaymentGatewayUnavailableException e) {
                model.addAttribute("warning", "No pudimos consultar a Mercado Pago el estado de tu pago. "
                        + "Volvé a cargar la página en unos minutos.");
            } catch (PaymentGatewayException e) {
                model.addAttribute("warning", e.getMessage());
            }
        }

        model.addAttribute("booking", booking);
        model.addAttribute("activity", booking.getAvailability().getActivity());
        Payment payment = paymentService.findPayment(id).orElse(null);
        model.addAttribute("payment", payment);
        // "En proceso": Mercado Pago recibió el pago pero todavía no lo aprobó ni lo rechazó
        model.addAttribute("inProcess", payment != null && payment.getStatus() == uy.edu.um.porytic2026.entities.PaymentStatus.PENDING
                && payment.getExternalReference() != null);
        model.addAttribute("paymentsConfigured", paymentService.isConfigured());
        model.addAttribute("publicKey", paymentService.getPublicKey());
        model.addAttribute("payerEmail", tourist.getEmail());
        model.addAttribute("forecast", weatherService.forecastFor(
                booking.getAvailability().getActivity().getLocation(), booking.getAvailability().getDate()));
        return "tourist/payment";
    }

    // El formulario de tarjeta manda el token (nunca el número de tarjeta) y el servidor cobra con la API
    // de Mercado Pago. Responde en JSON cómo quedó el pago; la página después se recarga y lo muestra.
    //   200 -> se procesó (aprobado, rechazado o en proceso)  |  400/409 -> error de datos o de la reserva
    //   503 -> Mercado Pago no respondió (no se cobró nada)
    @PostMapping("/card-payment")
    @ResponseBody
    public ResponseEntity<Map<String, String>> payWithCard(@PathVariable Long id, @RequestBody CardPaymentRequest card,
                                                           Authentication authentication) {
        Map<String, String> body = new LinkedHashMap<>();
        if (!paymentService.isConfigured()) {
            body.put("message", "Los pagos todavía no están configurados.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
        try {
            Payment payment = paymentService.payWithCard(id, currentTourist(authentication), card);
            body.put("status", payment.getStatus().name());
            body.put("message", switch (payment.getStatus()) {
                case APPROVED -> "¡Pago aprobado! Tu reserva está confirmada.";
                case REJECTED -> "El pago fue rechazado: " + payment.getRejectionReason() + ".";
                default -> "El pago está en proceso. Te avisamos apenas se confirme.";
            });
            return ResponseEntity.ok(body);
        } catch (BusinessException e) {
            body.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        } catch (PaymentGatewayUnavailableException e) {
            body.put("message", UNAVAILABLE_MESSAGE);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        } catch (PaymentGatewayException e) {
            body.put("message", "Mercado Pago no aceptó el pago: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }
    }

    // Estado del pago en JSON. Si el pago quedó "en proceso", la página lo consulta cada algunos segundos
    // y se actualiza sola cuando Mercado Pago lo aprueba o lo rechaza.
    //   200 -> estado actual   |   503 -> Mercado Pago no respondió
    @GetMapping("/payment-status")
    @ResponseBody
    public ResponseEntity<Map<String, String>> status(@PathVariable Long id, Authentication authentication) {
        Map<String, String> body = new LinkedHashMap<>();
        try {
            Booking booking = paymentService.refreshStatus(id, currentTourist(authentication));
            Payment payment = paymentService.findPayment(id).orElse(null);
            body.put("bookingStatus", booking.getStatus().name());
            body.put("paymentStatus", payment == null || payment.getStatus() == null ? "NONE" : payment.getStatus().name());
            return ResponseEntity.ok(body);
        } catch (BusinessException e) {
            body.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        } catch (PaymentGatewayException e) {
            body.put("error", "unavailable");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
    }

    private Tourist currentTourist(Authentication authentication) {
        return (Tourist) userService.findByUsername(authentication.getName()).orElseThrow();
    }
}
