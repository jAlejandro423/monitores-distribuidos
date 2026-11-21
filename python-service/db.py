"""
Capa de acceso a datos del servicio Python (PC3).

Soporta dos motores:
- sqlite (plan B, por defecto): archivo local, sin servidor.
- mysql (opción preferida del taller): servidor MySQL 8.0.

La elección se hace con la variable de entorno DB_ENGINE (sqlite | mysql).
Para MySQL, credenciales y host se leen de MYSQL_HOST, MYSQL_PORT,
MYSQL_USER, MYSQL_PASSWORD y MYSQL_DATABASE.
"""

import os
import sqlite3

try:
    import mysql.connector as mysql_connector
    from mysql.connector import errors as mysql_errors
except ImportError:  # pragma: no cover - solo si no está instalado el conector
    mysql_connector = None
    mysql_errors = None

DB_ENGINE = os.getenv("DB_ENGINE", "sqlite").lower()

# Excepción de integridad unificada (SQLite y MySQL).
if DB_ENGINE == "mysql" and mysql_errors is not None:
    IntegrityError = mysql_errors.IntegrityError
else:
    IntegrityError = sqlite3.IntegrityError


def es_duplicado(exc) -> bool:
    """Detecta clave primaria duplicada (1062 en MySQL)."""
    if DB_ENGINE == "mysql":
        return getattr(exc, "errno", None) == 1062
    mensaje = str(exc).lower()
    return "unique" in mensaje or "primary key" in mensaje


def es_foreign_key(exc) -> bool:
    """Detecta violación de llave foránea (1452 en MySQL)."""
    if DB_ENGINE == "mysql":
        return getattr(exc, "errno", None) == 1452
    return "foreign key" in str(exc).lower()


class _MySQLCursor:
    """Cursor de MySQL que traduce '?' a '%s' y devuelve filas como dict."""

    def __init__(self, cursor):
        self._cursor = cursor

    def execute(self, sql: str, params=()):
        self._cursor.execute(sql.replace("?", "%s"), params or ())

    def executemany(self, sql: str, seq):
        self._cursor.executemany(sql.replace("?", "%s"), seq)

    def fetchone(self):
        fila = self._cursor.fetchone()
        return dict(fila) if fila is not None else None

    def fetchall(self):
        return [dict(fila) for fila in self._cursor.fetchall()]


class _MySQLConnection:
    """Conexión a MySQL con la misma interfaz que sqlite3.Connection."""

    def __init__(self):
        self._conn = mysql_connector.connect(
            host=os.getenv("MYSQL_HOST", "127.0.0.1"),
            port=int(os.getenv("MYSQL_PORT", "3306")),
            user=os.getenv("MYSQL_USER", "root"),
            password=os.getenv("MYSQL_PASSWORD", ""),
            database=os.getenv("MYSQL_DATABASE", "monitores"),
            charset="utf8mb4",
        )

    def cursor(self):
        return _MySQLCursor(self._conn.cursor(dictionary=True))

    def execute(self, sql: str, params=()):
        return self.cursor().execute(sql, params)

    def commit(self):
        self._conn.commit()

    def close(self):
        self._conn.close()


def get_db_connection():
    if DB_ENGINE == "mysql":
        if mysql_connector is None:
            raise RuntimeError(
                "DB_ENGINE=mysql pero no está instalado mysql-connector-python. "
                "Instalar con: pip install mysql-connector-python"
            )
        return _MySQLConnection()

    db_file = os.getenv(
        "DB_FILE",
        os.path.join(os.path.dirname(os.path.abspath(__file__)), "gestion_monitores.db"),
    )
    conn = sqlite3.connect(db_file, timeout=10)
    conn.row_factory = sqlite3.Row
    # SQLite no revisa FK por defecto: hay que activarlo en cada conexión.
    conn.execute("PRAGMA foreign_keys = ON;")
    return conn
