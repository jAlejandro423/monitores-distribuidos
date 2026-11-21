-- Esquema de la base de datos (PC3) - Gestión de Monitores Académicos.
-- Versión para MySQL 8.0 (opción preferida del taller). Charset utf8mb4.
-- Para SQLite (plan B) ver python-service/main.py, que crea las tablas al arrancar.

CREATE TABLE IF NOT EXISTS materia (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS monitor (
    codigo VARCHAR(20) PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    correo VARCHAR(120) NOT NULL,
    materia_id INT NOT NULL,
    FOREIGN KEY (materia_id) REFERENCES materia(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Datos semilla
INSERT INTO materia (nombre) VALUES
    ('Algoritmos'),
    ('Redes I'),
    ('Ingeniería de Software III');
