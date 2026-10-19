package uy.edu.um.porytic2026.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uy.edu.um.porytic2026.dto.ProviderRegistrationForm;
import uy.edu.um.porytic2026.dto.TouristRegistrationForm;
import uy.edu.um.porytic2026.entities.Tourist;
import uy.edu.um.porytic2026.exceptions.RegistrationException;
import uy.edu.um.porytic2026.services.AuthUserDetailsService;
import uy.edu.um.porytic2026.services.ProviderService;
import uy.edu.um.porytic2026.services.UserService;
import uy.edu.um.porytic2026.validation.DocumentValidator;

import java.util.Objects;

@Controller
public class AuthController {

    private final ProviderService providerService;
    private final UserService userService;
    private final AuthUserDetailsService authUserDetailsService;

    // Guarda la sesión iniciada (se usa para dejar logueado al turista después de registrarse)
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(ProviderService providerService, UserService userService,
                          AuthUserDetailsService authUserDetailsService) {
        this.providerService = providerService;
        this.userService = userService;
        this.authUserDetailsService = authUserDetailsService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    // Pantalla para elegir si registrarse como turista o como prestador
    @GetMapping("/register")
    public String registerChoice() {
        return "auth/register-choice";
    }

    // ===================== Registro de turista (CU03) =====================

    @GetMapping("/register/tourist")
    public String touristForm(Model model) {
        model.addAttribute("form", new TouristRegistrationForm());
        return "auth/register-tourist";
    }

    @PostMapping("/register/tourist")
    public String registerTourist(@ModelAttribute("form") TouristRegistrationForm form,
                                  BindingResult result,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {

        validatePersonalData(form.getFirstName(), form.getLastName(), form.getEmail(), form.getPhone(),
                form.getPassword(), form.getConfirmPassword(), result);
        if (isBlank(form.getIdDocument())) {
            reject(result, "idDocument", "Ingresá tu cédula");
        } else if (!DocumentValidator.isValidCI(form.getIdDocument())) {
            reject(result, "idDocument", "Cédula no válida. Revisá los números y el dígito verificador (ej: 1.234.567-2)");
        }

        if (result.hasErrors()) {
            return "auth/register-tourist";
        }

        Tourist tourist;
        try {
            tourist = userService.registerTourist(form);
        } catch (RegistrationException e) {
            reject(result, e.getField(), e.getMessage());
            return "auth/register-tourist";
        }

        // CU03 paso 5: el turista queda con la sesión iniciada
        logIn(tourist.getUsername(), request, response);
        return "redirect:/register/tourist/success";
    }

    @GetMapping("/register/tourist/success")
    public String touristSuccess() {
        return "auth/register-tourist-success";
    }

    // ===================== Registro de prestador (CU01) =====================

    @GetMapping("/register/provider")
    public String providerForm(Model model) {
        model.addAttribute("form", new ProviderRegistrationForm());
        return "auth/register-provider";
    }

    @PostMapping("/register/provider")
    public String registerProvider(@ModelAttribute("form") ProviderRegistrationForm form,
                                   BindingResult result,
                                   RedirectAttributes redirectAttributes) {

        validatePersonalData(form.getFirstName(), form.getLastName(), form.getEmail(), form.getPhone(),
                form.getPassword(), form.getConfirmPassword(), result);
        validateBusinessData(form, result);

        if (result.hasErrors()) {
            return "auth/register-provider";
        }

        try {
            providerService.register(form);
        } catch (RegistrationException e) {
            reject(result, e.getField(), e.getMessage());
            return "auth/register-provider";
        }

        redirectAttributes.addFlashAttribute("businessName", form.getBusinessName());
        return "redirect:/register/provider/success";
    }

    @GetMapping("/register/provider/success")
    public String providerSuccess() {
        return "auth/register-success";
    }

    // ===================== Validaciones =====================

    // Datos que piden los dos registros (turista y prestador)
    private void validatePersonalData(String firstName, String lastName, String email, String phone,
                                      String password, String confirmPassword, BindingResult result) {
        if (isBlank(firstName)) {
            reject(result, "firstName", "Ingresá tu nombre");
        } else if (!DocumentValidator.isValidName(firstName)
                || firstName.trim().length() < 3 || firstName.trim().length() > 30) {
            reject(result, "firstName", "Nombre no válido. Usá solo letras (entre 3 y 30)");
        }
        if (isBlank(lastName)) {
            reject(result, "lastName", "Ingresá tu apellido");
        } else if (!DocumentValidator.isValidName(lastName)) {
            reject(result, "lastName", "Apellido no válido. Usá solo letras");
        }

        // CU01 y CU03, 4a: "correo no válido"
        if (isBlank(email) || !DocumentValidator.isValidEmail(email)) {
            reject(result, "email", "Correo no válido. Ej: nombre@dominio.com");
        }

        if (isBlank(phone)) {
            reject(result, "phone", "Ingresá un teléfono de contacto");
        } else if (!DocumentValidator.isValidPhone(phone)) {
            reject(result, "phone", "Teléfono no válido. Ej: 099 123 456 (celular) o 2400 1234 (fijo)");
        }

        if (isBlank(password) || password.length() < 8) {
            reject(result, "password", "La contraseña debe tener al menos 8 caracteres");
        }
        if (!Objects.equals(password, confirmPassword)) {
            reject(result, "confirmPassword", "Las contraseñas no coinciden");
        }
    }

    // Datos propios del prestador: documento según el tipo y datos del emprendimiento
    private void validateBusinessData(ProviderRegistrationForm form, BindingResult result) {
        if (form.isFisic()) {
            if (isBlank(form.getNationalId())) {
                reject(result, "nationalId", "Ingresá tu cédula");
            } else if (!DocumentValidator.isValidCI(form.getNationalId())) {
                reject(result, "nationalId", "Cédula no válida. Revisá los números y el dígito verificador (ej: 1.234.567-2)");
            }
        } else if (form.isJuridic()) {
            if (isBlank(form.getTaxId())) {
                reject(result, "taxId", "Ingresá el RUT de la empresa");
            } else if (!DocumentValidator.isValidRUT(form.getTaxId())) {
                reject(result, "taxId", "RUT no válido. Debe tener 12 dígitos (ej: 21 123456 0018)");
            }
            if (isBlank(form.getLegalRepresentative())) {
                reject(result, "legalRepresentative", "Ingresá el nombre del responsable");
            } else if (!DocumentValidator.isValidName(form.getLegalRepresentative())) {
                reject(result, "legalRepresentative", "Nombre del responsable no válido. Usá solo letras");
            }
        } else {
            reject(result, "providerType", "Elegí un tipo de proveedor");
        }

        if (isBlank(form.getBusinessName())) {
            reject(result, "businessName", "Ingresá el nombre del emprendimiento");
        }
        if (isBlank(form.getAddress())) {
            reject(result, "address", "Ingresá la dirección");
        }
        if (form.getServiceDescription() != null && form.getServiceDescription().length() > 255) {
            reject(result, "serviceDescription", "La descripción puede tener hasta 255 caracteres");
        }
    }

    // Agrega el error solo si ese campo no tiene ya otro (así no se repiten mensajes)
    private void reject(BindingResult result, String field, String message) {
        if (!result.hasFieldErrors(field)) {
            result.rejectValue(field, "invalid", message);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // Inicia la sesión de un usuario sin que tenga que pasar por el formulario de login
    private void logIn(String username, HttpServletRequest request, HttpServletResponse response) {
        UserDetails details = authUserDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(details, null, details.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
