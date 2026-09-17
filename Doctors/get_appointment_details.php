<?php
// File: Doctors/get_appointment_details.php
// Purpose: Return appointment details for both HUMAN and VET cases.
// Input : appointment_id (GET or POST)
// Output: JSON { success, is_vet_case, full_name, age, sex, address, date, doctor_name, species_breed }

header('Content-Type: application/json; charset=UTF-8');
header('Access-Control-Allow-Methods: GET, POST');

require_once("../db_connection.php");

// ---- Read input appointment_id (supports GET/POST via $_REQUEST) ----
$appointment_id = isset($_REQUEST['appointment_id']) ? trim($_REQUEST['appointment_id']) : null;

if (empty($appointment_id)) {
    echo json_encode(['success' => false, 'message' => 'appointment_id is required']);
    exit;
}
if (!ctype_digit($appointment_id)) {
    echo json_encode(['success' => false, 'message' => 'appointment_id must be numeric']);
    exit;
}

try {
    // 1) Fetch the appointment row (STRICT by id)
    $sql = "SELECT * FROM appointments WHERE appointment_id = ? LIMIT 1";
    if (!$stmt = $conn->prepare($sql)) {
        echo json_encode(['success' => false, 'message' => 'Prepare failed: ' . $conn->error]);
        exit;
    }
    $aid = (int)$appointment_id;
    $stmt->bind_param("i", $aid);

    if (!$stmt->execute()) {
        echo json_encode(['success' => false, 'message' => 'Execution failed: ' . $stmt->error]);
        exit;
    }

    $res = $stmt->get_result();
    if ($res->num_rows === 0) {
        echo json_encode(['success' => false, 'message' => 'Appointment not found']);
        exit;
    }

    $row = $res->fetch_assoc();
    $stmt->close();

    // 2) Determine if this is a vet case (fallback to 0 if not set)
    $is_vet_case = 0;
    if (isset($row['is_vet_case'])) {
        $is_vet_case = (int)$row['is_vet_case'];
    }

    // 3) Common fields — choose the best available column safely

    // Appointment date → prefer appointment_date, fallback created_at
    $date = '';
    if (!empty($row['appointment_date'])) {
        $date = (string)$row['appointment_date'];
    } elseif (!empty($row['created_at'])) {
        $date = (string)$row['created_at'];
    }

    // Doctor full name (LOOKUP):
    // Read doctors.full_name and continue sending it as 'doctor_name' to keep Android unchanged.
    $doctor_name = '';
    if (isset($row['doctor_id']) && ctype_digit((string)$row['doctor_id'])) {
        $docSql = "SELECT full_name FROM doctors WHERE doctor_id = ? LIMIT 1";
        if ($docStmt = $conn->prepare($docSql)) {
            $docId = (int)$row['doctor_id'];
            $docStmt->bind_param("i", $docId);
            if ($docStmt->execute()) {
                $docRes = $docStmt->get_result();
                if ($docRes && $docRes->num_rows > 0) {
                    $docRow = $docRes->fetch_assoc();
                    if (isset($docRow['full_name'])) {
                        $doctor_name = (string)$docRow['full_name'];
                    }
                }
            }
            $docStmt->close();
        }
    }

    // 4) Map fields differently for VET vs HUMAN (keys kept consistent for Android)
    if ($is_vet_case === 1) {
        // VET case: prefer animal/owner specific columns, keep keys same as Android expects.

        // Animal name (sent back in 'full_name' to prefill etAnimalName on the screen)
        $full_name = '';
        foreach (['animal_name', 'patient_name', 'full_name', 'name'] as $k) {
            if (isset($row[$k]) && $row[$k] !== null) { $full_name = (string)$row[$k]; break; }
        }

        // Age (animal age first, then generic)
        $age = '';
        foreach (['animal_age_years', 'animal_age', 'age', 'patient_age'] as $k) {
            if (isset($row[$k]) && $row[$k] !== null) { $age = (string)$row[$k]; break; }
        }

        // Sex
        $sex = '';
        foreach (['animal_sex', 'sex', 'patient_gender', 'gender'] as $k) {
            if (isset($row[$k]) && $row[$k] !== null) { $sex = (string)$row[$k]; break; }
        }

        // Address (owner → patient → generic)
        $address = '';
        foreach (['owner_address', 'patient_address', 'address'] as $k) {
            if (isset($row[$k]) && $row[$k] !== null) { $address = (string)$row[$k]; break; }
        }

        // Species/Breed helper (only for vet UI)
        // FIX: give priority to 'animal_breed' so breed shows up.
        $species_breed = '';
        foreach (['animal_breed', 'species_breed', 'breed', 'species'] as $k) {
            if (isset($row[$k]) && $row[$k] !== null && $row[$k] !== '') {
                $species_breed = (string)$row[$k];
                break;
            }
        }

        echo json_encode([
            'success'        => true,
            'is_vet_case'    => 1,
            'full_name'      => $full_name,     // used by Android as animal name field
            'age'            => $age,
            'sex'            => $sex,
            'address'        => $address,
            'date'           => $date,
            'doctor_name'    => $doctor_name,   // sent as 'doctor_name' (value from doctors.full_name)
            'species_breed'  => $species_breed  // now checks animal_breed first
        ]);
        exit;

    } else {
        // HUMAN case: prefer patient_* columns

        $full_name = '';
        foreach (['patient_name', 'full_name', 'name'] as $k) {
            if (isset($row[$k]) && $row[$k] !== null) { $full_name = (string)$row[$k]; break; }
        }

        $age = '';
        foreach (['patient_age', 'age'] as $k) {
            if (isset($row[$k]) && $row[$k] !== null) { $age = (string)$row[$k]; break; }
        }

        $sex = '';
        foreach (['patient_gender', 'gender', 'sex'] as $k) {
            if (isset($row[$k]) && $row[$k] !== null) { $sex = (string)$row[$k]; break; }
        }

        $address = '';
        foreach (['patient_address', 'address'] as $k) {
            if (isset($row[$k]) && $row[$k] !== null) { $address = (string)$row[$k]; break; }
        }

        echo json_encode([
            'success'        => true,
            'is_vet_case'    => 0,
            'full_name'      => $full_name,
            'age'            => $age,
            'sex'            => $sex,
            'address'        => $address,
            'date'           => $date,
            'doctor_name'    => $doctor_name,
            'species_breed'  => ''              // not applicable; keep key for consistency
        ]);
        exit;
    }

} catch (Throwable $e) {
    // Generic server error
    echo json_encode([
        'success' => false,
        'message' => 'Server error: ' . $e->getMessage()
    ]);
}

$conn->close();
