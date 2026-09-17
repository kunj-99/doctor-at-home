<?php
/**
 * Doctors/gethistory.php
 * Returns completed/cancelled history for a doctor, Java-compatible.
 * Fields returned per row:
 *  - appointment_id (string/int)
 *  - patient_id     (string/int)
 *  - patient_name   (string)
 *  - appointment_date (string)
 *  - time_slot        (string)
 *  - reason_for_visit (string)
 *  - status           (lowercase: completed / cancelled / cancelled_by_doctor)
 *  - flag             (boolean) -> true if payment completed, else false
 *
 * Optional GET params:
 *   doctor_id (required), limit (int), offset (int)
 */

header('Content-Type: application/json; charset=UTF-8');
require_once __DIR__ . '/../db_connection.php';

try {
    if (!isset($_REQUEST['doctor_id']) || !is_numeric($_REQUEST['doctor_id'])) {
        echo json_encode(['error' => 'Doctor ID not provided or invalid.']);
        exit;
    }

    $doctorId = (int) $_REQUEST['doctor_id'];

    // Optional pagination (safe defaults)
    $limit  = isset($_GET['limit'])  && is_numeric($_GET['limit'])  ? max(1, min(500, (int)$_GET['limit']))  : 200;
    $offset = isset($_GET['offset']) && is_numeric($_GET['offset']) ? max(0, (int)$_GET['offset'])           : 0;

    /**
     * PERFORMANCE NOTES:
     * 1) Ensure indexes:
     *    - appointments: INDEX(doctor_id, status, appointment_date), INDEX(appointment_id), INDEX(patient_id)
     *    - payment_history: INDEX(appointment_id), INDEX(doctor_id), INDEX(payment_status)
     *
     * 2) The payment flag is derived from payment_history rows for the same appointment:
     *    true  if ANY row has payment_status = 'Completed'
     *    false otherwise
     *
     * 3) If patient_name actually resides in a `patients` table, enable the join below.
     */

    // Base query: Only select the columns Android needs; compute payment flag.
    // Normalizes status to lowercase for consistent client checks.
    $sql = "
        SELECT 
            a.appointment_id,
            a.patient_id,
            /* If patient_name is in appointments: */
            a.patient_name,
            /* If patient_name is in patients table, comment line above and uncomment below:
            p.name AS patient_name,
            */

            a.appointment_date,
            COALESCE(a.time_slot, '')        AS time_slot,
            COALESCE(a.reason_for_visit, '') AS reason_for_visit,

            /* Normalize status to lowercase */
            LOWER(
                CASE 
                    WHEN a.status = 'Cancelled_by_Doctor' THEN 'cancelled_by_doctor'
                    WHEN a.status = 'Cancelled'          THEN 'cancelled'
                    WHEN a.status = 'Completed'          THEN 'completed'
                    ELSE LOWER(a.status)
                END
            ) AS status,

            /* payment flag: true if any payment_history row shows 'Completed' for this appointment */
            CASE 
                WHEN EXISTS (
                    SELECT 1 
                    FROM payment_history ph 
                    WHERE ph.appointment_id = a.appointment_id 
                      AND ph.payment_status = 'Completed'
                    LIMIT 1
                ) THEN 1
                ELSE 0
            END AS flag_int

        FROM appointments a
        /* If you keep patient_name in patients table, enable this:
        LEFT JOIN patients p ON p.patient_id = a.patient_id
        */

        WHERE a.doctor_id = ?
          AND a.status IN ('Completed','Cancelled','Cancelled_by_Doctor')
        ORDER BY a.appointment_date DESC, a.time_slot DESC
        LIMIT ? OFFSET ?
    ";

    if (!$stmt = $conn->prepare($sql)) {
        echo json_encode(['error' => 'Prepare failed: ' . $conn->error]);
        exit;
    }

    $stmt->bind_param('iii', $doctorId, $limit, $offset);
    $stmt->execute();
    $res = $stmt->get_result();

    $out = [];
    while ($row = $res->fetch_assoc()) {
        // Cast/format output exactly as the Android expects
        $out[] = [
            'appointment_id'    => (string)$row['appointment_id'],
            'patient_id'        => (string)$row['patient_id'],
            'patient_name'      => isset($row['patient_name']) ? (string)$row['patient_name'] : '',
            'appointment_date'  => (string)$row['appointment_date'],
            'time_slot'         => (string)$row['time_slot'],
            'reason_for_visit'  => (string)$row['reason_for_visit'],
            'status'            => (string)$row['status'],                    // already normalized to lowercase
            'flag'              => $row['flag_int'] == 1 ? true : false       // boolean for Android's isPaymentReceived()
        ];
    }
    $stmt->close();

    echo json_encode($out, JSON_UNESCAPED_UNICODE);

} catch (Throwable $e) {
    // Avoid leaking internal details; return generic error
    echo json_encode(['error' => 'Server error.']);
}
