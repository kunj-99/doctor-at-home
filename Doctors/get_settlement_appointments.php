<?php
/**************************************************************
 * GET /Doctors/get_settlement_appointments.php?doctor_id=1&summary_id=10[&debug=1]
 * Expands a single settlement summary into its appointment rows from payment_history
 * Respects the CSV order as stored in admin_doctor_payment_summary.last_payment_id
 **************************************************************/

header('Content-Type: application/json; charset=UTF-8');
header('Cache-Control: no-store, no-cache, must-revalidate, max-age=0');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');

date_default_timezone_set("Asia/Kolkata");

// Preflight
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}
if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['success'=>false,'message'=>'Method not allowed. Use GET.'], JSON_UNESCAPED_UNICODE);
    exit;
}

/* Debug switch */
$DEBUG = isset($_GET['debug']) ? (int)$_GET['debug'] : 0;
if ($DEBUG) {
    ini_set('display_errors', 1);
    error_reporting(E_ALL);
    mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);
} else {
    ini_set('display_errors', 0);
    error_reporting(E_ALL & ~E_NOTICE & ~E_WARNING);
}

/* DB include */
$ok = false;
try {
    $p1 = __DIR__ . '/../db_connection.php';
    $p2 = __DIR__ . '/db_connection.php';
    if (file_exists($p1)) { require_once $p1; $ok = true; }
    elseif (file_exists($p2)) { require_once $p2; $ok = true; }
} catch (Throwable $e) { $ok = false; }

if (!$ok || !isset($conn) || !($conn instanceof mysqli)) {
    http_response_code(500);
    echo json_encode(['success'=>false,'message'=>'DB include failed'], JSON_UNESCAPED_UNICODE);
    exit;
}
$conn->set_charset('utf8mb4');

/* helpers */
function num2($v): float {
    // return real number, not string (Android optDouble friendly)
    return round((float)$v, 2);
}
function fail($m, $c = 400, $d = null) {
    http_response_code($c);
    $o = ['success'=>false,'message'=>$m];
    if ($d !== null) $o['detail'] = $d;
    echo json_encode($o, JSON_UNESCAPED_UNICODE);
    exit;
}

/* inputs */
$doctor_id  = isset($_GET['doctor_id']) ? (int)$_GET['doctor_id'] : 0;
$summary_id = isset($_GET['summary_id']) ? (int)$_GET['summary_id'] : 0;

if ($doctor_id <= 0)  fail('doctor_id is required (int)');
if ($summary_id <= 0) fail('summary_id is required (int)');

try {
    // 1) Fetch summary and parse CSV of appointment_ids
    $stmt = $conn->prepare("
        SELECT doctor_id, last_payment_id
        FROM admin_doctor_payment_summary
        WHERE summary_id = ?
        LIMIT 1
    ");
    $stmt->bind_param('i', $summary_id);
    $stmt->execute();
    $res = $stmt->get_result();
    if (!$res || !$res->num_rows) fail('Summary not found', 404);
    $sum = $res->fetch_assoc();
    $stmt->close();

    if ((int)$sum['doctor_id'] !== $doctor_id) {
        fail('Summary does not belong to this doctor', 403);
    }

    $csv = trim((string)$sum['last_payment_id']);
    if ($csv === '') {
        echo json_encode([
            'success' => true,
            'summary_id' => $summary_id,
            'doctor_id' => $doctor_id,
            'data' => []
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }

    $ids = [];
    foreach (explode(',', $csv) as $x) {
        $id = (int)trim($x);
        if ($id > 0) $ids[] = $id;
    }

    if (empty($ids)) {
        echo json_encode([
            'success' => true,
            'summary_id' => $summary_id,
            'doctor_id' => $doctor_id,
            'data' => []
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }

    // optional hard cap for safety
    if (count($ids) > 500) {
        fail('Too many appointments in this settlement (cap 500).', 422);
    }

    // 2) Build dynamic IN + FIELD placeholders safely
    $placeholders = implode(',', array_fill(0, count($ids), '?'));
    $typesIds = str_repeat('i', count($ids));

    // IMPORTANT: only select columns we actually use (avoid "unknown column" crashes)
    $sql = "
        SELECT
            appointment_id,
            patient_id,
            COALESCE(NULLIF(patient_name,''), '') AS patient_name,
            amount,
            gst,
            deposit,
            deposit_status,
            payment_method,
            admin_commission,
            doctor_earning,
            payment_status,
            created_at,
            updated_at
        FROM payment_history
        WHERE doctor_id = ?
          AND appointment_id IN ($placeholders)
        ORDER BY FIELD(appointment_id, $placeholders)
    ";

    // bind: first doctor_id, then ids twice (for IN and FIELD)
    $allParams = array_merge([$doctor_id], $ids, $ids);
    $allTypes  = 'i' . $typesIds . $typesIds;

    $stmt = $conn->prepare($sql);
    if (!$stmt) throw new Exception($conn->error);

    // dynamic bind_param (by reference)
    $bind = [];
    $bind[] = &$allTypes;
    foreach ($allParams as $k => $v) {
        $bind[] = &$allParams[$k];
    }
    call_user_func_array([$stmt, 'bind_param'], $bind);

    $stmt->execute();
    $rs = $stmt->get_result();

    $out = [];
    while ($r = $rs->fetch_assoc()) {
        $amount = (float)$r['amount']; // amount incl GST (as per your logic)
        $gst    = (float)$r['gst'];
        $base   = max(0.0, $amount - $gst);

        $out[] = [
            'appointment_id'   => (int)$r['appointment_id'],
            'patient_id'       => (int)$r['patient_id'],
            'patient_name'     => (string)$r['patient_name'],

            'payment_method'   => (string)$r['payment_method'],
            'deposit'          => num2($r['deposit']),
            'deposit_status'   => (string)$r['deposit_status'],

            'amount_total'     => num2($amount),
            'gst'              => num2($gst),
            'base_ex_gst'      => num2($base),

            'admin_commission' => num2($r['admin_commission']),
            'doctor_earning'   => num2($r['doctor_earning']),

            'payment_status'   => (string)$r['payment_status'],
            'created_at'       => (string)$r['created_at'],
            'updated_at'       => (string)$r['updated_at'],
        ];
    }
    $stmt->close();

    echo json_encode([
        'success' => true,
        'summary_id' => $summary_id,
        'doctor_id' => $doctor_id,
        'data' => $out
    ], JSON_UNESCAPED_UNICODE);

} catch (Throwable $e) {
    if ($DEBUG) fail('DB error', 500, $e->getMessage());
    fail('Internal server error', 500);
}
