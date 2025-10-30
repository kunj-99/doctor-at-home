package com.infowave.thedoctorathomeuser.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
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

/**
 * Patient/Human Ongoing tab.
 * - Uses OngoingAdapter (no changes required in adapter).
 * - Smooth UI: diff signature, disabled heavy animations, prefetch enabled.
 * - Silent networking: no Toast/logs; pull-to-refresh + light polling.
 */
public class HumanOngoingFragment extends Fragment {

    private static final long POLL_INTERVAL_MS = 3000L; // 3s light polling
    private static final String REQ_TAG = "human_ongoing_poll";

    // UI
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progress;

    // Networking
    private RequestQueue queue;
    private String apiUrl;
    private String patientId;

    // Adapter + backing lists (as OngoingAdapter expects)
    private OngoingAdapter adapter;
    private final List<String>  doctorNames     = new ArrayList<>();
    private final List<String>  specialties     = new ArrayList<>();
    private final List<String>  hospitals       = new ArrayList<>();
    private final List<Float>   ratings         = new ArrayList<>();
    private final List<String>  profilePictures = new ArrayList<>();
    private final List<Integer> appointmentIds  = new ArrayList<>();
    private final List<String>  statuses        = new ArrayList<>();
    private final List<String>  durations       = new ArrayList<>();
    private final List<Integer> doctorIds       = new ArrayList<>();

    // Live refresh
    private final Handler liveHandler = new Handler();
    private Runnable liveRunnable;
    private boolean isPolling = false;

    // Lightweight diff
    private String lastSig = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Reuse a simple list layout: progress + SwipeRefresh + RecyclerView
        return inflater.inflate(R.layout.fragment_human_ongoing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        recyclerView = root.findViewById(R.id.recyclerView);
        swipeRefresh = root.findViewById(R.id.swipeRefresh);
        progress     = root.findViewById(R.id.progressBar);

        // Recycler tuning for smoothness
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setItemPrefetchEnabled(true);
        recyclerView.setLayoutManager(lm);
        recyclerView.setHasFixedSize(true);

        // Disable change animations to reduce jank when lists update
        RecyclerView.ItemAnimator ia = recyclerView.getItemAnimator();
        if (ia instanceof DefaultItemAnimator) {
            ((DefaultItemAnimator) ia).setSupportsChangeAnimations(false);
        } else {
            recyclerView.setItemAnimator(null);
        }

        queue = Volley.newRequestQueue(requireContext());

        // patient id
        SharedPreferences sp = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");
        if (patientId == null) patientId = "";
        if (patientId.trim().isEmpty()) return;

        apiUrl = ApiConfig.endpoint("getOngoingAppointment.php");

        // Adapter
        adapter = new OngoingAdapter(
                requireContext(),
                doctorNames, specialties, hospitals, ratings, profilePictures,
                appointmentIds, statuses, durations, doctorIds
        );
        recyclerView.setAdapter(adapter);

        // Pull-to-refresh
        swipeRefresh.setOnRefreshListener(() -> fetch(false));

        // Initial load
        setLoading(true);
        fetch(true);

        // Polling
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
        StringRequest req = new StringRequest(
                Request.Method.POST,
                apiUrl,
                resp -> {
                    setLoading(false);
                    try {
                        JSONObject json = new JSONObject(resp);
                        if (!json.optBoolean("success", false)) { applyEmpty(); return; }

                        JSONArray arr = json.optJSONArray("appointments");
                        if (arr == null || arr.length() == 0) { applyEmpty(); return; }

                        // temp store (human only)
                        List<Integer> tApptIds = new ArrayList<>();
                        List<Integer> tDocIds  = new ArrayList<>();
                        List<String>  tNames   = new ArrayList<>();
                        List<String>  tSpecs   = new ArrayList<>();
                        List<String>  tHosp    = new ArrayList<>();
                        List<Float>   tRating  = new ArrayList<>();
                        List<String>  tPics    = new ArrayList<>();
                        List<String>  tStats   = new ArrayList<>();
                        List<String>  tDur     = new ArrayList<>();
                        List<Long>    sortKeys = new ArrayList<>();

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject a = arr.getJSONObject(i);

                            int isVet = a.optInt("is_vet_case", 0);
                            if (isVet == 1) continue; // skip vet for this tab

                            int apptId   = a.optInt("appointment_id", 0);
                            int docId    = a.optInt("doctor_id", 0);
                            String date  = a.optString("appointment_date", "");
                            String time  = a.optString("time_slot", "");

                            tApptIds.add(apptId);
                            tDocIds.add(docId);
                            tNames.add(a.optString("doctor_name", ""));
                            tSpecs.add(a.optString("specialty", ""));
                            tHosp.add(a.optString("hospital_name", ""));
                            tRating.add((float) a.optDouble("experience", 0));
                            String pic = a.optString("profile_picture", "");
                            if (pic == null || pic.trim().isEmpty()) {
                                pic = ApiConfig.endpoint("doctor_images/default.png");
                            }
                            tPics.add(pic);
                            tStats.add(a.optString("status", ""));
                            tDur.add(a.optString("experience_duration", ""));
                            sortKeys.add(buildSortKey(date, time));
                        }

                        // order latest-first
                        List<Integer> order = orderDesc(sortKeys, tApptIds);

                        // final ordered lists
                        List<Integer> fApptIds = new ArrayList<>();
                        List<Integer> fDocIds  = new ArrayList<>();
                        List<String>  fNames   = new ArrayList<>();
                        List<String>  fSpecs   = new ArrayList<>();
                        List<String>  fHosp    = new ArrayList<>();
                        List<Float>   fRating  = new ArrayList<>();
                        List<String>  fPics    = new ArrayList<>();
                        List<String>  fStats   = new ArrayList<>();
                        List<String>  fDur     = new ArrayList<>();

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
                        }

                        boolean changed = applyIfChanged(
                                fNames, fSpecs, fHosp, fRating, fPics, fApptIds, fStats, fDur, fDocIds
                        );

                        if (changed) {
                            recyclerView.setAlpha(0f);
                            recyclerView.animate().alpha(1f).setDuration(180).start();
                        }

                    } catch (JSONException ignored) {
                        // silent
                    }
                },
                err -> setLoading(false)
        ) {
            @Override
            protected java.util.Map<String, String> getParams() {
                java.util.Map<String, String> p = new java.util.HashMap<>();
                p.put("patient_id", patientId);
                return p;
            }
            @Override public Priority getPriority() { return Priority.LOW; }
        };
        req.setTag(REQ_TAG);
        queue.add(req);
    }

    private void applyEmpty() {
        doctorNames.clear(); specialties.clear(); hospitals.clear(); ratings.clear();
        profilePictures.clear(); appointmentIds.clear(); statuses.clear(); durations.clear(); doctorIds.clear();
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
                                   List<Integer> newDoctorIds) {

        String sig = buildSig(newApptIds, newStatuses);
        if (sig.equals(lastSig)) return false;

        doctorNames.clear();     doctorNames.addAll(newNames);
        specialties.clear();     specialties.addAll(newSpecs);
        hospitals.clear();       hospitals.addAll(newHosp);
        ratings.clear();         ratings.addAll(newRatings);
        profilePictures.clear(); profilePictures.addAll(newPics);
        appointmentIds.clear();  appointmentIds.addAll(newApptIds);
        statuses.clear();        statuses.addAll(newStatuses);
        durations.clear();       durations.addAll(newDurations);
        doctorIds.clear();       doctorIds.addAll(newDoctorIds);

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

    /* ---- helpers ---- */

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
