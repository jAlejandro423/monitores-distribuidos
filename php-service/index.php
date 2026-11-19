<?php
/**
 * Controlador frontal del servicio PHP (PC2, puerto 8000).
 *
 * Arranque:  php -S 0.0.0.0:8000 index.php
 *
 * PHP solo reenvía a Python: mismo método, misma ruta, mismo cuerpo,
 * y devuelve el código de estado y el cuerpo de Python tal cual.
 * El reenvío real con cURL queda pendiente (por ahora las rutas responden 501).
 */

declare(strict_types=1);

// Nunca imprimir warnings/notices en la respuesta: Java espera solo JSON.
ini_set('display_errors', '0');
error_reporting(E_ALL);

require __DIR__ . '/src/config.php';

$inicio = microtime(true);
$metodo = $_SERVER['REQUEST_METHOD'] ?? 'GET';
$ruta   = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/';

/** Registra la petición en la consola: método, ruta, estado y duración. */
function registrar(string $metodo, string $ruta, int $estado, float $inicio): void
{
    $ms = (int) round((microtime(true) - $inicio) * 1000);
    error_log(sprintf('%s %s -> %d (%d ms)', $metodo, $ruta, $estado, $ms));
}

/** Responde JSON en UTF-8. Con 204 no se envía cuerpo. */
function responderJson(int $estado, $cuerpo = null): void
{
    http_response_code($estado);
    if ($estado === 204) {
        return;
    }
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($cuerpo, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
}

// Las 6 rutas del contrato (docs/AGENTS.md, sección 5).
$rutas = [
    ['GET',    '#^/api/materias$#'],
    ['GET',    '#^/api/monitores$#'],
    ['GET',    '#^/api/monitores/[^/]+$#'],
    ['POST',   '#^/api/monitores$#'],
    ['PUT',    '#^/api/monitores/[^/]+$#'],
    ['DELETE', '#^/api/monitores/[^/]+$#'],
];

$coincide = false;
$rutaExiste = false;
foreach ($rutas as [$m, $patron]) {
    if (preg_match($patron, $ruta)) {
        $rutaExiste = true;
        if ($m === $metodo) {
            $coincide = true;
            break;
        }
    }
}

if ($coincide) {
    // TODO: reenviar a config()['PYTHON_URL'] . $ruta con cURL (timeout PYTHON_TIMEOUT),
    // propagar estado y cuerpo, y responder 502 {"error": "Servicio Python no disponible"}
    // si no hay conexión.
    $estado = 501;
    responderJson($estado, ['error' => 'Ruta aún no implementada en el servicio PHP']);
} elseif ($rutaExiste) {
    $estado = 405;
    responderJson($estado, ['error' => "Método $metodo no permitido en $ruta"]);
} else {
    $estado = 404;
    responderJson($estado, ['error' => "Ruta no encontrada: $ruta"]);
}

registrar($metodo, $ruta, $estado, $inicio);
