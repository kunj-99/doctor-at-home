<?php
/**
 * doctor_login.php — Doctor OTP request API
 * - Verifies mobile in `doctors` table
 * - Sets OTP in DB
 * - Sends OTP via NinzaSMS (real users)
 * - Provides DEMO login bypass for Play review (no SMS)
 */

declare(strict_types=1);

include_once '../db_connection.php';  // must define $conn (mysqli)
header('Content-Type: application/json');

// ------------------ CONFIG ------------------
const DEMO_ENABLED = true;              // set false after approval
const DEMO_MOBILE  = '9999999999';      // give this in Play Console "App access"
const DEMO_OTP     = '1234';          // static OTP (no SMS)

const NINZA_URL    = 'https://ninzasms.in.net/auth/send_sms';
const NINZA_TOKEN  = 'NINZASMSf6e2000ba91482e5cc0116b1b2bf1bc20818fd77297c02ae50e9a70b';
const NINZA_SENDER = '15155';

// ------------------ HELPERS -----------------
function respond_ok(array $data = []): void {
    echo json_encode(array_merge(['success' => true], $data));
    exit();
}
function respond_fail(string $msg, int $httpCode = 200): void {
    http_response_code($httpCode);
    echo json_encode(['success' => false, 'message' => $msg]);
    exit();
}
function normalize_mobile(string $raw): string {
    // keep last 10 digits (handle +91, spaces, dashes)
    $digits = preg_replace('/\D+/', '', $raw);
    if (strlen($digits) >= 10) {
        return substr($digits, -10);
    }
    return $digits;
}

// ------------------ INPUT -------------------
$mobile = isset($_POST['mobile']) ? trim($_POST['mobile']) : '';
if ($mobile === '') respond_fail('Mobile number is required');

$mobile = normalize_mobile($mobile);
if (!preg_match('/^\d{10}$/', $mobile)) {
    respond_fail('Enter a valid 10-digit mobile number');
}

// ------------------ MAIN --------------------
try {
    if (!isset($conn) || !($conn instanceof mysqli) || $conn->connect_errno) {
        error_log('DB connection missing/failed in doctor_login.php');
        respond_fail('Database connection failed');
    }

    // 1) Check doctor existence
    $stmt = $conn->prepare("SELECT doctor_id, full_name, specialization FROM doctors WHERE TRIM(mobile) = ?");
    if (!$stmt) respond_fail('Database prepare error: ' . $conn->error);

    $stmt->bind_param('s', $mobile);
    if (!$stmt->execute()) respond_fail('Database execute error: ' . $stmt->error);

    $result = $stmt->get_result();
    if (!$result) respond_fail('Database result error');

    if ($result->num_rows === 0) {
        error_log('Mobile not registered in doctors: ' . $mobile);
        respond_fail('Mobile number not registered');
    }
    $doctor = $result->fetch_assoc();
    $stmt->close();

    // 2) Decide OTP & whether to send SMS
    $skipSms = false;
    if (DEMO_ENABLED && $mobile === DEMO_MOBILE) {
        $otp = DEMO_OTP;      // static OTP for reviewer
        $skipSms = true;      // do not call SMS
        error_log("[DEMO] Using static OTP {$otp} for {$mobile}");
    } else {
        // 4-digit OTP (matches your prior implementation)
        $otp = strval(rand(1000, 9999));
        error_log("Generated OTP {$otp} for {$mobile}");
    }

    // 3) Store OTP in DB
    $updateStmt = $conn->prepare("UPDATE doctors SET otp = ? WHERE TRIM(mobile) = ?");
    if (!$updateStmt) respond_fail('Database prepare error: ' . $conn->error);
    $updateStmt->bind_param('is', $otp, $mobile);

    if (!$updateStmt->execute()) {
        $err = $updateStmt->error;
        $updateStmt->close();
        respond_fail('Failed to update OTP: ' . $err);
    }
    $updateStmt->close();

    // 4) If not demo, send OTP via NinzaSMS
    if (!$skipSms) {
        $fields = [
            'sender_id'        => NINZA_SENDER,
            'variables_values' => $otp,
            'numbers'          => $mobile,
        ];
        error_log('SMS API fields: ' . json_encode($fields));

        $ch = curl_init();
        curl_setopt_array($ch, [
            CURLOPT_URL            => NINZA_URL,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_ENCODING       => '',
            CURLOPT_MAXREDIRS      => 10,
            CURLOPT_TIMEOUT        => 30,
            CURLOPT_SSL_VERIFYHOST => 0, // consider enabling in prod
            CURLOPT_SSL_VERIFYPEER => 0, // consider enabling in prod
            CURLOPT_HTTP_VERSION   => CURL_HTTP_VERSION_1_1,
            CURLOPT_CUSTOMREQUEST  => 'POST',
            CURLOPT_POSTFIELDS     => json_encode($fields),
            CURLOPT_HTTPHEADER     => [
                'authorization: ' . NINZA_TOKEN,
                'accept: */*',
                'cache-control: no-cache',
                'content-type: application/json',
            ],
        ]);

        $respBody = curl_exec($ch);
        $curlErr  = curl_error($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        error_log("NinzaSMS HTTP {$httpCode} response: " . $respBody);
        if ($curlErr) {
            error_log('cURL error: ' . $curlErr);
            respond_fail('OTP SMS sending failed: ' . $curlErr);
        }

        // Try parse provider response (not mandatory)
        $decoded = json_decode($respBody, true);
        if (json_last_error() === JSON_ERROR_NONE && isset($decoded['success']) && $decoded['success'] === false) {
            $msg = isset($decoded['message']) ? $decoded['message'] : 'SMS provider error';
            respond_fail('OTP SMS sending failed: ' . $msg);
        }

        // Success (real flow)
        respond_ok([
            'message' => 'OTP sent successfully',
            'doctor'  => $doctor,
        ]);
    }

    // 5) DEMO success (no SMS)
    respond_ok([
        'message' => 'Demo login enabled. Use OTP ' . DEMO_OTP . ' to proceed.',
        'doctor'  => $doctor,
        'demo'    => true,
    ]);

} catch (Throwable $e) {
    error_log('Exception in doctor_login.php: ' . $e->getMessage());
    respond_fail('Server Error: ' . $e->getMessage());
} finally {
    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
}
