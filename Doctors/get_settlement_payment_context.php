<?php
include_once("../db_connection.php"); // must provide $conn as mysqli
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store, no-cache, must-revalidate, max-age=0');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');

/** STATIC DOMAIN PREFIX (use your site root) */
define('SITE_BASE_URL', 'https://thedoctorathome.in/');

/** QR base folder (existing) */
define('ADMIN_QR_BASE_URL', 'https://thedoctorathome.in/Admin/Qr_Code/');

function respond($success, $message = '', $data = null, $httpCode = 200) {
    http_response_code($httpCode);
    echo json_encode([
        "success" => (bool)$success,
        "message" => (string)$message,
        "data"    => $data
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

function cleanStr($v, $maxLen = 500) {
    $v = isset($v) ? trim((string)$v) : '';
    if ($v === '' || strtolower($v) === 'null') return '';
    if (mb_strlen($v) > $maxLen) $v = mb_substr($v, 0, $maxLen);
    return $v;
}

function fetchAdminQrFromAppSettings(mysqli $conn) {
    $sql = "SELECT `value` FROM app_settings WHERE key_name = ? LIMIT 1";
    $st = $conn->prepare($sql);
    if (!$st) return ["found"=>false,"qr_text"=>null,"qr_image"=>null,"source"=>"app_settings"];

    $key = "Qr_code";
    $st->bind_param("s", $key);
    $st->execute();
    $res = $st->get_result();
    $row = $res ? $res->fetch_assoc() : null;
    $st->close();

    if ($row && isset($row["value"])) {
        $file = trim((string)$row["value"]);
        if ($file !== "") {
            $qrImage = preg_match('/^https?:\/\//i', $file)
                ? $file
                : rtrim(ADMIN_QR_BASE_URL, '/') . '/' . ltrim($file, '/');

            return ["found"=>true,"qr_text"=>null,"qr_image"=>$qrImage,"source"=>"app_settings"];
        }
    }

    return ["found"=>false,"qr_text"=>null,"qr_image"=>null,"source"=>"app_settings"];
}

function basename_utf8($path) {
    $path = (string)$path;
    if ($path === '') return '';
    $p = str_replace('\\', '/', $path);
    $b = basename($p);
    return $b;
}

/** Make absolute URL using SITE_BASE_URL if value is relative. */
function abs_url($path) {
    $p = cleanStr($path, 1200);
    if ($p === '') return '';
    if (preg_match('/^https?:\\/\\//i', $p)) return $p;
    return rtrim(SITE_BASE_URL, '/') . '/' . ltrim($p, '/');
}

/** Make absolute URL with /Admin/ injected for Admin->Doctor proofs only. */
function abs_url_admin_proof($path) {
    $p = cleanStr($path, 1200);
    if ($p === '') return '';
    if (preg_match('/^https?:\\/\\//i', $p)) return $p; // already absolute
    $p = ltrim(str_replace('\\', '/', $p), '/');
    if (stripos($p, 'Admin/') === 0) {
        return rtrim(SITE_BASE_URL, '/') . '/' . $p;
    }
    return rtrim(SITE_BASE_URL, '/') . '/Admin/' . $p;
}

// OPTIONS preflight
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}
if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    respond(false, "Method not allowed. Use GET.", null, 405);
}

// Validate DB connection
if (!isset($conn) || !($conn instanceof mysqli)) {
    respond(false, "Database connection not available (\$conn).", null, 500);
}
$conn->set_charset("utf8mb4");

// Input
$doctor_id  = isset($_GET["doctor_id"]) ? (int)$_GET["doctor_id"] : 0;
$summary_id = isset($_GET["summary_id"]) ? (int)$_GET["summary_id"] : 0;

if ($doctor_id <= 0 || $summary_id <= 0) {
    respond(false, "Required: doctor_id, summary_id", null, 400);
}

// 1) Settlement summary
$settlementSql = "SELECT
    summary_id,
    doctor_id,
    last_payment_id,
    total_appointments,
    online_appointments,
    offline_appointments,
    total_earning,
    total_gst,
    admin_collected_total,
    doctor_collected_total,
    admin_cut,
    doctor_cut,
    adjustment_amount,
    settlement_status,
    notes,
    created_at,
    updated_at,
    given_to_doctor,
    received_from_doctor
FROM admin_doctor_payment_summary
WHERE summary_id = ? AND doctor_id = ?
LIMIT 1";

$st = $conn->prepare($settlementSql);
if (!$st) {
    respond(false, "Prepare failed (admin_doctor_payment_summary): " . $conn->error, null, 500);
}
$st->bind_param("ii", $summary_id, $doctor_id);
$st->execute();
$rs = $st->get_result();
$settlement = $rs ? $rs->fetch_assoc() : null;
$st->close();

if (!$settlement) {
    respond(false, "Settlement not found for doctor_id=$doctor_id, summary_id=$summary_id", null, 404);
}

$givenToDoctor      = (float)($settlement["given_to_doctor"] ?? 0);
$receivedFromDoctor = (float)($settlement["received_from_doctor"] ?? 0);
$settlementStatus   = cleanStr($settlement["settlement_status"] ?? "Pending", 50);
$isSettled          = (strcasecmp($settlementStatus, "Settled") === 0);

// 2) Doctor transaction (Debit) Doctor → Admin (latest)
$doctorTxn = null;
$docSql = "SELECT
    transaction_id,
    doctor_id,
    reference_id,
    settlement_summary_id,
    transaction_type,
    method,
    amount,
    description,
    payment_proof,
    status,
    utr_number,
    submitted_at,
    reviewed_at,
    admin_review_note,
    created_at
FROM doctor_transactions
WHERE doctor_id = ?
  AND transaction_type = 'Debit'
  AND settlement_summary_id = ?
ORDER BY transaction_id DESC
LIMIT 1";
$st = $conn->prepare($docSql);
if ($st) {
    $st->bind_param("ii", $doctor_id, $summary_id);
    $st->execute();
    $res = $st->get_result();
    if ($res) $doctorTxn = $res->fetch_assoc();
    $st->close();
}

$doctorStatusRaw = $doctorTxn ? cleanStr($doctorTxn["status"] ?? "Pending", 50) : "Pending";
$doctorStatus    = $doctorStatusRaw !== "" ? $doctorStatusRaw : "Pending";
$doctorFinal     = in_array(strtolower($doctorStatus), ['approved', 'rejected'], true);
$doctorProofPath = $doctorTxn ? cleanStr($doctorTxn["payment_proof"] ?? "", 1200) : "";
$doctorProofName = $doctorProofPath !== "" ? basename_utf8($doctorProofPath) : "";
$doctorProofUrl  = $doctorProofPath !== "" ? abs_url($doctorProofPath) : "";

// 3) Admin transaction (Expense) Admin → Doctor proof (latest)
$adminTxn = null;
$admSql = "SELECT
    transaction_id,
    reference_id,
    settlement_summary_id,
    transaction_type,
    method,
    amount,
    description,
    payment_proof,
    payment_proofs,
    created_at
FROM admin_transactions
WHERE transaction_type = 'Expense'
  AND settlement_summary_id = ?
ORDER BY transaction_id DESC
LIMIT 1";
$st = $conn->prepare($admSql);
if ($st) {
    $st->bind_param("i", $summary_id);
    $st->execute();
    $res = $st->get_result();
    if ($res) $adminTxn = $res->fetch_assoc();
    $st->close();
}
$adminProofPath = $adminTxn ? cleanStr($adminTxn["payment_proof"] ?? "", 1200) : "";
$adminProofName = $adminProofPath !== "" ? basename_utf8($adminProofPath) : "";
$adminProofUrl  = $adminProofPath !== "" ? abs_url_admin_proof($adminProofPath) : "";

// 4) Admin QR (static from settings)
$qr = fetchAdminQrFromAppSettings($conn);

// ------------------- Decision logic -------------------

$doctorToAdminPending = ($receivedFromDoctor > 0) && (!$doctorFinal);
$adminToDoctorPending = ($givenToDoctor > 0) && ($adminProofPath === "");
$finalized            = $isSettled || $doctorFinal || ($adminProofPath !== "");
$shouldShowQr         = $doctorToAdminPending;

// Mode hint for UI:
$mode = "NONE";
if ($doctorToAdminPending) {
    $mode = "PAY_TO_ADMIN";
} elseif ($adminToDoctorPending) {
    $mode = "RECEIVE_FROM_ADMIN";
}

$hint = "";
if ($mode === "NONE") {
    if ($isSettled) {
        $hint = "Settlement is Settled.";
    } elseif ($doctorFinal) {
        $hint = "Doctor→Admin is $doctorStatus.";
    } elseif ($adminProofPath !== "") {
        $hint = "Admin payout proof present.";
    } else {
        $hint = "No pending action.";
    }
}

// ------------------- Response -------------------

$data = [
    "summary" => [
        "summary_id"           => (int)$settlement["summary_id"],
        "doctor_id"            => (int)$settlement["doctor_id"],

        "appointment_ids_csv"  => (string)($settlement["last_payment_id"] ?? ""),
        "appointment_count"    => (int)($settlement["total_appointments"] ?? 0),
        "online_appointments"  => (int)($settlement["online_appointments"] ?? 0),
        "offline_appointments" => (int)($settlement["offline_appointments"] ?? 0),

        "total_base_ex_gst"        => (float)($settlement["total_earning"] ?? 0),
        "total_gst"                => (float)($settlement["total_gst"] ?? 0),
        "admin_collected_total"    => (float)($settlement["admin_collected_total"] ?? 0),
        "doctor_collected_total"   => (float)($settlement["doctor_collected_total"] ?? 0),
        "admin_cut"                => (float)($settlement["admin_cut"] ?? 0),
        "doctor_cut"               => (float)($settlement["doctor_cut"] ?? 0),
        "adjustment_amount"        => (float)($settlement["adjustment_amount"] ?? 0),

        "given_to_doctor"          => $givenToDoctor,
        "received_from_doctor"     => $receivedFromDoctor,

        "settlement_status"        => $settlementStatus,
        "created_at"               => (string)($settlement["created_at"] ?? ""),
        "updated_at"               => (string)($settlement["updated_at"] ?? ""),
        "notes"                    => (string)($settlement["notes"] ?? "")
    ],

    // QR will be included ONLY when shouldShowQr = true
    "admin_qr" => [
        "show"     => (bool)$shouldShowQr,
        "found"    => $shouldShowQr ? (bool)$qr["found"] : false,
        "qr_text"  => $shouldShowQr ? $qr["qr_text"] : null,
        "qr_image" => $shouldShowQr ? $qr["qr_image"] : null, // legacy key; null when hidden
        "source"   => $shouldShowQr ? $qr["source"] : null
    ],

    // Doctor -> Admin (Debit)
    "doctor_to_admin" => [
        "exists"             => ($doctorTxn !== null),
        "transaction_id"     => $doctorTxn ? (int)$doctorTxn["transaction_id"] : null,
        "status"             => $doctorStatus,
        "is_final"           => $doctorFinal,
        "is_pending"         => $doctorToAdminPending,

        "amount"             => $doctorTxn ? (float)$doctorTxn["amount"] : $receivedFromDoctor,
        "method"             => $doctorTxn["method"] ?? "Bank Transfer",

        /** Proofs */
        "payment_proof"      => $doctorProofPath !== "" ? $doctorProofPath : null,  // raw DB path
        "payment_proof_path" => $doctorProofPath !== "" ? $doctorProofPath : null,  // duplicate for clarity
        "payment_proof_name" => $doctorProofName !== "" ? $doctorProofName : null,  // basename
        "payment_proof_url"  => $doctorProofUrl  !== "" ? $doctorProofUrl  : null,  // ABSOLUTE URL (with domain)

        "utr_number"         => $doctorTxn["utr_number"] ?? null,
        "submitted_at"       => $doctorTxn["submitted_at"] ?? null,
        "reviewed_at"        => $doctorTxn["reviewed_at"] ?? null,
        "admin_note"         => $doctorTxn["admin_review_note"] ?? null
    ],

    // Admin -> Doctor (Expense)
    "admin_to_doctor" => [
        "exists"             => ($adminTxn !== null),
        "transaction_id"     => $adminTxn ? (int)$adminTxn["transaction_id"] : null,
        "is_pending"         => $adminToDoctorPending,

        "amount"             => $adminTxn ? (float)$adminTxn["amount"] : $givenToDoctor,
        "method"             => $adminTxn["method"] ?? null,

        /** Proofs */
        "payment_proof"      => $adminProofPath !== "" ? $adminProofPath : null,   // raw DB path
        "payment_proof_path" => $adminProofPath !== "" ? $adminProofPath : null,   // duplicate for clarity
        "payment_proof_name" => $adminProofName !== "" ? $adminProofName : null,   // basename
        "payment_proof_url"  => $adminProofUrl  !== "" ? $adminProofUrl  : null,   // ABSOLUTE URL (with /Admin/ prefix)

        "payment_proofs_raw" => $adminTxn ? ($adminTxn["payment_proofs"] ?? null) : null,
        "created_at"         => $adminTxn["created_at"] ?? null
    ],

    // Useful flags for client logic
    "flags" => [
        "is_settled"       => $isSettled,
        "is_doctor_final"  => $doctorFinal,
        "has_admin_proof"  => ($adminProofPath !== ""),
        "finalized"        => $finalized,
        "should_show_qr"   => $shouldShowQr
    ],

    "pending_state" => [
        "doctor_to_admin_pending" => $doctorToAdminPending,
        "admin_to_doctor_pending" => $adminToDoctorPending,
        "any_pending"             => ($doctorToAdminPending || $adminToDoctorPending),
        "hint"                    => $hint
    ],

    "mode_hint" => $mode
];

respond(true, "OK", $data);
