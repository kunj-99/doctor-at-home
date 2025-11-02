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
import com.infowave.thedoctorathomeuser.VetAppointment;
import com.infowave.thedoctorathomeuser.adapter.VetOngoingAdapter;

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
 * Vet/Animal Ongoing tab.
 * - Uses VetOngoingAdapter.
 * - Silent networking, pull-to-refresh, light polling.
 * - Diff signature to minimize UI churn.
 */
public class VetOngoingFragment extends Fragment {

    private static final long POLL_INTERVAL_MS = 3000L; // 3s
    private static final String REQ_TAG = "vet_ongoing_poll";

    // UI
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progress;

    // Net
    private RequestQueue queue;
    private String apiUrl;
    private String patientId;

    // Adapter + data
    private VetOngoingAdapter adapter;
    private final List<VetAppointment> items = new ArrayList<>();

    // Polling
    private final Handler liveHandler = new Handler();
    private Runnable liveRunnable;
    private boolean isPolling = false;

    // Diff
    private String lastSig = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vet_ongoing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        recyclerView = root.findViewById(R.id.recyclerView);
        swipeRefresh = root.findViewById(R.id.swipeRefresh);
        progress     = root.findViewById(R.id.progressBar);

        // Recycler smoothness
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setItemPrefetchEnabled(true);
        recyclerView.setLayoutManager(lm);
        recyclerView.setHasFixedSize(true);

        // Disable change animations to avoid jank
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
        if (patientId.trim().isEmpty()) return;

        apiUrl = ApiConfig.endpoint("getOngoingAppointment.php");

        adapter = new VetOngoingAdapter(requireContext(), items);
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

                        // temp vet rows + sort keys
                        List<VetAppointment> tItems = new ArrayList<>();
                        List<Long> sortKeys = new ArrayList<>();
                        List<Integer> idOrder = new ArrayList<>(); // tie-breaker

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject a = arr.getJSONObject(i);

                            int isVet = a.optInt("is_vet_case", 0);
                            if (isVet != 1) continue; // skip humans

                            String animalName   = a.optString("animal_name", "");
                            String reason       = a.optString("reason_for_visit", "");
                            String doctor       = a.optString("doctor_name", "");
                            String date         = a.optString("appointment_date", "");
                            String time         = a.optString("time_slot", "");
                            String whenLabel    = a.optString("when_text", "");
                            String fee          = a.optString("fee_text", a.optString("fee",""));
                            String status       = a.optString("status", "");
                            String img          = a.optString("profile_picture", "");

                            // NEW: IDs used for "Track Doctor"
                            String doctorId     = a.optString("doctor_id",
                                    a.optString("doctorId", "")); // alternate key safety
                            String appointmentId= a.optString("appointment_id",
                                    a.optString("appt_id", ""));  // alternate key safety

                            String title = (animalName == null || animalName.trim().isEmpty()) ? "Pet" : animalName.trim();
                            String when  = !isEmpty(whenLabel) ? whenLabel : (safe(date) + (isEmpty(time) ? "" : (" • " + safe(time))));
                            String price = !isEmpty(fee) ? ("₹" + fee) : "";

                            tItems.add(new VetAppointment(
                                    title,
                                    safe(reason),
                                    safe(doctor),
                                    safe(when),
                                    price,
                                    safe(status),
                                    safe(img),
                                    safe(doctorId),
                                    safe(appointmentId)
                            ));

                            sortKeys.add(buildSortKey(date, time));
                            idOrder.add(i);
                        }

                        // order latest-first
                        List<Integer> order = orderDesc(sortKeys, idOrder);

                        // final ordered list
                        List<VetAppointment> fItems = new ArrayList<>(order.size());
                        for (int idx : order) fItems.add(tItems.get(idx));

                        boolean changed = applyIfChanged(fItems);
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
        items.clear();
        if (adapter != null) adapter.notifyDataSetChanged();
        lastSig = "";
    }

    @SuppressLint("NotifyDataSetChanged")
    private boolean applyIfChanged(List<VetAppointment> newItems) {
        String sig = buildSig(newItems);
        if (sig.equals(lastSig)) return false;

        items.clear();
        items.addAll(newItems);
        if (adapter != null) adapter.notifyDataSetChanged();
        lastSig = sig;
        return true;
    }

    private String buildSig(List<VetAppointment> list) {
        StringBuilder sb = new StringBuilder(list.size() * 24);
        for (VetAppointment v : list) {
            // Key fields that affect presentation/order
            sb.append(safe(v.getVetName())).append('|')
                    .append(safe(v.getWhen())).append('|')
                    .append(safe(v.getPetTitle())).append('|')
                    .append(safe(v.getStatus())).append(';');
        }
        return sb.toString();
    }

    /* ---- helpers ---- */

    private static boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }
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
