package com.infowave.thedoctorathomeuser.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log; // ✅ LOGS
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.thedoctorathomeuser.ApiConfig;
import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.adapter.OngoingAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HumanOngoingFragment extends Fragment {

    private static final String TAG = "HumanOngoingFragment"; // ✅
    private static final long POLL_INTERVAL_MS = 3000L;
    private static final String REQ_TAG = "human_ongoing_poll";

    // UI
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progress;

    // Net
    private RequestQueue queue;
    private String apiUrl;
    private String patientId;

    // Adapter + lists
    private OngoingAdapter adapter;
    private final List<String>  doctorNames       = new ArrayList<>();
    private final List<String>  specialties       = new ArrayList<>();
    private final List<String>  hospitals         = new ArrayList<>();
    private final List<Float>   ratings           = new ArrayList<>();
    private final List<String>  profilePictures   = new ArrayList<>();
    private final List<Integer> appointmentIds    = new ArrayList<>();
    private final List<String>  statuses          = new ArrayList<>();
    private final List<String>  durations         = new ArrayList<>();   // "6 Years"
    private final List<Integer> doctorIds         = new ArrayList<>();
    private final List<Integer> experienceYears   = new ArrayList<>();   // numeric
    private final List<String>  apptTimeDisplays  = new ArrayList<>();   // "Thu, 09 Oct • 02:15 PM"
    private final List<Float>   amounts           = new ArrayList<>();   // 650f

    // local
    private final Handler liveHandler = new Handler();
    private Runnable liveRunnable;
    private boolean isPolling = false;
    private String lastSig = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_human_ongoing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        recyclerView = root.findViewById(R.id.recyclerView);
        swipeRefresh = root.findViewById(R.id.swipeRefresh);
        progress     = root.findViewById(R.id.progressBar);

        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setItemPrefetchEnabled(true);
        recyclerView.setLayoutManager(lm);
        recyclerView.setHasFixedSize(true);

        RecyclerView.ItemAnimator ia = recyclerView.getItemAnimator();
        if (ia instanceof DefaultItemAnimator) {
            ((DefaultItemAnimator) ia).setSupportsChangeAnimations(false);
        } else {
            recyclerView.setItemAnimator(null);
        }

        queue = Volley.newRequestQueue(requireContext());

        SharedPreferences sp = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");
        if (patientId == null) patientId = "";
        if (patientId.trim().isEmpty()) {
            Log.w(TAG, "patient_id is empty; aborting load");
            return;
        }

        apiUrl = ApiConfig.endpoint("getOngoingAppointment.php");
        Log.d(TAG, "API URL -> " + apiUrl);

        // Adapter with extended signature (time + amount)
        adapter = new OngoingAdapter(
                requireContext(),
                doctorNames, specialties, hospitals, ratings, profilePictures,
                appointmentIds, statuses, durations,    doctorIds,
                experienceYears,
                apptTimeDisplays,
                amounts
        );
        recyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(() -> fetch(false));

        setLoading(true);
        fetch(true);

        liveRunnable = new Runnable() {
            @Override public void run() {
                if (!isAdded()) return;
                fetch(false);
                liveHandler.postDelayed(this, POLL_INTERVAL_MS);
            }
        };
    }

    @Override public void onResume() {
        super.onResume();
        if (!isPolling && liveRunnable != null) {
            isPolling = true;
            liveHandler.postDelayed(liveRunnable, POLL_INTERVAL_MS);
        }
    }

    @Override public void onPause() {
        super.onPause();
        if (isPolling) {
            isPolling = false;
            liveHandler.removeCallbacks(liveRunnable);
        }
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (queue != null) queue.cancelAll(REQ_TAG);
    }

    private void setLoading(boolean show) {
        if (progress != null) progress.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show && swipeRefresh != null && swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetch(boolean firstLoad) {
        Log.d(TAG, "fetch(firstLoad=" + firstLoad + ") sending request...");
        StringRequest req = new StringRequest(
                Request.Method.POST,
                apiUrl,
                resp -> {
                    setLoading(false);
                    // --- RAW RESPONSE LOG (trim to avoid logcat spam) ---
                    String preview = resp == null ? "null" : resp.substring(0, Math.min(resp.length(), 800));
                    Log.d(TAG, "Response(≤800): " + preview);

                    try {
                        JSONObject json = new JSONObject(resp);
                        boolean ok = json.optBoolean("success", false);
                        Log.d(TAG, "JSON success=" + ok);
                        if (!ok) { applyEmpty(); return; }

                        JSONArray arr = json.optJSONArray("appointments");
                        int len = (arr == null) ? 0 : arr.length();
                        Log.d(TAG, "appointments length=" + len);
                        if (len == 0) { applyEmpty(); return; }

                        // temp
                        List<Integer> tApptIds   = new ArrayList<>();
                        List<Integer> tDocIds    = new ArrayList<>();
                        List<String>  tNames     = new ArrayList<>();
                        List<String>  tSpecs     = new ArrayList<>();
                        List<String>  tHosp      = new ArrayList<>();
                        List<Float>   tRating    = new ArrayList<>();
                        List<String>  tPics      = new ArrayList<>();
                        List<String>  tStats     = new ArrayList<>();
                        List<String>  tDur       = new ArrayList<>();
                        List<Integer> tExpYears  = new ArrayList<>();
                        List<String>  tTimeDisp  = new ArrayList<>();
                        List<Float>   tAmounts   = new ArrayList<>();
                        List<Long>    sortKeys   = new ArrayList<>();

                        for (int i = 0; i < len; i++) {
                            JSONObject a = arr.getJSONObject(i);

                            // LOG keys existence for debugging time/amount
                            boolean hasTimeDisp = a.has("appointment_time_display");
                            boolean hasAmount   = a.has("amount");
                            Log.d(TAG, "idx=" + i
                                    + " appt_id=" + a.optInt("appointment_id", -1)
                                    + " has(appointment_time_display)=" + hasTimeDisp
                                    + " has(amount)=" + hasAmount
                                    + " raw_time='" + a.optString("appointment_time_display", "") + "'"
                                    + " raw_amount=" + a.optDouble("amount", -1.0));

                            if (a.optInt("is_vet_case", 0) == 1) {
                                Log.d(TAG, "idx=" + i + " skipped (is_vet_case=1)");
                                continue;
                            }

                            int apptId = a.optInt("appointment_id", 0);
                            int docId  = a.optInt("doctor_id", 0);
                            String date = a.optString("appointment_date", "");
                            String time = a.optString("time_slot", "");

                            tApptIds.add(apptId);
                            tDocIds.add(docId);
                            tNames.add(a.optString("doctor_name", ""));
                            tSpecs.add(a.optString("specialty", ""));
                            // hospital chip
                            tHosp.add(a.optString("hospital_name", ""));

                            tRating.add((float) a.optDouble("rating", 0.0));

                            String pic = a.optString("profile_picture", "");
                            if (pic == null || pic.trim().isEmpty()) {
                                pic = ApiConfig.endpoint("doctor_images/default.png");
                            }
                            tPics.add(pic);

                            tStats.add(a.optString("status", ""));

                            // Experience text ("6 Years") — built from number + unit
                            int number = a.optInt("experience_years", 0);
                            String unit = safe(a.optString("experience_duration", "")); // "Years"/"Months"
                            tExpYears.add(number);
                            String finalDuration;
                            if (number > 0 && unit.length() > 0) {
                                finalDuration = number + " " + unit;
                            } else if (unit.length() > 0) {
                                finalDuration = unit;
                            } else if (number > 0) {
                                finalDuration = number + " Years";
                            } else {
                                finalDuration = "";
                            }
                            tDur.add(finalDuration);

                            // ✅ FIXED: read actual keys from PHP
                            String disp = a.optString("appointment_time_display", "");
                            float amt   = (float) a.optDouble("amount", 0.0);
                            tTimeDisp.add(disp);
                            tAmounts.add(amt);

                            Log.d(TAG, "idx=" + i + " parsed -> timeDisp='" + disp + "', amount=" + amt);

                            sortKeys.add(buildSortKey(date, time));
                        }

                        List<Integer> order = orderDesc(sortKeys, tApptIds);

                        // final
                        List<Integer> fApptIds   = new ArrayList<>();
                        List<Integer> fDocIds    = new ArrayList<>();
                        List<String>  fNames     = new ArrayList<>();
                        List<String>  fSpecs     = new ArrayList<>();
                        List<String>  fHosp      = new ArrayList<>();
                        List<Float>   fRating    = new ArrayList<>();
                        List<String>  fPics      = new ArrayList<>();
                        List<String>  fStats     = new ArrayList<>();
                        List<String>  fDur       = new ArrayList<>();
                        List<Integer> fExpYears  = new ArrayList<>();
                        List<String>  fTimeDisp  = new ArrayList<>();
                        List<Float>   fAmounts   = new ArrayList<>();

                        for (int idx : order) {
                            fApptIds.add(tApptIds.get(idx));
                            fDocIds.add(tDocIds.get(idx));
                            fNames.add(tNames.get(idx));
                            fSpecs.add(tSpecs.get(idx));
                            fHosp.add(tHosp.get(idx));
                            fRating.add(tRating.get(idx));
                            fPics.add(tPics.get(idx));
                            fStats.add(tStats.get(idx));
                            fDur.add(tDur.get(idx));
                            fExpYears.add(tExpYears.get(idx));
                            fTimeDisp.add(tTimeDisp.get(idx));
                            fAmounts.add(tAmounts.get(idx));
                        }

                        Log.d(TAG, "final sizes -> names=" + fNames.size()
                                + " timeDisp=" + fTimeDisp.size()
                                + " amounts=" + fAmounts.size());

                        boolean changed = applyIfChanged(
                                fNames, fSpecs, fHosp, fRating, fPics, fApptIds, fStats, fDur, fDocIds,
                                fExpYears, fTimeDisp, fAmounts
                        );

                        Log.d(TAG, "applyIfChanged -> " + changed);
                        if (changed) {
                            recyclerView.setAlpha(0f);
                            recyclerView.animate().alpha(1f).setDuration(180).start();
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parse error: " + e.getMessage(), e);
                    }
                },
                err -> {
                    setLoading(false);
                    Log.e(TAG, "Volley error: " + err, err);
                }
        ) {
            @Override protected java.util.Map<String, String> getParams() {
                java.util.Map<String, String> p = new java.util.HashMap<>();
                p.put("patient_id", patientId);
                Log.d(TAG, "getParams -> patient_id=" + patientId);
                return p;
            }
            @Override public Priority getPriority() { return Priority.LOW; }
        };
        req.setTag(REQ_TAG);
        queue.add(req);
    }

    private void applyEmpty() {
        Log.w(TAG, "applyEmpty()");
        doctorNames.clear(); specialties.clear(); hospitals.clear(); ratings.clear();
        profilePictures.clear(); appointmentIds.clear(); statuses.clear(); durations.clear(); doctorIds.clear();
        experienceYears.clear(); apptTimeDisplays.clear(); amounts.clear();
        if (adapter != null) adapter.notifyDataSetChanged();
        lastSig = "";
    }

    @SuppressLint("NotifyDataSetChanged")
    private boolean applyIfChanged(List<String> newNames,
                                   List<String> newSpecs,
                                   List<String> newHosp,
                                   List<Float>  newRatings,
                                   List<String> newPics,
                                   List<Integer> newApptIds,
                                   List<String> newStatuses,
                                   List<String> newDurations,
                                   List<Integer> newDoctorIds,
                                   List<Integer> newExperienceYears,
                                   List<String>  newApptTimeDisp,
                                   List<Float>   newAmounts) {
        String sig = buildSig(newApptIds, newStatuses);
        boolean same = sig.equals(lastSig);
        Log.d(TAG, "buildSig same=" + same);
        if (same) return false;

        doctorNames.clear();       doctorNames.addAll(newNames);
        specialties.clear();       specialties.addAll(newSpecs);
        hospitals.clear();         hospitals.addAll(newHosp);
        ratings.clear();           ratings.addAll(newRatings);
        profilePictures.clear();   profilePictures.addAll(newPics);
        appointmentIds.clear();    appointmentIds.addAll(newApptIds);
        statuses.clear();          statuses.addAll(newStatuses);
        durations.clear();         durations.addAll(newDurations);
        doctorIds.clear();         doctorIds.addAll(newDoctorIds);
        experienceYears.clear();   experienceYears.addAll(newExperienceYears);
        apptTimeDisplays.clear();  apptTimeDisplays.addAll(newApptTimeDisp);
        amounts.clear();           amounts.addAll(newAmounts);

        // Extra visibility in logs for first few rows
        for (int i = 0; i < Math.min(3, appointmentIds.size()); i++) {
            Log.d(TAG, "Row " + i
                    + " -> apptId=" + appointmentIds.get(i)
                    + ", timeDisp='" + apptTimeDisplays.get(i) + "'"
                    + ", amount=" + amounts.get(i));
        }

        if (adapter != null) adapter.notifyDataSetChanged();
        lastSig = sig;
        return true;
    }

    private String buildSig(List<Integer> ids, List<String> stats) {
        StringBuilder sb = new StringBuilder(ids.size() * 12);
        for (int i = 0; i < ids.size(); i++) {
            sb.append(ids.get(i)).append('|')
                    .append(safe(i < stats.size() ? stats.get(i) : "")).append(';');
        }
        return sb.toString();
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }

    private long buildSortKey(String yyyyMmDd, String hhmmAMPM) {
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
            Date d = df.parse((yyyyMmDd == null ? "" : yyyyMmDd) + " " + (hhmmAMPM == null ? "" : hhmmAMPM));
            return (d != null) ? d.getTime() : 0L;
        } catch (ParseException e) {
            return 0L;
        }
    }

    private List<Integer> orderDesc(List<Long> keys, List<Integer> ids) {
        int n = keys.size();
        List<Integer> idx = new ArrayList<>(n);
        for (int i = 0; i < n; i++) idx.add(i);
        idx.sort(new Comparator<Integer>() {
            @Override public int compare(Integer a, Integer b) {
                int c = Long.compare(keys.get(b), keys.get(a));
                if (c != 0) return c;
                return Integer.compare(ids.get(b), ids.get(a));
            }
        });
        return idx;
    }
}
