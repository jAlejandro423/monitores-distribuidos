<?php
/**
 * Controlador frontal del servicio PHP (PC2, puerto 8000).
 *
 * Arranque:  php -S 0.0.0.0:8000 index.php
 *
 * PHP solo reenvía a Python: mismo método, misma ruta, mismo cuerpo,
 * y devuelve el código de estado y el cuerpo de Python tal cual.
 */

declare(strict_types=1);

// Nunca imprimir warnings/notices en la respuesta: Java espera solo JSON.
ini_set('display_errors', '0');
error_reporting(E_ALL);

require __DIR__ . '/src/config.php';
require __DIR__ . '/src/proxy.php';

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
        header_remove('Content-Type');
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
    $query = $_SERVER['QUERY_STRING'] ?? '';
    $destino = $ruta . ($query !== '' ? '?' . $query : '');
    $resp = reenviarAPython($metodo, $destino, (string) file_get_contents('php://input'));
    if ($resp === null) {
        $estado = 502;
        responderJson($estado, ['error' => 'Servicio Python no disponible']);
    } else {
        $estado = responderDesdePython($resp);
    }
} elseif ($rutaExiste) {
    $estado = 405;
    responderJson($estado, ['error' => "Método $metodo no permitido en $ruta"]);
} else {
    $estado = 404;
    responderJson($estado, ['error' => "Ruta no encontrada: $ruta"]);
}

registrar($metodo, $ruta, $estado, $inicio);
