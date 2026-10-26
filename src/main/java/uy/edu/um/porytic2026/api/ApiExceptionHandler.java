package uy.edu.um.porytic2026.api;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import uy.edu.um.porytic2026.api.dto.ApiDtos.ApiError;

import java.time.Instant;

// Convierte los errores de la API en una respuesta JSON con el código HTTP que corresponde:
//   400 Bad Request  -> parámetros que faltan o tienen mal formato
//   401 Unauthorized -> usuario o contraseña incorrectos al pedir el token
//   404 Not Found    -> el recurso no existe
//   500 Internal Server Error -> error inesperado del servidor
// Solo se aplica a los controladores de la API (paquete api), no a las páginas del sitio.
@RestControllerAdvice(basePackages = "uy.edu.um.porytic2026.api")
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({IllegalArgumentException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> badRequest(Exception e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> badBody(HttpMessageNotReadableException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo del pedido no es un JSON válido", request);
    }

    @ExceptionHandler(ApiUnauthorizedException.class)
    public ResponseEntity<ApiError> unauthorized(ApiUnauthorizedException e, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, e.getMessage(), request);
    }

    @ExceptionHandler(ApiNotFoundException.class)
    public ResponseEntity<ApiError> notFound(ApiNotFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> serverError(Exception e, HttpServletRequest request) {
        log.error("Error inesperado en la API", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado en el servidor", request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request) {
        ApiError body = new ApiError(status.value(), status.getReasonPhrase(), message,
                request.getRequestURI(), Instant.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
