package co.edu.ucaldas.monitores.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo del PUT /api/monitores/{codigo} (actualización, RF4).
 * No lleva código: el código va en la ruta y no se puede modificar.
 */
public record MonitorUpdateRequest(
        @NotBlank(message = "es obligatorio")
        @Size(min = 3, max = 120, message = "debe tener entre 3 y 120 caracteres")
        String nombre,

        @NotBlank(message = "es obligatorio")
        @Email(message = "no tiene un formato válido")
        @Size(max = 120, message = "debe tener máximo 120 caracteres")
        String correo,

        @NotNull(message = "es obligatorio")
        @Positive(message = "debe ser un entero positivo")
        Integer materiaId
) {
}
