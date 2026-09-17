<?php
header('Content-Type: application/json');
include_once ("../db_connection.php");

// Check if patient_id is provided via GET parameter.
if (!isset($_GET['patient_id'])) {
    echo json_encode([
        "success" => false,
        "error"   => "No patient_id provided."
    ]);
    exit;
}

$patient_id = intval($_GET['patient_id']);

// Prepare the SQL statement to safely query the database.
$stmt = $conn->prepare("SELECT patient_id, user_id, full_name, date_of_birth, gender, blood_group, address, mobile, email, emergency_contact_name, emergency_contact_number, medical_history, allergies, current_medications, profile_picture, created_at FROM patients WHERE patient_id = ?");
$stmt->bind_param("i", $patient_id);
$stmt->execute();

$result = $stmt->get_result();

if ($result->num_rows > 0) {
    $patients = [];
    while ($row = $result->fetch_assoc()) {
        $patients[] = $row;
    }
    echo json_encode([
        "success"  => true,
        "patients" => $patients
    ]);
} else {
    echo json_encode([
        "success" => false,
        "error"   => "No patient found with the given patient_id."
    ]);
}

// Clean up.
$stmt->close();
$conn->close();
?>
