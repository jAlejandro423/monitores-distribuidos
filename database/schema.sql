-- Esquema de la base de datos (PC3) - Gestión de Monitores Académicos.
-- Compatible con MySQL 8.0 y SQLite (plan B). En MySQL usar utf8mb4.

CREATE TABLE IF NOT EXISTS materia (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS monitor (
    codigo VARCHAR(20) PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    correo VARCHAR(120) NOT NULL,
    materia_id INTEGER NOT NULL,
    FOREIGN KEY (materia_id) REFERENCES materia(id)
);

-- Datos semilla
INSERT INTO materia (nombre) VALUES
    ('Algoritmos'),
    ('Redes I'),
    ('Ingeniería de Software III');
