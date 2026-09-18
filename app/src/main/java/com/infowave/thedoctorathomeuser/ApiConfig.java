package com.infowave.thedoctorathomeuser;

public class ApiConfig {

    // 🌐 Base URL (change only here)
    public static final String BASE_URL = "https://thedoctorathome.in/";

    // Server-side route calculation endpoint. Google Directions API key stays on backend.
    public static final String ROUTE_DISTANCE = BASE_URL + "get_route_distance.php";

    // ─── Doctor Booking Lock APIs (double-booking prevention) ───────────────
    /** Call BEFORE opening booking form — reserves the doctor slot for 10 min */
    public static final String RESERVE_DOCTOR  = BASE_URL + "reserve_doctor.php";
    /** Call on back/cancel to release the hold immediately */
    public static final String RELEASE_DOCTOR_LOCK = BASE_URL + "release_doctor_lock.php";

    // 👉 Simple endpoint (no params)
    // Example: ApiConfig.endpoint("login.php")
    public static String endpoint(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        return BASE_URL + path;
    }

    public static String endpoint(String path, String... params) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        StringBuilder url = new StringBuilder(BASE_URL).append(path);

        if (params != null && params.length > 0) {
            if (params.length % 2 != 0) {
                throw new IllegalArgumentException("Params must be key-value pairs");
            }

            url.append("?");
            for (int i = 0; i < params.length; i += 2) {
                if (i > 0) url.append("&");
                url.append(params[i]).append("=").append(params[i + 1]);
            }
        }
        return url.toString();
    }
}
