<?php
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);


header('Content-Type: application/json');
include '../db_connection.php';
$response = [];

$appointment_id = isset($_GET['appointment_id']) ? (int) $_GET['appointment_id'] : 0;

$sql = "SELECT 
            ph.appointment_id,
            ph.patient_name,
            ph.amount,
            ph.consultation_fee,
            ph.deposit,
            ph.distance,
            ph.distance_charge,
            ph.payment_method,
            ph.payment_status,
            ph.refund_status,
            ph.notes,
            ph.gst,
            ph.total_payment,
            ph.created_at,
            ph.doctor_id,
            d.full_name AS doctor_name
        FROM payment_history ph
        LEFT JOIN doctors d ON ph.doctor_id = d.doctor_id
        WHERE ph.appointment_id = $appointment_id
        ORDER BY ph.created_at DESC";

$result = $conn->query($sql);

if ($result && $result->num_rows > 0) {
    $payments = [];

    while ($row = $result->fetch_assoc()) {
        $payments[] = [
            "appointment_id"    => $row["appointment_id"],
            "patient_name"      => $row["patient_name"],
            "doctor_name"       => $row["doctor_name"] ?? "Unknown",
            "amount"            => number_format($row["amount"], 2),
            "consultation_fee"  => number_format($row["consultation_fee"], 2),
            "deposit"           => number_format($row["deposit"], 2),
            "distance"          => number_format($row["distance"], 2) . " km",
            "distance_charge"   => number_format($row["distance_charge"], 2),
            "payment_method"    => $row["payment_method"],
            "payment_status"    => $row["payment_status"],
            "refund_status"     => $row["refund_status"] ?? "N/A",
            "notes"             => $row["notes"] ?? "",
            "gst"               => number_format($row["gst"], 2),
            "total_payment"     => number_format($row["total_payment"], 2),
            "created_at"        => $row["created_at"]
        ];
    }

    $response = [
        "success" => true,
        "data" => $payments
    ];
} else {
    $response = [
        "success" => false,
        "message" => "No payment history found for this appointment."
    ];
}

echo json_encode($response);
$conn->close();
?>
