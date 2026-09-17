<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

include_once("../db_connection.php");

$data = json_decode(file_get_contents('php://input'), true);

if (isset($data['doctor_id']) && !empty($data['doctor_id'])) {

    $doctor_id = mysqli_real_escape_string($conn, $data['doctor_id']);
    $full_name = mysqli_real_escape_string($conn, $data['full_name']);
    $specialization = mysqli_real_escape_string($conn, $data['specialization']);
    $qualification = mysqli_real_escape_string($conn, $data['qualification']);
    $experience_years = mysqli_real_escape_string($conn, $data['experience_years']);
    $license_number = mysqli_real_escape_string($conn, $data['license_number']);
    $hospital_affiliation = mysqli_real_escape_string($conn, $data['hospital_affiliation']);
    $availability_schedule = mysqli_real_escape_string($conn, $data['availability_schedule']);

    // Initialize variable for image name
    $image_name = "";

    // Check if a profile picture has been provided and is not empty
    if (isset($data['profile_picture']) && !empty($data['profile_picture'])) {
        $profile_picture_data = $data['profile_picture'];

        // Remove the data URL prefix if it exists (e.g., "data:image/jpeg;base64,")
        if (strpos($profile_picture_data, 'data:image') === 0) {
            $parts = explode(',', $profile_picture_data);
            if(count($parts) > 1) {
                $profile_picture_data = $parts[1];
            }
        }

        // Decode the image
        $decoded_image = base64_decode($profile_picture_data);

        if ($decoded_image === false) {
            echo json_encode(["error" => "Invalid image data"]);
            exit;
        }

        // Generate a unique image name
        $image_name = "doctor_" . $doctor_id . "_" . time() . ".jpg";
        $upload_path = "../doctor_images/" . $image_name;

        // Save the image file to the uploads directory
        if (file_put_contents($upload_path, $decoded_image) === false) {
            echo json_encode(["error" => "Failed to upload image"]);
            exit;
        }
    }

    // Build the SQL update query. If a new image was uploaded, include the profile_picture column.
    $sql = "UPDATE doctors SET
            full_name='$full_name',
            specialization='$specialization',
            qualification='$qualification',
            experience_years='$experience_years',
            license_number='$license_number',
            hospital_affiliation='$hospital_affiliation',
            availability_schedule='$availability_schedule'";

    if (!empty($image_name)) {
        $sql .= ", profile_picture='$image_name'";
    }

    $sql .= " WHERE doctor_id='$doctor_id'";

    if (mysqli_query($conn, $sql)) {
        echo json_encode(["success" => "Profile updated successfully"]);
    } else {
        echo json_encode(["error" => mysqli_error($conn)]);
    }

} else {
    echo json_encode(["error" => "Doctor ID not provided"]);
}
?>
