package uy.edu.um.porytic2026.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.porytic2026.entities.*;
import uy.edu.um.porytic2026.exceptions.BusinessException;
import uy.edu.um.porytic2026.repository.ProviderRepository;
import uy.edu.um.porytic2026.repository.SanctionRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

// Sanciones que los administradores aplican a los prestadores.
// - Advertencia: queda registrada y el prestador la ve en su portal, pero sigue operando.
// - Suspensión: el prestador no puede iniciar sesión, sus actividades dejan de mostrarse y no se puede reservar,
//   hasta que un administrador la levanta.
@Service
public class SanctionService {

    public static final int MIN_REASON_LENGTH = 10;
    public static final int MAX_REASON_LENGTH = 500;

    private final SanctionRepository sanctionRepository;
    private final ProviderRepository providerRepository;
    private final Clock clock;

    public SanctionService(SanctionRepository sanctionRepository, ProviderRepository providerRepository, Clock clock) {
        this.sanctionRepository = sanctionRepository;
        this.providerRepository = providerRepository;
        this.clock = clock;
    }

    // Historial de sanciones del prestador, la más reciente primero
    public List<Sanction> findByProvider(Long providerId) {
        return sanctionRepository.findByProviderIdOrderByDateDesc(providerId);
    }

    public Provider findProvider(Long providerId) {
        return providerRepository.findById(providerId)
                .orElseThrow(() -> new BusinessException("No existe el prestador"));
    }

    // Aplica una sanción. La fecha se guarda sola (el momento en que el admin la aplica).
    @Transactional
    public Sanction apply(Long providerId, Administrator administrator, SanctionType type, String reason) {
        Provider provider = findProvider(providerId);

        // Solo se sanciona a prestadores aprobados: los pendientes o rechazados todavía no operan
        if (provider.getStatus() != ProviderStatus.APPROVED) {
            throw new BusinessException("Solo se puede sancionar a un prestador aprobado");
        }
        if (type == null) {
            throw new BusinessException("type", "Elegí el tipo de sanción");
        }
        String text = reason == null ? "" : reason.trim();
        if (text.length() < MIN_REASON_LENGTH) {
            throw new BusinessException("reason",
                    "Escribí el motivo de la sanción (al menos " + MIN_REASON_LENGTH + " caracteres)");
        }
        if (text.length() > MAX_REASON_LENGTH) {
            throw new BusinessException("reason", "El motivo puede tener hasta " + MAX_REASON_LENGTH + " caracteres");
        }
        if (type == SanctionType.SUSPENSION && provider.isSuspended()) {
            throw new BusinessException("type", provider.getBusinessName() + " ya está suspendido");
        }

        Sanction sanction = sanctionRepository.save(Sanction.builder()
                .provider(provider)
                .administrator(administrator)
                .type(type)
                .reason(text)
                .date(LocalDateTime.now(clock))
                .build());

        if (type == SanctionType.SUSPENSION) {
            provider.setSuspended(true);
            providerRepository.save(provider);
        }
        return sanction;
    }

    // Levanta la suspensión vigente: el prestador vuelve a operar y sus actividades se vuelven a mostrar
    @Transactional
    public Sanction liftSuspension(Long providerId) {
        Provider provider = findProvider(providerId);
        if (!provider.isSuspended()) {
            throw new BusinessException(provider.getBusinessName() + " no está suspendido");
        }
        Sanction suspension = sanctionRepository
                .findFirstByProviderIdAndTypeAndLiftedDateIsNullOrderByDateDesc(providerId, SanctionType.SUSPENSION)
                .orElse(null);
        if (suspension != null) {
            suspension.setLiftedDate(LocalDateTime.now(clock));
            sanctionRepository.save(suspension);
        }
        provider.setSuspended(false);
        providerRepository.save(provider);
        return suspension;
    }
}
