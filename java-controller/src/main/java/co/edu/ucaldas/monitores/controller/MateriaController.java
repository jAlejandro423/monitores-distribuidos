package co.edu.ucaldas.monitores.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.ucaldas.monitores.dto.Materia;
import co.edu.ucaldas.monitores.service.MonitorBackend;

/** GET /api/materias: lista para el desplegable del formulario (RF1). */
@RestController
@RequestMapping("/api/materias")
public class MateriaController {

    private final MonitorBackend backend;

    public MateriaController(MonitorBackend backend) {
        this.backend = backend;
    }

    @GetMapping
    public List<Materia> listar() {
        return backend.listarMaterias();
    }
}
