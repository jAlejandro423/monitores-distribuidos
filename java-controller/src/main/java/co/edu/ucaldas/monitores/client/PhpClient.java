package co.edu.ucaldas.monitores.client;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import co.edu.ucaldas.monitores.dto.Materia;
import co.edu.ucaldas.monitores.dto.Monitor;
import co.edu.ucaldas.monitores.dto.MonitorRequest;
import co.edu.ucaldas.monitores.dto.MonitorUpdateRequest;
import co.edu.ucaldas.monitores.service.MonitorBackend;

/**
 * Reenvía cada operación del CRUD al servicio PHP, con las mismas rutas del contrato.
 *
 * No decide nada por su cuenta: si PHP responde 4xx/5xx, RestClient lanza
 * RestClientResponseException, y si no hay conexión lanza ResourceAccessException.
 * Las dos las convierte GlobalExceptionHandler (mismo estado y cuerpo, o 502).
 * Por eso aquí no hay try/catch.
 */
@Component
public class PhpClient implements MonitorBackend {

    private final RestClient php;

    public PhpClient(RestClient phpRestClient) {
        this.php = phpRestClient;
    }

    @Override
    public List<Materia> listarMaterias() {
        return php.get().uri("/api/materias")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Materia>>() { });
    }

    @Override
    public List<Monitor> listarMonitores() {
        return php.get().uri("/api/monitores")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Monitor>>() { });
    }

    @Override
    public Monitor buscarMonitor(String codigo) {
        return php.get().uri("/api/monitores/{codigo}", codigo)
                .retrieve()
                .body(Monitor.class);
    }

    @Override
    public Monitor crearMonitor(MonitorRequest datos) {
        return php.post().uri("/api/monitores")
                .contentType(MediaType.APPLICATION_JSON)
                .body(datos)
                .retrieve()
                .body(Monitor.class);
    }

    @Override
    public Monitor actualizarMonitor(String codigo, MonitorUpdateRequest datos) {
        return php.put().uri("/api/monitores/{codigo}", codigo)
                .contentType(MediaType.APPLICATION_JSON)
                .body(datos)
                .retrieve()
                .body(Monitor.class);
    }

    @Override
    public void eliminarMonitor(String codigo) {
        php.delete().uri("/api/monitores/{codigo}", codigo)
                .retrieve()
                .toBodilessEntity();
    }
}
