<?php
/**************************************************************
 * GET /Doctors/get_doctor_settlements.php?doctor_id=1&status=Pending|Settled|Partially%20Settled|All[&debug=1]
 *
 * SOURCE OF TRUTH:
 * - admin_doctor_payment_summary (settlement amounts + settlement_status)
 * - doctor_transactions (Doctor -> Admin : Debit + status {Pending,Submitted,Approved,Rejected})
 * - admin_transactions  (Admin  -> Doctor : Expense; NO status column; use paid_amount/proof)
 *
 * Pending screen rules (only when status=Pending):
 * 1) Doctor->Admin NOT pending if latest doctor_transactions.status ∈ {Approved, Rejected}.
 * 2) Admin->Doctor NOT pending if admin_transactions (Expense) exists AND
 *    (paid_amount >= amount AND (payment_proof != '' OR payment_proofs != '')).
 * 3) Include settlement iff (doctor_to_admin_pending OR admin_to_doctor_pending).
 *
 * History screen (status=Settled / Partially Settled) and status=All:
 * - Do NOT apply pending filter; just return rows by settlement_status (or all).
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
function cleanStr($v, $maxLen = 255) {
    $v = isset($v) ? trim((string)$v) : '';
    if ($v === '' || strtolower($v) === 'null') return '';
    if (mb_strlen($v) > $maxLen) $v = mb_substr($v, 0, $maxLen);
    return $v;
}
function num2($v): float { return round((float)$v, 2); }
function fail($m, $c = 400, $d = null) {
    http_response_code($c);
    $o = ['success'=>false,'message'=>$m];
    if ($d !== null) $o['detail'] = $d;
    echo json_encode($o, JSON_UNESCAPED_UNICODE);
    exit;
}
function bindParams(mysqli_stmt $stmt, string $types, array $params) {
    $bind = [];
    $bind[] = &$types;
    for ($i = 0; $i < count($params); $i++) {
        $bind[] = &$params[$i];
    }
    call_user_func_array([$stmt, 'bind_param'], $bind);
}

/* inputs */
$doctor_id = isset($_GET['doctor_id']) ? (int)$_GET['doctor_id'] : 0;
if ($doctor_id <= 0) fail('doctor_id is required (int)');

$status = isset($_GET['status']) ? trim((string)$_GET['status']) : 'Pending';
$filterAll        = (strcasecmp($status, 'All') === 0);
$isPendingRequest = (strcasecmp($status, 'Pending') === 0);

