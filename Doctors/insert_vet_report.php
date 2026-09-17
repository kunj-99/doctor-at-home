<?php
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

// File: Doctors/insert_vet_report.php
header('Content-Type: application/json; charset=UTF-8');
header('Access-Control-Allow-Methods: POST, OPTIONS');

require_once("../db_connection.php");

// --- Logging ---
$LOG_FILE = __DIR__ . '/../animal_report_log.txt';
function logx($msg){
    global $LOG_FILE;
    @file_put_contents($LOG_FILE, "[".date("Y-m-d H:i:s")."] $msg\n", FILE_APPEND);
    error_log("[insert_vet_report] $msg"); // also to PHP error log
}

// ---- BEGIN REQUEST ----
$raw = file_get_contents('php://input') ?: '';
logx("---- NEW REQUEST ----");
logx("RAW JSON BODY: " . substr($raw, 0, 800) . (strlen($raw) > 800 ? '...[TRUNCATED]' : ''));

$data = [];
if ($raw !== '') {
    $tmp = json_decode($raw, true);
    if (is_array($tmp)) $data = $tmp;
}
if (empty($data)) {
    logx("ERROR: No data received or invalid JSON.");
    echo json_encode(['success' => false, 'message' => 'No data received']); exit;
}

// --- Helpers ---
function s($arr, $k, $default = null) {
    if (!is_array($arr)) return $default;
    if (!array_key_exists($k, $arr)) return $default;
    $v = $arr[$k];
    if ($v === '' || $v === null) return $default;
    return is_string($v) ? trim($v) : $v;
}
function dec_or_null($v) {
    if ($v === null || $v === '') return null;
    if (is_numeric($v)) return (string)$v;
    return null;
}
function int_or_null($v) {
    if ($v === null || $v === '') return null;
    if (is_numeric($v)) return (string)intval($v);
    return null;
}
function bool01($v) { return ($v === true || $v === 1 || $v === '1' || $v === 'true' || $v === 'on') ? 1 : 0; }
function ymd_or_today($v) {
    if (!$v) return date('Y-m-d');
    $ts = strtotime($v);
    if ($ts === false) return date('Y-m-d');
    return date('Y-m-d', $ts);
}

// ====== Extract top-level fields (flat, exactly as Java sends) ======
$appointment_id      = s($data, 'appointment_id');
$animal_name         = s($data, 'animal_name');
$species_breed       = s($data, 'species_breed');
$sex                 = s($data, 'sex');
$age_years           = dec_or_null(s($data, 'age_years'));
$weight_kg           = dec_or_null(s($data, 'weight_kg'));
$owner_address       = s($data, 'owner_address');

$report_date_in      = s($data, 'report_date');     // <-- Java sends report_date
$report_date         = ymd_or_today($report_date_in);
$report_title        = s($data, 'report_title', 'Animal Virtual Report');
$report_type         = s($data, 'report_type');
$doctor_signature    = s($data, 'doctor_signature'); // <-- Java sends doctor_signature

$attachment_b64      = s($data, 'attachment_url');   // base64 string or empty
$attachment_url      = null;

$next_visit_date_in  = s($data, 'next_visit_date');
$next_visit_date     = $next_visit_date_in ? ymd_or_today($next_visit_date_in) : null;
$is_followup         = bool01(s($data, 'is_followup'));

// Vitals (all flat)
$temperature_c       = dec_or_null(s($data, 'temperature_c'));
$pulse_bpm           = int_or_null(s($data, 'pulse_bpm'));
$spo2_pct            = int_or_null(s($data, 'spo2_pct'));
$bp_mmhg             = s($data, 'bp_mmhg');
$resp_rate_bpm       = int_or_null(s($data, 'respiratory_rate_bpm'));
$pain_score          = int_or_null(s($data, 'pain_score_0_10'));
$hydration_status    = s($data, 'hydration_status');
$mucous_membranes    = s($data, 'mucous_membranes');
$crt_sec             = dec_or_null(s($data, 'crt_sec'));

// Clinical notes (flat)
$behavior_gait       = s($data, 'behavior_gait');
$skin_coat           = s($data, 'skin_coat');
$symptoms            = s($data, 'symptoms');
$respiratory_system  = s($data, 'respiratory_system');
$reasons             = s($data, 'reasons');

// Investigation (flat)
$requires_invest     = bool01(s($data, 'requires_investigation'));
$investigation_notes = s($data, 'investigation_notes');

// Vaccination (flat, optional)
$vaccination_id      = int_or_null(s($data, 'vaccination_id'));
$vaccination_name    = s($data, 'vaccination_name');
$vaccination_notes   = s($data, 'vaccination_notes');

// Medications JSON strings (already prepared by app)
$medications_json    = s($data, 'medications_json', '[]');
$dosage_json         = s($data, 'dosage_json', '[]');

