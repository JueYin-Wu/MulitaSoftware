package uy.edu.um.porytic2026.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uy.edu.um.porytic2026.entities.Tourist;
import uy.edu.um.porytic2026.entities.User;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.porytic2026.dto.TouristRegistrationForm;
import uy.edu.um.porytic2026.exceptions.RegistrationException;
import uy.edu.um.porytic2026.repository.TouristRepository;
import uy.edu.um.porytic2026.repository.UserRepository;
import uy.edu.um.porytic2026.validation.DocumentValidator;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TouristRepository touristRepository;
    private final PasswordEncoder passwordEncoder;

    // Spring pasa automáticamente los repositorios y el encoder (inyección por constructor)
    public UserService(UserRepository userRepository, TouristRepository touristRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.touristRepository = touristRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // CU03 - Registro de turista. El formulario ya viene validado desde el controller;
    // acá se controla lo que necesita la base de datos (correo y cédula sin repetir).
    @Transactional
    public Tourist registerTourist(TouristRegistrationForm form) {
        String email = form.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new RegistrationException("email", "Correo no válido: ya existe una cuenta con ese correo");
        }

        String idDocument = DocumentValidator.onlyDigits(form.getIdDocument());
        if (!DocumentValidator.isValidCI(idDocument)) {
            throw new RegistrationException("idDocument", "Cédula no válida");
        }
        // CU03 4b: "cédula ya registrada"
        if (touristRepository.existsByIdDocument(idDocument)) {
            throw new RegistrationException("idDocument", "Cédula ya registrada");
        }

        Tourist tourist = Tourist.builder()
                .firstName(form.getFirstName().trim())
                .lastName(form.getLastName().trim())
                .email(email)
                .username(generateUsername(form.getFirstName().trim(), form.getLastName().trim()))
                .password(passwordEncoder.encode(form.getPassword())) // se guarda encriptada
                .phone(DocumentValidator.onlyDigits(form.getPhone()))
                .idDocument(idDocument)
                .build();

        return userRepository.save(tourist);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // Arma "nombre.apellido" y, si ya existe, le agrega un número: juan.perez1, juan.perez2...
    // Es público porque también lo usa ProviderService al registrar proveedores.
    public String generateUsername(String firstName, String lastName) {
        String base = (firstName + "." + lastName).toLowerCase().replace(" ", "")
                .replaceAll("[áà]", "a").replaceAll("[éè]", "e").replaceAll("[íì]", "i")
                .replaceAll("[óò]", "o").replaceAll("[úùü]", "u").replace("ñ", "n");
        String username = base;
        int i = 1;
        while (userRepository.existsByUsername(username)) {
            username = base + i;
            i++;
        }
        return username;
    }
}