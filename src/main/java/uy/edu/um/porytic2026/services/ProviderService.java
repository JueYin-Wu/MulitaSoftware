package uy.edu.um.porytic2026.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.porytic2026.dto.ProviderRegistrationForm;
import uy.edu.um.porytic2026.entities.DocumentType;
import uy.edu.um.porytic2026.entities.FisicProvider;
import uy.edu.um.porytic2026.entities.JuridicProvider;
import uy.edu.um.porytic2026.entities.Provider;
import uy.edu.um.porytic2026.entities.ProviderStatus;
import uy.edu.um.porytic2026.exceptions.RegistrationException;
import uy.edu.um.porytic2026.repository.FisicProviderRepository;
import uy.edu.um.porytic2026.repository.JuridicProviderRepository;
import uy.edu.um.porytic2026.repository.ProviderRepository;
import uy.edu.um.porytic2026.repository.UserRepository;
import uy.edu.um.porytic2026.validation.DocumentValidator;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProviderService {

    private final ProviderRepository providerRepository;
    private final FisicProviderRepository fisicProviderRepository;
    private final JuridicProviderRepository juridicProviderRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public ProviderService(ProviderRepository providerRepository,
                           FisicProviderRepository fisicProviderRepository,
                           JuridicProviderRepository juridicProviderRepository,
                           UserRepository userRepository,
                           UserService userService,
                           PasswordEncoder passwordEncoder) {
        this.providerRepository = providerRepository;
        this.fisicProviderRepository = fisicProviderRepository;
        this.juridicProviderRepository = juridicProviderRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    // CU01 - Registro de proveedor: queda en estado PENDING hasta que un admin lo revise
    @Transactional
    public Provider register(ProviderRegistrationForm form) {
        String email = form.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new RegistrationException("email", "Correo no válido: ya existe una cuenta con ese correo");
        }

        Provider provider;
        if (form.isFisic()) {
            DocumentType documentType = DocumentType.fromValue(form.getDocumentType());
            if (documentType == null) {
                throw new RegistrationException("documentType", "Elegí cédula o pasaporte");
            }
            String problem = DocumentValidator.documentProblem(documentType, form.getNationalId());
            if (problem != null) {
                throw new RegistrationException("nationalId", problem);
            }
            String nationalId = DocumentValidator.normalizeDocument(documentType, form.getNationalId());
            // CU03 4b: "cédula ya registrada"
            if (fisicProviderRepository.existsByDocumentTypeAndNationalId(documentType, nationalId)) {
                throw new RegistrationException("nationalId",
                        documentType == DocumentType.CI ? "Cédula ya registrada" : "Pasaporte ya registrado");
            }
            FisicProvider fisic = new FisicProvider();
            fisic.setDocumentType(documentType);
            fisic.setNationalId(nationalId);
            provider = fisic;
        } else {
            String taxId = DocumentValidator.onlyDigits(form.getTaxId());
            if (!DocumentValidator.isValidRUT(taxId)) {
                throw new RegistrationException("taxId", "RUT no válido");
            }
            if (juridicProviderRepository.existsByTaxId(taxId)) {
                throw new RegistrationException("taxId", "RUT ya registrado");
            }
            JuridicProvider juridic = new JuridicProvider();
            juridic.setTaxId(taxId);
            juridic.setLegalRepresentative(form.getLegalRepresentative().trim());
            provider = juridic;
        }

        // Datos comunes a los dos tipos de proveedor
        provider.setFirstName(form.getFirstName().trim());
        provider.setLastName(form.getLastName().trim());
        provider.setEmail(email);
        provider.setPhone(DocumentValidator.onlyDigits(form.getPhone()));
        provider.setUsername(userService.generateUsername(form.getFirstName().trim(), form.getLastName().trim()));
        provider.setPassword(passwordEncoder.encode(form.getPassword()));
        provider.setBusinessName(form.getBusinessName().trim());
        provider.setAddress(form.getAddress().trim());
        provider.setServiceDescription(form.getServiceDescription());
        provider.setStatus(ProviderStatus.PENDING);
        provider.setRegistrationDate(LocalDateTime.now());

        return providerRepository.save(provider);
    }

    public List<Provider> findByStatus(ProviderStatus status) {
        return providerRepository.findByStatusOrderByRegistrationDateAsc(status);
    }

    public long countByStatus(ProviderStatus status) {
        return providerRepository.countByStatus(status);
    }

    // Prestador aprobado, para su página pública. Si no existe o no está aprobado, error.
    public Provider findApprovedById(Long id) {
        Provider provider = findById(id);
        if (provider.getStatus() != ProviderStatus.APPROVED) {
            throw new IllegalArgumentException("El prestador no está disponible");
        }
        return provider;
    }

    public Provider findById(Long id) {
        return providerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe el proveedor " + id));
    }

    // CU02 - El administrador acepta la solicitud
    @Transactional
    public Provider approve(Long id) {
        Provider provider = findPending(id);
        provider.setStatus(ProviderStatus.APPROVED);
        provider.setRejectionReason(null);
        return providerRepository.save(provider);
    }

    // CU02 - El administrador rechaza la solicitud, con un motivo opcional
    @Transactional
    public Provider reject(Long id, String reason) {
        Provider provider = findPending(id);
        provider.setStatus(ProviderStatus.REJECTED);
        provider.setRejectionReason(reason == null || reason.isBlank() ? null : reason.trim());
        return providerRepository.save(provider);
    }

    private Provider findPending(Long id) {
        Provider provider = findById(id);
        if (provider.getStatus() != ProviderStatus.PENDING) {
            throw new IllegalStateException("La solicitud de " + provider.getBusinessName() + " ya fue revisada");
        }
        return provider;
    }

}