// Ensure medications JSON are valid JSON arrays
$__mj = json_decode($medications_json, true);
if (!is_array($__mj)) $medications_json = json_encode([]);
$__dj = json_decode($dosage_json, true);
if (!is_array($__dj)) $dosage_json = json_encode([]);

// ====== Basic validation ======
if (!ctype_digit((string)$appointment_id)) {
    logx("ERROR: appointment_id is required & must be numeric");
    echo json_encode(['success' => false, 'message' => 'appointment_id is required & must be numeric']); exit;
}
if ($animal_name === null || $animal_name === '') {
    logx("ERROR: animal_name is required");
    echo json_encode(['success' => false, 'message' => 'animal_name is required']); exit;
}
// For virtual reports we require symptoms. If an image is sent, allow empty symptoms.
if (empty($attachment_b64) && ($symptoms === null || $symptoms === '')) {
    logx("ERROR: symptoms is required for virtual reports (no image)");
    echo json_encode(['success' => false, 'message' => 'symptoms is required for virtual reports']); exit;
}

// ====== Save base64 image if present ======
if (!empty($attachment_b64)) {
    logx("Attachment base64 received (length=" . strlen($attachment_b64) . ")");
    $destRoot = realpath(__DIR__ . '/..');
    if ($destRoot === false) {
        logx("ERROR: Server path error on realpath()");
        http_response_code(500);
        echo json_encode(['success' => false, 'message' => 'Server path error']); exit;
    }
    $destDir = $destRoot . DIRECTORY_SEPARATOR . 'medical_reports' . DIRECTORY_SEPARATOR;
    if (!is_dir($destDir)) {
        logx("medical_reports/ directory does not exist, trying to create...");
        if (!@mkdir($destDir, 0775, true) && !is_dir($destDir)) {
            logx("ERROR: Failed to create medical_reports directory");
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Failed to create medical_reports directory']); exit;
        }
        logx("medical_reports/ directory created");
    }

    // Strip data URL header if present
    $attachment_b64_clean = $attachment_b64;
    if (preg_match('/^data:image\/(\w+);base64,/', $attachment_b64, $matches)) {
        $attachment_b64_clean = substr($attachment_b64, strpos($attachment_b64, ',') + 1);
        $ext = strtolower($matches[1]);
    } else {
        $ext = 'jpg';
    }
    logx("Image extension detected: $ext");

    $img_data = base64_decode($attachment_b64_clean);
    if ($img_data === false) {
        logx("ERROR: Invalid base64 image data!");
        echo json_encode(['success' => false, 'message' => 'Invalid image base64']); exit;
    }
    logx("Decoded image size: " . strlen($img_data));

    $base = 'report_' . (int)$appointment_id . '_' . time() . '_' . bin2hex(random_bytes(3)) . '.' . $ext;
    $destPath = $destDir . $base;
    logx("Image will be saved to: $destPath");

    if (@file_put_contents($destPath, $img_data) === false) {
        logx("ERROR: Failed to save image file ($destPath)");
        echo json_encode(['success' => false, 'message' => 'Failed to save image file']); exit;
    } else {
        logx("Image file saved successfully: $destPath");
    }

    $attachment_url = 'medical_reports/' . $base;
    if ($report_type === null || $report_type === '') {
        $report_type = 'photo';
        logx("Report type set to 'photo' (auto)");
    }
}

// ====== Build INSERT (escaped where needed) ======
$E = fn($v) => $v === null ? null : $conn->real_escape_string((string)$v);

// Animal category resolution (fallback from appointments)
$animal_id = int_or_null(s($data, 'animal_id'));
if ($animal_id === null) {
    $animal_id = int_or_null(s($data, 'animal_category_id'));
}
if ($animal_id === null) {
    logx("No animal_id given, fetching from appointments table");
    if ($stmt = $conn->prepare("SELECT animal_category_id FROM appointments WHERE appointment_id = ? LIMIT 1")) {
        $aid = (int)$appointment_id;
        $stmt->bind_param("i", $aid);
        if ($stmt->execute()) {
            $stmt->bind_result($cat);
            if ($stmt->fetch()) {
                if ($cat !== null) $animal_id = (int)$cat;
                logx("Fetched animal_category_id: " . $animal_id);
            } else {
                logx("No row found in appointments for appointment_id: $appointment_id");
            }
        } else {
            logx("ERROR executing appointment lookup: " . $stmt->error);
        }
        $stmt->close();
    } else {
        logx("ERROR: Could not prepare SELECT animal_category_id statement");
    }
}

