<?php
// Doctors/save_doctor_token.php
header('Content-Type: application/json; charset=utf-8');
// header('Access-Control-Allow-Origin: *'); // enable only if you truly need cross-origin

ini_set('display_errors', '0');
mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

require_once __DIR__ . '/../db_connection.php';

function fail(int $http, string $msg, array $extra = []): void {
    http_response_code($http);
    $out = array_merge(['success' => false, 'message' => $msg], $extra);
    error_log('[save_doctor_token] ' . json_encode($out, JSON_UNESCAPED_SLASHES));
    echo json_encode($out);
    exit;
}

if (!isset($conn) || $conn->connect_errno) {
    fail(500, 'DB connection failed', ['mysql' => $conn ? $conn->connect_error : 'no $conn']);
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    fail(405, 'Method Not Allowed');
}

/* -------- Inputs -------- */
$doctor_id = isset($_POST['doctor_id'])   ? (int)$_POST['doctor_id']                       : 0;
$fcm_token = isset($_POST['fcm_token'])   ? trim((string)$_POST['fcm_token'])              : '';
$device_id = isset($_POST['device_id'])   ? trim((string)$_POST['device_id'])              : '';
$platform  = isset($_POST['platform'])    ? trim((string)$_POST['platform'])               : 'android';
$model     = isset($_POST['model'])       ? trim((string)$_POST['model'])                  : null;
$app_ver   = isset($_POST['app_version']) ? trim((string)$_POST['app_version'])            : null;

/* -------- Validation -------- */
if ($doctor_id <= 0 || $fcm_token === '' || $device_id === '') {
    fail(400, 'Missing doctor_id/device_id/fcm_token', [
        'got' => [
            'doctor_id' => $doctor_id,
            'device_id' => $device_id,
            'fcm_len'   => strlen($fcm_token),
        ]
    ]);
}
if (strlen($fcm_token) > 1024)  fail(400, 'fcm_token too long',  ['fcm_len'   => strlen($fcm_token)]);
if (strlen($device_id) > 128)   fail(400, 'device_id too long',  ['device_len' => strlen($device_id)]);
if (strlen($platform)  > 32)    $platform  = substr($platform, 0, 32);
if (!empty($model) && strlen($model) > 128)       $model = substr($model, 0, 128);
if (!empty($app_ver) && strlen($app_ver) > 32)    $app_ver = substr($app_ver, 0, 32);

/*
 Table used: doctor_tokens
 Columns (as per your current table):
   token_id INT PK AI,
   doctor_id INT NOT NULL,
   device_id VARCHAR(128) NOT NULL,
   platform VARCHAR(32) NOT NULL DEFAULT 'android',
   model VARCHAR(128) NULL,
   app_version VARCHAR(32) NULL,
   fcm_token VARCHAR(1024) NOT NULL,
   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
   updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
 Indexes:
   UNIQUE KEY uniq_doctor_device (doctor_id, device_id)
   (others are fine)
*/

/* -------- Single-step UPSERT keyed by (doctor_id, device_id) -------- */
try {
    $sql = "INSERT INTO doctor_tokens
                (doctor_id, device_id, platform, model, app_version, fcm_token, created_at, last_updated)
            VALUES
                (?,         ?,         ?,       ?,     ?,           ?,          NOW(),     NOW())
            ON DUPLICATE KEY UPDATE
                fcm_token   = VALUES(fcm_token),
                platform    = VALUES(platform),
                model       = VALUES(model),
                app_version = VALUES(app_version),
                last_updated= NOW()";

    $stmt = $conn->prepare($sql);
    if (!$stmt) fail(500, 'Prepare failed');

    // types: i s s s s s  (doctor_id int, rest strings; model/app_ver can be null)
    $stmt->bind_param(
        'isssss',
        $doctor_id,
        $device_id,
        $platform,
        $model,
        $app_ver,
        $fcm_token
    );

    $ok = $stmt->execute();
    $affected = $stmt->affected_rows; // 1 insert, or 2 update (depends on MySQL version)
    $stmt->close();
    $conn->close();

    if ($ok) {
        echo json_encode([
            'success'  => true,
            'action'   => ($affected === 1 ? 'inserted' : 'updated'),
            'affected' => $affected
        ]);
    } else {
        fail(500, 'Execute failed');
    }
} catch (Throwable $e) {
    fail(500, 'Server error', ['error' => $e->getMessage(), 'code' => $e->getCode()]);
}
