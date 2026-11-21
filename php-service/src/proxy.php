<?php
/**
 * Reenvío de peticiones al servicio Python con cURL.
 *
 * PHP no reinterpreta nada: mismo método, misma ruta, mismo cuerpo hacia Python,
 * y el código de estado y el cuerpo de Python vuelven tal cual hacia Java.
 */

declare(strict_types=1);

/**
 * Reenvía la petición a Python.
 *
 * Devuelve ['estado' => int, 'cuerpo' => string|null]. Si no hay conexión con
 * Python (caído, IP errada, timeout), devuelve null.
 */
function reenviarAPython(string $metodo, string $rutaConQuery, string $cuerpo): ?array
{
    $cfg = config();
    $url = $cfg['PYTHON_URL'] . $rutaConQuery;
    $timeout = max(1, $cfg['PYTHON_TIMEOUT']);

    $cabeceras = ['Accept: application/json'];
    $conCuerpo = in_array($metodo, ['POST', 'PUT'], true);
    if ($conCuerpo) {
        $cabeceras[] = 'Content-Type: application/json';
    }

    $ch = curl_init($url);
    if ($ch === false) {
        return null;
    }
    curl_setopt_array($ch, [
        CURLOPT_CUSTOMREQUEST  => $metodo,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_HTTPHEADER     => $cabeceras,
        CURLOPT_CONNECTTIMEOUT => min(3, $timeout),
        CURLOPT_TIMEOUT        => $timeout,
        CURLOPT_FOLLOWLOCATION => false,
    ]);
    if ($conCuerpo) {
        curl_setopt($ch, CURLOPT_POSTFIELDS, $cuerpo);
    }

    $respuesta = curl_exec($ch);
    if ($respuesta === false) {
        error_log('No se pudo conectar con Python (' . $url . '): ' . curl_error($ch));
        return null;
    }

    return [
        'estado' => (int) curl_getinfo($ch, CURLINFO_HTTP_CODE),
        'cuerpo' => $respuesta,
    ];
}

/**
 * Envía a Java la respuesta de Python, propagando estado y cuerpo.
 *
 * Solo interviene si el cuerpo no es JSON válido, para que Java siempre reciba
 * el formato del contrato: en errores pone {"error": ...} con el mismo código,
 * y en un 2xx no JSON responde 502. Devuelve el estado que se envió.
 */
function responderDesdePython(array $resp): int
{
    $estado = $resp['estado'];
    $cuerpo = trim((string) $resp['cuerpo']);

    if ($estado === 204) {
        responderJson(204);
        return 204;
    }

    json_decode($cuerpo);
    $esJson = $cuerpo !== '' && json_last_error() === JSON_ERROR_NONE;

    if (!$esJson) {
        if ($estado >= 400) {
            responderJson($estado, ['error' => "El servicio Python respondió con error $estado"]);
            return $estado;
        }
        responderJson(502, ['error' => 'Respuesta inválida del servicio Python']);
        return 502;
    }

    http_response_code($estado);
    header('Content-Type: application/json; charset=utf-8');
    echo $cuerpo;
    return $estado;
}
