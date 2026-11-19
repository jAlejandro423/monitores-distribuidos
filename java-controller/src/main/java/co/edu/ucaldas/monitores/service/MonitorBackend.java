package co.edu.ucaldas.monitores.service;

import java.util.List;

import co.edu.ucaldas.monitores.dto.Materia;
import co.edu.ucaldas.monitores.dto.Monitor;
import co.edu.ucaldas.monitores.dto.MonitorRequest;
import co.edu.ucaldas.monitores.dto.MonitorUpdateRequest;

/**
 * Lo que el controlador necesita de la capa siguiente.
 * Hoy lo implementa DatosPruebaBackend (datos en memoria); en el siguiente paso lo implementará
 * PhpClient, que reenvía cada operación al servicio PHP. Los controladores no cambian.
 */
public interface MonitorBackend {

    List<Materia> listarMaterias();

    List<Monitor> listarMonitores();

    /** @throws co.edu.ucaldas.monitores.exception.ApiException 404 si no existe */
    Monitor buscarMonitor(String codigo);

    /** @throws co.edu.ucaldas.monitores.exception.ApiException 409 si el código ya existe */
    Monitor crearMonitor(MonitorRequest datos);

    /** @throws co.edu.ucaldas.monitores.exception.ApiException 404 si no existe */
    Monitor actualizarMonitor(String codigo, MonitorUpdateRequest datos);

    /** @throws co.edu.ucaldas.monitores.exception.ApiException 404 si no existe */
    void eliminarMonitor(String codigo);
}
