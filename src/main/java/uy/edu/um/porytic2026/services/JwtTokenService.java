package uy.edu.um.porytic2026.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import uy.edu.um.porytic2026.api.dto.ApiDtos.TokenResponse;

import java.time.Instant;
import java.util.stream.Collectors;

// Crea los tokens JWT de la API. Un JWT tiene tres partes (encabezado, datos y firma). La firma se hace
// con la clave secreta del servidor, así nadie puede inventar ni modificar un token sin que se note.
@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final long expirationMinutes;

    public JwtTokenService(JwtEncoder jwtEncoder,
                           @Value("${app.jwt.expiration-minutes:60}") long expirationMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.expirationMinutes = expirationMinutes;
    }

    public TokenResponse createToken(UserDetails user) {
        Instant now = Instant.now();
        String roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("uruguay-mnatural")
                .subject(user.getUsername())          // a quién pertenece el token
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirationMinutes * 60))  // vence solo
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", expirationMinutes * 60);
    }
}
