<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

include_once("../db_connection.php");

// Handle JSON input (Volley often sends requests as form data, but JSON is safer)
$data = json_decode(file_get_contents('php://input'), true);

// Check if POST data or JSON input was received
$doctor_id = $_POST['doctor_id'] ?? $data['doctor_id'] ?? '';

if (!empty($doctor_id)) {
    $doctor_id = mysqli_real_escape_string($conn, $doctor_id);

    $sql = "SELECT * FROM doctors WHERE doctor_id='$doctor_id'";
    $result = mysqli_query($conn, $sql);

    if ($result && mysqli_num_rows($result) > 0) {
        $doctorData = mysqli_fetch_assoc($result);

        // Define your base URL for the uploads directory
        $base_url = "https://thedoctorathome.in/doctor_images/";

        // If a profile picture exists, build a full URL
        if (isset($doctorData['profile_picture']) && !empty($doctorData['profile_picture'])) {
            $doctorData['profile_picture'] = $doctorData['profile_picture'];
        }

        echo json_encode($doctorData);
    } else {
        echo json_encode(["error" => "Doctor not found"]);
    }
} else {
    echo json_encode(["error" => "doctor_id not provided"]);
}
?>
