package com.infowave.thedoctorathomeuser;

public class ApiConfig {

    // 🌐 Base URL (change only here)
    public static final String BASE_URL = "http://sxm.a58.mytemp.website/";

    // 👉 Simple endpoint (no params)
    // Example: ApiConfig.endpoint("login.php")
    public static String endpoint(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        return BASE_URL + path;
    }

    // 👉 Endpoint with params (key-value pairs)
    // Example: ApiConfig.endpoint("gethistory.php", "user_id", "10")
    // Result : http://thedoctorathome.in/api/gethistory.php?user_id=10

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
