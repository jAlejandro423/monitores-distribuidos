package co.edu.ucaldas.monitores.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo del POST /api/monitores (registro, RF1).
 * Las anotaciones son la validación básica que exige el taller (RF6): si alguna falla, Java responde 400.
 */
public record MonitorRequest(
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9-]{3,20}$",
                message = "debe tener entre 3 y 20 caracteres (letras, números o guion)")
        String codigo,

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
