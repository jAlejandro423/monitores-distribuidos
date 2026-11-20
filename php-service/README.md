# Servicio PHP (PC2, puerto 8000)

Capa intermedia del sistema de monitores: recibe las peticiones del controlador Java
y las reenvía al servicio Python, devolviendo el código de estado y el cuerpo tal cual.
El contrato de la API está en [`docs/AGENTS.md`](../docs/AGENTS.md) (sección 5).

## Requisitos

- PHP **8.0 o superior** (ideal 8.2+), con la extensión **curl** activada.
- Versión usada en PC2: **PHP 8.5.12** (CLI, Windows x64), extensión `curl` activada.
  No usa Composer ni dependencias externas.

Para comprobarlo:

```bash
php -v
php -m | grep -i curl
```

En Windows, si `curl` no aparece, quitar el `;` de la línea `extension=curl` en el `php.ini`.

## Configuración

```bash
cp .env.example .env
```

Editar `.env` y poner la IP real de PC3 en `PYTHON_URL` (por ejemplo `http://<IP-PC3>:5000`).
También se puede definir `PYTHON_URL` como variable de entorno; tiene prioridad sobre `.env`.
El archivo `.env` no se sube al repositorio.

| Variable | Por defecto | Uso |
|---|---|---|
| `PYTHON_URL` | `http://localhost:5000` | URL base del servicio Python (PC3). |
| `PYTHON_TIMEOUT` | `5` | Segundos máximos de espera hacia Python (conexión: máximo 3 s). |

## Ejecución

Desde la carpeta `php-service/`:

```bash
php -S 0.0.0.0:8000 index.php
```

Escucha en todas las interfaces (no usar `localhost`), así PC1 puede conectarse.

Para una prueba rápida sin `.env`, apuntando a otra URL de Python:

```bash
PYTHON_URL=http://<IP-PC3>:5000 php -S 0.0.0.0:8000 index.php
```

En PowerShell: `$env:PYTHON_URL="http://<IP-PC3>:5000"; php -S 0.0.0.0:8000 index.php`.

## Comportamiento

- Las 6 rutas del contrato se reenvían a Python con el mismo método, la misma ruta,
  el mismo cuerpo y `Content-Type: application/json`.
- El código de estado y el cuerpo de Python se devuelven tal cual (200, 201, 204 sin cuerpo, 400, 404, 409...).
- Si Python no responde (apagado, IP errada o más de `PYTHON_TIMEOUT` s) → **502** `{"error": "Servicio Python no disponible"}`.
- Si Python responde un error sin JSON (por ejemplo, una página HTML) → mismo código con
  `{"error": "El servicio Python respondió con error N"}`; si responde 2xx sin JSON válido → **502**
  `{"error": "Respuesta inválida del servicio Python"}`.
- Otra ruta → 404, y otro método en una ruta existente → 405, ambos con `{"error": ...}`.

## Prueba rápida

Con Python apagado debe responder 502:

```bash
curl -i http://localhost:8000/api/monitores
```
Cada petición se registra en la consola con método, ruta, estado y duración.
Recordar abrir el puerto 8000 en el firewall de Windows (regla de entrada TCP).

## Estructura

```
php-service/
├── index.php        ← controlador frontal y enrutador de las 6 rutas
├── src/config.php   ← carga PYTHON_URL y PYTHON_TIMEOUT desde el entorno o .env
├── src/proxy.php    ← reenvío a Python con cURL y propagación de la respuesta
├── .env.example     ← plantilla de configuración (sin IPs reales)
└── README.md
```

## Estado

- [x] Estructura base, configuración y registro de peticiones.
- [x] Enrutador con las 6 rutas del contrato.
- [x] Reenvío a Python con cURL, propagando estado y cuerpo.
- [x] 502 `{"error": "Servicio Python no disponible"}` si Python no responde.
- [ ] Prueba de punta a punta con el servicio Python real (integración).
