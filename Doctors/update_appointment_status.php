<?php
// Doctors/update_appointment_status.php
include_once("../db_connection.php");

header("Content-Type: application/json; charset=UTF-8");
error_reporting(E_ALL);
ini_set('display_errors', 1);

/**
 * ────────────────────────────────────────────────────────────────────────────
 * FILE LOGGING (same folder)
 * ────────────────────────────────────────────────────────────────────────────
 */
$LOG_FILE = __DIR__ . '/update_appointment_status.log';
function logx($msg, $context = null) {
    global $LOG_FILE;
    $ts = date('Y-m-d H:i:s');
    $ms = sprintf('%.3f', fmod(microtime(true), 1));
    $line = "[$ts $ms] $msg";
    if ($context !== null) {
        $safe = json_decode(json_encode($context), true);
        $mask = ['access_token','private_key','assertion','fcm_token'];
        if (is_array($safe)) {
            foreach ($mask as $k) {
                if (isset($safe[$k]) && is_string($safe[$k])) {
                    $safe[$k] = substr($safe[$k], 0, 8) . '…(masked)';
                }
            }
        }
        $line .= ' | ' . (is_string($context) ? $context : json_encode($safe));
    }
    @file_put_contents($LOG_FILE, $line . PHP_EOL, FILE_APPEND | LOCK_EX);
}

logx('==== REQUEST START ====', ['ip'=>$_SERVER['REMOTE_ADDR'] ?? '?', 'uri'=>$_SERVER['REQUEST_URI'] ?? '?']);

/**
 * ────────────────────────────────────────────────────────────────────────────
 * FCM (PATIENT APP) — SAME PROJECT, WITH FALLBACK FILE PATH
 * ────────────────────────────────────────────────────────────────────────────
 * 1) Try outside webroot (same as other pages)
 * 2) Fallback to Doctors/secure_directory copy you created
 */
$FIREBASE_PROJECT_ID = 'doctor-at-home-bf9f0';
$ANDROID_CHANNEL_ID  = null;

$FIREBASE_JSON_PATHS = [
    '/home/u357694546/fcm-doctorathome-user.json',
    __DIR__ . '/secure_directory/fcm-doctorathome-user.json',
];

$FIREBASE_JSON_PATH = null;
foreach ($FIREBASE_JSON_PATHS as $p) {
    if (is_readable($p)) { $FIREBASE_JSON_PATH = $p; break; }
}
logx('FCM CONFIG', ['project'=>$FIREBASE_PROJECT_ID, 'json_candidates'=>$FIREBASE_JSON_PATHS, 'using'=>$FIREBASE_JSON_PATH, 'channel'=>$ANDROID_CHANNEL_ID]);

/**
 * ────────────────────────────────────────────────────────────────────────────
 * INPUT
 * ────────────────────────────────────────────────────────────────────────────
 */
$raw = file_get_contents("php://input");
$input = json_decode($raw, true);
logx('RAW INPUT', $raw);

$resp = [
    "success" => false,
    "message" => "Unknown error",
    "payment_history_updated" => false,
    "refund_created" => false,
    "wallet_credited" => false,
    "debug" => []
];

if (!isset($input['appointment_id']) || (!isset($input['action']) && !isset($input['status']))) {
    $resp["message"] = "Appointment ID and either action or status are required.";
    logx('VALIDATION FAIL', $resp["message"]);
    echo json_encode($resp); exit;
}

$appointment_id = (int)$input['appointment_id'];

/**
 * Resolve new status
 */
