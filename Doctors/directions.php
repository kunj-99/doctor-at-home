<?php
// directions.php

// (Optional) Include your database connection if needed.
include_once("../db_connection.php");

// Set the response content type to JSON.
header("Content-Type: application/json");

// Check if the required GET parameters are provided.
if (!isset($_GET['origin']) || !isset($_GET['destination'])) {
    http_response_code(400);
    echo json_encode([
        "error" => "Missing required parameters: origin and destination."
    ]);
    exit;
}

// Retrieve and trim the parameters.
$origin = trim($_GET['origin']);
$destination = trim($_GET['destination']);
$mode = isset($_GET['mode']) ? trim($_GET['mode']) : 'driving';

// URL-encode the parameters.
$originEncoded = urlencode($origin);
$destinationEncoded = urlencode($destination);
$modeEncoded = urlencode($mode);

// Insert your actual Directions API key here.
$apiKey = "AIzaSyDgbzIYTb6ydLaVxoSAh8eu1JTfCcH-jDk";

// Build the full URL to call the Google Directions API.
$directionsUrl = "https://maps.googleapis.com/maps/api/directions/json?origin={$originEncoded}&destination={$destinationEncoded}&mode={$modeEncoded}&key={$apiKey}";

// Initialize a cURL session.
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $directionsUrl);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false); // In production, it's better to verify SSL

// Execute the cURL request.
$response = curl_exec($ch);

// If the request failed, return an error.
if ($response === false) {
    http_response_code(500);
    echo json_encode([
        "error" => "Failed to fetch directions: " . curl_error($ch)
    ]);
    curl_close($ch);
    exit;
}
curl_close($ch);

// Output the response received from Google.
echo $response;
?>
