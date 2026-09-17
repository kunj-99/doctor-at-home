<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Methods: GET, POST');

header('Content-Type: application/json; charset=UTF-8');
error_reporting(E_ALL);
ini_set('display_errors', 1);           // ✅ Show errors on screen
ini_set('log_errors', 1);
ini_set('error_log', __DIR__ . '/error_log.txt');
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

$status = "Requested";
$sql = "SELECT * FROM appointments WHERE doctor_id = ? AND status = ?";

if ($stmt = $conn->prepare($sql)) {
    $stmt->bind_param("is", $doctor_id, $status);

    if ($stmt->execute()) {
        $result = $stmt->get_result();
        $appointments = [];

        while ($row = $result->fetch_assoc()) {
            $appointment_id = $row['appointment_id'];

            // Get total_payment and payment_method from payment_history
            $paymentQuery = "SELECT amount, payment_method FROM payment_history WHERE appointment_id = ?";
            $paymentStmt = $conn->prepare($paymentQuery);
            $paymentStmt->bind_param("i", $appointment_id);
            $paymentStmt->execute();
            $paymentResult = $paymentStmt->get_result();
            $paymentRow = $paymentResult->fetch_assoc();

            $amount = $paymentRow['amount'] ?? "0.00";
            $payment_method = $paymentRow['payment_method'] ?? "Unknown";
            $paymentStmt->close();

            // --- Vet case logic ---
            $is_vet_case = isset($row['is_vet_case']) ? intval($row['is_vet_case']) : 0;
            $animal_category_id = isset($row['animal_category_id']) ? intval($row['animal_category_id']) : null;
            $animal_category_name = null;

            if ($is_vet_case == 1 && !empty($animal_category_id)) {
                // Animal Category Name from animal_categories
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
                'reason_for_visit'      => $row['reason_for_visit'],
                'patient_map_link'      => $row['patient_map_link'],
                'amount'                => $amount,
                'payment_method'        => $payment_method,
                'is_vet_case'           => $is_vet_case,
                'animal_category_id'    => $animal_category_id,
                'animal_category_name'  => $animal_category_name,
                'animal_breed'          => $row['animal_breed'],
                'vaccination_id'        => $row['vaccination_id'] ?? null,
                'vaccination_name'      => $row['vaccination_name'] ?? null
                // vaccination_due_date REMOVED as per instruction
            ];
        }

        echo json_encode([
            'success' => true,
            'data' => $appointments
        ]);
    } else {
        echo json_encode([
            'success' => false,
            'message' => 'Failed to execute query.'
        ]);
    }

    $stmt->close();
} else {
    echo json_encode([
        'success' => false,
        'message' => 'Failed to prepare SQL statement.'
    ]);
}
?>
