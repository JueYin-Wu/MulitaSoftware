package uy.edu.um.porytic2026.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.porytic2026.dto.ProfileForm;
import uy.edu.um.porytic2026.entities.JuridicProvider;
import uy.edu.um.porytic2026.entities.Provider;
import uy.edu.um.porytic2026.entities.Tourist;
import uy.edu.um.porytic2026.entities.User;
import uy.edu.um.porytic2026.exceptions.RegistrationException;
import uy.edu.um.porytic2026.repository.UserRepository;
import uy.edu.um.porytic2026.validation.DocumentValidator;

import java.time.Clock;
import java.time.LocalDate;

// Editar el perfil propio (turistas y prestadores; los administradores no tienen perfil).
@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final Clock clock;

    public ProfileService(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    // Carga en el formulario los datos actuales del usuario
    public ProfileForm toForm(User user) {
        ProfileForm form = new ProfileForm();
        form.setFirstName(user.getFirstName());
        form.setLastName(user.getLastName());
        form.setEmail(user.getEmail());
        form.setPhone(user.getPhone());
        if (user instanceof Tourist tourist && tourist.getBirthDate() != null) {
            form.setBirthDate(tourist.getBirthDate().toString());
        }
        if (user instanceof Provider provider) {
            form.setBusinessName(provider.getBusinessName());
            form.setAddress(provider.getAddress());
            form.setServiceDescription(provider.getServiceDescription());
        }
        if (user instanceof JuridicProvider company) {
            form.setLegalRepresentative(company.getLegalRepresentative());
        }
        return form;
    }

    // Guarda los cambios. El formulario ya viene validado (largos, formato del correo);
    // acá se controla lo que depende del tipo de usuario y de la base de datos.
    // Si algo está mal tira RegistrationException con el campo, para mostrar el mensaje al lado.
    @Transactional
    public User update(User user, ProfileForm form) {
        String email = trim(form.getEmail()).toLowerCase();
        if (!email.equalsIgnoreCase(user.getEmail())) {
            userRepository.findByEmail(email)
                    .filter(other -> !other.getId().equals(user.getId()))
                    .ifPresent(other -> {
                        throw new RegistrationException("email", "Ya existe otra cuenta con ese correo");
                    });
        }

        LocalDate birthDate = null;
        if (user instanceof Tourist) {
            String problem = DocumentValidator.birthDateProblem(form.getBirthDate(), LocalDate.now(clock));
            if (problem != null) {
                throw new RegistrationException("birthDate", problem.replace("Ingresá la", "Ingresá tu"));
            }
            birthDate = DocumentValidator.parseBirthDate(form.getBirthDate());
        }
        if (user instanceof Provider) {
            if (isBlank(form.getPhone())) {
                throw new RegistrationException("phone", "Ingresá un teléfono de contacto");
            }
            if (isBlank(form.getBusinessName())) {
                throw new RegistrationException("businessName", "Ingresá el nombre del emprendimiento");
            }
            if (isBlank(form.getAddress())) {
                throw new RegistrationException("address", "Ingresá la dirección");
            }
        }
        if (user instanceof JuridicProvider && isBlank(form.getLegalRepresentative())) {
            throw new RegistrationException("legalRepresentative", "Ingresá el responsable de la empresa");
        }

        user.setFirstName(trim(form.getFirstName()));
        user.setLastName(trim(form.getLastName()));
        user.setEmail(email);
        user.setPhone(isBlank(form.getPhone()) ? null : form.getPhone().trim());
        if (user instanceof Tourist tourist) {
            tourist.setBirthDate(birthDate);
        }
        if (user instanceof Provider provider) {
            provider.setBusinessName(trim(form.getBusinessName()));
            provider.setAddress(trim(form.getAddress()));
            provider.setServiceDescription(isBlank(form.getServiceDescription()) ? null : form.getServiceDescription().trim());
        }
        if (user instanceof JuridicProvider company) {
            company.setLegalRepresentative(trim(form.getLegalRepresentative()));
        }
        return userRepository.save(user);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