$vals = [
    "appointment_id"         => (int)$appointment_id,
    "animal_name"            => $E($animal_name),
    "species_breed"          => $species_breed === null ? null : $E($species_breed),
    "sex"                    => $sex === null ? null : $E($sex),
    "age_years"              => $age_years === null ? null : $E($age_years),
    "weight_kg"              => $weight_kg === null ? null : $E($weight_kg),
    "owner_address"          => $owner_address === null ? null : $E($owner_address),
    "animal_id"              => $animal_id === null ? null : (int)$animal_id,
    "report_date"            => $E($report_date),
    "report_title"           => $E($report_title),
    "report_type"            => $report_type === null ? null : $E($report_type),
    "doctor_signature"       => $doctor_signature === null ? null : $E($doctor_signature),

    // Vitals
    "temperature_c"          => $temperature_c === null ? null : $E($temperature_c),
    "pulse_bpm"              => $pulse_bpm === null ? null : (int)$pulse_bpm,
    "spo2_pct"               => $spo2_pct === null ? null : (int)$spo2_pct,
    "bp_mmhg"                => $bp_mmhg === null ? null : $E($bp_mmhg),
    "respiratory_rate_bpm"   => $resp_rate_bpm === null ? null : (int)$resp_rate_bpm,
    "pain_score_0_10"        => $pain_score === null ? null : (int)$pain_score,
    "hydration_status"       => $hydration_status === null ? null : $E($hydration_status),
    "mucous_membranes"       => $mucous_membranes === null ? null : $E($mucous_membranes),
    "crt_sec"                => $crt_sec === null ? null : $E($crt_sec),

    // Notes
    "symptoms"               => $symptoms === null ? null : $E($symptoms),
    "behavior_gait"          => $behavior_gait === null ? null : $E($behavior_gait),
    "skin_coat"              => $skin_coat === null ? null : $E($skin_coat),
    "respiratory_system"     => $respiratory_system === null ? null : $E($respiratory_system),
    "reasons"                => $reasons === null ? null : $E($reasons),

    // Investigation
    "requires_investigation" => (int)$requires_invest,
    "investigation_notes"    => $investigation_notes === null ? null : $E($investigation_notes),

    // Medications JSON (already serialized)
    "medications_json"       => $E($medications_json),
    "dosage_json"            => $E($dosage_json),

    // Vaccination
    "vaccination_id"         => $vaccination_id === null ? null : (int)$vaccination_id,
    "vaccination_name"       => $vaccination_name === null ? null : $E($vaccination_name),
    "vaccination_notes"      => $vaccination_notes === null ? null : $E($vaccination_notes),

    // Attachment & next visit
    "attachment_url"         => $attachment_url === null ? null : $E($attachment_url),
    "next_visit_date"        => $next_visit_date === null ? null : $E($next_visit_date),

    "is_followup"            => (int)$is_followup
];

// LOG ALL SQL VALUES
foreach ($vals as $k=>$v) logx("SQL VAL $k=" . var_export($v, true));

// Build SQL strings
$columns = [];
$values  = [];
foreach ($vals as $col => $val) {
    $columns[] = "`$col`";
    if ($val === null) {
        $values[] = "NULL";
    } else {
        if (is_int($val)) {
            $values[] = (string)$val;
        } else {
            $values[] = "'$val'";
        }
    }
}
$sql = "INSERT INTO `animal_medical_reports` (".implode(',', $columns).") VALUES (".implode(',', $values).")";
logx("INSERT SQL → $sql");

if (!isset($conn) || !$conn) {
    logx("ERROR: MySQL connection object is not set or false!");
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'No DB connection']);
    exit;
}

// Execute
if (!$conn->query($sql)) {
    logx("MySQL ERROR: ".$conn->error);
    logx("FULL SQL: $sql");
    logx("MySQL thread id: ".$conn->thread_id);
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'DB error: '.$conn->error]);
    exit;
}

logx("MySQL insert affected_rows: ".$conn->affected_rows);
$rid = (int)$conn->insert_id;
$out = ['success' => true, 'report_id' => $rid];
if ($attachment_url) $out['attachment_url'] = $attachment_url;

logx("SUCCESS: Inserted report_id=$rid");

// --- After successful insert, update has_report in appointments table ---
$update_ok = false;
$update_err = '';
if ($appointment_id && $rid > 0) {
    $update_sql = "UPDATE appointments SET has_report=1 WHERE appointment_id=" . intval($appointment_id) . " LIMIT 1";
    logx("UPDATE SQL → $update_sql");
    if ($conn->query($update_sql)) {
        $update_ok = true;
        logx("has_report updated to 1 for appointment_id=$appointment_id");
    } else {
        $update_err = $conn->error;
        logx("MySQL ERROR (update has_report): $update_err");
    }
    $out['has_report_update'] = $update_ok ? 'ok' : 'failed';
    if (!$update_ok) $out['has_report_error'] = $update_err;
}

echo json_encode($out);
