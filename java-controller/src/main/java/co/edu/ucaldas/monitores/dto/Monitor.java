package co.edu.ucaldas.monitores.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Monitor tal como se devuelve en las consultas:
 * {"codigo": "MON001", "nombre": "Ana Pérez", "correo": "ana@ucaldas.edu.co", "materiaId": 2, "materiaNombre": "Redes I"}.
 * Si materiaNombre viene nulo, no se incluye en el JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Monitor(String codigo, String nombre, String correo, Integer materiaId, String materiaNombre) {
}
