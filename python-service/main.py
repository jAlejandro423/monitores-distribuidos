"""
Servicio de datos (PC3) - Gestión de Monitores Académicos.

FastAPI + SQLite (plan B del taller). Expone las 6 rutas del contrato de
docs/AGENTS.md (sección 5) y responde siempre en la forma {"error": "..."}.

Arranque:
    uvicorn main:app --host 0.0.0.0 --port 5000
o bien:
    python main.py
"""

import logging
import os
import sqlite3
import time
from typing import List, Optional

from fastapi import FastAPI, HTTPException, Request, Response, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from starlette.exceptions import HTTPException as StarletteHTTPException

# ---------------------------------------------------------------------------
# Configuración
# ---------------------------------------------------------------------------
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
)
logger = logging.getLogger("monitores-python")

# Carga opcional de .env (sin dependencias externas), como en el servicio PHP.
def _cargar_env(ruta: str) -> None:
    if not os.path.isfile(ruta):
        return
    with open(ruta, encoding="utf-8") as f:
        for linea in f:
            linea = linea.strip()
            if not linea or linea.startswith("#") or "=" not in linea:
                continue
            clave, valor = linea.split("=", 1)
            clave = clave.strip()
            valor = valor.strip().strip("\"'")
            if clave and clave not in os.environ:
                os.environ[clave] = valor


_cargar_env(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".env"))

DB_FILE = os.getenv(
    "DB_FILE",
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "gestion_monitores.db"),
)
HOST = os.getenv("HOST", "0.0.0.0")
PORT = int(os.getenv("PORT", "5000"))

app = FastAPI(title="Servicio de Datos - Gestión de Monitores (PC3)")

# CORS no es estrictamente necesario (solo lo llama PHP), pero no molesta.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ---------------------------------------------------------------------------
# Base de datos (SQLite, plan B)
# ---------------------------------------------------------------------------
def get_db_connection() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_FILE, timeout=10)
    conn.row_factory = sqlite3.Row
    # SQLite no revisa FK por defecto: hay que activarlo en cada conexión.
    conn.execute("PRAGMA foreign_keys = ON;")
    return conn


def init_db() -> None:
    conn = get_db_connection()
    try:
        cur = conn.cursor()
        cur.execute(
            """
            CREATE TABLE IF NOT EXISTS materia (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL UNIQUE
            );
            """
        )
        cur.execute(
            """
            CREATE TABLE IF NOT EXISTS monitor (
                codigo TEXT PRIMARY KEY,
                nombre TEXT NOT NULL,
                correo TEXT NOT NULL,
                materia_id INTEGER NOT NULL,
                FOREIGN KEY (materia_id) REFERENCES materia(id)
            );
            """
        )
        cur.execute("SELECT COUNT(*) FROM materia;")
        if cur.fetchone()[0] == 0:
            cur.executemany(
                "INSERT INTO materia (nombre) VALUES (?);",
                [
                    ("Algoritmos",),
                    ("Redes I",),
                    ("Ingeniería de Software III",),
                ],
            )
        conn.commit()
    finally:
        conn.close()


@app.on_event("startup")
def _startup() -> None:
    init_db()
    logger.info("Base de datos SQLite lista en %s", DB_FILE)


# ---------------------------------------------------------------------------
# Modelos Pydantic (contrato)
# ---------------------------------------------------------------------------
class MateriaResponse(BaseModel):
    id: int
    nombre: str


class MonitorCreate(BaseModel):
    codigo: str
    nombre: str
    correo: str
    materiaId: int


class MonitorUpdate(BaseModel):
    nombre: str
    correo: str
    materiaId: int


class MonitorResponse(BaseModel):
    codigo: str
    nombre: str
    correo: str
    materiaId: int
    materiaNombre: str


# ---------------------------------------------------------------------------
# Manejadores de error: siempre {"error": "..."}
# ---------------------------------------------------------------------------
def _error(status_code: int, mensaje: str) -> JSONResponse:
    return JSONResponse(status_code=status_code, content={"error": mensaje})


@app.exception_handler(HTTPException)
async def _http_exception_handler(request: Request, exc: HTTPException) -> JSONResponse:
    if isinstance(exc.detail, dict) and "error" in exc.detail:
        mensaje = exc.detail["error"]
    elif isinstance(exc.detail, str):
        mensaje = exc.detail
    else:
        mensaje = str(exc.detail)
    return _error(exc.status_code, mensaje)


@app.exception_handler(StarletteHTTPException)
async def _starlette_exception_handler(
    request: Request, exc: StarletteHTTPException
) -> JSONResponse:
    if exc.status_code == 404:
        mensaje = f"Ruta no encontrada: {request.url.path}"
    elif exc.status_code == 405:
        mensaje = f"Método {request.method} no permitido en {request.url.path}"
    else:
        mensaje = exc.detail if isinstance(exc.detail, str) else str(exc.detail)
    return _error(exc.status_code, mensaje)


@app.exception_handler(RequestValidationError)
async def _validation_exception_handler(
    request: Request, exc: RequestValidationError
) -> JSONResponse:
    errores = exc.errors()
    if errores and any(e.get("type") == "json_invalid" for e in errores):
        return _error(status.HTTP_400_BAD_REQUEST, "El cuerpo de la petición no es un JSON válido")
    if errores:
        primero = errores[0]
        campo = ".".join(str(x) for x in primero.get("loc", []) if x not in ("body",))
        mensaje = primero.get("msg", "Datos inválidos")
        if campo:
            mensaje = f"{campo}: {mensaje}"
    else:
        mensaje = "Datos inválidos"
    return _error(status.HTTP_400_BAD_REQUEST, mensaje)


