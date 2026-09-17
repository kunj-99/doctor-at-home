<?php
// verify_doctor_otp.php

include_once '../db_connection.php'; // Include database connection

header("Content-Type: application/json"); // Set JSON response header

// Ensure database connection is established
if (!$conn) {
    echo json_encode(["success" => false, "message" => "Database connection failed"]);
    http_response_code(500);
    exit();
}

// Get and sanitize input
$mobile = isset($_POST['mobile']) ? trim($_POST['mobile']) : '';
$entered_otp = isset($_POST['otp']) ? trim($_POST['otp']) : '';

if (empty($mobile) || empty($entered_otp)) {
    error_log("Mobile number or OTP missing");
    echo json_encode(["success" => false, "message" => "Mobile number and OTP are required"]);
    http_response_code(400);
    exit();
}

try {
    // Fetch doctor record and stored OTP
    $stmt = $conn->prepare("SELECT doctor_id, full_name, specialization, otp FROM doctors WHERE TRIM(mobile) = ?");
    if (!$stmt) {
        echo json_encode(["success" => false, "message" => "Database prepare error: " . $conn->error]);
        http_response_code(500);
        exit();
    }
    $stmt->bind_param("s", $mobile);
    $stmt->execute();
    $stmt->store_result();

    if ($stmt->num_rows === 0) {
        error_log("Mobile number not found: " . $mobile);
        echo json_encode(["success" => false, "message" => "Mobile number not found"]);
        http_response_code(404);
        $stmt->close();
        exit();
    }

    $stmt->bind_result($doctor_id, $full_name, $specialization, $stored_otp);
    $stmt->fetch();
    $stmt->close();

    if ($entered_otp == $stored_otp) {
        // Clear OTP after successful verification
        $clearStmt = $conn->prepare("UPDATE doctors SET otp = NULL WHERE TRIM(mobile) = ?");
        if (!$clearStmt) {
            echo json_encode(["success" => false, "message" => "Database prepare error: " . $conn->error]);
            http_response_code(500);
            exit();
        }
        $clearStmt->bind_param("s", $mobile);
        if (!$clearStmt->execute()) {
            error_log("Failed to clear OTP for mobile {$mobile}: " . $clearStmt->error);
        }
        $clearStmt->close();

        // Return the doctor details upon successful verification
        echo json_encode([
            "success" => true,
            "message" => "OTP Verified Successfully",
            "doctor_id" => $doctor_id,
            "full_name" => $full_name,
            "specialization" => $specialization
        ]);
        http_response_code(200);
    } else {
        error_log("Invalid OTP for mobile " . $mobile);
        echo json_encode(["success" => false, "message" => "Invalid OTP"]);
        http_response_code(401);
    }
} catch (Exception $e) {
    error_log("Exception: " . $e->getMessage());
    echo json_encode(["success" => false, "message" => "Server Error: " . $e->getMessage()]);
    http_response_code(500);
}

$conn->close();
?>
