-- ============================================================
-- TEMP_doctor_booking_lock.sql
-- Run this on your MySQL/MariaDB database ONCE to create
-- the temporary lock table for preventing double-booking.
-- ============================================================

CREATE TABLE IF NOT EXISTS doctor_booking_locks (
  lock_id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  doctor_id                 INT NOT NULL,
  patient_id                INT NOT NULL,
  reservation_token         VARCHAR(100) NOT NULL,
  is_vet_case               TINYINT(1) NOT NULL DEFAULT 0,
  status                    ENUM('HELD','CONFIRMED','EXPIRED','CANCELLED') NOT NULL DEFAULT 'HELD',
  expires_at                DATETIME NOT NULL,
  created_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  confirmed_appointment_id  INT DEFAULT NULL,
  PRIMARY KEY (lock_id),
  UNIQUE KEY uq_reservation_token (reservation_token),
  KEY idx_doctor_status_expiry (doctor_id, status, expires_at),
  KEY idx_patient_status (patient_id, status),
  KEY idx_confirmed_appointment (confirmed_appointment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Verify table was created
SHOW TABLES LIKE 'doctor_booking_locks';
DESCRIBE doctor_booking_locks;
