package com.infowave.thedoctorathomeuser.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.infowave.thedoctorathomeuser.ApiConfig;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * One list-level doctor availability poller.
 *
 * Replaces the old per-ViewHolder polling pattern where every visible doctor card
 * called checkDoctorAppointment.php independently every 2-5 seconds.
 *
 * The authoritative reserve_doctor.php check is still performed when the user taps
 * Book Appointment, so a 10-second display refresh cannot create a double booking.
 */
public final class DoctorAvailabilityBatchPoller {

    public static final long DEFAULT_POLL_INTERVAL_MS = 10_000L;

    public interface DoctorIdProvider {
        @NonNull List<String> getDoctorIds();
    }

    public interface Listener {
        void onStatuses(@NonNull Map<String, Status> statuses);
    }

    public static final class Status {
        public final int doctorId;
        public final boolean hasActiveAppointment;
        public final boolean hasConfirmedAppointment;
        public final boolean hasHeldLock;
        public final int requestCount;
        public final int pendingCount;
        public final int totalEtaMinutes;

        private Status(int doctorId,
                       boolean hasActiveAppointment,
                       boolean hasConfirmedAppointment,
                       boolean hasHeldLock,
                       int requestCount,
                       int pendingCount,
                       int totalEtaMinutes) {
            this.doctorId = doctorId;
            this.hasActiveAppointment = hasActiveAppointment;
            this.hasConfirmedAppointment = hasConfirmedAppointment;
            this.hasHeldLock = hasHeldLock;
            this.requestCount = requestCount;
            this.pendingCount = pendingCount;
            this.totalEtaMinutes = totalEtaMinutes;
        }

        static Status fromJson(@NonNull JSONObject obj) {
            return new Status(
                    obj.optInt("doctor_id", 0),
                    obj.optBoolean("has_active_appointment", false),
                    obj.optBoolean("has_confirmed_appointment", false),
                    obj.optBoolean("has_held_lock", false),
                    obj.optInt("request_count", 0),
                    obj.optInt("pending_count", 0),
                    obj.optInt("total_eta", 0)
            );
        }
    }

    private final RequestQueue requestQueue;
    private final DoctorIdProvider doctorIdProvider;
    private final Listener listener;
    private final long pollIntervalMs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Object requestTag = new Object();

    private boolean started = false;
    private boolean requestInFlight = false;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!started) return;
            fetchOnce();
            handler.postDelayed(this, pollIntervalMs);
        }
    };

    public DoctorAvailabilityBatchPoller(@NonNull Context context,
                                         @NonNull DoctorIdProvider doctorIdProvider,
                                         @NonNull Listener listener) {
        this(context, doctorIdProvider, listener, DEFAULT_POLL_INTERVAL_MS);
    }

    public DoctorAvailabilityBatchPoller(@NonNull Context context,
                                         @NonNull DoctorIdProvider doctorIdProvider,
                                         @NonNull Listener listener,
                                         long pollIntervalMs) {
        Context appContext = context.getApplicationContext();
        this.requestQueue = VolleySingleton.getInstance(appContext).getRequestQueue();
        this.doctorIdProvider = doctorIdProvider;
        this.listener = listener;
        this.pollIntervalMs = Math.max(5_000L, pollIntervalMs);
    }

    public void start() {
        if (started) return;
        started = true;
        handler.removeCallbacks(pollRunnable);
        handler.post(pollRunnable);
    }

    public void stop() {
        started = false;
        requestInFlight = false;
        handler.removeCallbacks(pollRunnable);
        requestQueue.cancelAll(requestTag);
    }

    public void refreshNow() {
        if (!started) return;
        fetchOnce();
    }

    private void fetchOnce() {
        if (!started || requestInFlight) return;

        List<String> provided = doctorIdProvider.getDoctorIds();
        if (provided == null || provided.isEmpty()) return;

        Set<String> uniqueIds = new LinkedHashSet<>();
        for (String raw : provided) {
            if (raw == null) continue;
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) continue;
            try {
                int id = Integer.parseInt(trimmed);
                if (id > 0) uniqueIds.add(String.valueOf(id));
            } catch (NumberFormatException ignored) {
                // Ignore malformed IDs rather than failing the whole list refresh.
            }
        }

        if (uniqueIds.isEmpty()) return;

        List<String> ids = new ArrayList<>(uniqueIds);
        if (ids.size() > 250) {
            ids = new ArrayList<>(ids.subList(0, 250));
        }

        String csv = join(ids, ",");
        String url = ApiConfig.endpoint(
                "checkDoctorAppointmentBatch.php",
                "doctor_ids", csv
        );

        requestInFlight = true;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    requestInFlight = false;
                    if (!started) return;

                    JSONObject data = response.optJSONObject("data");
                    if (!response.optBoolean("success", false) || data == null) {
                        return;
                    }

                    Map<String, Status> result = new HashMap<>();
                    Iterator<String> keys = data.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        JSONObject item = data.optJSONObject(key);
                        if (item != null) {
                            result.put(key, Status.fromJson(item));
                        }
                    }

                    listener.onStatuses(Collections.unmodifiableMap(result));
                },
                error -> requestInFlight = false
        );

        request.setTag(requestTag);
        request.setShouldCache(false);
        request.setRetryPolicy(new DefaultRetryPolicy(
                8_000,
                0,
                1.0f
        ));
        requestQueue.add(request);
    }

    private static String join(@NonNull List<String> values, @NonNull String separator) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) out.append(separator);
            out.append(values.get(i));
        }
        return out.toString();
    }
}

// Last Updated: 2026-09-17 14:11 IST