if (isset($input['action'])) {
    $act = strtolower(trim($input['action']));
    switch ($act) {
        case "complete": $new_status = "Completed"; break;
        case "cancel":   $new_status = "Cancelled_by_doctor"; break;
        case "confirm":  $new_status = "Confirmed"; break;
        default:
            $resp["message"] = "Invalid action. Use 'complete', 'cancel', or 'confirm'.";
            logx('ACTION INVALID', ['action'=>$act]);
            echo json_encode($resp); exit;
    }
} else {
    $new_status = trim((string)$input['status']);
    $allowed = ["Pending", "Cancelled_by_doctor", "Completed", "Confirmed"];
    if (!in_array($new_status, $allowed, true)) {
        $resp["message"] = "Invalid status. Must be one of: " . implode(", ", $allowed);
        logx('STATUS INVALID', ['status'=>$new_status]);
        echo json_encode($resp); exit;
    }
}
logx('STATUS RESOLVED', ['appointment_id'=>$appointment_id, 'new_status'=>$new_status]);

/**
 * Fetch essentials
 */
$info_q = $conn->prepare(
    "SELECT doctor_id, patient_id, payment_method, appointment_mode, patient_name
     FROM appointments WHERE appointment_id = ?"
);
$info_q->bind_param("i", $appointment_id);
$info_q->execute();
$info_q->bind_result($doctor_id, $patient_id, $appt_payment_method, $appointment_mode, $patient_name);
$info_q->fetch();
$info_q->close();

$resp["debug"]["appt"] = [
    "doctor_id" => $doctor_id,
    "patient_id" => $patient_id,
    "payment_method_from_appt" => $appt_payment_method,
    "appointment_mode" => $appointment_mode
];
logx('APPT FETCH', $resp["debug"]["appt"]);

if (!$doctor_id) {
    $resp["message"] = "Doctor not found for this appointment.";
    logx('APPT INVALID: MISSING DOCTOR');
    echo json_encode($resp); exit;
}

/**
 * Guards
 */
if ($new_status === "Pending") {
    $q = $conn->prepare("SELECT COUNT(*) FROM appointments WHERE doctor_id = ? AND status = 'Pending'");
    $q->bind_param("i", $doctor_id);
    $q->execute(); $q->bind_result($cnt); $q->fetch(); $q->close();
    logx('GUARD Pending', ['pending_count'=>$cnt]);
    if ($cnt > 0) { $resp["message"] = "An appointment is already pending. Please complete it before accepting another."; logx('BLOCK Pending'); echo json_encode($resp); exit; }
}
if ($new_status === "Confirmed") {
    $q = $conn->prepare("SELECT COUNT(*) FROM appointments WHERE doctor_id = ? AND status = 'Confirmed'");
    $q->bind_param("i", $doctor_id);
    $q->execute(); $q->bind_result($cnt); $q->fetch(); $q->close();
    logx('GUARD Confirmed', ['confirmed_count'=>$cnt]);
    if ($cnt > 0) { $resp["message"] = "An appointment is already ongoing. Please complete it before accepting another."; logx('BLOCK Confirmed'); echo json_encode($resp); exit; }
}

/**
 * ETA
 */
$eta_value = (isset($input['eta']) && is_numeric($input['eta'])) ? (int)$input['eta'] : 0;
$eta_unit  = 'minutes';
if (isset($input['eta_unit'])) {
    $u = strtolower(trim($input['eta_unit']));
    if (in_array($u, ['minutes','hours'], true)) $eta_unit = $u;
}
$resp["debug"]["eta"] = ["value"=>$eta_value, "unit"=>$eta_unit];
logx('ETA', $resp["debug"]["eta"]);

/**
 * Helpers (unchanged)
 */
