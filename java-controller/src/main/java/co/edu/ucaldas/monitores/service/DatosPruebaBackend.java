package co.edu.ucaldas.monitores.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import co.edu.ucaldas.monitores.dto.Materia;
import co.edu.ucaldas.monitores.dto.Monitor;
import co.edu.ucaldas.monitores.dto.MonitorRequest;
import co.edu.ucaldas.monitores.dto.MonitorUpdateRequest;
import co.edu.ucaldas.monitores.exception.ApiException;

/**
 * Implementación TEMPORAL con datos de prueba en memoria (hito del jueves en la mañana).
 * Responde igual que lo hará la cadena PHP → Python (200/201/204/404/409), para poder probar
 * los 6 endpoints en Postman sin depender del resto del equipo.
 * Los datos se pierden al reiniciar. Se reemplaza por PhpClient en el siguiente paso.
 */
@Service
public class DatosPruebaBackend implements MonitorBackend {

    /** Mismas materias que el INSERT del esquema inicial. */
    private final Map<Integer, Materia> materias = new LinkedHashMap<>();
    private final Map<String, Monitor> monitores = new LinkedHashMap<>();

    public DatosPruebaBackend() {
        materias.put(1, new Materia(1, "Algoritmos"));
        materias.put(2, new Materia(2, "Redes I"));
        materias.put(3, new Materia(3, "Ingeniería de Software III"));

        guardar(new Monitor("MON001", "Ana Pérez", "ana@ucaldas.edu.co", 2, null));
        guardar(new Monitor("MON002", "Carlos Gómez", "carlos@ucaldas.edu.co", 1, null));
    }

    @Override
    public synchronized List<Materia> listarMaterias() {
        return new ArrayList<>(materias.values());
    }

    @Override
    public synchronized List<Monitor> listarMonitores() {
        return new ArrayList<>(monitores.values());
    }

    @Override
    public synchronized Monitor buscarMonitor(String codigo) {
        Monitor monitor = monitores.get(codigo);
        if (monitor == null) {
            throw noEncontrado(codigo);
        }
        return monitor;
    }

    @Override
    public synchronized Monitor crearMonitor(MonitorRequest datos) {
        if (monitores.containsKey(datos.codigo())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un monitor con el código " + datos.codigo());
        }
        validarMateria(datos.materiaId());
        return guardar(new Monitor(datos.codigo(), datos.nombre(), datos.correo(), datos.materiaId(), null));
    }

    @Override
    public synchronized Monitor actualizarMonitor(String codigo, MonitorUpdateRequest datos) {
        if (!monitores.containsKey(codigo)) {
            throw noEncontrado(codigo);
        }
        validarMateria(datos.materiaId());
        return guardar(new Monitor(codigo, datos.nombre(), datos.correo(), datos.materiaId(), null));
    }

    @Override
    public synchronized void eliminarMonitor(String codigo) {
        if (monitores.remove(codigo) == null) {
            throw noEncontrado(codigo);
        }
    }

    /** Guarda el monitor agregándole el nombre de su materia, como hará el JOIN en Python. */
    private Monitor guardar(Monitor m) {
        String materiaNombre = materias.get(m.materiaId()).nombre();
        Monitor completo = new Monitor(m.codigo(), m.nombre(), m.correo(), m.materiaId(), materiaNombre);
        monitores.put(m.codigo(), completo);
        return completo;
    }

    /** Simula la llave foránea de la BD: materia inexistente → 400. */
    private void validarMateria(Integer materiaId) {
        if (!materias.containsKey(materiaId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La materia " + materiaId + " no existe");
        }
    }

    private ApiException noEncontrado(String codigo) {
        return new ApiException(HttpStatus.NOT_FOUND, "No existe un monitor con el código " + codigo);
    }
}
