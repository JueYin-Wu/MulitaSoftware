package uy.edu.um.porytic2026.api;

// Usuario o contraseña incorrectos al pedir un token -> la API responde 401 Unauthorized
public class ApiUnauthorizedException extends RuntimeException {

    public ApiUnauthorizedException(String message) {
        super(message);
    }
}
