<?php
// get_slider_images.php
header("Content-Type: application/json; charset=UTF-8");
$servername = "localhost";
$username = "e9h6dq9bf9bj";
$password = "hF5k2Jn@JNUK";
$database = "doctorathome";

// Create connection
$conn = new mysqli($servername, $username, $password, $database);

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}
// use your existing db config

$response = [];
$sql = "SELECT image_url FROM slider_images ORDER BY id DESC";

$result = $conn->query($sql);
if ($result && $result->num_rows > 0) {
    $images = [];
    while ($row = $result->fetch_assoc()) {
        $images[] = $row['image_url'];
    }
    $response = [
        "success" => true,
        "images" => $images
    ];
} else {
    $response = [
        "success" => false,
        "message" => "No images found"
    ];
}

echo json_encode($response);
$conn->close();
