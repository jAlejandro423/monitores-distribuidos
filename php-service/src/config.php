<?php
/**
 * Carga la configuración del servicio.
 *
 * Prioridad: variables de entorno del sistema > archivo .env > valores por defecto.
 * Nunca se escriben IPs ni URLs reales en el código.
 */

function cargarEnv(string $ruta): array
{
    $valores = [];
    if (!is_readable($ruta)) {
        return $valores;
    }
    foreach (file($ruta, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES) as $linea) {
        $linea = trim($linea);
        if ($linea === '' || str_starts_with($linea, '#') || !str_contains($linea, '=')) {
            continue;
        }
        [$clave, $valor] = explode('=', $linea, 2);
        $valores[trim($clave)] = trim(trim($valor), "\"'");
    }
    return $valores;
}

function config(): array
{
    static $config = null;
    if ($config !== null) {
        return $config;
    }

    $env = cargarEnv(__DIR__ . '/../.env');
    $leer = function (string $clave, ?string $porDefecto = null) use ($env): ?string {
        $valor = getenv($clave);
        if ($valor !== false && $valor !== '') {
            return $valor;
        }
        return $env[$clave] ?? $porDefecto;
    };

    $config = [
        'PYTHON_URL'     => rtrim((string) $leer('PYTHON_URL', ''), '/'),
        'PYTHON_TIMEOUT' => (int) $leer('PYTHON_TIMEOUT', '5'),
    ];
    return $config;
}