try {
    // ----------------------------
    // 1) Read summaries (filter by settlement_status unless 'All')
    // ----------------------------
    if ($filterAll) {
        $sql = "SELECT
                    summary_id, doctor_id, last_payment_id, total_appointments,
                    online_appointments, offline_appointments,
                    total_earning, total_gst,
                    admin_collected_total, doctor_collected_total,
                    admin_cut, doctor_cut, adjustment_amount,
                    settlement_status, notes, created_at, updated_at,
                    given_to_doctor, received_from_doctor
                FROM admin_doctor_payment_summary
                WHERE doctor_id = ?
                ORDER BY created_at DESC, summary_id DESC
                LIMIT 1000";
        $st = $conn->prepare($sql);
        if (!$st) throw new Exception($conn->error);
        $st->bind_param("i", $doctor_id);
    } else {
        // EXACT enum values from your table: Pending | Settled | Partially Settled
        $sql = "SELECT
                    summary_id, doctor_id, last_payment_id, total_appointments,
                    online_appointments, offline_appointments,
                    total_earning, total_gst,
                    admin_collected_total, doctor_collected_total,
                    admin_cut, doctor_cut, adjustment_amount,
                    settlement_status, notes, created_at, updated_at,
                    given_to_doctor, received_from_doctor
                FROM admin_doctor_payment_summary
                WHERE doctor_id = ? AND settlement_status = ?
                ORDER BY created_at DESC, summary_id DESC
                LIMIT 1000";
        $st = $conn->prepare($sql);
        if (!$st) throw new Exception($conn->error);
        $st->bind_param("is", $doctor_id, $status);
    }

    $st->execute();
    $rs = $st->get_result();
    $summaries = [];
    $summaryIds = [];

    while ($r = $rs->fetch_assoc()) {
        $sid = (int)$r['summary_id'];
        $summaries[] = $r;
        $summaryIds[] = $sid;
    }
    $st->close();

    if (empty($summaryIds)) {
        echo json_encode(['success'=>true,'data'=>[]], JSON_UNESCAPED_UNICODE);
        exit;
    }

    // Build IN (?, ?, ...)
    $inPlace = implode(',', array_fill(0, count($summaryIds), '?'));
    $inTypes = str_repeat('i', count($summaryIds));

    // ----------------------------
    // 2) Latest doctor_transactions (Debit) for these summaries
    // ----------------------------
    $doctorTxnMap = [];
    $docSql = "SELECT
                    transaction_id, doctor_id, reference_id, settlement_summary_id,
                    transaction_type, method, amount, description, payment_proof,
                    status, utr_number, submitted_at, reviewed_at, admin_review_note, created_at
               FROM doctor_transactions
               WHERE doctor_id = ?
                 AND transaction_type = 'Debit'
                 AND (
                        settlement_summary_id IN ($inPlace)
                        OR reference_id IN ($inPlace)
                     )
               ORDER BY transaction_id DESC";

    $st = $conn->prepare($docSql);
    if ($st) {
        $params = array_merge([$doctor_id], $summaryIds, $summaryIds);
        $types  = 'i' . $inTypes . $inTypes;
        bindParams($st, $types, $params);

        $st->execute();
        $res = $st->get_result();
        while ($row = $res->fetch_assoc()) {
            $sid = (int)($row['settlement_summary_id'] ?? 0);
            if ($sid <= 0) $sid = (int)($row['reference_id'] ?? 0);
            if ($sid <= 0) continue;

            if (!isset($doctorTxnMap[$sid])) {
                $doctorTxnMap[$sid] = $row; // latest wins
            }
        }
        $st->close();
    }

    // ----------------------------
    // 3) Latest admin_transactions (Expense) for these summaries
    //    (no 'status' column in your table)
    // ----------------------------
    $adminTxnMap = [];
    $admSql = "SELECT
                    transaction_id, reference_id, settlement_summary_id, doctor_id,
                    transaction_type, method, amount, paid_amount,
                    description, payment_proof, payment_proofs, created_at
               FROM admin_transactions
               WHERE transaction_type = 'Expense'
                 AND (
                        settlement_summary_id IN ($inPlace)
                        OR reference_id IN ($inPlace)
                     )
                 AND (doctor_id = ? OR doctor_id IS NULL)
               ORDER BY transaction_id DESC";

    $st = $conn->prepare($admSql);
    if ($st) {
        $params = array_merge($summaryIds, $summaryIds, [$doctor_id]);
        $types  = $inTypes . $inTypes . 'i';
        bindParams($st, $types, $params);

        $st->execute();
        $res = $st->get_result();
        while ($row = $res->fetch_assoc()) {
            $sid = (int)($row['settlement_summary_id'] ?? 0);
            if ($sid <= 0) $sid = (int)($row['reference_id'] ?? 0);
            if ($sid <= 0) continue;

            if (!isset($adminTxnMap[$sid])) {
                $adminTxnMap[$sid] = $row; // latest wins
            }
        }
        $st->close();
    }

    // ----------------------------
    // 4) Build response
    // ----------------------------
    $out = [];

    // Final states for doctor -> admin
    $doctorFinalStates = ['approved', 'rejected'];

    foreach ($summaries as $r) {
        $sid = (int)$r['summary_id'];

        $givenToDoctor      = (float)($r['given_to_doctor'] ?? 0);      // Admin -> Doctor
        $receivedFromDoctor = (float)($r['received_from_doctor'] ?? 0); // Doctor -> Admin

        $doctorTxn = $doctorTxnMap[$sid] ?? null;
        $adminTxn  = $adminTxnMap[$sid] ?? null;

        // Doctor -> Admin pending?
        $doctorStatus      = $doctorTxn ? cleanStr($doctorTxn['status'] ?? '', 50) : '';
        $doctorFinal       = in_array(strtolower($doctorStatus), $doctorFinalStates, true);
        $doctorToAdminPending = ($receivedFromDoctor > 0) && (!$doctorFinal);

        // Admin -> Doctor pending?
        $adminProof     = $adminTxn ? cleanStr($adminTxn['payment_proof'] ?? '', 500) : '';
        $adminProofs    = $adminTxn ? cleanStr($adminTxn['payment_proofs'] ?? '', 2000) : '';
        $adminProofExists = ($adminProof !== '' || $adminProofs !== '');

        $adminAmt   = $adminTxn ? (float)($adminTxn['amount'] ?? 0) : (float)$givenToDoctor;
        $adminPaid  = $adminTxn ? (float)($adminTxn['paid_amount'] ?? 0) : 0.0;

        $adminComplete = ($adminTxn !== null) && ($adminPaid >= $adminAmt) && $adminProofExists;
        $adminToDoctorPending = ($givenToDoctor > 0) && (!$adminComplete);

        $anyPending = ($doctorToAdminPending || $adminToDoctorPending);

        // Apply pending-only filter ONLY for Pending requests
        if ($isPendingRequest && !$anyPending) {
            continue;
        }

        // Keep JSON shape used by Android
        $csv = trim((string)($r['last_payment_id'] ?? ''));

        $out[] = [
            'summary_id'              => $sid,
            'doctor_id'               => (int)$r['doctor_id'],

            'appointment_ids_csv'     => $csv,
            'appointment_count'       => (int)($r['total_appointments'] ?? 0),
            'online_appointments'     => (int)($r['online_appointments'] ?? 0),
            'offline_appointments'    => (int)($r['offline_appointments'] ?? 0),

            'total_base_ex_gst'       => num2($r['total_earning'] ?? 0),
            'total_gst'               => num2($r['total_gst'] ?? 0),
            'admin_collected_total'   => num2($r['admin_collected_total'] ?? 0),
            'doctor_collected_total'  => num2($r['doctor_collected_total'] ?? 0),

            'admin_cut'               => num2($r['admin_cut'] ?? 0),
            'doctor_cut'              => num2($r['doctor_cut'] ?? 0),
            'adjustment_amount'       => num2($r['adjustment_amount'] ?? 0),

            'settlement_status'       => (string)($r['settlement_status'] ?? 'Pending'),
            'notes'                   => (string)($r['notes'] ?? ''),
            'created_at'              => (string)($r['created_at'] ?? ''),
            'updated_at'              => (string)($r['updated_at'] ?? ''),

            'given_to_doctor'         => num2($givenToDoctor),
            'received_from_doctor'    => num2($receivedFromDoctor),

            // Diagnostics (safe for Android to ignore)
            'pending_state' => [
                'doctor_to_admin_pending' => (bool)$doctorToAdminPending,
                'admin_to_doctor_pending' => (bool)$adminToDoctorPending,
                'any_pending'             => (bool)$anyPending,
                'doctor_txn_status'       => (string)$doctorStatus,
                'admin_paid_vs_amount'    => ['paid'=>num2($adminPaid), 'amount'=>num2($adminAmt)],
                'admin_proof_exists'      => (bool)$adminProofExists
            ]
        ];
    }

    echo json_encode(['success'=>true,'data'=>$out], JSON_UNESCAPED_UNICODE);

} catch (Throwable $e) {
    if ($DEBUG) fail('DB error', 500, $e->getMessage());
    fail('Internal server error', 500);
}
