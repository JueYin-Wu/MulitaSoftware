package uy.edu.um.porytic2026.api;

// El recurso pedido no existe -> la API responde 404 Not Found
public class ApiNotFoundException extends RuntimeException {

    public ApiNotFoundException(String message) {
        super(message);
    }
}
