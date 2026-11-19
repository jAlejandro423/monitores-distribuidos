package co.edu.ucaldas.monitores.service;

import java.util.List;

import co.edu.ucaldas.monitores.dto.Materia;
import co.edu.ucaldas.monitores.dto.Monitor;
import co.edu.ucaldas.monitores.dto.MonitorRequest;
import co.edu.ucaldas.monitores.dto.MonitorUpdateRequest;

/**
 * Lo que el controlador necesita de la capa siguiente.
 * La implementa PhpClient, que reenvía cada operación al servicio PHP (PC2).
 * Los controladores dependen de esta interfaz, no de cómo se habla con PHP.
 * Los errores (404, 409, 400 de la BD) llegan desde PHP y los propaga GlobalExceptionHandler.
 */
public interface MonitorBackend {

    List<Materia> listarMaterias();

    List<Monitor> listarMonitores();

    Monitor buscarMonitor(String codigo);

    Monitor crearMonitor(MonitorRequest datos);

    Monitor actualizarMonitor(String codigo, MonitorUpdateRequest datos);

    void eliminarMonitor(String codigo);
}
