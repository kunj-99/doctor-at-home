<?php
header('Content-Type: application/json');
require_once("../db_connection.php");

$doctor_id = null;

// Accept doctor_id via POST or GET for flexibility
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $doctor_id = isset($_POST['doctor_id']) ? trim($_POST['doctor_id']) : null;
} else if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $doctor_id = isset($_GET['doctor_id']) ? trim($_GET['doctor_id']) : null;
}

if (!$doctor_id || !ctype_digit($doctor_id)) {
    echo json_encode(['success' => false, 'message' => 'Invalid or missing doctor_id.']);
    exit;
}

$sql = "SELECT status FROM doctors WHERE doctor_id = ?";
$stmt = $conn->prepare($sql);
if (!$stmt) {
    echo json_encode(['success' => false, 'message' => 'Prepare failed: ' . $conn->error]);
    exit;
}

$stmt->bind_param("i", $doctor_id);
$stmt->execute();
$stmt->bind_result($status);

if ($stmt->fetch()) {
    echo json_encode([
        'success' => true,
        'status'  => $status // Will be 'Active', 'Inactive', or 'Ongoing Appointment'
    ]);
} else {
    echo json_encode(['success' => false, 'message' => 'Doctor not found.']);
}

$stmt->close();
$conn->close();
?>
