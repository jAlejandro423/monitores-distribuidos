# Servicio Python (PC3, puerto 5000)

Capa de datos del sistema de monitores: persiste en SQLite (plan B del taller) y
expone las 6 rutas del contrato (`docs/AGENTS.md`, sección 5).

## Base de datos

La arquitectura del taller prefiere **MySQL 8.0** (ver `database/schema.sql`).
Este servicio usa **SQLite por defecto** como plan B, para poder ejecutarse sin
instalar un servidor de BD. Si se dispone de MySQL en PC3, se puede migrar la
capa de conexión (con `mysql-connector-python` o similar) manteniendo el mismo
contrato; el esquema está en `database/schema.sql`.

## Requisitos

- Python **3.10 o superior** (probado con 3.13).
- Dependencias en `requirements.txt`.

## Instalación

```bash
python -m venv .venv
# Windows
.venv\Scripts\activate
# Linux/macOS
source .venv/bin/activate

pip install -r requirements.txt
```

## Configuración

Variables de entorno (opcionales):

| Variable | Por defecto | Uso |
|---|---|---|
| `DB_FILE` | `python-service/gestion_monitores.db` | Ruta del archivo SQLite. |
| `HOST` | `0.0.0.0` | Interfaz de escucha. |
| `PORT` | `5000` | Puerto de escucha. |

También se puede crear un `.env` (no se sube al repositorio); el servicio lo carga
automáticamente al arrancar si existe.

## Ejecución

```bash
uvicorn main:app --host 0.0.0.0 --port 5000
```

o bien:

```bash
python main.py
```

Escucha en todas las interfaces (`0.0.0.0`), así PC2 (PHP) puede conectarse.

## Comportamiento

- Las 6 rutas del contrato se sirven con los códigos de estado acordados (200, 201, 204, 400, 404, 409).
- Todos los errores salen como `{"error": "mensaje en español"}`.
- Códigos duplicados → 409; materia inexistente → 400; monitor inexistente → 404.
- Las claves foráneas de SQLite se activan en cada conexión (`PRAGMA foreign_keys = ON`).
- Cada petición se registra en la consola (método, ruta, estado, duración).

## Prueba rápida

```bash
curl -i http://localhost:5000/api/materias
curl -i http://localhost:5000/api/monitores/MON999
```

Recordar abrir el puerto **5000** en el firewall de Windows (regla de entrada TCP).

## Estructura

```
python-service/
├── main.py            ← servicio FastAPI
├── requirements.txt   ← dependencias con versiones exactas
├── README.md
└── gestion_monitores.db  ← se crea automáticamente si no existe
```