function fetch_payment_history($conn, $appointment_id) {
    $sql = "SELECT * FROM payment_history WHERE appointment_id = ? LIMIT 1";
    $st = $conn->prepare($sql); if (!$st) return [null, "prepare_failed:payment_history_select"];
    $st->bind_param("i", $appointment_id);
    if (!$st->execute()) { $st->close(); return [null, "execute_failed:payment_history_select"]; }
    $res = $st->get_result(); $row = $res->fetch_assoc(); $st->close();
    return [$row ?: null, null];
}
function create_refund_request($conn, $data, $statusOverride = null, &$err = null) {
    $status = $statusOverride ?: 'Requested';
    $sql = "INSERT INTO refund_requests
            (appointment_id, payment_id, patient_id, doctor_id, initiated_by, payment_method, appointment_mode,
             deposit_cut, refundable_amount, refund_to, upi_id, status, notes, created_at, updated_at)
            VALUES (?, ?, ?, ?, 'doctor', ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
    $st = $conn->prepare($sql); if (!$st) { $err = "prepare_failed:refund_insert ".$conn->error; return false; }
    $st->bind_param(
        "iiisssddssss",
        $data['appointment_id'],
        $data['payment_id'],
        $data['patient_id'],
        $data['doctor_id'],
        $data['payment_method'],
        $data['appointment_mode'],
        $data['deposit_cut'],
        $data['refundable_amount'],
        $data['refund_to'],
        $data['upi_id'],
        $status,
        $data['notes']
    );
    $ok = $st->execute(); if (!$ok) $err = "execute_failed:refund_insert ".$st->error;
    $st->close(); return $ok;
}
function credit_wallet($conn, $patient_id, $amount, $reason, &$err = null) {
    $st = $conn->prepare("UPDATE patients SET wallet_balance = wallet_balance + ? , updated_at = NOW() WHERE patient_id = ?");
    if (!$st) { $err = "prepare_failed:wallet_update ".$conn->error; return false; }
    $st->bind_param("di", $amount, $patient_id);
    if (!$st->execute()) { $err = "execute_failed:wallet_update ".$st->error; $st->close(); return false; }
    $st->close();

    $st2 = $conn->prepare("INSERT INTO wallet_transactions (`patient_id`,`amount`,`type`,`reason`,`timestamp`) VALUES (?, ?, 'credit', ?, NOW())");
    if (!$st2) { $err = "prepare_failed:wallet_txn_insert ".$conn->error; return false; }
    $st2->bind_param("ids", $patient_id, $amount, $reason);
    $ok = $st2->execute(); if (!$ok) $err = "execute_failed:wallet_txn_insert ".$st2->error;
    $st2->close(); return $ok;
}
function ensure_ph_from_wallet_debit($conn, $appointment_id, $patient_id, $doctor_id, $patient_name, &$seedPhRow, &$err = null) {
    $q = $conn->prepare("
        SELECT amount, reason, `timestamp`
        FROM wallet_transactions
        WHERE patient_id = ? AND type = 'debit'
          AND `timestamp` >= DATE_SUB(NOW(), INTERVAL 7 DAY)
        ORDER BY `timestamp` DESC LIMIT 1
    ");
    if (!$q) { $err = "prepare_failed:wallet_probe ".$conn->error; return false; }
    $q->bind_param("i", $patient_id);
    if (!$q->execute()) { $err = "execute_failed:wallet_probe ".$q->error; $q->close(); return false; }
    $res = $q->get_result(); $row = $res->fetch_assoc(); $q->close();
    if (!$row) { $err = "no_wallet_debit_found"; return false; }

    $deposit = (float)$row['amount']; if ($deposit <= 0) { $err = "wallet_debit_nonpositive"; return false; }

    $st = $conn->prepare("
        INSERT INTO payment_history
        ( patient_id, appointment_id, doctor_id, patient_name, amount, consultation_fee, deposit, deposit_status,
          payment_method, distance, distance_charge, gst, total_payment, admin_commission, doctor_earning,
          payment_status, refund_status, notes, upi_id, created_at, updated_at )
        VALUES (?, ?, ?, ?, 0.00, 0.00, ?, 'Wallet Debited',
                'Offline', 0.00, 0.00, 0.00, 0.00, 0.00, 0.00,
                'Pending', 'None', 'seeded_by_cancel_fallback', NULL, NOW(), NOW())
    ");
    if (!$st) { $err = "prepare_failed:ph_seed2 ".$conn->error; return false; }
    $st->bind_param("iiisd", $patient_id, $appointment_id, $doctor_id, $patient_name, $deposit);
    if (!$st->execute()) { $err = "execute_failed:ph_seed ".$st->error; $st->close(); return false; }
    $st->close();

    $seedPhRow = [
        'payment_id'     => $conn->insert_id,
        'payment_method' => 'Offline',
        'payment_status' => 'Pending',
        'deposit_status' => 'Wallet Debited',
        'deposit'        => $deposit,
        'total_payment'  => 0.00,
        'upi_id'         => null
    ];
    return true;
}

/**
 * ────────────────────────────────────────────────────────────────────────────
 * TRANSACTION
 * ────────────────────────────────────────────────────────────────────────────
 */
$conn->begin_transaction();
try {
    $st = $conn->prepare("UPDATE appointments SET status = ?, eta_value = ?, eta_unit = ? WHERE appointment_id = ?");
    if (!$st) throw new Exception("prepare_failed:appt_update ".$conn->error);
    $st->bind_param("sisi", $new_status, $eta_value, $eta_unit, $appointment_id);
    if (!$st->execute()) throw new Exception("execute_failed:appt_update ".$st->error);
    $st->close();
    logx('APPT UPDATED', ['appointment_id'=>$appointment_id, 'status'=>$new_status, 'eta'=>"$eta_value $eta_unit"]);

    // Completed + Offline ⇒ close payment_history
    if ($new_status === "Completed" && strcasecmp((string)$appt_payment_method, "Offline") === 0) {
        $uph = $conn->prepare("UPDATE payment_history SET payment_status = 'Completed', updated_at = NOW() WHERE appointment_id = ?");
        if ($uph) {
            $uph->bind_param("i", $appointment_id);
            $uph->execute();
            $resp["payment_history_updated"] = $uph->affected_rows > 0;
            $uph->close();
            logx('PAYMENT_HISTORY UPDATED (Offline->Completed)', ['updated'=>$resp["payment_history_updated"]]);
        }
    }

    // Doctor cancel ⇒ refund rules
    if ($new_status === "Cancelled_by_doctor") {
        list($ph, $phErr) = fetch_payment_history($conn, $appointment_id);
        if ($phErr) { $resp["debug"]["ph_error"] = $phErr; logx('PH ERROR', $phErr); }

        if (!$ph && strcasecmp((string)$appt_payment_method, "Offline") === 0) {
            $seed=null; $seedErr=null;
            if (ensure_ph_from_wallet_debit($conn, $appointment_id, $patient_id, $doctor_id, (string)$patient_name, $seed, $seedErr)) {
                $resp["debug"]["ph_seeded"] = $seed; $ph = $seed; logx('PH SEEDED FROM WALLET', $seed);
            } else { $resp["debug"]["ph_seed_error"] = $seedErr ?: 'seed_unknown_error'; logx('PH SEED FAIL', $seedErr); }
        }

        if ($ph) {
            $pm_method      = (string)$ph['payment_method'];
            $pm_status      = (string)$ph['payment_status'];
            $deposit_status = (string)$ph['deposit_status'];
            $deposit        = isset($ph['deposit']) ? (float)$ph['deposit'] : 0.0;
            $amount_paid    = isset($ph['amount']) ? (float)$ph['amount'] : 0.0;   // user-paid total
            $upi            = isset($ph['upi_id']) ? trim((string)$ph['upi_id']) : null;
            $payment_id     = isset($ph['payment_id']) ? (int)$ph['payment_id'] : 0;

            $resp["debug"]["ph"] = [
                "payment_method"=>$pm_method,
                "payment_status"=>$pm_status,
                "deposit_status"=>$deposit_status,
                "deposit"=>$deposit,
                "amount_paid"=>$amount_paid,
                "upi"=>$upi ? substr($upi,0,3).'…' : null
            ];
            logx('PH SNAPSHOT', $resp["debug"]["ph"]);

            if (strcasecmp($pm_method,'Online')===0 && strcasecmp($pm_status,'Completed')===0) {
                $addDeposit   = (strcasecmp($deposit_status,'Wallet Debited')===0) ? $deposit : 0.0;
                $refundAmount = $amount_paid + $addDeposit;
                $depositCut   = $addDeposit;
                $resp["debug"]["refund_calc"] = ["base_amount"=>$amount_paid,"add_deposit"=>$addDeposit,"refund_total"=>$refundAmount];
                logx('REFUND CALC (Online Completed)', $resp["debug"]["refund_calc"]);

                $err=null;
                $ok = create_refund_request($conn, [
                    'appointment_id'=>$appointment_id,
                    'payment_id'=>$payment_id,
                    'patient_id'=>$patient_id,
                    'doctor_id'=>$doctor_id,
                    'payment_method'=>$pm_method,
                    'appointment_mode'=>$appointment_mode,
                    'deposit_cut'=>$depositCut,
                    'refundable_amount'=>$refundAmount,
                    'refund_to'=>'UPI',
                    'upi_id'=>$upi ?: '',
                    'notes'=>(strcasecmp($deposit_status,'Wallet Debited')===0)
                                ? 'Doctor cancelled: refund = amount + deposit'
                                : 'Doctor cancelled: refund = amount only'
                ], null, $err);

                if ($ok) {
                    $resp["refund_created"] = true;
                    $st2 = $conn->prepare("UPDATE payment_history SET refund_status = 'Requested', updated_at = NOW() WHERE appointment_id = ?");
                    $st2->bind_param("i", $appointment_id);
                    $st2->execute(); $st2->close();
                    logx('REFUND REQUESTED (UPI)', ['amount'=>$refundAmount]);
                } else {
                    $resp["debug"]["refund_error"] = $err ?: 'unknown_error_refund_insert';
                    logx('REFUND ERROR', $resp["debug"]["refund_error"]);
                }
            } elseif (strcasecmp($pm_method,'Offline')===0 && strcasecmp($deposit_status,'Wallet Debited')===0 && $deposit>0) {
                $werr=null;
                $okC = credit_wallet($conn, $patient_id, $deposit, 'Doctor cancellation: deposit refund', $werr);
                $resp["wallet_credited"] = $okC;
                if (!$okC) { $resp["debug"]["wallet_error"]=$werr ?: 'unknown_wallet_error'; logx('WALLET CREDIT ERROR', $resp["debug"]["wallet_error"]); }
                else { logx('WALLET CREDITED', ['deposit'=>$deposit]); }

                $rerr=null;
                $okR = create_refund_request($conn, [
                    'appointment_id'=>$appointment_id,
                    'payment_id'=>$payment_id,
                    'patient_id'=>$patient_id,
                    'doctor_id'=>$doctor_id,
                    'payment_method'=>$pm_method,
                    'appointment_mode'=>$appointment_mode,
                    'deposit_cut'=>0.0,
                    'refundable_amount'=>$deposit,
                    'refund_to'=>'Wallet',
                    'upi_id'=>'',
                    'notes'=>'Doctor cancelled: deposit wallet refund'
                ], 'Completed', $rerr);
                if ($okR) { $resp["refund_created"] = true; logx('REFUND RECORDED (Wallet)', ['amount'=>$deposit]); }
                else { $resp["debug"]["refund_error"]=$rerr ?: 'unknown_error_refund_insert'; logx('REFUND ERROR (Wallet)', $resp["debug"]["refund_error"]); }
            } else {
                $resp["debug"]["refund_reason"] = 'conditions_not_matched';
                logx('REFUND SKIP: CONDITIONS NOT MET', ['method'=>$pm_method,'status'=>$pm_status,'deposit_status'=>$deposit_status]);
            }
        } else {
            $resp["debug"]["refund_reason"] = 'no_payment_history';
            logx('REFUND SKIP: NO PAYMENT_HISTORY');
        }
    }

    $conn->commit();
    $resp["success"] = true;
    $resp["message"] = "Appointment updated to '{$new_status}' with ETA {$eta_value} {$eta_unit}.";
    logx('TX COMMIT OK', ['message'=>$resp["message"]]);

} catch (Exception $e) {
    $conn->rollback();
    $resp["success"] = false;
    $resp["message"] = $e->getMessage();
    logx('TX ROLLBACK', $resp["message"]);
}

/**
 * ────────────────────────────────────────────────────────────────────────────
 * PATIENT NOTIFICATION
 * ────────────────────────────────────────────────────────────────────────────
 */
$resp["debug"]["fcm"] = ["attempted"=>false];

function b64url($d){ return rtrim(strtr(base64_encode($d), '+/', '-_'), '='); }
function getAccessToken_patient($jsonKeyFilePath) {
    if (!$jsonKeyFilePath || !is_readable($jsonKeyFilePath)) return [null, "Service key not readable at: $jsonKeyFilePath"];
    $creds = json_decode(file_get_contents($jsonKeyFilePath), true);
    if (!$creds || empty($creds['client_email']) || empty($creds['private_key']) || empty($creds['token_uri'])) return [null, "Invalid service account JSON"];

    $now=time();
    $hdr=['alg'=>'RS256','typ'=>'JWT'];
    $claim=['iss'=>$creds['client_email'],'sub'=>$creds['client_email'],'aud'=>$creds['token_uri'],'iat'=>$now,'exp'=>$now+3600,'scope'=>'https://www.googleapis.com/auth/firebase.messaging'];
    $unsigned=b64url(json_encode($hdr)).'.'.b64url(json_encode($claim));
    if(!openssl_sign($unsigned,$sig,$creds['private_key'],'sha256')) return [null,"Failed to sign JWT"];
    $assertion=$unsigned.'.'.b64url($sig);

    $post=http_build_query(['grant_type'=>'urn:ietf:params:oauth:grant-type:jwt-bearer','assertion'=>$assertion]);
    $ch=curl_init($creds['token_uri']);
    curl_setopt_array($ch,[CURLOPT_RETURNTRANSFER=>true,CURLOPT_POST=>true,CURLOPT_HTTPHEADER=>['Content-Type: application/x-www-form-urlencoded'],CURLOPT_POSTFIELDS=>$post,CURLOPT_CONNECTTIMEOUT=>10,CURLOPT_TIMEOUT=>20]);
    $resp=curl_exec($ch); $http=curl_getinfo($ch,CURLINFO_HTTP_CODE); $err=curl_error($ch); curl_close($ch);
    if($resp===false || $http!==200) return [null,"Token HTTP $http: $err | $resp"];
    $json=json_decode($resp,true); $token=$json['access_token']??null;
    if(!$token) return [null,"No access_token in response"];
    return [$token,null];
}
function sendFCM_v1_patient($accessToken,$projectId,$fcmToken,$title,$body,$androidChannelId=null,$data=[]) {
    $url="https://fcm.googleapis.com/v1/projects/$projectId/messages:send";
    $payload=['message'=>[
        'token'=>$fcmToken,
        'notification'=>['title'=>$title,'body'=>$body],
        'android'=>array_filter([
            'priority'=>'HIGH',
            'notification'=>array_filter([
                'sound'=>'default',
                'channel_id'=>$androidChannelId
            ])
        ]),
        'data'=>array_map('strval',$data)
    ]];
    $ch=curl_init($url);
    curl_setopt_array($ch,[CURLOPT_POST=>true,CURLOPT_HTTPHEADER=>["Authorization: Bearer $accessToken","Content-Type: application/json"],CURLOPT_RETURNTRANSFER=>true,CURLOPT_POSTFIELDS=>json_encode($payload),CURLOPT_CONNECTTIMEOUT=>10,CURLOPT_TIMEOUT=>20]);
    $response=curl_exec($ch); $httpCode=curl_getinfo($ch,CURLINFO_HTTP_CODE); $curlErr=curl_error($ch); curl_close($ch);
    return [$httpCode,$response,$curlErr];
}
function fetch_patient_tokens($conn,$patient_id){
    $tokens=[]; $sql="SELECT DISTINCT fcm_token FROM patient_fcm_tokens WHERE patient_id = ? AND fcm_token IS NOT NULL AND fcm_token <> ''";
    $st=$conn->prepare($sql); if(!$st) return $tokens; $st->bind_param("i",$patient_id); if($st->execute()){ $res=$st->get_result(); while($r=$res->fetch_assoc()){ $t=trim((string)$r['fcm_token']); if($t!=='') $tokens[]=$t; } } $st->close();
    return array_values(array_unique($tokens));
}
function build_patient_message($status,$appointment_mode,$eta_value,$eta_unit){
    $s=(string)$status; $mode=$appointment_mode?ucfirst(strtolower((string)$appointment_mode)):null;
    switch($s){
        case 'Confirmed': $title="Appointment Confirmed"; $body=$mode?"Your $mode appointment is confirmed.":"Your appointment is confirmed."; if($eta_value>0 && $eta_unit){ $body.=" ETA: {$eta_value} {$eta_unit}."; } break;
        case 'Cancelled_by_doctor': $title="Appointment Cancelled"; $body="Your appointment was cancelled by the doctor. Refund will be processed if applicable."; break;
        case 'Completed': $title="Appointment Completed"; $body="Your appointment has been marked as completed. Thank you for using The Doctor At Home."; break;
        case 'Pending': default: $title="Appointment Updated"; $body="Your appointment status changed to: {$s}."; break;
    }
    return [$title,$body];
}

if ($resp["success"] === true && $patient_id > 0) {
    list($title,$body) = build_patient_message($new_status, $appointment_mode, $eta_value, $eta_unit);
    logx('FCM MESSAGE', ['title'=>$title,'body'=>$body]);

    $tokens = fetch_patient_tokens($conn, $patient_id);
    logx('PATIENT TOKENS', ['patient_id'=>$patient_id,'count'=>count($tokens)]);
    $resp["debug"]["fcm"]["tokens_found"] = count($tokens);

    if (!empty($tokens)) {
        if ($FIREBASE_JSON_PATH === null) {
            $resp["debug"]["fcm"]["error"] = "access_token: key file not readable in any known path";
            logx('FCM ACCESS TOKEN ERROR', $resp["debug"]["fcm"]["error"]);
        } else {
            list($accessToken, $tokErr) = getAccessToken_patient($FIREBASE_JSON_PATH);
            if (!$accessToken) {
                $resp["debug"]["fcm"]["error"] = "access_token: " . $tokErr;
                logx('FCM ACCESS TOKEN ERROR', $tokErr);
            } else {
                $resp["debug"]["fcm"]["attempted"] = true;
                $sent=0; $fail=0; $errs=[];

                $dataPayload = ['type'=>'appointment_status','appointment_id'=>(string)$appointment_id,'status'=>(string)$new_status];

                foreach ($tokens as $tkn) {
                    logx('FCM SEND TRY', ['token_len'=>strlen($tkn)]);
                    list($code,$raw,$curlErr) = sendFCM_v1_patient($accessToken,$FIREBASE_PROJECT_ID,$tkn,$title,$body,$ANDROID_CHANNEL_ID,$dataPayload);
                    $snippet = is_string($raw) ? substr($raw,0,400) : '';
                    if ((int)$code === 200 && strpos((string)$raw,'"name":') !== false) {
                        $sent++; logx('FCM SEND OK', ['http'=>$code,'resp_snippet'=>$snippet]);
                    } else {
                        $fail++; $errs[]=['http'=>$code,'curl'=>$curlErr ?: null,'resp_snippet'=>$snippet ?: null];
                        logx('FCM SEND FAIL', end($errs));
                    }
                }
                $resp["debug"]["fcm"]["sent"]=$sent; $resp["debug"]["fcm"]["fail"]=$fail;
                if(!empty($errs)) $resp["debug"]["fcm"]["errors"]=array_slice($errs,0,5);
                logx('FCM SUMMARY', ['sent'=>$sent,'fail'=>$fail]);
            }
        }
    } else {
        $resp["debug"]["fcm"]["note"] = "No patient tokens found";
        logx('FCM SKIP: NO TOKENS');
    }
}

$resp['debug']['log_file'] = basename($LOG_FILE);
logx('==== REQUEST END ====');
echo json_encode($resp);
