package co.edu.ucaldas.monitores.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.ucaldas.monitores.dto.Monitor;
import co.edu.ucaldas.monitores.dto.MonitorRequest;
import co.edu.ucaldas.monitores.dto.MonitorUpdateRequest;
import co.edu.ucaldas.monitores.service.MonitorBackend;
import jakarta.validation.Valid;

/**
 * Los 5 endpoints de monitores del contrato (RF1–RF5).
 * Solo recibe la petición, valida (@Valid → 400) y delega en el backend.
 */
@RestController
@RequestMapping("/api/monitores")
public class MonitorController {

    private final MonitorBackend backend;

    public MonitorController(MonitorBackend backend) {
        this.backend = backend;
    }

    /** RF2: listar todos. */
    @GetMapping
    public List<Monitor> listar() {
        return backend.listarMonitores();
    }

    /** RF3: buscar uno por código (200 o 404). */
    @GetMapping("/{codigo}")
    public Monitor buscar(@PathVariable String codigo) {
        return backend.buscarMonitor(codigo);
    }

    /** RF1 + RF6: registrar (201, 400 incompleto, 409 duplicado). */
    @PostMapping
    public ResponseEntity<Monitor> crear(@Valid @RequestBody MonitorRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(backend.crearMonitor(datos));
    }

    /** RF4 + RF6: actualizar nombre, correo o materia (200, 400, 404). */
    @PutMapping("/{codigo}")
    public Monitor actualizar(@PathVariable String codigo, @Valid @RequestBody MonitorUpdateRequest datos) {
        return backend.actualizarMonitor(codigo, datos);
    }

    /** RF5: eliminar (204 o 404). */
    @DeleteMapping("/{codigo}")
    public ResponseEntity<Void> eliminar(@PathVariable String codigo) {
        backend.eliminarMonitor(codigo);
        return ResponseEntity.noContent().build();
    }
}
