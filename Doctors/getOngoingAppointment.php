<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Methods: GET, POST');

include_once("../db_connection.php");

// Fetch doctor_id from GET or POST
$doctor_id = isset($_REQUEST['doctor_id']) ? $_REQUEST['doctor_id'] : null;

if (empty($doctor_id)) {
    echo json_encode([
        'success' => false,
        'message' => 'Doctor ID is missing.'
    ]);
    exit;
}

if (!is_numeric($doctor_id)) {
    echo json_encode([
        'success' => false,
        'message' => 'Invalid doctor ID.'
    ]);
    exit;
}

$sql = "SELECT * FROM appointments WHERE doctor_id = ? AND status = 'Confirmed' ORDER BY appointment_date DESC";
$stmt = $conn->prepare($sql);

if (!$stmt) {
    echo json_encode([
        'success' => false,
        'message' => 'Prepare failed: ' . $conn->error
    ]);
    exit;
}

$stmt->bind_param("i", $doctor_id);

if (!$stmt->execute()) {
    echo json_encode([
        'success' => false,
        'message' => 'Execution failed: ' . $stmt->error
    ]);
    exit;
}

$result = $stmt->get_result();
$appointments = [];

while ($row = $result->fetch_assoc()) {
    $appointment_id = $row['appointment_id'];

    // Fetch payment amount and method from payment_history
    $paymentQuery = "SELECT amount, payment_method FROM payment_history WHERE appointment_id = ?";
    $paymentStmt  = $conn->prepare($paymentQuery);

    if ($paymentStmt) {
        $paymentStmt->bind_param("i", $appointment_id);
        $paymentStmt->execute();
        $paymentResult = $paymentStmt->get_result();
        $paymentRow = $paymentResult ? $paymentResult->fetch_assoc() : null;
        $paymentStmt->close();
    } else {
        $paymentRow = null;
    }

    $amount = $paymentRow['amount'] ?? "0.00";
    $payment_method = $paymentRow['payment_method'] ?? "Unknown";

    $is_vet_case = isset($row['is_vet_case']) ? (int)$row['is_vet_case'] : 0;
    $animal_category_id = isset($row['animal_category_id']) ? intval($row['animal_category_id']) : null;
    $animal_category_name = null;

    // Only fetch animal_category_name if vet case and id present
    if ($is_vet_case == 1 && !empty($animal_category_id)) {
        $catStmt = $conn->prepare("SELECT category_name FROM animal_categories WHERE category_id = ?");
        $catStmt->bind_param("i", $animal_category_id);
        $catStmt->execute();
        $catResult = $catStmt->get_result();
        $catRow = $catResult->fetch_assoc();
        if ($catRow) $animal_category_name = $catRow['category_name'];
        $catStmt->close();
    }

    $appointments[] = [
        'appointment_id'        => $row['appointment_id'],
        'patient_name'          => $row['patient_name'],
        'appointment_date'      => $row['appointment_date'],
        'time_slot'             => $row['time_slot'],
        'reason_for_visit'      => $row['reason_for_visit'],
        'appointment_mode'      => $row['appointment_mode'],
        'patient_map_link'      => $row['patient_map_link'],
        'status'                => $row['status'],
        'created_at'            => $row['created_at'],
        'updated_at'            => $row['updated_at'],
        'has_report'            => (int)$row['has_report'],
        'amount'                => $amount,
        'payment_method'        => $payment_method,
        'is_vet_case'           => $is_vet_case,
        'animal_category_id'    => $animal_category_id,
        'animal_category_name'  => $animal_category_name,
        'animal_breed'          => $row['animal_breed'],
        'vaccination_id'        => $row['vaccination_id'] ?? null,
        'vaccination_name'      => $row['vaccination_name'] ?? null
    ];
}

if (count($appointments) > 0) {
    echo json_encode([
        'success' => true,
        'appointments' => $appointments
    ]);
} else {
    echo json_encode([
        'success' => false,
        'message' => 'No confirmed appointments found for this doctor.'
    ]);
}

$stmt->close();
$conn->close();
?>
