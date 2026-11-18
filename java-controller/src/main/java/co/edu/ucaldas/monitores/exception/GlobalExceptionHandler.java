package co.edu.ucaldas.monitores.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Convierte cualquier error en la forma del contrato: {"error": "mensaje"}.
 * Así la interfaz siempre sabe dónde leer el mensaje para mostrarlo.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Errores de negocio (404, 409, ...). */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> manejarApi(ApiException ex) {
        return error(ex.getStatus(), ex.getMessage());
    }

    /** Falla de Bean Validation → 400 con "<campo>: <motivo>" (RF6). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> manejarValidacion(MethodArgumentNotValidException ex) {
        FieldError campo = ex.getBindingResult().getFieldError();
        String mensaje = campo != null
                ? campo.getField() + ": " + campo.getDefaultMessage()
                : "Datos inválidos";
        return error(HttpStatus.BAD_REQUEST, mensaje);
    }

    /** Cuerpo vacío o JSON mal formado (ej. materiaId = "abc") → 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> manejarJsonInvalido(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> manejarMetodo(HttpRequestMethodNotSupportedException ex) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Método " + ex.getMethod() + " no permitido en esta ruta");
    }

    /** Cualquier otro error inesperado → 500, sin exponer detalles internos. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> manejarInesperado(Exception ex) {
        log.error("Error inesperado", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del controlador Java");
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("error", mensaje));
    }
}
