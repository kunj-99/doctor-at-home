<?php
header('Content-Type: application/json');
include_once ("../db_connection.php");

// Ensure the request method is POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode([
        'success' => false,
        'message' => 'Invalid request method.'
    ]);
    exit;
}

// Check for required POST parameters
if (!isset($_POST['doctor_id']) || !isset($_POST['status'])) {
    echo json_encode([
        'success' => false,
        'message' => 'Required parameters are missing.'
    ]);
    exit;
}

$doctor_id = $_POST['doctor_id'];
$status = $_POST['status'];

// Validate doctor_id and status
if (!is_numeric($doctor_id)) {
    echo json_encode([
        'success' => false,
        'message' => 'Invalid doctor ID.'
    ]);
    exit;
}

$allowedStatuses = ['active', 'inactive'];
if (!in_array($status, $allowedStatuses)) {
    echo json_encode([
        'success' => false,
        'message' => 'Invalid status value.'
    ]);
    exit;
}



// Prepare the SQL statement to update the doctor's status
// Assume your table is named "doctors" and it has columns "doctor_id" and "status"
$sql = "UPDATE doctors SET status = ? WHERE doctor_id = ?";
$stmt = $conn->prepare($sql);
if (!$stmt) {
    echo json_encode([
        'success' => false,
        'message' => 'Prepare failed: ' . $conn->error
    ]);
    exit;
}

$stmt->bind_param("si", $status, $doctor_id);

// Execute the update
if ($stmt->execute()) {
    // Check if any row was actually updated
    if ($stmt->affected_rows > 0) {
        echo json_encode([
            'success' => true,
            'message' => 'Status updated successfully.'
        ]);
    } else {
        echo json_encode([
            'success' => false,
            'message' => 'No doctor found with the given ID or status unchanged.'
        ]);
    }
} else {
    echo json_encode([
        'success' => false,
        'message' => 'Execution failed: ' . $stmt->error
    ]);
}

// Close connections
$stmt->close();
$conn->close();
?>