# Middleware de registro de peticiones.
@app.middleware("http")
async def _registrar_peticion(request: Request, call_next):
    inicio = time.perf_counter()
    response = await call_next(request)
    duracion_ms = int((time.perf_counter() - inicio) * 1000)
    logger.info(
        "%s %s -> %d (%d ms)",
        request.method,
        request.url.path,
        response.status_code,
        duracion_ms,
    )
    return response


# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------
@app.get("/api/materias", response_model=List[MateriaResponse], status_code=status.HTTP_200_OK)
def listar_materias():
    conn = get_db_connection()
    try:
        cur = conn.cursor()
        cur.execute("SELECT id, nombre FROM materia ORDER BY id;")
        filas = cur.fetchall()
        return [{"id": f["id"], "nombre": f["nombre"]} for f in filas]
    finally:
        conn.close()


@app.get("/api/monitores", response_model=List[MonitorResponse], status_code=status.HTTP_200_OK)
def listar_monitores():
    conn = get_db_connection()
    try:
        cur = conn.cursor()
        cur.execute(
            """
            SELECT m.codigo, m.nombre, m.correo, m.materia_id AS materiaId,
                   mat.nombre AS materiaNombre
            FROM monitor m
            JOIN materia mat ON m.materia_id = mat.id
            ORDER BY m.codigo;
            """
        )
        filas = cur.fetchall()
        return [dict(f) for f in filas]
    finally:
        conn.close()


@app.get("/api/monitores/{codigo}", response_model=MonitorResponse, status_code=status.HTTP_200_OK)
def obtener_monitor(codigo: str):
    conn = get_db_connection()
    try:
        cur = conn.cursor()
        cur.execute(
            """
            SELECT m.codigo, m.nombre, m.correo, m.materia_id AS materiaId,
                   mat.nombre AS materiaNombre
            FROM monitor m
            JOIN materia mat ON m.materia_id = mat.id
            WHERE m.codigo = ?;
            """,
            (codigo,),
        )
        fila = cur.fetchone()
        if not fila:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail={"error": f"No existe un monitor con código {codigo}"},
            )
        return dict(fila)
    finally:
        conn.close()


@app.post("/api/monitores", response_model=MonitorResponse, status_code=status.HTTP_201_CREATED)
def crear_monitor(data: MonitorCreate):
    conn = get_db_connection()
    try:
        cur = conn.cursor()

        cur.execute("SELECT codigo FROM monitor WHERE codigo = ?;", (data.codigo,))
        if cur.fetchone():
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail={"error": f"Ya existe un monitor con código {data.codigo}"},
            )

        cur.execute("SELECT id, nombre FROM materia WHERE id = ?;", (data.materiaId,))
        materia = cur.fetchone()
        if not materia:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail={"error": f"La materia {data.materiaId} no existe"},
            )

        cur.execute(
            "INSERT INTO monitor (codigo, nombre, correo, materia_id) VALUES (?, ?, ?, ?);",
            (data.codigo, data.nombre, data.correo, data.materiaId),
        )
        conn.commit()

        return {
            "codigo": data.codigo,
            "nombre": data.nombre,
            "correo": data.correo,
            "materiaId": data.materiaId,
            "materiaNombre": materia["nombre"],
        }
    except sqlite3.IntegrityError as exc:
        # Respaldo por si la BD detecta la violación antes que nosotros.
        mensaje = str(exc).lower()
        if "unique" in mensaje or "primary key" in mensaje:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail={"error": f"Ya existe un monitor con código {data.codigo}"},
            )
        if "foreign key" in mensaje:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail={"error": f"La materia {data.materiaId} no existe"},
            )
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"error": "No se pudo registrar el monitor"},
        )
    finally:
        conn.close()


@app.put("/api/monitores/{codigo}", response_model=MonitorResponse, status_code=status.HTTP_200_OK)
def actualizar_monitor(codigo: str, data: MonitorUpdate):
    conn = get_db_connection()
    try:
        cur = conn.cursor()

        cur.execute("SELECT codigo FROM monitor WHERE codigo = ?;", (codigo,))
        if not cur.fetchone():
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail={"error": f"No existe un monitor con código {codigo}"},
            )

        cur.execute("SELECT id, nombre FROM materia WHERE id = ?;", (data.materiaId,))
        materia = cur.fetchone()
        if not materia:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail={"error": f"La materia {data.materiaId} no existe"},
            )

        cur.execute(
            "UPDATE monitor SET nombre = ?, correo = ?, materia_id = ? WHERE codigo = ?;",
            (data.nombre, data.correo, data.materiaId, codigo),
        )
        conn.commit()

        return {
            "codigo": codigo,
            "nombre": data.nombre,
            "correo": data.correo,
            "materiaId": data.materiaId,
            "materiaNombre": materia["nombre"],
        }
    except sqlite3.IntegrityError as exc:
        mensaje = str(exc).lower()
        if "foreign key" in mensaje:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail={"error": f"La materia {data.materiaId} no existe"},
            )
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"error": "No se pudo actualizar el monitor"},
        )
    finally:
        conn.close()


@app.delete("/api/monitores/{codigo}", status_code=status.HTTP_204_NO_CONTENT)
def eliminar_monitor(codigo: str):
    conn = get_db_connection()
    try:
        cur = conn.cursor()
        cur.execute("SELECT codigo FROM monitor WHERE codigo = ?;", (codigo,))
        if not cur.fetchone():
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail={"error": f"No existe un monitor con código {codigo}"},
            )
        cur.execute("DELETE FROM monitor WHERE codigo = ?;", (codigo,))
        conn.commit()
        return None
    finally:
        conn.close()


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("main:app", host=HOST, port=PORT, reload=False)
