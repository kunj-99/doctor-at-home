-- phpMyAdmin SQL Dump
-- version 5.2.2
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1:3306
-- Generation Time: Apr 29, 2026 at 03:35 PM
-- Server version: 11.8.6-MariaDB-log
-- PHP Version: 7.2.34

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `u357694546_doctorathome`
--

-- --------------------------------------------------------

--
-- Table structure for table `admins`
--

CREATE TABLE `admins` (
  `id` int(11) NOT NULL,
  `first_name` varchar(100) DEFAULT NULL,
  `last_name` varchar(100) DEFAULT NULL,
  `dob` date DEFAULT NULL,
  `email` varchar(150) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `country` varchar(100) DEFAULT NULL,
  `city` varchar(100) DEFAULT NULL,
  `postal_code` varchar(20) DEFAULT NULL,
  `role` varchar(50) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `profile_image` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- Dumping data for table `admins`
--

INSERT INTO `admins` (`id`, `first_name`, `last_name`, `dob`, `email`, `phone`, `country`, `city`, `postal_code`, `role`, `password`, `created_at`, `profile_image`) VALUES
(2, 'Vansh', 'Mandanka', '2333-11-22', 'infowave.ml@gmail.com', '6354355617', 'India', 'gondal', '364470', 'Admin', '$2y$10$m0T1mlFQPd8t14bmIdDGrOOGOfceqcjt5PIuR.CX.kQDIXr.wKfdy', '2025-05-11 11:20:09', '1746962409_ChatGPT_Image_Apr_10__2025__04_32_23_AM.png'),
(4, 'Dr.Hridaynath', 'Lad', '1964-06-13', 'thedoctorathome2025@gmail.com', '+91 96652 63377', 'INDIA', 'PUNE', '411011', 'Admin', '$2y$10$fsU/0gUglWRQ5z.m0WfJUuo4uvsgp3JBBfHX3Ohy3dWy/qk9aXViC', '2025-07-02 18:42:14', 'admin_4_1777380762.jpg'),
(5, 'Harshil', 'Bhudiya', '2006-08-23', 'bhudiyaharshil6@gmail.com', '9313442240', 'India', 'Mankuva', '370030', 'Admin', '$2y$10$fsU/0gUglWRQ5z.m0WfJUuo4uvsgp3JBBfHX3Ohy3dWy/qk9aXViC', '2025-07-03 05:29:08', 'admin_5_1772523452.jpg');

-- --------------------------------------------------------

--
-- Table structure for table `admin_doctor_payment_summary`
--

CREATE TABLE `admin_doctor_payment_summary` (
  `summary_id` int(11) NOT NULL,
  `doctor_id` int(11) NOT NULL,
  `last_payment_id` text DEFAULT NULL,
  `total_appointments` int(11) DEFAULT 0,
  `online_appointments` int(11) DEFAULT 0,
  `offline_appointments` int(11) DEFAULT 0,
  `total_earning` decimal(10,2) DEFAULT 0.00,
  `total_gst` decimal(10,2) NOT NULL DEFAULT 0.00,
  `admin_collected_total` decimal(10,2) NOT NULL DEFAULT 0.00,
  `doctor_collected_total` decimal(10,2) NOT NULL DEFAULT 0.00,
  `admin_cut` decimal(10,2) DEFAULT 0.00,
  `doctor_cut` decimal(10,2) DEFAULT 0.00,
  `adjustment_amount` decimal(10,2) DEFAULT 0.00,
  `settlement_status` enum('Pending','Settled','Partially Settled') DEFAULT 'Pending',
  `notes` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `given_to_doctor` decimal(10,2) DEFAULT 0.00,
  `received_from_doctor` decimal(10,2) DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

-- --------------------------------------------------------

--
-- Table structure for table `admin_transactions`
--

CREATE TABLE `admin_transactions` (
  `transaction_id` int(11) NOT NULL,
  `reference_id` int(11) DEFAULT NULL,
  `settlement_summary_id` int(11) DEFAULT NULL,
  `doctor_id` int(11) DEFAULT NULL,
  `transaction_type` enum('Income','Expense') NOT NULL,
  `method` enum('Bank','Wallet','Cash') DEFAULT 'Bank',
  `amount` decimal(10,2) NOT NULL,
  `paid_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `description` varchar(255) DEFAULT NULL,
  `payment_proof` varchar(255) DEFAULT NULL,
  `payment_proofs` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- Triggers `admin_transactions`
--
DELIMITER $$
CREATE TRIGGER `bi_admin_transactions_sync` BEFORE INSERT ON `admin_transactions` FOR EACH ROW BEGIN
  IF NEW.settlement_summary_id IS NULL AND NEW.reference_id IS NOT NULL THEN
    SET NEW.settlement_summary_id = NEW.reference_id;
  END IF;

  IF NEW.reference_id IS NULL AND NEW.settlement_summary_id IS NOT NULL THEN
    SET NEW.reference_id = NEW.settlement_summary_id;
  END IF;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `bu_admin_transactions_sync` BEFORE UPDATE ON `admin_transactions` FOR EACH ROW BEGIN
  IF NEW.settlement_summary_id IS NULL AND NEW.reference_id IS NOT NULL THEN
    SET NEW.settlement_summary_id = NEW.reference_id;
  END IF;

  IF NEW.reference_id IS NULL AND NEW.settlement_summary_id IS NOT NULL THEN
    SET NEW.reference_id = NEW.settlement_summary_id;
  END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `animal_breeds`
--

CREATE TABLE `animal_breeds` (
  `breed_id` int(11) NOT NULL,
  `category_id` int(11) NOT NULL,
  `breed_name` varchar(120) NOT NULL,
  `slug` varchar(120) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `animal_categories`
--

CREATE TABLE `animal_categories` (
  `category_id` int(11) NOT NULL,
  `category_name` varchar(80) NOT NULL,
  `category_image` varchar(255) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

--
-- Dumping data for table `animal_categories`
--

INSERT INTO `animal_categories` (`category_id`, `category_name`, `category_image`, `is_active`, `created_at`, `updated_at`) VALUES
(19, 'Dog', 'Admin/uploads/categories/category_1770638589_6989ccfd18eef1.21796268.jpg', 1, '2026-02-09 12:03:09', '2026-02-09 12:03:09'),
(20, 'Cat', 'Admin/uploads/categories/category_1770639478_6989d0766cdef7.73046343.jpg', 1, '2026-02-09 12:17:58', '2026-02-09 12:17:58');

-- --------------------------------------------------------

--
-- Table structure for table `animal_medical_reports`
--

CREATE TABLE `animal_medical_reports` (
  `report_id` int(11) NOT NULL,
  `appointment_id` int(11) NOT NULL,
  `animal_name` varchar(120) NOT NULL,
  `species_breed` varchar(120) DEFAULT NULL,
  `sex` varchar(20) DEFAULT NULL,
  `age_years` decimal(5,2) DEFAULT NULL,
  `weight_kg` decimal(6,2) DEFAULT NULL,
  `owner_address` text DEFAULT NULL,
  `animal_id` int(11) DEFAULT NULL,
  `report_date` date NOT NULL,
  `report_title` varchar(150) NOT NULL,
  `report_type` varchar(100) DEFAULT NULL,
  `doctor_signature` varchar(120) DEFAULT NULL,
  `temperature_c` decimal(4,1) DEFAULT NULL,
  `pulse_bpm` int(11) DEFAULT NULL,
  `spo2_pct` int(11) DEFAULT NULL,
  `bp_mmhg` varchar(20) DEFAULT NULL,
  `respiratory_rate_bpm` int(11) DEFAULT NULL,
  `pain_score_0_10` tinyint(4) DEFAULT NULL,
  `hydration_status` varchar(50) DEFAULT NULL,
  `mucous_membranes` varchar(50) DEFAULT NULL,
  `crt_sec` decimal(4,2) DEFAULT NULL,
  `symptoms` text DEFAULT NULL,
  `behavior_gait` text DEFAULT NULL,
  `skin_coat` text DEFAULT NULL,
  `respiratory_system` text DEFAULT NULL,
  `reasons` text DEFAULT NULL,
  `requires_investigation` tinyint(1) NOT NULL DEFAULT 0,
  `investigation_notes` text DEFAULT NULL,
  `medications_json` longtext DEFAULT NULL,
  `dosage_json` longtext DEFAULT NULL,
  `vaccination_id` int(11) DEFAULT NULL,
  `vaccination_name` varchar(120) DEFAULT NULL,
  `vaccination_notes` text DEFAULT NULL,
  `attachment_url` varchar(255) DEFAULT NULL,
  `next_visit_date` date DEFAULT NULL,
  `is_followup` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

-- --------------------------------------------------------

--
-- Table structure for table `animal_vaccinations`
--

CREATE TABLE `animal_vaccinations` (
  `vaccination_id` int(11) NOT NULL,
  `category_id` int(11) NOT NULL,
  `vaccination_name` varchar(120) NOT NULL,
  `slug` varchar(120) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL DEFAULT 0.00,
  `currency` char(3) NOT NULL DEFAULT 'INR',
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `appointments`
--

CREATE TABLE `appointments` (
  `appointment_id` int(11) NOT NULL,
  `patient_id` int(11) NOT NULL,
  `animal_category_id` int(11) DEFAULT NULL,
  `animal_name` varchar(120) DEFAULT NULL,
  `animal_gender` enum('Male','Female','Unknown') DEFAULT NULL,
  `animal_age` int(3) DEFAULT NULL,
  `animal_breed` varchar(120) DEFAULT NULL,
  `vaccination_id` int(11) DEFAULT NULL,
  `vaccination_name` varchar(120) DEFAULT NULL,
  `is_vet_case` tinyint(1) DEFAULT 0,
  `patient_name` varchar(255) NOT NULL,
  `address` text NOT NULL,
  `gender` enum('Male','Female','Other') NOT NULL,
  `pincode` varchar(10) NOT NULL,
  `age` int(3) NOT NULL,
  `patient_map_link` text DEFAULT NULL,
  `doctor_id` int(11) NOT NULL,
  `appointment_date` date NOT NULL,
  `time_slot` varchar(50) NOT NULL,
  `status` enum('Requested','Pending','Confirmed','Completed','Cancelled','Cancelled_by_doctor') NOT NULL,
  `reason_for_visit` text NOT NULL,
  `appointment_mode` enum('Online','Offline') NOT NULL DEFAULT 'Offline',
  `payment_method` text NOT NULL DEFAULT 'Cash',
  `has_report` tinyint(1) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `live_location` text DEFAULT NULL,
  `live_latitude` decimal(10,7) DEFAULT NULL,
  `live_longitude` decimal(10,7) DEFAULT NULL,
  `eta_value` int(11) DEFAULT NULL,
  `eta_unit` enum('minutes','hours') DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

-- --------------------------------------------------------

--
-- Table structure for table `app_settings`
--

CREATE TABLE `app_settings` (
  `id` int(11) NOT NULL,
  `key_name` varchar(100) NOT NULL,
  `value` varchar(64) NOT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- Dumping data for table `app_settings`
--

INSERT INTO `app_settings` (`id`, `key_name`, `value`, `updated_at`) VALUES
(3, 'gst_percent', '2', '2025-11-17 18:06:31'),
(4, 'base_distance', '3', '2025-11-09 05:05:37'),
(5, 'extra_cost_per_km', '20', '2025-11-18 15:58:17'),
(6, 'platform_charge', '50', '2025-10-11 22:46:58'),
(8, 'admin_commission_pct', '20', '2025-11-03 01:39:11'),
(9, 'night_start_time', '20:00', '2025-11-13 17:13:00'),
(10, 'night_end_time', '08:00', '2025-11-13 17:13:00'),
(11, 'Qr_code', 'Qr_Code.jpeg', '2025-12-23 10:28:55');

-- --------------------------------------------------------

--
-- Table structure for table `Article`
--

CREATE TABLE `Article` (
  `id` int(11) NOT NULL,
  `title` text NOT NULL,
  `cover` varchar(255) NOT NULL,
  `pdf` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- Dumping data for table `Article`
--

INSERT INTO `Article` (`id`, `title`, `cover`, `pdf`) VALUES
(10, 'INFERTILITY', '', 'pdf_69a1abe0d1b000.08962772.pdf');

-- --------------------------------------------------------

--
-- Table structure for table `Contact`
--

CREATE TABLE `Contact` (
  `id` int(11) NOT NULL,
  `Whatsapp_number` text NOT NULL,
  `phone_number` text NOT NULL,
  `email` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- Dumping data for table `Contact`
--

INSERT INTO `Contact` (`id`, `Whatsapp_number`, `phone_number`, `email`) VALUES
(3, '9665263377', '9665263377', 'thedoctorathome2@gmail.com');

-- --------------------------------------------------------

--
-- Table structure for table `doctors`
--

CREATE TABLE `doctors` (
  `doctor_id` int(11) NOT NULL,
  `full_name` varchar(255) NOT NULL,
  `doctor_type` enum('specialist','general') DEFAULT NULL,
  `category_id` int(11) NOT NULL,
  `animal_category_id` varchar(255) DEFAULT NULL COMMENT 'Free-text/CSV; no FK',
  `qualification` varchar(255) NOT NULL,
  `experience_years` int(20) NOT NULL,
  `experience_duration` varchar(255) DEFAULT NULL,
  `specialization` varchar(255) NOT NULL,
  `doctor_location` text DEFAULT NULL,
  `consultation_fee` decimal(10,2) DEFAULT NULL,
  `availability_schedule` text NOT NULL,
  `hospital_affiliation` varchar(255) DEFAULT NULL,
  `profile_picture` varchar(255) DEFAULT NULL,
  `license_number` varchar(50) NOT NULL,
  `licence_photo` text NOT NULL,
  `status` enum('Active','Inactive','Ongoing Appointment') DEFAULT 'Active',
  `auto_status` enum('Active','Inactive','Ongoing Appointment') NOT NULL DEFAULT 'Active',
  `rating` decimal(3,2) NOT NULL DEFAULT 0.00,
  `pincodes` text NOT NULL,
  `mobile` varchar(255) DEFAULT NULL,
  `otp` int(10) DEFAULT NULL,
  `is_vet` tinyint(1) DEFAULT 0,
  `email` text NOT NULL,
  `upi_id` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- Dumping data for table `doctors`
--

INSERT INTO `doctors` (`doctor_id`, `full_name`, `doctor_type`, `category_id`, `animal_category_id`, `qualification`, `experience_years`, `experience_duration`, `specialization`, `doctor_location`, `consultation_fee`, `availability_schedule`, `hospital_affiliation`, `profile_picture`, `license_number`, `licence_photo`, `status`, `auto_status`, `rating`, `pincodes`, `mobile`, `otp`, `is_vet`, `email`, `upi_id`) VALUES
(15, 'Kunj Patel ', '', 1, NULL, 'None', 3, 'Months  ', 'Neurosurgeon   sds', 'https://www.google.com/maps/search/?api=1&query=23.1366245%2C72.5493086', 51.00, '10 - 2 PM', 'Central Clinic', 'https://thedoctorathome.in/doctor_images/doctor_15_profile.png', 'LIC654321', 'https://thedoctorathome.in/doctor_licenses/doctor_15_license.png', 'Active', 'Active', 0.00, '123456,360005,654785', '8849227088', NULL, 0, 'kunj@gmail.com', 'Kunj@upi');

-- --------------------------------------------------------

--
-- Table structure for table `doctor_animal_categories`
--

CREATE TABLE `doctor_animal_categories` (
  `id` int(11) NOT NULL,
  `doctor_id` int(11) NOT NULL,
  `animal_category_id` int(11) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `doctor_animal_categories`
--

INSERT INTO `doctor_animal_categories` (`id`, `doctor_id`, `animal_category_id`, `created_at`) VALUES
(1, 5, 1, '2025-11-11 12:15:21'),
(2, 5, 17, '2025-11-11 12:28:31');

--
-- Triggers `doctor_animal_categories`
--
DELIMITER $$
CREATE TRIGGER `trg_dac_ad_sync_doctors` AFTER DELETE ON `doctor_animal_categories` FOR EACH ROW BEGIN
  UPDATE doctors d
     SET d.animal_category_id =
         (SELECT MIN(animal_category_id)
            FROM doctor_animal_categories
           WHERE doctor_id = OLD.doctor_id)
   WHERE d.doctor_id = OLD.doctor_id;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_dac_ai_sync_doctors` AFTER INSERT ON `doctor_animal_categories` FOR EACH ROW BEGIN
  UPDATE doctors d
     SET d.animal_category_id =
         (SELECT MIN(animal_category_id)
            FROM doctor_animal_categories
           WHERE doctor_id = NEW.doctor_id)
   WHERE d.doctor_id = NEW.doctor_id;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_dac_au_sync_doctors` AFTER UPDATE ON `doctor_animal_categories` FOR EACH ROW BEGIN
  UPDATE doctors d
     SET d.animal_category_id =
         (SELECT MIN(animal_category_id)
            FROM doctor_animal_categories
           WHERE doctor_id = NEW.doctor_id)
   WHERE d.doctor_id = NEW.doctor_id;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `doctor_categories`
--

CREATE TABLE `doctor_categories` (
  `category_id` int(11) NOT NULL,
  `category_name` varchar(255) NOT NULL,
  `is_vet` tinyint(1) NOT NULL DEFAULT 0,
  `price` decimal(10,2) DEFAULT 0.00,
  `price_night` decimal(10,2) DEFAULT NULL,
  `disease` text DEFAULT NULL,
  `image` text DEFAULT NULL,
  `status` enum('Active','Inactive') DEFAULT 'Active',
  `doctor_type` enum('specialist','general') DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- Dumping data for table `doctor_categories`
--

INSERT INTO `doctor_categories` (`category_id`, `category_name`, `is_vet`, `price`, `price_night`, `disease`, `image`, `status`, `doctor_type`, `created_at`, `updated_at`) VALUES
(1, 'General Physician', 0, 51.00, 199.00, 'Fever, Cold & Cough, General Checkup, Body Pain, Weakness, Headache', 'https://thedoctorathome.in/Admin/category_images/download_1761858083_4039.jpg', 'Active', NULL, '2025-10-30 21:01:23', '2025-11-16 09:39:06'),
(2, 'vet general physcian', 1, 51.00, 99.00, 'cleaning', 'https://thedoctorathome.in/Admin/category_images/thrst_1762071176_3260.png', 'Active', 'general', '2025-11-02 08:12:56', '2025-11-14 10:59:30');

-- --------------------------------------------------------

--
-- Table structure for table `doctor_leads`
--

CREATE TABLE `doctor_leads` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `name` varchar(120) NOT NULL,
  `mobile` varchar(15) NOT NULL,
  `degree` varchar(120) NOT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `user_agent` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `doctor_leads`
--

INSERT INTO `doctor_leads` (`id`, `name`, `mobile`, `degree`, `ip_address`, `user_agent`, `created_at`) VALUES
(2, 'H Lad', '9822964454', 'BAMS MD', '2401:4900:78ea:4cde:d5b5:f188:e669:5f3e', 'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/29.0 Chrome/136.0.0.0 Mobile Safari/537.36', '2026-02-08 08:51:16'),
(4, 'Hanmant Dilip Khobare', '9011083413', 'B.E Mech', '2402:3a80:464a:17dd:d8e2:9dff:feae:5179', 'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36', '2026-02-09 11:13:25'),
(5, 'DR HRIDAYNATH LAD', '9822964454', 'BAMS MD', '2409:40c1:21:31f:f461:e648:a8d6:519d', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36 Edg/145.0.0.0', '2026-02-18 14:11:00'),
(6, 'Dr Md Golam Yasdani', '8101946905', 'MBBS', '42.105.194.103', 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.3 Mobile/15E148 Safari/604.1', '2026-03-03 10:41:02'),
(7, 'Charvi Nagar', '7620657782', 'MBBS', '2401:4900:1c44:48e9:db6:e147:e36b:4d29', 'Mozilla/5.0 (Linux; Android 14; CPH2381 Build/UKQ1.230924.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/146.0.7680.168 Mobile Safari/537.36 [FB_IAB/FB4A;FBAV/554.0.0.57.70;IABMV/1;]', '2026-04-04 02:33:58'),
(8, 'Dr Jigna DAMA', '8050026776', 'BAMS', '2401:4900:78eb:3085:107d:82ff:fe49:ef96', 'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Mobile Safari/537.36', '2026-04-14 14:03:54'),
(9, 'Dr Hemlata Bankar', '9552566246', 'BAMS', '42.104.226.113', 'Mozilla/5.0 (iPhone; CPU iPhone OS 26_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/23D8133 Safari/604.1 [FBAN/FBIOS;FBAV/555.0.0.36.63;FBBV/923840166;FBDV/iPhone14,7;FBMD/iPhone;FBSN/iOS;FBSV/26.3.1;FBSS/3;FBID/phone;FBLC/en_GB;FBOP/', '2026-04-16 06:30:20'),
(10, 'Dr Hemlata Bankar', '9552566246', 'BAMS', '42.104.226.113', 'Mozilla/5.0 (iPhone; CPU iPhone OS 26_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/23D8133 Safari/604.1 [FBAN/FBIOS;FBAV/555.0.0.36.63;FBBV/923840166;FBDV/iPhone14,7;FBMD/iPhone;FBSN/iOS;FBSV/26.3.1;FBSS/3;FBID/phone;FBLC/en_GB;FBOP/', '2026-04-16 06:30:21'),
(11, 'Kavita Dhavalikar', '9588402210', 'BHMS', '2409:40c2:10c:689a:8000::', 'Mozilla/5.0 (Linux; Android 16; CPH2695 Build/BP2A.250605.015; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/145.0.7632.104 Mobile Safari/537.36 [FB_IAB/FB4A;FBAV/549.0.0.61.62;IABMV/1;]', '2026-04-17 03:25:27'),
(12, 'Harshawardhan Mhatray', '9325431288', 'BAMS', '2409:40c2:1032:683d:8452:daff:fe78:d82d', 'Mozilla/5.0 (Linux; Android 15; V2355 Build/AP3A.240905.015.A2; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.86 Mobile Safari/537.36 [FB_IAB/FB4A;FBAV/556.1.0.63.64;IABMV/1;]', '2026-04-17 06:36:07'),
(13, 'Dr.Nilesh Bhati', '8446702070', 'BHMS, PGDEMS, CCGP', '152.58.32.240', 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_7_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.7 Mobile/15E148 Safari/604.1', '2026-04-17 10:39:01'),
(14, 'Saanvi Ambapkar', '9170135151', 'BAMS', '117.212.28.136', 'Mozilla/5.0 (iPhone; CPU iPhone OS 26_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/23D8133 Safari/604.1 [FBAN/FBIOS;FBAV/555.0.0.36.63;FBBV/923840166;FBDV/iPhone17,1;FBMD/iPhone;FBSN/iOS;FBSV/26.3.1;FBSS/3;FBID/phone;FBLC/en_GB;FBOP/', '2026-04-18 12:00:23'),
(15, 'Dr.Nilesh Bhati', '8446702070', 'BHMS, PGDEMS, CCGP', '2409:40c2:104b:eab1:99f2:15f:51c8:8660', 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_7_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.7 Mobile/15E148 Safari/604.1', '2026-04-19 10:17:39'),
(16, 'sunil Malusare', '9422003868', 'BAMS', '2409:40c2:4016:e135:c9b5:e6ca:926a:a943', 'Mozilla/5.0 (Linux; Android 16; SM-E156B Build/BP2A.250605.031.A3; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.86 Mobile Safari/537.36 [FB_IAB/FB4A;FBAV/556.1.0.63.64;IABMV/1;]', '2026-04-19 10:47:51');

-- --------------------------------------------------------

--
-- Table structure for table `doctor_tokens`
--

CREATE TABLE `doctor_tokens` (
  `token_id` int(11) NOT NULL,
  `doctor_id` int(11) NOT NULL,
  `device_id` varchar(128) NOT NULL,
  `platform` varchar(32) NOT NULL DEFAULT 'android',
  `model` varchar(128) DEFAULT NULL,
  `app_version` varchar(32) DEFAULT NULL,
  `fcm_token` varchar(1024) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `last_updated` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `doctor_tokens`
--

INSERT INTO `doctor_tokens` (`token_id`, `doctor_id`, `device_id`, `platform`, `model`, `app_version`, `fcm_token`, `created_at`, `last_updated`) VALUES
(315, 3, 'ewsD0FQVRrSAtD-YoSt99D', 'android', 'Xiaomi 2201117PI', '3.1', 'ewsD0FQVRrSAtD-YoSt99D:APA91bEmuOS89D5VQMRSwwCI92Hf1cfgkCsDP6LoX5yHBZCr_25P7JSQ0IZBXySysBFBuVKMLSVFST41wILxtB3fe75ST87oAsck-pMtxnp0mAr3b6MSMww', '2026-01-13 06:07:43', '2026-01-13 06:07:43'),
(316, 15, 'fydZkHVyREKm0TLHPAadyL', 'android', 'Xiaomi 2201117PI', '3.2', 'fydZkHVyREKm0TLHPAadyL:APA91bHm2gsCpNA1QtMc9YIpz4B3Z3PXFPN3bkodl15dee1qGDBJydhkHAnVEXWjHItu6QloZy570BsEMsT7j2QpAFgGBk9skVzmKGmlBgMON569ipPqvCk', '2026-01-13 16:04:14', '2026-02-28 07:01:07'),
(320, 3, 'dT7UFevaWiCP85Zp373ZY_', 'android', 'Google sdk_gphone64_x86_64', '3.1', 'dT7UFevaWiCP85Zp373ZY_:APA91bF4pAd0ZDuFRLvNIIOLgg8YZJIljEq0rRSdAFrFzjaoy3ZCZCCSCm1bBM8-x9Bp3RaEgr-fkGxPDGV6lPiGAcrN9x-f2IOu5N93bu09ea1Ju7DG7qc', '2026-02-21 06:07:18', '2026-02-21 06:07:18');

-- --------------------------------------------------------

--
-- Table structure for table `doctor_transactions`
--

CREATE TABLE `doctor_transactions` (
  `transaction_id` int(11) NOT NULL,
  `doctor_id` int(11) NOT NULL,
  `reference_id` int(11) DEFAULT NULL,
  `settlement_summary_id` int(11) DEFAULT NULL,
  `transaction_type` enum('Credit','Debit') NOT NULL,
  `method` enum('Cash','Bank Transfer','Wallet') DEFAULT 'Bank Transfer',
  `amount` decimal(10,2) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `payment_proof` varchar(255) DEFAULT NULL,
  `status` enum('Pending','Submitted','Approved','Rejected') NOT NULL DEFAULT 'Pending',
  `utr_number` varchar(80) DEFAULT NULL,
  `submitted_at` timestamp NULL DEFAULT NULL,
  `reviewed_at` timestamp NULL DEFAULT NULL,
  `admin_review_note` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ;

--
-- Triggers `doctor_transactions`
--
DELIMITER $$
CREATE TRIGGER `bi_doctor_transactions_sync` BEFORE INSERT ON `doctor_transactions` FOR EACH ROW BEGIN
  IF NEW.settlement_summary_id IS NULL AND NEW.reference_id IS NOT NULL THEN
    SET NEW.settlement_summary_id = NEW.reference_id;
  END IF;

  IF NEW.reference_id IS NULL AND NEW.settlement_summary_id IS NOT NULL THEN
    SET NEW.reference_id = NEW.settlement_summary_id;
  END IF;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `bu_doctor_transactions_sync` BEFORE UPDATE ON `doctor_transactions` FOR EACH ROW BEGIN
  IF NEW.settlement_summary_id IS NULL AND NEW.reference_id IS NOT NULL THEN
    SET NEW.settlement_summary_id = NEW.reference_id;
  END IF;

  IF NEW.reference_id IS NULL AND NEW.settlement_summary_id IS NOT NULL THEN
    SET NEW.reference_id = NEW.settlement_summary_id;
  END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `healthtip`
--

CREATE TABLE `healthtip` (
  `id` int(11) NOT NULL,
  `title` text NOT NULL,
  `description` text NOT NULL,
  `image` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

-- --------------------------------------------------------

--
-- Table structure for table `medical_reports`
--

CREATE TABLE `medical_reports` (
  `report_id` int(250) NOT NULL,
  `appointment_id` int(250) NOT NULL,
  `patient_name` varchar(255) NOT NULL,
  `age` int(3) NOT NULL,
  `sex` enum('Male','Female','Other') NOT NULL,
  `weight` decimal(5,2) DEFAULT NULL,
  `patient_address` varchar(255) NOT NULL,
  `visit_date` date NOT NULL,
  `temperature` varchar(10) DEFAULT NULL,
  `pulse` varchar(10) DEFAULT NULL,
  `spo2` varchar(10) DEFAULT NULL,
  `blood_pressure` varchar(20) DEFAULT NULL,
  `respiratory_system` varchar(50) DEFAULT NULL,
  `symptoms` text DEFAULT NULL,
  `medications` text DEFAULT NULL,
  `dosage` text NOT NULL,
  `doctor_name` text NOT NULL,
  `speciality` varchar(255) DEFAULT NULL,
  `registration_no` varchar(50) DEFAULT NULL,
  `doctor_signature` varchar(255) DEFAULT NULL,
  `investigations` text DEFAULT NULL,
  `caspar_photo` varchar(255) DEFAULT NULL,
  `report_type` varchar(50) DEFAULT NULL,
  `report_photo` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

-- --------------------------------------------------------

--
-- Table structure for table `patients`
--

CREATE TABLE `patients` (
  `patient_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `full_name` varchar(255) NOT NULL,
  `date_of_birth` date DEFAULT NULL,
  `gender` enum('Male','Female','Other') NOT NULL,
  `blood_group` enum('A+','A-','B+','B-','AB+','AB-','O+','O-') DEFAULT NULL,
  `address` text DEFAULT NULL,
  `mobile` varchar(20) NOT NULL,
  `email` varchar(255) NOT NULL,
  `emergency_contact_name` varchar(255) DEFAULT NULL,
  `emergency_contact_number` varchar(20) DEFAULT NULL,
  `medical_history` text DEFAULT NULL,
  `allergies` text DEFAULT NULL,
  `current_medications` text DEFAULT NULL,
  `profile_picture` varchar(255) DEFAULT NULL,
  `wallet_balance` decimal(10,2) DEFAULT 0.00,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- Dumping data for table `patients`
--

INSERT INTO `patients` (`patient_id`, `user_id`, `full_name`, `date_of_birth`, `gender`, `blood_group`, `address`, `mobile`, `email`, `emergency_contact_name`, `emergency_contact_number`, `medical_history`, `allergies`, `current_medications`, `profile_picture`, `wallet_balance`, `created_at`, `updated_at`) VALUES
(1, 1, 'Vansh Mandanka', '2007-07-11', 'Male', 'A+', 'balaji hotael taramba Rajkot ', '6354355617', 'mandankavansh@gmail.com', '9586397290', 'null', 'null', 'null', '', 'profile_1_1761857222.jpg', 1.00, '2025-10-30 20:42:10', '2026-01-05 06:39:28'),
(3, 3, 'harshil', '2006-08-23', 'Male', 'B+', 'Ahemdabad', '9313442240', 'bhudiyaharshil6@gmail.com', '', '', '', '', '', 'profile_3_1762115408.jpg', 200.00, '2025-11-02 17:14:40', '2026-03-03 07:07:51'),
(4, 4, 'kunj', '2006-07-22', 'Male', 'A+', '', '8849227088', 'kunjp925@gmail.com', '', '', '', '', '', 'profile_4_1762844967.jpg', 400.00, '2025-11-07 12:41:45', '2026-01-05 08:43:16'),
(5, 5, 'Punit', NULL, 'Male', NULL, '', '8849166463', 'punitpurohit73@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-11-09 07:08:59', '2025-11-09 07:08:59'),
(6, 6, 'Yash', NULL, 'Male', NULL, '', '9923906425', 'parwaniy6@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-11-09 07:15:28', '2025-11-09 07:15:28'),
(7, 7, 'HANMANT KHOBARE', '1988-10-22', 'Male', 'B+', 'Shikarpur pune', '9011083413', 'hanmant030609@gmail.com', 'MAYURI KHOBARE', '+916357469910', 'TREATMENT IS GOING ON', '', '', 'profile_7_1762677623.jpg', 1.00, '2025-11-09 08:31:46', '2025-11-09 08:40:24'),
(8, 8, 'fgff', NULL, 'Female', NULL, '', '9601311556', 'darshalimandanka@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-11-09 17:24:31', '2025-11-09 17:24:31'),
(9, 9, 'utkarsh', NULL, 'Male', NULL, '', '9503568552', 'theteutkarsh@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-11-14 05:53:19', '2025-11-14 05:53:19'),
(10, 10, 'devd', NULL, 'Male', NULL, '', '9890933074', 'devesh.lad@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-01 18:02:02', '2025-12-01 18:02:02'),
(11, 11, 'Mujeeb Shaikh', NULL, 'Male', NULL, '', '9764714141', 'mujeebshaikh333@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2025-12-26 11:54:42', '2025-12-26 11:54:42'),
(12, 12, 'darshit andrapiya', NULL, 'Male', NULL, '', '9979842096', 'darshitandrapiya@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-01-01 18:55:06', '2026-01-01 18:55:06'),
(13, 13, 'Argish', NULL, 'Male', NULL, '', '9724929081', 'argish@yopmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-01-12 09:35:42', '2026-01-12 09:35:42'),
(14, 14, 'raibhy jamdar', NULL, 'Male', NULL, '', '9881166654', 'dodrawingnow@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-01-13 04:56:38', '2026-01-13 04:56:38'),
(15, 15, 'Yaseen Sab', NULL, 'Male', NULL, '', '8197475979', 'yaseensab24@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-01-15 06:03:59', '2026-01-15 06:03:59'),
(16, 16, 'M.Talukdar', NULL, 'Female', NULL, '', '9073471318', 'mohuasneha08@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-01-16 04:31:24', '2026-01-16 04:31:24'),
(17, 17, 'Dr Hridaynath Lad', NULL, 'Male', NULL, '', '9822964454', 'support@thedoctorathome.in', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-01-17 06:35:24', '2026-01-17 06:35:24'),
(18, 18, 'mk', NULL, 'Male', NULL, '', '9983514585', 'mukeshchodhary1969@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-01-20 19:07:54', '2026-01-20 19:07:54'),
(19, 19, 'Haram Ali', NULL, 'Male', NULL, '', '8100014582', 'aliharam729@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-01-25 04:32:14', '2026-01-25 04:32:14'),
(20, 20, 'aryan reddy', NULL, 'Male', NULL, '', '8247401076', 'reddyarya.1437@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-01-30 14:00:53', '2026-01-30 14:00:53'),
(21, 21, 'Kajal Talmale', NULL, 'Female', NULL, '', '7507061672', 'kajalbawankar2016@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-02-01 18:43:59', '2026-02-01 18:43:59'),
(22, 22, 'yash mahajan', NULL, 'Male', NULL, '', '7982213747', 'yashmh1010@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-02-03 07:19:40', '2026-02-03 07:19:40'),
(23, 23, 'Sakshi Timkikar', NULL, 'Female', NULL, '', '9552122419', 'sakshitimkikar2607@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-02-17 09:49:37', '2026-02-17 09:49:37'),
(24, 24, 'jo', NULL, 'Female', NULL, '', '9840481187', 'joannasusan96@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-02-20 12:14:16', '2026-02-20 12:14:16'),
(25, 25, 'Shaikishaq', NULL, 'Male', NULL, '', '9036666243', 'shaikishaq6874@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-02-22 14:55:28', '2026-02-22 14:55:28'),
(26, 26, 'Nimesh', NULL, 'Male', NULL, '', '9316930412', 'nimeshvekariya0@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-02-24 07:07:38', '2026-02-24 07:07:38'),
(27, 29, 'vivek', NULL, 'Male', NULL, '', '8866200163', 'yoganandivivek@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-03-09 16:57:47', '2026-03-09 16:57:47'),
(28, 30, 'Rajiv Sehgal', NULL, 'Male', NULL, '', '9818445453', 'r_sehgal57@yahoo.co.in', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-03-11 14:09:10', '2026-03-11 14:09:10'),
(29, 31, 'Shreyas Anbhule', NULL, 'Male', NULL, '', '9504050909', 'SHREYAS.ANBHULE@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-03-13 12:32:19', '2026-03-13 12:32:19'),
(30, 32, 'hh', '0000-00-00', 'Male', 'A+', '', '', '', '', '', '', '', '', '', 0.00, '2026-03-14 02:10:52', '2026-03-14 02:12:33'),
(31, 33, 'Pooja kavithiya', NULL, 'Female', NULL, '', '8618714160', 'vishalkavithiya142@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-03-16 12:25:27', '2026-03-16 12:25:27'),
(32, 34, 'Hiten Gawade', NULL, 'Male', NULL, '', '9029217946', 'hitengawade218@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-03-18 06:18:19', '2026-03-18 06:18:19'),
(33, 35, 'madhu', NULL, 'Female', NULL, '', '7428667626', 'snbk7428@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-03-20 12:38:10', '2026-03-20 12:38:10'),
(34, 36, 'Akhil Agrawal', NULL, 'Male', NULL, '', '9999646066', 'akhilagrawal1982@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-04-11 15:20:04', '2026-04-11 15:20:04'),
(35, 38, 'Akhil Agrawal', NULL, 'Male', NULL, '', '8178305723', 'akhilagrawal2202@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-04-11 15:20:36', '2026-04-11 15:20:36'),
(36, 39, 'Brijesh Gami', NULL, 'Male', NULL, '', '9726306800', 'brijeshgami19@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-04-21 14:13:42', '2026-04-21 14:13:42'),
(37, 40, 'vinay kumar', NULL, 'Male', NULL, '', '9811009858', 'arush.kumar64@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-04-22 16:30:44', '2026-04-22 16:30:44'),
(38, 41, 'A Sahana', NULL, 'Male', NULL, '', '7845473676', 'asahana934@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, 0.00, '2026-04-29 09:22:20', '2026-04-29 09:22:20');

-- --------------------------------------------------------

--
-- Table structure for table `patient_fcm_tokens`
--

CREATE TABLE `patient_fcm_tokens` (
  `id` int(11) NOT NULL,
  `patient_id` int(11) NOT NULL,
  `device_id` varchar(128) NOT NULL,
  `platform` varchar(32) NOT NULL DEFAULT 'android',
  `model` varchar(128) DEFAULT NULL,
  `app_version` varchar(32) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `fcm_token` varchar(1024) NOT NULL,
  `last_updated` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `patient_fcm_tokens`
--

INSERT INTO `patient_fcm_tokens` (`id`, `patient_id`, `device_id`, `platform`, `model`, `app_version`, `created_at`, `fcm_token`, `last_updated`) VALUES
(276, 2, 'eo_x__yBQnCu8MjhPgEIUK', 'android', 'Google sdk_gphone64_x86_64', '5.2', '2025-11-07 12:18:37', 'eo_x__yBQnCu8MjhPgEIUK:APA91bGNukfVOuLFEVRngNgQmX67d6vMhuypPvNv5F4SFE1i137eebk7VmT7hODR_jiEEm_OV_ocDgiTrYxot8aFyKWwBVR3ftt84zleCwSvJjjdFjKQ0fo', '2025-11-07 12:18:37'),
(277, 4, 'eo_x__yBQnCu8MjhPgEIUK', 'android', 'Google sdk_gphone64_x86_64', '5.2', '2025-11-07 12:42:05', 'eo_x__yBQnCu8MjhPgEIUK:APA91bHGlss_FRRIrJHICzR5Qib5sTPdvdqszosbhxgSrYNKioW8EmVr6SjY35cPbx0SoW35OWxCwqcxp-LEtQ3XWb9UbWKwB6QiWpb1UY8dJfVyTXqaWLE', '2026-02-21 06:07:03'),
(278, 1, 'dMXenLFGQM63auThk3IB-D', 'android', 'motorola moto g84 5G', '5.2', '2025-11-08 11:55:13', 'dMXenLFGQM63auThk3IB-D:APA91bH55U0qTJek0jrqL9aa9I8UbFjrmqlZYcV8Z0CFQANyAP4F59f5_RTWZD4Dy9owLJZPqYt0yW-zVpIVGz8tn0FV2eBdV1wrhYXlVUfBt-EuYjPkxnQ', '2026-04-29 11:43:45'),
(279, 4, 'fcyO7eO5QHqNHSW6to3NnP', 'android', 'Xiaomi 2201117PI', '5.2', '2025-11-08 13:12:28', 'fcyO7eO5QHqNHSW6to3NnP:APA91bHFRsHjiMUy3_MPyMW3yfTY5PlqLmXaX7oJoLFnhN1HYc-TaVO-oT6dmSAt7E1PHAgkPfa8NGFG5ryx5gWLlzBGy1QIS40_TBUQEXvzwk8ds4sx7GM', '2026-01-06 11:19:50'),
(280, 3, 'cl_Fm4uqQAKbYe8hPuct2L', 'android', 'vivo V2415', '5.2', '2025-11-09 04:44:24', 'cl_Fm4uqQAKbYe8hPuct2L:APA91bH5nGHPGaSAbAFlo70XTCFxzr_FhbLNgYA8O7vIwI6-he9dRNKprGaWufx7UH8vr9acN0b9cjXR_Ev2V8JnFPbdlVE5dFZ17dYU1ZJyKygYEJEni-M', '2025-11-09 05:42:20'),
(282, 3, 'e3mBWxgUSJaxHFtffd4Stg', 'android', 'vivo V2415', '5.2', '2025-11-09 06:00:27', 'e3mBWxgUSJaxHFtffd4Stg:APA91bGfhPUbyb7sD8-f4YVeZgnhAbvf_jaC-xwrBer4DfcHNpsy9Ew8x0Z6xXf8oGGWybg_pSvWbgGDvd0Eq6vThTJFTFjFoD4LycQNslAcMHi4wb9mKvc', '2025-11-16 18:55:42'),
(283, 98, 'dymBKi5-R4OUklnL8I2DGl', 'android', 'OnePlus CPH2423', '5.2', '2025-11-09 07:06:34', 'dymBKi5-R4OUklnL8I2DGl:APA91bG_yImbE3aeAr7Vp4m2-kiw9ndSwCxkXuMSqf_jc-eMHnIPrqpD640jPwzhw82gldVZF9GUGIDZrD1SECojg5GuTxJYmq3qNllrsMHQbpg1-rtwEMs', '2025-11-13 06:46:30'),
(284, 5, 'ed9PY-fpSL-6jlVjDygbN3', 'android', 'motorola moto g64 5G', '5.2', '2025-11-09 07:09:17', 'ed9PY-fpSL-6jlVjDygbN3:APA91bFh6F0zzBEK8zgkAIAX2qGijnG3ygqmHe3zaU1-9-ooqsQVN78w-dhXTNRC4bD2e0vMwxnqERPTmuCNurDLxWNLrG8pkScczWW5CSISLgEWoshatnA', '2026-02-08 09:15:48'),
(285, 6, 'f0LzT2tjTbOPHI2wTkJp7P', 'android', 'samsung SM-S938B', '5.2', '2025-11-09 07:15:45', 'f0LzT2tjTbOPHI2wTkJp7P:APA91bFalJmNuMD38cb3iiye9SjQlrljTv_VEb0S6YrOOqDwOgJZyoLHnH39u9BRN4e8TzidxzimebpukbdJkIHneY4h9ZQvCldyW8T_rjXtvRCUsn51nEE', '2025-11-15 17:20:34'),
(286, 7, 'dZ5-7HgWSEywzA3mHInGyr', 'android', 'vivo V2303', '5.2', '2025-11-09 08:32:10', 'dZ5-7HgWSEywzA3mHInGyr:APA91bFGd6T5SmguK-b1pxuZ8OT7ldB4oWuOEjdGkaBg-I-Ng93gRxD6NauLs7P18lIWS6IrImDc8r3M50fTBrUN3ccVjzlrj9wzVLZwzGIr9Yk1nYWLWPU', '2026-02-28 15:51:55'),
(288, 9, 'ek3-R8ksSPGgD2PZ6m1Snz', 'android', 'motorola motorola edge 50 fusion', '5.2', '2025-11-14 05:53:42', 'ek3-R8ksSPGgD2PZ6m1Snz:APA91bHEuaIyqcOm_unqtmk3z2EfegsuOigGdGMmKn2DbYGtDP5wj_DKIn0zt2MYGylH4i0Gofur20K9DtwhMf0dNctlpvMLu1A3g9gzVeVr1xkPL9zaT3Q', '2025-11-15 15:55:59'),
(289, 1, 'cg-c3swmSDeA7RKhuFCpiH', 'android', 'motorola moto g84 5G', '5.2', '2025-11-14 12:53:23', 'cg-c3swmSDeA7RKhuFCpiH:APA91bE7wwLs7-YWW9PyEz44rkgzHwtR5hFUlIEi5dpxSgtzQGACPnLaH3cNXk5u70TKUiA0Ls-aH9f_Knwyyq3uz1_CxiH8ubGL4s_d4RVA93capnvxwRg', '2025-12-03 08:00:23'),
(290, 6, 'dpVMZdynQTy6LZ3eJgCd6A', 'android', 'samsung SM-S938B', '5.2', '2025-11-17 16:47:17', 'dpVMZdynQTy6LZ3eJgCd6A:APA91bHjuM0kYo_43-pzzj7ZFihhtiBmjl623d42XqIYGxmizTAS6xnqwp--Xh3pawZMagALY33zUgPxOz-KtMcw1xJ9O6bVxBgQ_MxQLV5-xT6GSOxKqZU', '2026-01-03 03:58:11'),
(291, 3, 'd0me28zJSPKo7komy46ePq', 'android', 'vivo V2415', '5.2', '2025-11-18 16:42:04', 'd0me28zJSPKo7komy46ePq:APA91bGyBr4tuG416269gRB1oDOhHV_07_fvyS26kmc9sR9SBcOituzYex3MPj-4M_p5lzQ2MrKxQRp_L7YgqhuSy3SUrmNn5DbqXoXMhUq8yzqaOP5Azac', '2025-11-28 15:48:58'),
(292, 3, 'c4ml0hUHThO9FjduzaPy9n', 'android', 'vivo V2415', '5.2', '2025-11-28 16:00:42', 'c4ml0hUHThO9FjduzaPy9n:APA91bFfpA-cQeAqV_6xV5nu67fyUHwO1PHNaDAAZBTPl4ycLXrHLvC-2D1XOLyfJZ03nG7TFpkZDzeyfWNCwkMqqIRmm9V12rJndxhU_uIgqXbAau_CEC8', '2026-01-07 12:46:25'),
(293, 10, 'eCy-PVviQ-as-2TGzOIws5', 'android', 'OnePlus CPH2381', '5.2', '2025-12-01 18:02:39', 'eCy-PVviQ-as-2TGzOIws5:APA91bHJuzVjb2t2NisqxlB2JIM3J68H6Cig6avwp1_ThrFbyuj5xd5TU8uZJTmA0jQ0rsKD8bfQOtIXjVyN5aWDCQLPKH6Swwgch49xMeffEi5f2qzi4Hk', '2025-12-01 18:09:17'),
(294, 3, 'di1KxrzTT_OeMhKvIHIS3P', 'android', 'vivo V2415', '5.2', '2026-01-09 04:50:04', 'di1KxrzTT_OeMhKvIHIS3P:APA91bE-c8F5xlTZfWP3zNjU0l4mBK8iQibG0ywttSCLMNa1VSIP0pGKvLnYNEnwjL0c1OQtv6m8TijWmPpUivPc_bW04gpPkC9kOmH6uVXpiLEVcVGRKBM', '2026-01-09 04:51:10'),
(296, 3, 'ctRGxdPuT2WX8hBwwzGCep', 'android', 'vivo V2415', '5.2', '2026-01-11 19:03:54', 'ctRGxdPuT2WX8hBwwzGCep:APA91bHEtt8u2xRarYiiqQnZmMGusnD8HbBeo8bRafBchWmo-NPiX3PJXnoR2RADaqyeC-Jhe1YaTp4TksK7MexdCrLf-kBmcqSB4-SaPZdJN7GNL_IpJog', '2026-01-12 18:47:12'),
(297, 13, 'fNVz26EzTQO3QXMHws9PVn', 'android', 'samsung SM-G770F', '5.2', '2026-01-12 09:36:07', 'fNVz26EzTQO3QXMHws9PVn:APA91bHWy6H8rCbPZs5LoFP8mqlg7upU5vKQO3Ey07PpgC2uKbdaSsbV7d89dVyjJcW8UNdaK1mpzX6ELKSmek1CMkXtvGs8PoGhQCZznmIG1TPfVl-tM_4', '2026-01-12 09:42:21'),
(298, 3, 'diFMU_mQQXmR2AmnSZIcKw', 'android', 'vivo V2415', '5.2', '2026-01-12 19:26:08', 'diFMU_mQQXmR2AmnSZIcKw:APA91bH5R363OosZqdVd5RaltGFBZB1gp069_XYxBYMSWe9ykl7EBypH44fL4yBeNGF4Nm3StNy4ScZboZOyf-xIUgPfRn3E2-1rGLeGHVm2qRSmNebBj7I', '2026-04-21 14:13:22'),
(299, 14, 'fmgAO70CRWeeI8PZfP41-w', 'android', 'vivo V2420', '5.2', '2026-01-13 04:57:36', 'fmgAO70CRWeeI8PZfP41-w:APA91bE0XzW3h7V2A24H-d2vXEkDIr5ZKXyxGS8D6OSTVDl8MKtaeftQXbNQ1lg95SwvVxZxJG_2I9qDfQyoNKKG-hTo4bB9HMX5OcEVxDmzNh1LW7BgRjQ', '2026-01-30 08:22:24'),
(300, 4, 'dSbRRE8zQ3aoguNshemc52', 'android', 'Xiaomi 2201117PI', '5.2', '2026-01-13 15:55:07', 'dSbRRE8zQ3aoguNshemc52:APA91bHTXe7rQKWv5fzTavia6vWawL1KCZ6r9krEDrLYMKcCj69fnEV9tqoRne3AOW6uDOOOJlIviIEGA8JANu4ijBnyG4ctjITTaUjFCTEyqlN1Uun3n1I', '2026-04-29 15:17:15'),
(302, 15, 'ctF129KQQpqQ7g6Fm856x5', 'android', 'vivo V2321', '5.2', '2026-01-15 06:04:23', 'ctF129KQQpqQ7g6Fm856x5:APA91bFci2XxmOiStasJYUK7ECoRGPuZrwlzroPyUGwcHt_RxHLw1IGd5hTRX3SwYEE_WVTV4iYoIMjB8Y_K2AHfRN9oBN6R6ClsKRpRsP1mUEEgfss1Mks', '2026-01-15 06:04:47'),
(303, 16, 'eb6VTRTUR1W9KerkupyEbn', 'android', 'OPPO CPH2455', '5.2', '2026-01-16 04:31:45', 'eb6VTRTUR1W9KerkupyEbn:APA91bHWep7hfKE_KMCUpb19xnDHfq4MgX9gWnszFRJUNaRcIwjFetAOJp0JNXtNW5kNHQU-a7WnW7Ns9aA_40pKT1_n9xSF1FkNid6x-nx2awiTmeBGgLc', '2026-01-16 04:31:57'),
(304, 18, 'dcBduXiUS3aDUfdh94P0fe', 'android', 'samsung SM-F966B', '5.2', '2026-01-20 19:08:17', 'dcBduXiUS3aDUfdh94P0fe:APA91bGQxaDD8BvMaMO0XEQYsuQdwGRh1BHhJHCmY2viUQOeDN7H4ksXIROriDsbhIafx4icPF2GU92s6SD-WVlVVxEcXpy_0ZygJGSz1eptLZj9IObPUZ4', '2026-01-20 19:10:16'),
(305, 19, 'f1v5u051R1yifJQSfxMAtQ', 'android', 'vivo V2355', '5.2', '2026-01-25 04:32:45', 'f1v5u051R1yifJQSfxMAtQ:APA91bHbTgs8RUnwMpdFpFoOuepiRaM0DkxBKH-Dh2-s1DdDAmliblC4XMLcDoZzb32AI8bKlaNGWvqegoWhWH9WvxfwY-Qr8QBVpkCI4WmH9tBaqAzn81w', '2026-01-25 04:33:17'),
(306, 11, 'fcTcidDwSqGwZQuBzC0lwY', 'android', 'vivo V2050', '5.2', '2026-01-29 11:40:36', 'fcTcidDwSqGwZQuBzC0lwY:APA91bFQa2Kb_1EUIO60p20ss8BB_nul5TYcyBVaglUrBJTs6Py_HAwQ_QuJlHyQNLnLOekqZ34Ugtk287S9FDJKqZDT6QBPZq7DgXDWaExHn7ZGYEwGSB0', '2026-01-29 12:21:05'),
(307, 17, 'e9S2NN6QRPqNxrDpzLX5q1', 'android', 'samsung SM-S928B', '5.2', '2026-01-29 11:41:12', 'e9S2NN6QRPqNxrDpzLX5q1:APA91bG17KY7dFSLdZ_4OO3CpkcgQ0MZOAp8KytlfI5jmLtBhMBmEEzeMYfNf--45XUgI_BNx_QMPF-O-Oa9usqoi5eIL_nAK1z5hLZWdzMWucBNy131sNo', '2026-02-08 09:17:42'),
(308, 20, 'dWb6HbzeRBqiWQ70xdFniA', 'android', 'vivo I2306', '5.2', '2026-01-30 14:01:15', 'dWb6HbzeRBqiWQ70xdFniA:APA91bHuyCXRYOQGof9bVSE3O1JLsY64lXn0kuKXewHew1yhE5mQHpQJKApj0BRdLPKNTLafXcRl44qTaIP5NI8Mf0mjcgYTBK_r405brgNA1Dqd_g_-8iY', '2026-01-30 14:01:32'),
(309, 21, 'fhqw85LKQ_eN2PDgEb5eQE', 'android', 'OPPO CPH2251', '5.2', '2026-02-01 18:44:28', 'fhqw85LKQ_eN2PDgEb5eQE:APA91bHSjm65xJZyPzRH6_WtkfkGF53_WeqlE7N22rRX4Nk7DOWfUh4hdZemy82GE4I8OeJEZZTAYWmslafBi19UkhjGb19xj0vMZdKju7Yq39UfH68XCz0', '2026-02-01 18:45:01'),
(310, 5, 'dbShXyuPT_GywhZpZ5NYTm', 'android', 'motorola moto g64 5G', '5.2', '2026-02-08 09:16:39', 'dbShXyuPT_GywhZpZ5NYTm:APA91bGvHtwtZCjp2QZd2er6JoQcc3MlpYbvAI5ku7dcZJcX4uZAGjxBQ5CJ7tYsLb1bofa7RXBjbylgb9YqJwqCb7Gvp-sG59DNfLAgKWfJfGFYBj1MyM8', '2026-02-08 09:21:00'),
(312, 17, 'frxMk3lZSIa7QGbolNy5V0', 'android', 'samsung SM-S928B', '5.2', '2026-02-08 09:18:22', 'frxMk3lZSIa7QGbolNy5V0:APA91bFzalr8fNzNdjN9vOdZNXdZLp8AD1SATSDB43-5MA6f9rMjlc5IaxojbCEmIxcSb_9WDjppp1Laqp1Nq43C4f9r29VIamxwk_oOxD8cm8DS1MB2F7I', '2026-02-08 09:18:40'),
(313, 105, 'dKjTRl1CR1ql9k0wGGyoYd', 'android', 'vivo V2141', '5.2', '2026-02-09 05:40:30', 'dKjTRl1CR1ql9k0wGGyoYd:APA91bF9AMs_AtdHjhRZMRPyEnJcfxcL2BvOZK5vmOoHU5ZvkcuWZqiM0bRhBNACPA94xrFclXW3PVnnLtG0vKRsXE0fY5RSyNahWWVb_StvJhNFjU_1dP4', '2026-04-28 04:19:54'),
(314, 23, 'dhMlyiPxT0-2c9ChAT1WAW', 'android', 'realme RMX2156', '5.2', '2026-02-17 09:50:03', 'dhMlyiPxT0-2c9ChAT1WAW:APA91bGYjNDdaAZPSENibinhdFU-vmoQ_YaF778sfmpQxscLdvPsO_5mOH3mpkIp7Dott8bFEWIJP_6XVWpdOYl1JnDqBE2iE3QJHh7kkh_kZATGsfoKUYw', '2026-02-17 09:50:34'),
(315, 24, 'e7QkiGigQO6GZ1_o3v8h3h', 'android', 'OPPO CPH2705', '5.2', '2026-02-20 12:14:53', 'e7QkiGigQO6GZ1_o3v8h3h:APA91bH3BJp4ttvSeUOc3_3LYPzrrGXuTO8Oy3Tp2i4IZdamiOxSmhonLhmrFQIgFsjpiMtijb4w1rcurwD7FmJujjBpn2O2Zw2kD1RKnoqUFWWu99c8luw', '2026-02-20 12:15:36'),
(316, 25, 'dSpHTW2lTOGL2Oo9lnORLu', 'android', 'OnePlus CPH2467', '5.2', '2026-02-22 14:55:58', 'dSpHTW2lTOGL2Oo9lnORLu:APA91bFg6XE1DuqBVwvqeHwT2mpzhfGsd3uUV75rJ1rtaTyUFmUV5zv5X_7jmn97sP0VdQslrjtPiwmxotGJv7r6uKVEwlgEt-zs0f2PkPXMCLrkleJT_yA', '2026-02-22 15:47:00'),
(317, 12, 'edRjDE87RF2gRB9gjzAO6w', 'android', 'vivo V2246', '5.2', '2026-03-05 20:18:00', 'edRjDE87RF2gRB9gjzAO6w:APA91bFtFenH6CpAozweUenffYvSvbcRXe1l_T-lWe7nLMl15rFZQ5tXf6S98fyGPVjsIVJvi57DPeZPNgODAAxJhmgS1Pj4dkpcbpmdievIa1YMQCiv65c', '2026-03-05 20:19:08'),
(318, 27, 'fSo_1YdmTmCuVqLnRT_psI', 'android', 'samsung SM-S911B', '5.2', '2026-03-09 16:58:06', 'fSo_1YdmTmCuVqLnRT_psI:APA91bG1D6VSk6lqC1AaWKX5d6OKT8duajD7dSVr1POjYMBubSzK2wQGaCebKy6UkFxXutyqpaQ2Itl7dxAsegRBEF_yd7VxM5h1WAI4rBGdJhWjYPORNZI', '2026-03-09 16:59:04'),
(319, 28, 'fhy7yeqkQZ2WQ_En_MJ7_u', 'android', 'samsung SM-A156M', '5.2', '2026-03-11 14:09:36', 'fhy7yeqkQZ2WQ_En_MJ7_u:APA91bGaRdAtfhHSOCxmu6KD_KGJO2e5HuQoS0n4zuc95bHS6F8NR8RcQ1nk3BkoloztDFqJsMZmim466fw0UoOekwwGzACMttZ6Wp8_WDc6XAX05Yxzl64', '2026-03-11 14:10:10'),
(320, 29, 'dwgb9fEcRFe9kdb4qGk_NJ', 'android', 'OnePlus CPH2585', '5.2', '2026-03-13 12:32:54', 'dwgb9fEcRFe9kdb4qGk_NJ:APA91bGgzEJzWiSqALEM1o4cTqT7FNKo4ozmBxqTs5Dyvid9a-HlIyaQCeXaGltEHux4BbG05OKrNLGDvspW8CRouVVKhJHhqY_Thh6J-fgwmR-yxgNCvaA', '2026-03-15 08:45:42'),
(321, 30, 'c8r5-OSrRw2D09L6X7H7-B', 'android', 'samsung SM-A146B', '5.2', '2026-03-14 02:11:27', 'c8r5-OSrRw2D09L6X7H7-B:APA91bEIdM7RGVByK2UqxtO6HGIfKTT6ECj49p0iaN28ZRbAgoGPRV9gTVthejhipWUMmcsnp17_w2_RTB8bMMAln0jgii4QZwXFsKduv8PlIcNMvMWD4Yk', '2026-03-14 02:12:54'),
(322, 103, 'cHspKES4RIup24KeJBF5WL', 'android', 'vivo I2202', '5.2', '2026-03-17 14:46:46', 'cHspKES4RIup24KeJBF5WL:APA91bHFJRlLgtqtrAlDqTIV7VP2UO6ODx67d827LQsrgOhITmDe83iCxzlwiEPLGUk1suD4dr3Tm3DhQqfuvuagae6sMDs-es9VX86IvO9_Q4ggivyfpyk', '2026-03-17 14:48:10'),
(323, 32, 'eYMxPbruS6yQE9CNWd9NRd', 'android', 'motorola motorola edge 40 neo', '5.2', '2026-03-18 06:18:46', 'eYMxPbruS6yQE9CNWd9NRd:APA91bGH4MYe-76vOOjC1p22sd65u62LigLUrTkjvIvXCBzx2DYF0wN0oKagxHDLvHJ7MldBIiR8QpxI3jjdDY8z-k79STLb0wE6MNM-iv_OAXC_vwbNnk8', '2026-03-18 06:19:29'),
(324, 33, 'dOF1H6aqSmWBAWJ5B00s4Y', 'android', 'motorola motorola edge 50 pro', '5.2', '2026-03-20 12:38:37', 'dOF1H6aqSmWBAWJ5B00s4Y:APA91bHXZ6quaBEHgzjBAYX8BCihZPc33LaX2PRHRjfhaNbql6pPytmMTiKxPYekR-J0lbe1PT4vY-9oGT8spTqdjyLIJvq4Y16seHFsfhXRAOub0iHKsdM', '2026-03-20 12:38:51'),
(325, 35, 'cro-63rzQjO8WsobNXucqB', 'android', 'OnePlus CPH2585', '5.2', '2026-04-11 15:21:01', 'cro-63rzQjO8WsobNXucqB:APA91bG0LCc8zuyYa7xO1wZh69upLml_WWpQfS5Bg398znBeij3CyXGab0EXrhqobFA7luq-bOXKRUQr3zCD2qqn2nC6oE-gYknZw3U-iIazRwhGP16zqhA', '2026-04-15 07:39:45'),
(326, 36, 'fq1bKYttTPenXo_ldFz5Et', 'android', 'OPPO CPH2637', '5.2', '2026-04-21 14:14:24', 'fq1bKYttTPenXo_ldFz5Et:APA91bHt6Jv-KKol4fk1sPrWG4M6BMK8iCAeNWiMPMBQlUHHd1xr26he2d2dH5Or3Zg40N2CzOkIJIE_3qV4AKWWFjKF3v59d2inHLSekqRAbxZdWtNkqZ8', '2026-04-22 05:36:34'),
(327, 37, 'ePGnw2ieRdawLR11zE8V27', 'android', 'TECNO TECNO KM8', '5.2', '2026-04-22 16:31:10', 'ePGnw2ieRdawLR11zE8V27:APA91bFi-IH0tPfW5YPB7CddznPZn7YPtIOfzOrNafyeZghshQc9wUZwGxUVdP7a2YONXm2KVb12byilbZxBhBqIs8G1Lltw7Buc-jHKQuJwLgqGAJm7tsc', '2026-04-22 16:31:37');

-- --------------------------------------------------------

--
-- Table structure for table `payment_history`
--

CREATE TABLE `payment_history` (
  `payment_id` int(11) NOT NULL,
  `patient_id` int(11) NOT NULL,
  `appointment_id` int(11) NOT NULL,
  `doctor_id` int(11) NOT NULL,
  `patient_name` varchar(255) NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `consultation_fee` decimal(10,2) DEFAULT 0.00,
  `deposit` decimal(10,2) DEFAULT 0.00,
  `deposit_status` enum('Wallet Debited','Added in Bill','Pending','Refunded') DEFAULT 'Pending',
  `payment_method` enum('Online','Offline') NOT NULL,
  `distance` decimal(10,2) DEFAULT 0.00,
  `distance_charge` decimal(10,2) DEFAULT 0.00,
  `gst` decimal(10,2) DEFAULT 0.00,
  `total_payment` decimal(10,2) DEFAULT 0.00,
  `admin_commission` decimal(10,2) DEFAULT 0.00,
  `doctor_earning` decimal(10,2) DEFAULT 0.00,
  `payment_status` enum('Pending','Completed','Failed','Refunded') DEFAULT 'Pending',
  `refund_status` enum('None','Requested','Processing','Completed') DEFAULT 'None',
  `notes` text DEFAULT NULL,
  `upi_id` text DEFAULT NULL,
  `payment_reference` varchar(299) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

-- --------------------------------------------------------

--
-- Table structure for table `pending_doctors`
--

CREATE TABLE `pending_doctors` (
  `pending_id` int(11) NOT NULL,
  `full_name` varchar(100) DEFAULT NULL,
  `doctor_type` enum('Human','Vet') NOT NULL DEFAULT 'Human',
  `category_id` int(11) DEFAULT NULL,
  `qualification` varchar(100) DEFAULT NULL,
  `experience_years` int(11) DEFAULT NULL,
  `experience_duration` varchar(20) DEFAULT NULL,
  `specialization` varchar(100) DEFAULT NULL,
  `doctor_location` varchar(255) DEFAULT NULL,
  `consultation_fee` float DEFAULT NULL,
  `availability_schedule` text DEFAULT NULL,
  `hospital_affiliation` varchar(100) DEFAULT NULL,
  `profile_picture` varchar(255) DEFAULT NULL,
  `license_number` varchar(100) DEFAULT NULL,
  `licence_photo` varchar(255) DEFAULT NULL,
  `pincodes` text DEFAULT NULL,
  `mobile` varchar(20) DEFAULT NULL,
  `email` text NOT NULL,
  `submitted_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `status` enum('Pending','Accepted','Rejected') DEFAULT 'Pending',
  `upi_id` text NOT NULL,
  `reply` text DEFAULT NULL,
  `reply_date` datetime DEFAULT NULL,
  `animal_category_ids` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- Dumping data for table `pending_doctors`
--

INSERT INTO `pending_doctors` (`pending_id`, `full_name`, `doctor_type`, `category_id`, `qualification`, `experience_years`, `experience_duration`, `specialization`, `doctor_location`, `consultation_fee`, `availability_schedule`, `hospital_affiliation`, `profile_picture`, `license_number`, `licence_photo`, `pincodes`, `mobile`, `email`, `submitted_at`, `status`, `upi_id`, `reply`, `reply_date`, `animal_category_ids`) VALUES
(16, 'Kunj Patel', 'Human', 1, 'None', 3, 'Months', 'Neurosurgeon', 'https://www.google.com/maps/search/?api=1&query=23.1366245%2C72.5493086', 51, '10 - 2 PM', 'Central Clinic', 'uploads/Leaf_1768320157.png', 'LIC654321', 'uploads/coming_soon_1768320157.png', '123456,360005,654785', '8849227088', 'kunj@gmail.com', '2026-01-13 16:02:37', 'Accepted', 'Kunj@upi', 'Congratulations! Your application has been accepted. Welcome aboard.', '2026-01-13 16:03:16', ''),
(17, 'HarshilBhudiya', 'Human', 1, 'adfadf', 5, 'Years', 'adfaafas', 'https://www.google.com/maps/search/?api=1&query=22.9943%2C72.5828', 51, 'fadadfdafadfdf', 'adfdf', 'uploads/20240530_123104_1770541194.jpg', 'MED12345', 'uploads/B612_20230215_213027_873_1770541194.jpg', '123456', '9313442240', 'harshil@gmail.com', '2026-02-08 08:59:54', 'Rejected', 'harshil@gmail.com', 'Dear Dr. HarshilBhudiya,\r\n\r\nThank you for your application. After review, we are unable to proceed at this time.\r\n\r\nReason:', '2026-02-08 09:00:28', ''),
(18, 'HarshilBhudiya', 'Vet', 2, 'adfadf', 5, 'Years', 'adfaafas', 'https://www.google.com/maps/search/?api=1&query=23.044765149494193%2C72.56533372986136', 51, 'adafaf', 'adfdf', 'uploads/20240530_123104_1770541395.jpg', 'MED12345', 'uploads/B612_20230215_213027_873_1770541395.jpg', '123456', '9313442240', 'harshil@gmail.com', '2026-02-08 09:03:15', 'Rejected', 'harshil@gmail.com', 'Dear Dr. HarshilBhudiya,\r\n\r\nThank you for your application. After review, we are unable to proceed at this time.\r\n\r\nReason:', '2026-02-08 09:03:25', '18');

-- --------------------------------------------------------

--
-- Table structure for table `pg_phonepe_orders`
--

CREATE TABLE `pg_phonepe_orders` (
  `id` int(11) NOT NULL,
  `patient_id` int(11) NOT NULL,
  `merchant_order_id` varchar(63) NOT NULL,
  `order_id` varchar(64) DEFAULT NULL,
  `token` text DEFAULT NULL,
  `amount_paise` int(11) NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `state` enum('PENDING','COMPLETED','FAILED') DEFAULT 'PENDING',
  `instrument` varchar(32) DEFAULT NULL,
  `webhook_payload` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`webhook_payload`)),
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- Dumping data for table `pg_phonepe_orders`
--

INSERT INTO `pg_phonepe_orders` (`id`, `patient_id`, `merchant_order_id`, `order_id`, `token`, `amount_paise`, `amount`, `state`, `instrument`, `webhook_payload`, `created_at`, `updated_at`) VALUES
(31, 12, 'DAH_12_1772741900', 'OMO2603060149089683406790W', 'hq4wOGdzX31IuPyyh7/7AYOLiipO42P8QtgmusudZHta7zUAMbV5uMV5f6kF1hmvheryrLtNiVUwS2xgVeHEKhBe3yf2AOT09iof5VAjn1BfD4aeZqZgcjJ128ANhYYEJ9xiveZvqZLm32l9UU6MnAcCv4zBPKAf9zhknNKL7k0FJpI6sXEVfO+J7vZDPQBPq379BjXQItQaghUsLGGOaGdB+AVa2nRKuOCMXZeLo5zkfSaCsvZC8Ho8I0ecLPXQP08faOoz9mMus67VwQqPAlbYLGR61eqp1vEDNTqcN2x9feP0EIYxP7aht+98a3Xi1kSNPgGcWPqbsXDhuE9rBlW5bGFMj5p5bpyD2c5Xjl/IA6RfhJseimDujFuMbO2xBqMhWaqjENg9cfQ897yaZnV5Zbwk7TAL/YxKmVTXEevOSYPX/iZh7XW27tL3y9o0H1o2ds6k6sG4O/my6Omb1cluHcz6Brv29rvwmuAh74CT5vpWACQt63Y4N4ArRgAVaive81vHml60BL7q32KMxKm5HA==', 10000, 100.00, 'PENDING', 'WALLET_TOPUP', NULL, '2026-03-05 20:18:21', '2026-03-05 20:18:56'),
(32, 1, 'DAH_1_1777462858', 'OMO2604291710587886222213V', 'hq4wOGdzX31IuPyyh7/7AYOLiipO42P8QtgmusudZHta7zUAMbV5uMV5f6kF1hmvheryrLtNiVUwS2xgVeHEKhBe3yf2AOT09iof5VA2iS0UD4aeL6Zwan502+ZMnrhZPcJIg/xHk7i+8kBbH02inAcBkbaIPrAh8ztkocuJhU4WIJEfkXgKYOKx4e1QOCpyu3+iRyrDG/JFnjwKAmiOTmhCnT9G2BNOi+2NQZ6Ao/PmekCdiPNozXg8PEveFJDINXZ6Veg19UYCt72ywQmKIEzdAXgx7IC1ychmCDiaNElRefCTEIU0HaykmZBhUHWG00KOGy2YS52bsnXDokpGHlC/c3ZJiZlcbKOcgNNsjgLSE7RhnogrtCPEpX3Cb8OxBqAPY+CgAOo+WZYigNncc2Y+KuYJuFl76YRclUnSRvy/EqP+7hVW2mfJ1fKFy9wmCEkmH77x+4WsM9CFqOHtwrxdHLz+eb7nsMmWiZdS/o3/uKxAABAft3MnI9Ekfh9jcENeSNsDXfnTJ8Xj+7+uWXLD', 100, 1.00, 'PENDING', 'WALLET_TOPUP', NULL, '2026-04-29 11:40:58', '2026-04-29 11:41:49'),
(33, 1, 'DAH_1_1777462931', 'OMO2604291712119109944453V', 'hq4wOGdzX31IuPyyh7/7AYOLiipO42P8QtgmusudZHta7zUAMbV5uMV5f6kF1hmvheryrLtNiVUwS2xgVeHEKhBe3yf2AOT09iof5VA2iS0UD4aeL6Zwan939cRMnrhZPcJIg/xHk7i+8kBbH02inAcBkbaIPqA5vztkocuJhU4WIJEfkXgKYOKx4e1QOCpyu3+iRyrDG/JFnjwKAmiOTmhCnT9G2BNOi+2NQZ6Ao/PmekCdiPNozXg8PEveFJDINXZ6Veg19UYCt72ywQmKIEzdAXgx7IC1ychmCDiaNElRefCTEIU0HaykmZBhUHWG00KOGy2YS52bsnXDokpGHlC/c3ZJiZlcbKOcgNNsjgLSE7RhnogrtCPEpX3Cb8OxBqAPY+ChPvJ6WZYirdudTxUmdpU3oTN7/7JUj0HnJfe5c7rJuStM0lrVy6rUy/1AKl8EJN7Vk8H/IomuwdKT7IJMEc7zVt7ispLHmdJs7Yj2oNBBJywg8UsFAY4oRjZ4UHVeOID2d/00D3ZGTHEjG0MI', 200, 2.00, 'PENDING', 'WALLET_TOPUP', NULL, '2026-04-29 11:42:11', '2026-04-29 11:42:48');

-- --------------------------------------------------------

--
-- Table structure for table `refund_requests`
--

CREATE TABLE `refund_requests` (
  `refund_id` int(11) NOT NULL,
  `appointment_id` int(11) NOT NULL,
  `payment_id` int(11) DEFAULT NULL,
  `patient_id` int(11) NOT NULL,
  `doctor_id` int(11) NOT NULL,
  `initiated_by` enum('patient','doctor') NOT NULL,
  `payment_method` enum('Online','Offline') NOT NULL,
  `appointment_mode` enum('Online','Offline') NOT NULL,
  `deposit_cut` decimal(10,2) NOT NULL DEFAULT 0.00,
  `refundable_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `refund_to` enum('UPI','Wallet') NOT NULL,
  `upi_id` varchar(128) DEFAULT NULL,
  `status` enum('Requested','Processing','Completed','Rejected') NOT NULL DEFAULT 'Requested',
  `notes` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Triggers `refund_requests`
--
DELIMITER $$
CREATE TRIGGER `trg_refund_requests_ai` AFTER INSERT ON `refund_requests` FOR EACH ROW BEGIN
  UPDATE payment_history
     SET refund_status = NEW.status,
         updated_at    = NOW()
   WHERE appointment_id = NEW.appointment_id;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_refund_requests_au` AFTER UPDATE ON `refund_requests` FOR EACH ROW BEGIN
  UPDATE payment_history
     SET refund_status = NEW.status,
         updated_at    = NOW()
   WHERE appointment_id = NEW.appointment_id;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `refund_transactions`
--

CREATE TABLE `refund_transactions` (
  `refund_id` int(11) NOT NULL,
  `payment_id` int(11) NOT NULL,
  `refund_amount` decimal(10,2) NOT NULL,
  `refund_type` enum('Patient','Doctor','Admin') DEFAULT 'Patient',
  `refund_reason` varchar(255) DEFAULT NULL,
  `refund_proof` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `reviews`
--

CREATE TABLE `reviews` (
  `review_id` int(11) NOT NULL,
  `patient_id` int(11) NOT NULL,
  `doctor_id` int(11) NOT NULL,
  `rating` enum('1','2','3','4','5') DEFAULT NULL,
  `review_comment` text DEFAULT NULL,
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
  `review_canceled` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- Dumping data for table `reviews`
--

INSERT INTO `reviews` (`review_id`, `patient_id`, `doctor_id`, `rating`, `review_comment`, `timestamp`, `review_canceled`) VALUES
(4, 3, 4, '4', 'xx db', '2025-11-04 07:41:40', 0),
(5, 4, 3, NULL, NULL, '2025-11-09 04:15:21', 1),
(6, 3, 3, '4', 'hi', '2025-11-09 05:04:52', 0);

-- --------------------------------------------------------

--
-- Table structure for table `slider_images`
--

CREATE TABLE `slider_images` (
  `id` int(11) NOT NULL,
  `image_url` varchar(255) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- Dumping data for table `slider_images`
--

INSERT INTO `slider_images` (`id`, `image_url`, `created_at`) VALUES
(7, 'https://thedoctorathome.in/Admin/slider/1748324569_Slider-10.jpg', '2025-05-07 06:25:18'),
(9, 'https://thedoctorathome.in/Admin/slider/1748334449_1600w-PdjF3CCiHLs.webp', '2025-05-07 06:51:01'),
(10, 'https://thedoctorathome.in/Admin/slider/1772202457_Screenshot__4_.png', '2025-05-27 05:40:09'),
(11, 'https://thedoctorathome.in/Admin/slider/1758384238_statue-6823436_1280.webp', '2025-09-20 16:03:58'),
(12, 'https://thedoctorathome.in/Admin/slider/1772202433_WhatsApp_Image_2026-02-27_at_7.50.47_PM.jpeg', '2025-09-20 16:04:14');

-- --------------------------------------------------------

--
-- Table structure for table `support`
--

CREATE TABLE `support` (
  `support_id` int(20) NOT NULL,
  `name` text NOT NULL,
  `email` text NOT NULL,
  `subject` text NOT NULL,
  `message` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- Dumping data for table `support`
--

INSERT INTO `support` (`support_id`, `name`, `email`, `subject`, `message`) VALUES
(1, 'Kunj Patel', 'kunj@gmail.com', '', 'Hello Mother fuckin Bolliaone');

-- --------------------------------------------------------

--
-- Table structure for table `Terms`
--

CREATE TABLE `Terms` (
  `id` int(11) NOT NULL,
  `data` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- Dumping data for table `Terms`
--

INSERT INTO `Terms` (`id`, `data`) VALUES
(1, 'Patient Terms and Conditions for the \"Doctor at Home\" App\n\nThese Terms and Conditions (\"Terms\") apply to the services (\"Services\") provided by The Doctor At Home mobile application (\"doctor at home\") and [Infowave] (\"Company,\" \"we,\" \"our,\" or \"us\").\n\nPlease read these Terms carefully before using The Doctor At Home App and Services.\n\n1. Acceptance of Terms\n\nBy downloading, installing, or using the App, you acknowledge that you have read, understood, and agreed to comply with these Terms. If you do not agree with these Terms, do not use the App or Services.\n\n2. Description of Services\n\nThis App connects patients with qualified healthcare professionals (\"Doctors\") for home visits. These services include:\n\nScheduling home doctor visits.\n\nCommunicating with doctors via the in-app messaging system or video calls (if applicable).\n\nAccessing and managing medical records through the App.\n\nConducting Zoom or Google Meet consultations.\n\nRequesting prescription refills (if applicable and legally permitted).\n\nNote: The Company does not provide medical advice. Doctors are independent professionals responsible for the medical advice and treatment they offer.\n\n\n');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `user_id` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `mobile` varchar(20) NOT NULL,
  `city` varchar(100) DEFAULT NULL,
  `age` int(11) DEFAULT NULL,
  `pincode` varchar(10) DEFAULT NULL,
  `gender` enum('Male','Female','Other') DEFAULT NULL,
  `otp` varchar(10) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`user_id`, `name`, `email`, `mobile`, `city`, `age`, `pincode`, `gender`, `otp`, `created_at`, `updated_at`) VALUES
(1, 'Vansh Mandanka', 'mandankavansh@gmail.com', '6354355617', 'Rajkot', 19, '360020', 'Male', NULL, '2025-10-30 20:42:10', '2025-11-30 15:23:20'),
(3, 'harshil', 'bhudiyaharshil6@gmail.com', '9313442240', 'Ahmedabad', 19, '360020', 'Male', NULL, '2025-11-02 17:14:40', '2026-02-28 10:34:42'),
(4, 'kunj', 'kunjp925@gmail.com', '8849227088', 'Rajkot', 19, '360002', 'Male', NULL, '2025-11-07 12:41:45', '2026-01-13 15:55:06'),
(5, 'Punit', 'punitpurohit73@gmail.com', '8849166463', 'Bhavnagar', 28, '364140', 'Male', NULL, '2025-11-09 07:08:59', '2026-02-08 09:16:39'),
(6, 'Yash', 'parwaniy6@gmail.com', '9923906425', 'Pune', 23, '411006', 'Male', NULL, '2025-11-09 07:15:28', '2025-11-17 16:47:17'),
(7, 'HANMANT KHOBARE', 'hanmant030609@gmail.com', '9011083413', 'Pune', 36, '412206', 'Male', NULL, '2025-11-09 08:31:46', '2026-02-28 15:49:14'),
(8, 'fgff', 'darshalimandanka@gmail.com', '9601311556', 'Surat', 45, '395010', 'Female', NULL, '2025-11-09 17:24:31', '2025-11-09 17:24:52'),
(9, 'utkarsh', 'theteutkarsh@gmail.com', '9503568552', 'Pune', 22, '411046', 'Male', NULL, '2025-11-14 05:53:19', '2025-11-14 05:54:57'),
(10, 'devd', 'devesh.lad@gmail.com', '9890933074', 'Pune', 39, '411041', 'Male', NULL, '2025-12-01 18:02:02', '2025-12-01 18:02:38'),
(11, 'Mujeeb Shaikh', 'mujeebshaikh333@gmail.com', '9764714141', 'Pune', 30, '411004', 'Male', NULL, '2025-12-26 11:54:42', '2026-01-29 11:40:35'),
(12, 'darshit andrapiya', 'darshitandrapiya@gmail.com', '9979842096', 'Rajkot', 18, '360001', 'Male', NULL, '2026-01-01 18:55:06', '2026-03-05 20:18:00'),
(13, 'Argish', 'argish@yopmail.com', '9724929081', 'Gandhinagar', 37, '382016', 'Male', NULL, '2026-01-12 09:35:42', '2026-01-12 09:36:05'),
(14, 'raibhy jamdar', 'dodrawingnow@gmail.com', '9881166654', 'Pune', 18, '411011', 'Male', NULL, '2026-01-13 04:56:38', '2026-01-13 04:57:36'),
(15, 'Yaseen Sab', 'yaseensab24@gmail.com', '8197475979', 'Bengaluru', 26, '560070', 'Male', NULL, '2026-01-15 06:03:59', '2026-01-15 06:04:22'),
(16, 'M.Talukdar', 'mohuasneha08@gmail.com', '9073471318', 'Kolkata', 52, '700084', 'Female', NULL, '2026-01-16 04:31:24', '2026-01-16 04:31:44'),
(17, 'Dr Hridaynath Lad', 'support@thedoctorathome.in', '9822964454', 'Pune', 61, '411011', 'Male', NULL, '2026-01-17 06:35:24', '2026-02-08 09:18:22'),
(18, 'mk', 'mukeshchodhary1969@gmail.com', '9983514585', 'delhi', 29, '110063', 'Male', NULL, '2026-01-20 19:07:54', '2026-01-20 19:08:17'),
(19, 'Haram Ali', 'aliharam729@gmail.com', '8100014582', 'Kolkata', 19, '700017', 'Male', NULL, '2026-01-25 04:32:14', '2026-01-25 04:32:45'),
(20, 'aryan reddy', 'reddyarya.1437@gmail.com', '8247401076', 'Vizianagram', 44, '795149', 'Male', NULL, '2026-01-30 14:00:53', '2026-01-30 14:01:15'),
(21, 'Kajal Talmale', 'kajalbawankar2016@gmail.com', '7507061672', 'Pune', 28, '411062', 'Female', NULL, '2026-02-01 18:43:59', '2026-02-01 18:44:25'),
(22, 'yash mahajan', 'yashmh1010@gmail.com', '7982213747', 'delhi', 18, '110051', 'Male', '8895', '2026-02-03 07:19:40', '2026-02-03 07:31:46'),
(23, 'Sakshi Timkikar', 'sakshitimkikar2607@gmail.com', '9552122419', 'pune', 19, '411033', 'Female', NULL, '2026-02-17 09:49:37', '2026-02-17 09:50:02'),
(24, 'jo', 'joannasusan96@gmail.com', '9840481187', 'chennai', 18, '60007', 'Female', NULL, '2026-02-20 12:14:16', '2026-02-20 12:14:53'),
(25, 'Shaikishaq', 'shaikishaq6874@gmail.com', '9036666243', 'Bangalore', 65, '560078', 'Male', NULL, '2026-02-22 14:55:28', '2026-02-22 14:55:58'),
(26, 'Nimesh', 'nimeshvekariya0@gmail.com', '9316930412', 'Rajkot', 18, '340020', 'Male', '5860', '2026-02-24 07:07:38', '2026-03-15 16:58:27'),
(28, 'DR HRIDAYNATH LAD', 'hanmant_khobare@welspun.com', '9967876444', 'PUNE', 65, ' 435666', 'Male', NULL, '2026-02-28 16:10:05', '2026-02-28 16:10:05'),
(29, 'vivek', 'yoganandivivek@gmail.com', '8866200163', 'Porbandar', 32, '360750', 'Male', NULL, '2026-03-09 16:57:47', '2026-03-09 16:58:06'),
(30, 'Rajiv Sehgal', 'r_sehgal57@yahoo.co.in', '9818445453', 'Noida', 69, '201301', 'Male', NULL, '2026-03-11 14:09:10', '2026-03-11 14:09:35'),
(31, 'Shreyas Anbhule', 'SHREYAS.ANBHULE@gmail.com', '9504050909', 'Shrigonda', 18, '413701', 'Male', NULL, '2026-03-13 12:32:19', '2026-03-13 12:32:53'),
(32, 'Nitin Rajput', 'rumasingh040@gmail.com', '8929536103', 'delhi', 18, '110042', 'Male', NULL, '2026-03-14 02:10:52', '2026-03-14 02:11:27'),
(33, 'Pooja kavithiya', 'vishalkavithiya142@gmail.com', '8618714160', 'mangalur Karnataka India', 32, '123456', 'Female', '1549', '2026-03-16 12:25:27', '2026-03-16 12:30:48'),
(34, 'Hiten Gawade', 'hitengawade218@gmail.com', '9029217946', 'Thane', 34, '421202', 'Male', NULL, '2026-03-18 06:18:19', '2026-03-18 06:18:45'),
(35, 'madhu', 'snbk7428@gmail.com', '7428667626', 'rohini', 50, '110085', 'Female', NULL, '2026-03-20 12:38:10', '2026-03-20 12:38:37'),
(36, 'Akhil Agrawal', 'akhilagrawal1982@gmail.com', '9999646066', 'Greater Noida West', 45, '201306', 'Male', NULL, '2026-04-11 15:20:04', '2026-04-11 15:20:04'),
(38, 'Akhil Agrawal', 'akhilagrawal2202@gmail.com', '8178305723', 'Greater Noida West', 45, '201306', 'Male', NULL, '2026-04-11 15:20:36', '2026-04-11 15:21:00'),
(39, 'Brijesh Gami', 'brijeshgami19@gmail.com', '9726306800', 'Ahmedabad', 21, '380017', 'Male', NULL, '2026-04-21 14:13:42', '2026-04-21 14:14:23'),
(40, 'vinay kumar', 'arush.kumar64@gmail.com', '9811009858', 'new delhi', 33, '110003', 'Male', NULL, '2026-04-22 16:30:44', '2026-04-22 16:31:06'),
(41, 'A Sahana', 'asahana934@gmail.com', '7845473676', 'dharapuram', 28, '638657', 'Male', NULL, '2026-04-29 09:22:20', '2026-04-29 09:22:20');

-- --------------------------------------------------------

--
-- Stand-in structure for view `v_doctors_legacy`
-- (See below for the actual view)
--
CREATE TABLE `v_doctors_legacy` (
`doctor_id` int(11)
,`full_name` varchar(255)
,`doctor_type` enum('specialist','general')
,`category_id` int(11)
,`animal_category_id` varchar(255)
,`qualification` varchar(255)
,`experience_years` int(20)
,`experience_duration` varchar(255)
,`specialization` varchar(255)
,`doctor_location` text
,`consultation_fee` decimal(10,2)
,`availability_schedule` text
,`hospital_affiliation` varchar(255)
,`profile_picture` varchar(255)
,`license_number` varchar(50)
,`licence_photo` text
,`status` enum('Active','Inactive','Ongoing Appointment')
,`auto_status` enum('Active','Inactive','Ongoing Appointment')
,`rating` decimal(3,2)
,`pincodes` text
,`mobile` varchar(255)
,`otp` int(10)
,`is_vet` tinyint(1)
,`email` text
,`upi_id` text
,`animal_category_id_legacy` int(11)
,`animal_category_ids_legacy` longtext
);

-- --------------------------------------------------------

--
-- Stand-in structure for view `v_doctor_animal_category_names`
-- (See below for the actual view)
--
CREATE TABLE `v_doctor_animal_category_names` (
`doctor_id` int(11)
,`animal_category_names` longtext
);

-- --------------------------------------------------------

--
-- Table structure for table `wallet_transactions`
--

CREATE TABLE `wallet_transactions` (
  `id` int(11) NOT NULL,
  `patient_id` int(11) NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `type` enum('credit','debit') NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- Dumping data for table `wallet_transactions`
--

INSERT INTO `wallet_transactions` (`id`, `patient_id`, `amount`, `type`, `reason`, `timestamp`) VALUES
(1, 1, 1.00, 'credit', 'DAH_1_1761860462', '2025-10-30 21:41:22'),
(2, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-10-30 21:42:46'),
(3, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-10-30 22:00:08'),
(4, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-10-30 22:01:47'),
(5, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-10-30 22:02:30'),
(6, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-02 08:03:27'),
(7, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-02 08:38:28'),
(8, 2, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-02 08:48:55'),
(9, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-02 08:57:21'),
(10, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-02 09:35:20'),
(11, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-02 09:38:18'),
(12, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-02 10:15:04'),
(13, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-02 10:23:14'),
(14, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-02 10:33:09'),
(15, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-02 10:48:07'),
(16, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-02 10:54:19'),
(17, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-02 11:01:20'),
(18, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-02 11:04:02'),
(19, 3, 1.00, 'credit', 'DAH_3_1762103697', '2025-11-02 17:15:11'),
(20, 3, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-02 17:17:54'),
(21, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-02 17:23:40'),
(22, 3, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-02 17:27:10'),
(23, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-03 01:48:51'),
(24, 2, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-03 08:10:54'),
(25, 3, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-03 17:38:38'),
(26, 3, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-03 18:52:32'),
(27, 3, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-03 19:06:40'),
(28, 3, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-04 07:39:56'),
(29, 3, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-04 07:40:50'),
(30, 3, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-05 20:27:28'),
(31, 3, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-05 20:29:49'),
(32, 3, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-05 20:48:51'),
(33, 3, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-05 21:45:21'),
(34, 3, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-05 22:56:19'),
(35, 3, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-05 22:59:34'),
(39, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-06 00:13:11'),
(40, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-06 00:15:56'),
(41, 1, 50.00, 'credit', 'Doctor cancellation: deposit refund', '2025-11-06 00:22:47'),
(42, 1, 50.00, 'credit', 'Doctor cancellation: deposit refund', '2025-11-06 00:23:59'),
(43, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-06 00:25:18'),
(44, 3, 50.00, 'credit', 'Doctor cancellation: deposit refund', '2025-11-07 02:35:27'),
(45, 1, 50.00, 'debit', 'Platform charge for online appointment (wallet debit)', '2025-11-07 02:39:43'),
(46, 1, 50.00, 'debit', 'Platform charge for online appointment (wallet debit)', '2025-11-07 02:53:58'),
(47, 1, 50.00, 'debit', 'Platform charge for online appointment (wallet debit)', '2025-11-07 03:48:46'),
(48, 1, 50.00, 'debit', 'Platform charge for online appointment (wallet debit)', '2025-11-07 03:53:30'),
(49, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-07 04:09:01'),
(50, 1, 50.00, 'debit', 'Platform charge for online appointment (wallet debit)', '2025-11-07 04:10:49'),
(51, 1, 50.00, 'credit', 'Doctor cancellation: deposit refund', '2025-11-07 04:34:39'),
(52, 1, 50.00, 'credit', 'Doctor cancellation: deposit refund', '2025-11-07 16:10:55'),
(53, 1, 50.00, 'credit', 'Doctor cancellation: deposit refund', '2025-11-07 16:10:56'),
(54, 4, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-07 16:12:19'),
(55, 4, 50.00, 'credit', 'Doctor cancellation: deposit refund', '2025-11-07 16:18:15'),
(56, 4, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-07 16:18:56'),
(57, 4, 50.00, 'credit', 'Doctor cancellation: deposit refund', '2025-11-07 16:19:55'),
(58, 4, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-07 16:30:17'),
(59, 4, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-07 16:45:01'),
(60, 4, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-07 16:45:22'),
(61, 4, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-07 16:45:45'),
(62, 4, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-07 16:46:03'),
(63, 4, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-07 16:57:59'),
(64, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-08 11:44:14'),
(65, 4, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-08 13:23:23'),
(66, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-08 13:43:45'),
(67, 1, 50.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-08 13:47:05'),
(68, 4, 50.00, 'debit', 'Platform charge for online appointment (wallet debit)', '2025-11-09 04:15:16'),
(69, 4, 1.00, 'debit', 'Platform charge for online appointment (wallet debit)', '2025-11-09 04:31:56'),
(70, 3, 3.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-09 04:45:45'),
(71, 3, 3.00, 'debit', 'Platform charge for online appointment (wallet debit)', '2025-11-09 04:54:53'),
(72, 4, 3.00, 'debit', 'Platform charge for offline appointment booking', '2025-11-09 05:04:03'),
(73, 3, 3.00, 'debit', 'Platform charge for online appointment (wallet debit)', '2025-11-09 05:32:42'),
(74, 7, 1.00, 'credit', 'DAH_7_1762677149', '2025-11-09 08:32:55'),
(75, 1, 1.00, 'credit', 'DAH_1_1767595147', '2026-01-05 06:39:28'),
(76, 3, 3.00, 'debit', 'Platform charge for offline appointment booking', '2026-01-05 06:48:53'),
(77, 3, 212.00, 'debit', 'Platform charge for offline appointment booking', '2026-01-05 06:51:56'),
(78, 4, 3.00, 'debit', 'Platform charge for offline appointment booking', '2026-01-05 07:07:32'),
(79, 4, 3.00, 'debit', 'Platform charge for offline appointment booking', '2026-01-05 08:43:16'),
(80, 3, 153.00, 'debit', 'Platform charge for offline appointment booking', '2026-01-11 19:33:18'),
(81, 3, 153.00, 'debit', 'Platform charge for offline appointment booking', '2026-01-11 19:36:45'),
(82, 3, 153.00, 'debit', 'Platform charge for offline appointment booking', '2026-01-12 19:36:46'),
(83, 3, 3.00, 'debit', 'Platform charge for offline appointment booking', '2026-03-03 07:07:51');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `admins`
--
ALTER TABLE `admins`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `admin_doctor_payment_summary`
--
ALTER TABLE `admin_doctor_payment_summary`
  ADD PRIMARY KEY (`summary_id`),
  ADD KEY `doctor_id` (`doctor_id`);

--
-- Indexes for table `admin_transactions`
--
ALTER TABLE `admin_transactions`
  ADD PRIMARY KEY (`transaction_id`),
  ADD UNIQUE KEY `uq_admin_settlement_type` (`settlement_summary_id`,`transaction_type`),
  ADD KEY `idx_admin_settlement_summary_id` (`settlement_summary_id`),
  ADD KEY `idx_admin_doctor_id` (`doctor_id`),
  ADD KEY `idx_admin_txn_settlement_type` (`settlement_summary_id`,`transaction_type`);

--
-- Indexes for table `animal_breeds`
--
ALTER TABLE `animal_breeds`
  ADD PRIMARY KEY (`breed_id`),
  ADD UNIQUE KEY `uq_category_breed` (`category_id`,`breed_name`),
  ADD UNIQUE KEY `slug` (`slug`);

--
-- Indexes for table `animal_categories`
--
ALTER TABLE `animal_categories`
  ADD PRIMARY KEY (`category_id`),
  ADD UNIQUE KEY `category_name` (`category_name`);

--
-- Indexes for table `animal_medical_reports`
--
ALTER TABLE `animal_medical_reports`
  ADD PRIMARY KEY (`report_id`),
  ADD KEY `idx_amr_appointment` (`appointment_id`),
  ADD KEY `idx_amr_animal` (`animal_id`),
  ADD KEY `idx_amr_report_date` (`report_date`),
  ADD KEY `idx_amr_vaccination` (`vaccination_id`);

--
-- Indexes for table `animal_vaccinations`
--
ALTER TABLE `animal_vaccinations`
  ADD PRIMARY KEY (`vaccination_id`),
  ADD UNIQUE KEY `uq_category_vaccine` (`category_id`,`vaccination_name`),
  ADD KEY `idx_category_id` (`category_id`),
  ADD KEY `idx_vaccination_name` (`vaccination_name`),
  ADD KEY `idx_slug` (`slug`);

--
-- Indexes for table `appointments`
--
ALTER TABLE `appointments`
  ADD PRIMARY KEY (`appointment_id`),
  ADD KEY `patient_id` (`patient_id`),
  ADD KEY `doctor_id` (`doctor_id`),
  ADD KEY `idx_appt_isvet` (`is_vet_case`),
  ADD KEY `idx_appt_animal` (`animal_category_id`),
  ADD KEY `idx_appt_doc_status_date` (`doctor_id`,`status`,`appointment_date`),
  ADD KEY `idx_appt_id` (`appointment_id`),
  ADD KEY `idx_appt_patient` (`patient_id`);

--
-- Indexes for table `app_settings`
--
ALTER TABLE `app_settings`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `key_name` (`key_name`),
  ADD UNIQUE KEY `uniq_key_name` (`key_name`);

--
-- Indexes for table `Article`
--
ALTER TABLE `Article`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `Contact`
--
ALTER TABLE `Contact`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `doctors`
--
ALTER TABLE `doctors`
  ADD PRIMARY KEY (`doctor_id`),
  ADD UNIQUE KEY `license_number` (`license_number`),
  ADD KEY `idx_doctors_animal_category_id` (`animal_category_id`);

--
-- Indexes for table `doctor_animal_categories`
--
ALTER TABLE `doctor_animal_categories`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_doctor_animal` (`doctor_id`,`animal_category_id`),
  ADD KEY `idx_doctor` (`doctor_id`),
  ADD KEY `idx_animal` (`animal_category_id`);

--
-- Indexes for table `doctor_categories`
--
ALTER TABLE `doctor_categories`
  ADD PRIMARY KEY (`category_id`),
  ADD UNIQUE KEY `category_name` (`category_name`),
  ADD KEY `idx_is_vet` (`is_vet`),
  ADD KEY `idx_status` (`status`);

--
-- Indexes for table `doctor_leads`
--
ALTER TABLE `doctor_leads`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_mobile` (`mobile`),
  ADD KEY `idx_created_at` (`created_at`);

--
-- Indexes for table `doctor_tokens`
--
ALTER TABLE `doctor_tokens`
  ADD PRIMARY KEY (`token_id`),
  ADD UNIQUE KEY `uniq_doctor_device` (`doctor_id`,`device_id`),
  ADD KEY `idx_token` (`fcm_token`(768)),
  ADD KEY `idx_updated` (`last_updated`);

--
-- Indexes for table `doctor_transactions`
--
ALTER TABLE `doctor_transactions`
  ADD PRIMARY KEY (`transaction_id`),
  ADD UNIQUE KEY `uq_doctor_settlement_type` (`doctor_id`,`settlement_summary_id`,`transaction_type`),
  ADD KEY `doctor_id` (`doctor_id`),
  ADD KEY `idx_dt_doctor_status` (`doctor_id`,`status`),
  ADD KEY `idx_dt_summary` (`settlement_summary_id`),
  ADD KEY `idx_doctor_settlement_type` (`doctor_id`,`settlement_summary_id`,`transaction_type`);

--
-- Indexes for table `healthtip`
--
ALTER TABLE `healthtip`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `medical_reports`
--
ALTER TABLE `medical_reports`
  ADD PRIMARY KEY (`report_id`),
  ADD KEY `fk_appointment_id` (`appointment_id`);

--
-- Indexes for table `patients`
--
ALTER TABLE `patients`
  ADD PRIMARY KEY (`patient_id`),
  ADD UNIQUE KEY `mobile` (`mobile`),
  ADD UNIQUE KEY `email` (`email`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `patient_fcm_tokens`
--
ALTER TABLE `patient_fcm_tokens`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uniq_patient_device` (`patient_id`,`device_id`),
  ADD KEY `idx_updated` (`last_updated`),
  ADD KEY `idx_token` (`fcm_token`(256));

--
-- Indexes for table `payment_history`
--
ALTER TABLE `payment_history`
  ADD PRIMARY KEY (`payment_id`),
  ADD KEY `patient_id` (`patient_id`),
  ADD KEY `appointment_id` (`appointment_id`),
  ADD KEY `doctor_id` (`doctor_id`),
  ADD KEY `idx_ph_appt` (`appointment_id`),
  ADD KEY `idx_ph_doc` (`doctor_id`),
  ADD KEY `idx_ph_status` (`payment_status`);

--
-- Indexes for table `pending_doctors`
--
ALTER TABLE `pending_doctors`
  ADD PRIMARY KEY (`pending_id`);

--
-- Indexes for table `pg_phonepe_orders`
--
ALTER TABLE `pg_phonepe_orders`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `merchant_order_id` (`merchant_order_id`),
  ADD KEY `idx_pg_phonepe_patient` (`patient_id`);

--
-- Indexes for table `refund_requests`
--
ALTER TABLE `refund_requests`
  ADD PRIMARY KEY (`refund_id`),
  ADD UNIQUE KEY `uk_refund_once` (`appointment_id`),
  ADD KEY `ix_patient` (`patient_id`),
  ADD KEY `ix_doctor` (`doctor_id`),
  ADD KEY `fk_rr_pay` (`payment_id`),
  ADD KEY `idx_rr_appt` (`appointment_id`);

--
-- Indexes for table `refund_transactions`
--
ALTER TABLE `refund_transactions`
  ADD PRIMARY KEY (`refund_id`),
  ADD KEY `payment_id` (`payment_id`);

--
-- Indexes for table `reviews`
--
ALTER TABLE `reviews`
  ADD PRIMARY KEY (`review_id`),
  ADD UNIQUE KEY `unique_review` (`patient_id`,`doctor_id`),
  ADD KEY `doctor_id` (`doctor_id`);

--
-- Indexes for table `slider_images`
--
ALTER TABLE `slider_images`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `support`
--
ALTER TABLE `support`
  ADD PRIMARY KEY (`support_id`);

--
-- Indexes for table `Terms`
--
ALTER TABLE `Terms`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `email` (`email`),
  ADD UNIQUE KEY `mobile` (`mobile`);

--
-- Indexes for table `wallet_transactions`
--
ALTER TABLE `wallet_transactions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_patient_id` (`patient_id`),
  ADD KEY `idx_wt_patient` (`patient_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `admins`
--
ALTER TABLE `admins`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT for table `admin_doctor_payment_summary`
--
ALTER TABLE `admin_doctor_payment_summary`
  MODIFY `summary_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `admin_transactions`
--
ALTER TABLE `admin_transactions`
  MODIFY `transaction_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `animal_breeds`
--
ALTER TABLE `animal_breeds`
  MODIFY `breed_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `animal_categories`
--
ALTER TABLE `animal_categories`
  MODIFY `category_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=21;

--
-- AUTO_INCREMENT for table `animal_medical_reports`
--
ALTER TABLE `animal_medical_reports`
  MODIFY `report_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `animal_vaccinations`
--
ALTER TABLE `animal_vaccinations`
  MODIFY `vaccination_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `appointments`
--
ALTER TABLE `appointments`
  MODIFY `appointment_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `app_settings`
--
ALTER TABLE `app_settings`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT for table `Article`
--
ALTER TABLE `Article`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `Contact`
--
ALTER TABLE `Contact`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `doctors`
--
ALTER TABLE `doctors`
  MODIFY `doctor_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT for table `doctor_animal_categories`
--
ALTER TABLE `doctor_animal_categories`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=15;

--
-- AUTO_INCREMENT for table `doctor_categories`
--
ALTER TABLE `doctor_categories`
  MODIFY `category_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `doctor_leads`
--
ALTER TABLE `doctor_leads`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=17;

--
-- AUTO_INCREMENT for table `doctor_tokens`
--
ALTER TABLE `doctor_tokens`
  MODIFY `token_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=322;

--
-- AUTO_INCREMENT for table `doctor_transactions`
--
ALTER TABLE `doctor_transactions`
  MODIFY `transaction_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `healthtip`
--
ALTER TABLE `healthtip`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT for table `medical_reports`
--
ALTER TABLE `medical_reports`
  MODIFY `report_id` int(250) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `patients`
--
ALTER TABLE `patients`
  MODIFY `patient_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=39;

--
-- AUTO_INCREMENT for table `patient_fcm_tokens`
--
ALTER TABLE `patient_fcm_tokens`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=328;

--
-- AUTO_INCREMENT for table `payment_history`
--
ALTER TABLE `payment_history`
  MODIFY `payment_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `pending_doctors`
--
ALTER TABLE `pending_doctors`
  MODIFY `pending_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=19;

--
-- AUTO_INCREMENT for table `pg_phonepe_orders`
--
ALTER TABLE `pg_phonepe_orders`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=34;

--
-- AUTO_INCREMENT for table `refund_requests`
--
ALTER TABLE `refund_requests`
  MODIFY `refund_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `refund_transactions`
--
ALTER TABLE `refund_transactions`
  MODIFY `refund_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `reviews`
--
ALTER TABLE `reviews`
  MODIFY `review_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT for table `slider_images`
--
ALTER TABLE `slider_images`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

--
-- AUTO_INCREMENT for table `support`
--
ALTER TABLE `support`
  MODIFY `support_id` int(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `Terms`
--
ALTER TABLE `Terms`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `user_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=42;

--
-- AUTO_INCREMENT for table `wallet_transactions`
--
ALTER TABLE `wallet_transactions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=84;

-- --------------------------------------------------------

--
-- Structure for view `v_doctors_legacy`
--
DROP TABLE IF EXISTS `v_doctors_legacy`;

CREATE ALGORITHM=UNDEFINED DEFINER=`u357694546_doctorathome`@`127.0.0.1` SQL SECURITY DEFINER VIEW `v_doctors_legacy`  AS SELECT `d`.`doctor_id` AS `doctor_id`, `d`.`full_name` AS `full_name`, `d`.`doctor_type` AS `doctor_type`, `d`.`category_id` AS `category_id`, `d`.`animal_category_id` AS `animal_category_id`, `d`.`qualification` AS `qualification`, `d`.`experience_years` AS `experience_years`, `d`.`experience_duration` AS `experience_duration`, `d`.`specialization` AS `specialization`, `d`.`doctor_location` AS `doctor_location`, `d`.`consultation_fee` AS `consultation_fee`, `d`.`availability_schedule` AS `availability_schedule`, `d`.`hospital_affiliation` AS `hospital_affiliation`, `d`.`profile_picture` AS `profile_picture`, `d`.`license_number` AS `license_number`, `d`.`licence_photo` AS `licence_photo`, `d`.`status` AS `status`, `d`.`auto_status` AS `auto_status`, `d`.`rating` AS `rating`, `d`.`pincodes` AS `pincodes`, `d`.`mobile` AS `mobile`, `d`.`otp` AS `otp`, `d`.`is_vet` AS `is_vet`, `d`.`email` AS `email`, `d`.`upi_id` AS `upi_id`, min(`dac`.`animal_category_id`) AS `animal_category_id_legacy`, group_concat(distinct `dac`.`animal_category_id` order by `dac`.`animal_category_id` ASC separator ',') AS `animal_category_ids_legacy` FROM (`doctors` `d` left join `doctor_animal_categories` `dac` on(`dac`.`doctor_id` = `d`.`doctor_id`)) GROUP BY `d`.`doctor_id` ;

-- --------------------------------------------------------

--
-- Structure for view `v_doctor_animal_category_names`
--
DROP TABLE IF EXISTS `v_doctor_animal_category_names`;

CREATE ALGORITHM=UNDEFINED DEFINER=`u357694546_doctorathome`@`127.0.0.1` SQL SECURITY DEFINER VIEW `v_doctor_animal_category_names`  AS SELECT `d`.`doctor_id` AS `doctor_id`, group_concat(`ac`.`category_name` order by `ac`.`category_name` ASC separator ', ') AS `animal_category_names` FROM ((`doctors` `d` join `doctor_animal_categories` `dac` on(`dac`.`doctor_id` = `d`.`doctor_id`)) join `animal_categories` `ac` on(`ac`.`category_id` = `dac`.`animal_category_id`)) GROUP BY `d`.`doctor_id` ;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `admin_doctor_payment_summary`
--
ALTER TABLE `admin_doctor_payment_summary`
  ADD CONSTRAINT `admin_doctor_payment_summary_ibfk_1` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`doctor_id`),
  ADD CONSTRAINT `fk_summary_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`doctor_id`) ON UPDATE CASCADE;

--
-- Constraints for table `admin_transactions`
--
ALTER TABLE `admin_transactions`
  ADD CONSTRAINT `fk_admin_txn_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`doctor_id`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_admin_txn_summary` FOREIGN KEY (`settlement_summary_id`) REFERENCES `admin_doctor_payment_summary` (`summary_id`) ON UPDATE CASCADE;

--
-- Constraints for table `animal_breeds`
--
ALTER TABLE `animal_breeds`
  ADD CONSTRAINT `fk_breeds_category` FOREIGN KEY (`category_id`) REFERENCES `animal_categories` (`category_id`) ON UPDATE CASCADE;

--
-- Constraints for table `animal_medical_reports`
--
ALTER TABLE `animal_medical_reports`
  ADD CONSTRAINT `fk_amr_appointment` FOREIGN KEY (`appointment_id`) REFERENCES `appointments` (`appointment_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_amr_vaccination` FOREIGN KEY (`vaccination_id`) REFERENCES `animal_vaccinations` (`vaccination_id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `animal_vaccinations`
--
ALTER TABLE `animal_vaccinations`
  ADD CONSTRAINT `fk_vaccine_category` FOREIGN KEY (`category_id`) REFERENCES `animal_categories` (`category_id`) ON UPDATE CASCADE;

--
-- Constraints for table `appointments`
--
ALTER TABLE `appointments`
  ADD CONSTRAINT `appointments_ibfk_1` FOREIGN KEY (`patient_id`) REFERENCES `patients` (`patient_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `appointments_ibfk_2` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`doctor_id`) ON DELETE CASCADE;

--
-- Constraints for table `doctor_animal_categories`
--
ALTER TABLE `doctor_animal_categories`
  ADD CONSTRAINT `fk_dac_animal` FOREIGN KEY (`animal_category_id`) REFERENCES `animal_categories` (`category_id`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_dac_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`doctor_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `doctor_transactions`
--
ALTER TABLE `doctor_transactions`
  ADD CONSTRAINT `doctor_transactions_ibfk_1` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`doctor_id`),
  ADD CONSTRAINT `fk_doctor_txn_summary` FOREIGN KEY (`settlement_summary_id`) REFERENCES `admin_doctor_payment_summary` (`summary_id`) ON UPDATE CASCADE;

--
-- Constraints for table `medical_reports`
--
ALTER TABLE `medical_reports`
  ADD CONSTRAINT `fk_appointment_id` FOREIGN KEY (`appointment_id`) REFERENCES `appointments` (`appointment_id`);

--
-- Constraints for table `patients`
--
ALTER TABLE `patients`
  ADD CONSTRAINT `patients_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `payment_history`
--
ALTER TABLE `payment_history`
  ADD CONSTRAINT `payment_history_ibfk_1` FOREIGN KEY (`patient_id`) REFERENCES `patients` (`patient_id`),
  ADD CONSTRAINT `payment_history_ibfk_2` FOREIGN KEY (`appointment_id`) REFERENCES `appointments` (`appointment_id`),
  ADD CONSTRAINT `payment_history_ibfk_3` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`doctor_id`);

--
-- Constraints for table `refund_requests`
--
ALTER TABLE `refund_requests`
  ADD CONSTRAINT `fk_rr_appt` FOREIGN KEY (`appointment_id`) REFERENCES `appointments` (`appointment_id`),
  ADD CONSTRAINT `fk_rr_pay` FOREIGN KEY (`payment_id`) REFERENCES `payment_history` (`payment_id`);

--
-- Constraints for table `refund_transactions`
--
ALTER TABLE `refund_transactions`
  ADD CONSTRAINT `refund_transactions_ibfk_1` FOREIGN KEY (`payment_id`) REFERENCES `payment_history` (`payment_id`);

--
-- Constraints for table `reviews`
--
ALTER TABLE `reviews`
  ADD CONSTRAINT `reviews_ibfk_1` FOREIGN KEY (`patient_id`) REFERENCES `patients` (`patient_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `reviews_ibfk_2` FOREIGN KEY (`doctor_id`) REFERENCES `doctors` (`doctor_id`) ON DELETE CASCADE;

--
-- Constraints for table `wallet_transactions`
--
ALTER TABLE `wallet_transactions`
  ADD CONSTRAINT `fk_patient_id` FOREIGN KEY (`patient_id`) REFERENCES `patients` (`patient_id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
