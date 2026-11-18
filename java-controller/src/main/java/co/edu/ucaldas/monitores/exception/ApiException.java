package co.edu.ucaldas.monitores.exception;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio con su código HTTP (404 no existe, 409 duplicado, etc.).
 * El GlobalExceptionHandler la convierte en {"error": "mensaje"}.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
