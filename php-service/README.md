# Servicio PHP (PC2, puerto 8000)

Capa intermedia del sistema de monitores: recibe las peticiones del controlador Java
y las reenvía al servicio Python, devolviendo el código de estado y el cuerpo tal cual.
El contrato de la API está en [`docs/AGENTS.md`](../docs/AGENTS.md) (sección 5).

## Requisitos

- PHP **8.0 o superior** (ideal 8.2+), con la extensión **curl** activada.
- Versión usada en PC2: _pendiente de anotar (`php -v`)_.

## Configuración

```bash
cp .env.example .env
```

Editar `.env` y poner la IP real de PC3 en `PYTHON_URL` (por ejemplo `http://<IP-PC3>:5000`).
También se puede definir `PYTHON_URL` como variable de entorno; tiene prioridad sobre `.env`.
El archivo `.env` no se sube al repositorio.

## Ejecución

Desde la carpeta `php-service/`:

```bash
php -S 0.0.0.0:8000 index.php
```

Escucha en todas las interfaces (no usar `localhost`), así PC1 puede conectarse.
Cada petición se registra en la consola con método, ruta, estado y duración.
Recordar abrir el puerto 8000 en el firewall de Windows (regla de entrada TCP).

## Estructura

```
php-service/
├── index.php        ← controlador frontal y enrutador de las 6 rutas
├── src/config.php   ← carga PYTHON_URL y PYTHON_TIMEOUT desde el entorno o .env
├── .env.example     ← plantilla de configuración (sin IPs reales)
└── README.md
```

## Estado

- [x] Estructura base, configuración y registro de peticiones.
- [x] Enrutador con las 6 rutas del contrato (por ahora responden 501).
- [ ] Reenvío a Python con cURL, propagando estado y cuerpo.
- [ ] 502 `{"error": "Servicio Python no disponible"}` si Python no responde.
