<?php
// ongoing_appointment_count.php
include_once("../db_connection.php");
// Enable error reporting (for development; disable in production)
error_reporting(E_ALL);
ini_set('display_errors', 1);

header("Content-Type: application/json; charset=UTF-8");

// Include your database connection file


// Retrieve doctor_id from POST
$doctor_id = isset($_POST['doctor_id']) ? trim($_POST['doctor_id']) : '';

if (empty($doctor_id)) {
    echo json_encode(["success" => false, "message" => "Doctor ID is required"]);
    exit();
}

// Prepare the SQL query to count ongoing appointments for the doctor.
// "Ongoing" appointments are those with status Requested, Pending, or Confirmed.
$sql = "SELECT COUNT(*) AS count FROM appointments 
        WHERE doctor_id = ? AND status IN ('Requested', 'Pending', 'Confirmed')";

$stmt = $conn->prepare($sql);
if (!$stmt) {
    echo json_encode(["success" => false, "message" => "Database prepare error: " . $conn->error]);
    exit();
}

$stmt->bind_param("i", $doctor_id);
$stmt->execute();
$result = $stmt->get_result();

if ($result) {
    $row = $result->fetch_assoc();
    $count = $row['count'];
    echo json_encode(["success" => true, "ongoing_count" => $count]);
    $result->free();
} else {
    echo json_encode(["success" => false, "message" => "No result returned"]);
}

$stmt->close();
$conn->close();
?>
