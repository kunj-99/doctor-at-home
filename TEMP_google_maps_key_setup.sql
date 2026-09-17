-- TEMP_google_maps_key_setup.sql
-- Run this once on your MySQL server (Hostinger phpmyadmin or CLI).
-- This inserts (or updates) the Google Directions API key used server-side by
-- get_route_distance.php to call Google Directions API without exposing the key
-- to Android clients.
--
-- BEFORE RUNNING: Replace PASTE_GOOGLE_MAPS_API_KEY_HERE with your real key.
--   Your key must have "Directions API" enabled in Google Cloud Console.
--   Billing must be active on the Google Cloud project.
--
-- Table `app_settings` must already exist (it is used by get_app_config.php).

INSERT INTO app_settings (key_name, value)
VALUES ('google_directions_api_key', 'PASTE_GOOGLE_MAPS_API_KEY_HERE')
ON DUPLICATE KEY UPDATE value = VALUES(value);

-- Verify the insert:
-- SELECT key_name, LEFT(value,8) AS key_preview FROM app_settings WHERE key_name = 'google_directions_api_key';
