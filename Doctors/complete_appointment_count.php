<?php
// completed_appointment.php

include_once '../db_connection.php'; // Include your database connection file

header("Content-Type: application/json"); // Set response header

// Retrieve doctor_id from POST request and trim whitespace
$doctor_id = isset($_POST['doctor_id']) ? trim($_POST['doctor_id']) : '';

if (empty($doctor_id)) {
    echo json_encode(["success" => false, "message" => "Doctor ID is required"]);
    exit();
}

try {
    // Prepare the SQL statement to count completed appointments for the given doctor_id
    $stmt = $conn->prepare("SELECT COUNT(*) AS completed_count FROM appointments WHERE doctor_id = ? AND status = 'Completed'");
    if (!$stmt) {
        echo json_encode(["success" => false, "message" => "Database prepare error: " . $conn->error]);
        exit();
    }
    
    // Bind the doctor_id as an integer parameter
    $stmt->bind_param("i", $doctor_id);
    $stmt->execute();
    
    // Get the result and fetch the count
    $result = $stmt->get_result();
    if ($result) {
        $row = $result->fetch_assoc();
        $completed_count = $row['completed_count'];
        echo json_encode(["success" => true, "completed_count" => $completed_count]);
    } else {
        echo json_encode(["success" => false, "message" => "No result returned"]);
    }
    
    $stmt->close();
} catch (Exception $e) {
    echo json_encode(["success" => false, "message" => "Exception: " . $e->getMessage()]);
}

$conn->close();
?>
