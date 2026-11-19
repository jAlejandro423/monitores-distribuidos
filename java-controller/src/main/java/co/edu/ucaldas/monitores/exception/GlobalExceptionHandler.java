package co.edu.ucaldas.monitores.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Convierte cualquier error en la forma del contrato: {"error": "mensaje"}.
 * Así la interfaz siempre sabe dónde leer el mensaje para mostrarlo.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ObjectMapper json;

    public GlobalExceptionHandler(ObjectMapper json) {
        this.json = json;
    }

    // ---------- Errores que detecta Java (RF6) ----------

    /** Falla de Bean Validation → 400 con "<campo>: <motivo>". */
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

    // ---------- Errores que vienen de PHP ----------

    /**
     * PHP respondió 4xx/5xx (404 no existe, 409 duplicado, 400 materia inexistente...).
     * Java devuelve el MISMO código y el MISMO cuerpo: no reinterpreta la respuesta.
     * Única excepción: si el cuerpo no trae {"error": ...} (por ejemplo, una página HTML de error
     * de PHP), se reemplaza por un mensaje JSON para no romper la interfaz.
     */
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<?> manejarRespuestaPhp(RestClientResponseException ex) {
        String cuerpo = ex.getResponseBodyAsString();
        if (traeCampoError(cuerpo)) {
            return ResponseEntity.status(ex.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo);
        }
        log.warn("PHP respondió {} sin el formato {{\"error\": ...}}: {}", ex.getStatusCode().value(), cuerpo);
        return error(ex.getStatusCode().value(),
                "El servicio PHP respondió con error " + ex.getStatusCode().value());
    }

    /** No hay conexión con PHP (apagado, IP equivocada, firewall, tiempo agotado) → 502. */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, String>> manejarSinConexion(ResourceAccessException ex) {
        return error(HttpStatus.BAD_GATEWAY, "Servicio PHP no disponible");
    }

    /** PHP respondió 2xx pero con un cuerpo que no se puede leer como el contrato → 502. */
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, String>> manejarRespuestaInvalida(RestClientException ex) {
        log.warn("Respuesta inválida del servicio PHP", ex);
        return error(HttpStatus.BAD_GATEWAY, "Respuesta inválida del servicio PHP");
    }

    // ---------- Cualquier otro ----------

    /** Error inesperado → 500, sin exponer detalles internos. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> manejarInesperado(Exception ex) {
        log.error("Error inesperado", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del controlador Java");
    }

    private boolean traeCampoError(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            return false;
        }
        try {
            JsonNode nodo = json.readTree(cuerpo);
            return nodo.isObject() && nodo.hasNonNull("error");
        } catch (Exception e) {
            return false;
        }
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String mensaje) {
        return error(status.value(), mensaje);
    }

    private ResponseEntity<Map<String, String>> error(int status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("error", mensaje));
    }
}
