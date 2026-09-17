<?php
// insert_medical_report.php (Doctor Control)
include_once("../db_connection.php");

ini_set('display_errors', 0);
error_reporting(E_ALL);
header("Content-Type: application/json; charset=UTF-8");

// If your site base URL differs, update this:
$BASE_URL = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http")
            . "://".$_SERVER['HTTP_HOST']
            . rtrim(dirname($_SERVER['SCRIPT_NAME']), '/\\');
$UPLOAD_DIR_REL = "../medical_reports/";
$UPLOAD_DIR_URL = $BASE_URL . "/medical_reports/";

$rawInput = file_get_contents("php://input");
file_put_contents("log_raw_input.json", $rawInput);

$input = json_decode($rawInput, true);
if (json_last_error() !== JSON_ERROR_NONE) {
    file_put_contents("log_json_error.txt", json_last_error_msg());
    echo json_encode(["success" => false, "message" => "Invalid JSON input"]);
    exit;
}

// Required
if (!isset($input['appointment_id'], $input['patient_name'], $input['age'], $input['sex'])) {
    echo json_encode(["success" => false, "message" => "Required fields missing"]);
    exit;
}

// Helpers
$trim = function($v) { return is_string($v) ? trim($v) : $v; };
$nullIfEmpty = function($v) use ($trim){ $v = $trim($v); return ($v === '' || $v === null) ? null : $v; };

// Extract
$appointment_id     = intval($input['appointment_id']);
$patient_name       = $trim($input['patient_name']);
$age                = intval($input['age']);
$sex                = $trim($input['sex']);

$weight             = $nullIfEmpty($input['weight']         ?? null);
$patient_address    = $nullIfEmpty($input['patient_address'] ?? null);
$visit_date         = $nullIfEmpty($input['visit_date']      ?? null);
$temperature        = $nullIfEmpty($input['temperature']     ?? null);
$pulse              = $nullIfEmpty($input['pulse']           ?? null);
$spo2               = $nullIfEmpty($input['spo2']            ?? null);
$blood_pressure     = $nullIfEmpty($input['blood_pressure']  ?? null);
$respiratory_system = $nullIfEmpty($input['respiratory_system'] ?? null);
$symptoms           = $nullIfEmpty($input['symptoms']        ?? null);

// Medicines: JSON arrays (names & doses) — as you had
$medicinesNames = "";
$dosages = "";
if (isset($input['medicines']) && is_array($input['medicines'])) {
    $namesArray = [];
    $dosagesArray = [];
    foreach ($input['medicines'] as $medicine) {
        if (isset($medicine['medicine_name'], $medicine['dosage'])) {
            $namesArray[]   = $trim($medicine['medicine_name']);
            $dosagesArray[] = $trim($medicine['dosage']);
        }
    }
    $medicinesNames = json_encode($namesArray, JSON_UNESCAPED_UNICODE);
    $dosages        = json_encode($dosagesArray, JSON_UNESCAPED_UNICODE);
}

$doctor_name      = $trim($input['doctor_name'] ?? '');
$speciality       = $nullIfEmpty($input['speciality']      ?? null);
$registration_no  = $nullIfEmpty($input['registration_no'] ?? null);
$doctor_signature = $nullIfEmpty($input['doctor_signature'] ?? $doctor_name);
$investigations   = $nullIfEmpty($input['investigations']  ?? null);
$caspar_photo     = $nullIfEmpty($input['caspar_photo']    ?? null);
$report_type      = $nullIfEmpty($input['report_type']     ?? null);
$report_photo_b64 = $input['report_photo'] ?? null;

// Save report image if provided (accepts raw base64 or data URI)
$report_photo_filename = null;
$report_photo_url = null;
if (!empty($report_photo_b64)) {
    // Strip data URI prefix if present
    if (strpos($report_photo_b64, 'base64,') !== false) {
        $report_photo_b64 = explode('base64,', $report_photo_b64, 2)[1];
    }
    $image_data = base64_decode($report_photo_b64);
    if ($image_data !== false) {
        if (!file_exists($UPLOAD_DIR_REL)) {
            @mkdir($UPLOAD_DIR_REL, 0777, true);
        }
        // Try detect mime -> extension
        $ext = 'jpg';
        if (function_exists('finfo_open')) {
            $f = finfo_open(FILEINFO_MIME_TYPE);
            $mime = finfo_buffer($f, $image_data);
            finfo_close($f);
            if ($mime === 'image/png') $ext = 'png';
            elseif ($mime === 'image/jpeg') $ext = 'jpg';
            elseif ($mime === 'image/webp') $ext = 'webp';
        }
        $filename = "report_" . uniqid() . "." . $ext;
        $filepath = $UPLOAD_DIR_REL . $filename;

        if (file_put_contents($filepath, $image_data)) {
            $report_photo_filename = $filename;
            $report_photo_url = $UPLOAD_DIR_URL . $filename;
        } else {
            file_put_contents("log_file_error.txt", "Failed to save image at: " . $filepath);
        }
    }
}

// Insert
$sql = "INSERT INTO medical_reports (
    appointment_id, patient_name, age, sex, weight, patient_address, visit_date,
    temperature, pulse, spo2, blood_pressure, respiratory_system, symptoms,
    medications, dosage, doctor_name, speciality, registration_no, doctor_signature,
    investigations, caspar_photo, report_type, report_photo
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

$stmt = $conn->prepare($sql);
if (!$stmt) {
    file_put_contents("log_prepare_error.txt", $conn->error);
    echo json_encode(["success" => false, "message" => "SQL prepare failed"]);
    exit;
}

// NOTE: bind_param will pass PHP NULLs as SQL NULLs.
$stmt->bind_param(
    "isissssssssssssssssssss",
    $appointment_id, $patient_name, $age, $sex, $weight, $patient_address, $visit_date,
    $temperature, $pulse, $spo2, $blood_pressure, $respiratory_system, $symptoms,
    $medicinesNames, $dosages, $doctor_name, $speciality, $registration_no, $doctor_signature,
    $investigations, $caspar_photo, $report_type, $report_photo_filename
);

$updateSuccess = false;

if ($stmt->execute()) {
    // Mark appointment as having a report
    $updateSql = "UPDATE appointments SET has_report = 1 WHERE appointment_id = ?";
    if ($updateStmt = $conn->prepare($updateSql)) {
        $updateStmt->bind_param("i", $appointment_id);
        $updateSuccess = $updateStmt->execute();
        $updateStmt->close();
    } else {
        file_put_contents("log_update_prepare.txt", $conn->error);
    }

    echo json_encode([
        "success" => true,
        "message" => "Medical report saved successfully" . ($updateSuccess ? " and status updated." : ""),
        "report_photo_filename" => $report_photo_filename,
        "report_photo_url" => $report_photo_url
    ]);
} else {
    file_put_contents("log_execute_error.txt", $stmt->error);
    echo json_encode([
        "success" => false,
        "message" => "Failed to insert report"
    ]);
}

$stmt->close();
$conn->close();
