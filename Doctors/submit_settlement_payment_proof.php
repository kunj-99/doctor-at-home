<?php
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');

include_once("../db_connection.php"); // must provide $conn as mysqli

// ----------------------------
// CONFIG
// ----------------------------
define('ADMIN_QR_BASE_URL', 'https://thedoctorathome.in/Admin');

// ✅ REQUIRED: Save images here AND store same path in DB:
// Admin/settlement_proof/Doctor_transactions/<file>
define('ADMIN_PROOF_REL_DIR', 'Admin/settlement_proof/Doctor_transactions'); // stored in DB
define('MAX_FILE_BYTES', 8 * 1024 * 1024); // 8 MB

define('DESCRIPTION_ON_INSERT', 'Settlement proof upload');

function respond($success, $message = '', $data = null, $httpCode = 200) {
    http_response_code($httpCode);
    echo json_encode([
        "success" => (bool)$success,
        "message" => (string)$message,
        "data"    => $data
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

function cleanStr($v, $maxLen = 200) {
    $v = isset($v) ? trim((string)$v) : '';
    if ($v === '' || strtolower($v) === 'null') return '';
    if (mb_strlen($v) > $maxLen) $v = mb_substr($v, 0, $maxLen);
    return $v;
}

function ensureDir($path) {
    if (is_dir($path)) return true;
    return @mkdir($path, 0755, true);
}

function detectImageExtAndMime($tmpPath) {
    if (class_exists('finfo')) {
        $finfo = new finfo(FILEINFO_MIME_TYPE);
        $mime = $finfo->file($tmpPath);
    } else {
        $mime = mime_content_type($tmpPath);
    }

    $map = [
        'image/jpeg' => 'jpg',
        'image/png'  => 'png',
        'image/webp' => 'webp',
    ];

    if (!isset($map[$mime])) return [null, null];
    return [$map[$mime], $mime];
}

// ----------------------------
// Preflight
// ----------------------------
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    respond(true, "OK");
}
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    respond(false, "Method not allowed. Use POST.", null, 405);
}

// ----------------------------
// Validate DB connection
// ----------------------------
if (!isset($conn) || !($conn instanceof mysqli)) {
    respond(false, "Database connection not available (\$conn).", null, 500);
}
$conn->set_charset("utf8mb4");

// ----------------------------
// Input
// ----------------------------
$doctor_id  = isset($_POST["doctor_id"]) ? (int)$_POST["doctor_id"] : 0;
$summary_id = isset($_POST["summary_id"]) ? (int)$_POST["summary_id"] : 0;
$utr_number = cleanStr($_POST["utr_number"] ?? "", 80); // varchar(80)

if ($doctor_id <= 0 || $summary_id <= 0) {
    respond(false, "Required: doctor_id, summary_id", null, 400);
}

// file check
if (!isset($_FILES["proof_file"])) {
    respond(false, "Required: proof_file (image)", null, 400);
}
$f = $_FILES["proof_file"];

if (!isset($f["error"]) || $f["error"] !== UPLOAD_ERR_OK) {
    $err = $f["error"] ?? -1;
    respond(false, "Upload failed (error=$err).", null, 400);
}
if (!is_uploaded_file($f["tmp_name"])) {
    respond(false, "Invalid upload source.", null, 400);
}
if (($f["size"] ?? 0) <= 0) {
    respond(false, "Empty file.", null, 400);
}
if (($f["size"] ?? 0) > MAX_FILE_BYTES) {
    respond(false, "File too large. Max " . (MAX_FILE_BYTES / 1024 / 1024) . " MB.", null, 400);
}

list($ext, $mime) = detectImageExtAndMime($f["tmp_name"]);
if ($ext === null) {
    respond(false, "Only JPG/PNG/WEBP images are allowed.", null, 400);
}

// ----------------------------
// 1) Validate settlement belongs to doctor & must be PAY_TO_ADMIN
// ----------------------------
$settlementSql = "SELECT summary_id, doctor_id, received_from_doctor
                  FROM admin_doctor_payment_summary
                  WHERE summary_id = ? AND doctor_id = ?
                  LIMIT 1";
$st = $conn->prepare($settlementSql);
if (!$st) {
    respond(false, "Prepare failed (summary): " . $conn->error, null, 500);
}
$st->bind_param("ii", $summary_id, $doctor_id);
$st->execute();
$rs = $st->get_result();
$settlement = $rs ? $rs->fetch_assoc() : null;
$st->close();

if (!$settlement) {
    respond(false, "Settlement not found for doctor_id=$doctor_id, summary_id=$summary_id", null, 404);
}

$receivedFromDoctor = (float)($settlement["received_from_doctor"] ?? 0);
if ($receivedFromDoctor <= 0) {
    respond(false, "This settlement is not payable by doctor (PAY_TO_ADMIN).", null, 400);
}

$amount = $receivedFromDoctor;

// ----------------------------
// 2) Check existing doctor Debit txn status (lock if Submitted/Approved)
// ----------------------------
$existing = null;
$checkSql = "SELECT transaction_id, status, admin_review_note, payment_proof, submitted_at, reviewed_at, amount, utr_number
             FROM doctor_transactions
             WHERE doctor_id = ?
               AND transaction_type = 'Debit'
               AND (settlement_summary_id = ? OR reference_id = ?)
             ORDER BY transaction_id DESC
             LIMIT 1";
$st = $conn->prepare($checkSql);
if ($st) {
    $st->bind_param("iii", $doctor_id, $summary_id, $summary_id);
    $st->execute();
    $res = $st->get_result();
    if ($res) $existing = $res->fetch_assoc();
    $st->close();
}

if ($existing) {
    $curStatus = trim((string)($existing["status"] ?? ""));
    if (strcasecmp($curStatus, "Submitted") === 0 || strcasecmp($curStatus, "Approved") === 0) {
        respond(false, "Payment proof already $curStatus for this settlement.", [
            "doctor_to_admin" => [
                "exists"         => true,
                "transaction_id" => (int)$existing["transaction_id"],
                "status"         => $curStatus,
                "amount"         => (float)($existing["amount"] ?? $amount),
                "payment_proof"  => $existing["payment_proof"] ?? null,
                "utr_number"     => $existing["utr_number"] ?? null,
                "submitted_at"   => $existing["submitted_at"] ?? null,
                "reviewed_at"    => $existing["reviewed_at"] ?? null,
                "admin_note"     => $existing["admin_review_note"] ?? null
            ]
        ], 409);
    }
}

// ----------------------------
// 3) Save file inside Admin/settlement_proof/Doctor_transactions/
// ----------------------------
$adminDir = realpath(__DIR__ . "/../Admin");
if ($adminDir === false) {
    $adminDir = realpath(__DIR__ . "/../admin");
}
if ($adminDir === false) {
    respond(false, "Admin directory not found on server. Expected ../Admin relative to this script.", null, 500);
}

// ✅ Ensure BOTH directories exist
$proofRootFs = $adminDir . "/settlement_proof";
if (!ensureDir($proofRootFs)) {
    respond(false, "Failed to create proof directory: " . $proofRootFs, null, 500);
}

$proofDirFs = $proofRootFs . "/Doctor_transactions";
if (!ensureDir($proofDirFs)) {
    respond(false, "Failed to create proof directory: " . $proofDirFs, null, 500);
}

$ts = time();
$rand = bin2hex(random_bytes(6));
$fileName = "doctor_{$doctor_id}_summary_{$summary_id}_{$ts}_{$rand}.{$ext}";
$destFs = $proofDirFs . "/" . $fileName;

if (!move_uploaded_file($f["tmp_name"], $destFs)) {
    respond(false, "Failed to save uploaded file.", null, 500);
}

// ✅ Store DB path EXACTLY as required
$proofRelPath = rtrim(ADMIN_PROOF_REL_DIR, "/") . "/" . $fileName;

// ----------------------------
// 4) Upsert doctor_transactions (Debit) -> status Submitted
// ----------------------------
$conn->begin_transaction();

try {
    if ($existing) {
        $txnId = (int)$existing["transaction_id"];

        $upd = "UPDATE doctor_transactions
                SET settlement_summary_id = ?,
                    reference_id = ?,
                    payment_proof = ?,
                    utr_number = ?,
                    status = 'Submitted',
                    submitted_at = NOW()
                WHERE transaction_id = ? AND doctor_id = ? AND transaction_type = 'Debit'
                LIMIT 1";
        $st = $conn->prepare($upd);
        if (!$st) throw new Exception("Prepare failed (update): " . $conn->error);

        $st->bind_param(
            "iissii",
            $summary_id,
            $summary_id,
            $proofRelPath,
            $utr_number,
            $txnId,
            $doctor_id
        );

        if (!$st->execute()) {
            $st->close();
            throw new Exception("Update failed: " . $conn->error);
        }
        $st->close();

    } else {
        $desc = DESCRIPTION_ON_INSERT;

        $ins = "INSERT INTO doctor_transactions
                (doctor_id, reference_id, settlement_summary_id, transaction_type,
                 amount, description, payment_proof, status, utr_number, submitted_at)
                VALUES
                (?, ?, ?, 'Debit', ?, ?, ?, 'Submitted', ?, NOW())";
        $st = $conn->prepare($ins);
        if (!$st) throw new Exception("Prepare failed (insert): " . $conn->error);

        $st->bind_param(
            "iiidsss",
            $doctor_id,
            $summary_id,
            $summary_id,
            $amount,
            $desc,
            $proofRelPath,
            $utr_number
        );

        if (!$st->execute()) {
            $st->close();
            throw new Exception("Insert failed: " . $conn->error);
        }
        $txnId = (int)$conn->insert_id;
        $st->close();
    }

    // Re-fetch latest row for response
    $get = "SELECT transaction_id, status, amount, payment_proof, utr_number, submitted_at, reviewed_at, admin_review_note, description
            FROM doctor_transactions
            WHERE transaction_id = ? LIMIT 1";
    $st = $conn->prepare($get);
    if (!$st) throw new Exception("Prepare failed (select): " . $conn->error);
    $st->bind_param("i", $txnId);
    $st->execute();
    $res = $st->get_result();
    $row = $res ? $res->fetch_assoc() : null;
    $st->close();

    $conn->commit();

    respond(true, "Payment proof submitted successfully.", [
        "doctor_to_admin" => [
            "exists"         => true,
            "transaction_id" => (int)($row["transaction_id"] ?? $txnId),
            "status"         => (string)($row["status"] ?? "Submitted"),
            "amount"         => (float)($row["amount"] ?? $amount),
            "payment_proof"  => $row["payment_proof"] ?? $proofRelPath,
            "utr_number"     => $row["utr_number"] ?? null,
            "submitted_at"   => $row["submitted_at"] ?? null,
            "reviewed_at"    => $row["reviewed_at"] ?? null,
            "admin_note"     => $row["admin_review_note"] ?? null,
            "description"    => $row["description"] ?? null
        ]
    ]);

} catch (Exception $e) {
    $conn->rollback();

    // cleanup saved file if DB failed
    if (isset($destFs) && is_file($destFs)) @unlink($destFs);

    respond(false, "Server error: " . $e->getMessage(), null, 500);
}
