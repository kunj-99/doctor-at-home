package com.infowave.thedoctorathomeuser.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.thedoctorathomeuser.ApiConfig;
import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.adapter.VetHistoryAdapter;

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
 * Vet (Animal) appointment history with section headers & live silent refresh.
 * No changes in adapter or other files are required.
 */
public class VetHistoryFragment extends Fragment {

    private static final long POLL_INTERVAL_MS = 6000L;
    private static final String REQ_TAG = "vet_history_poll";

    // UI
    private RecyclerView rvVetHistory;
    private ProgressBar pbVetHistory;
    private SwipeRefreshLayout srlVetHistory;

    // Networking
    private RequestQueue volleyQueue;
    private String vetHistoryUrl;
    private String patientId;

    // Data lists for Vet cards (kept exactly as adapter expects)
    private final List<Integer>   vetIds               = new ArrayList<>();
    private final List<String>    vetNames             = new ArrayList<>();
    private final List<String>    vetSpecialties       = new ArrayList<>();
    private final List<String>    vetAppointmentDates  = new ArrayList<>();
    private final List<String>    vetAppointmentFees   = new ArrayList<>();
    private final List<String>    vetProfilePhotos     = new ArrayList<>();
    private final List<Integer>   vetAppointmentIds    = new ArrayList<>();
    private final List<String>    vetAppointmentStates = new ArrayList<>();

    // Section headers & epochs (parallel to items)
    private final List<String> sectionHeaders = new ArrayList<>();
    private final List<Long>   epochs         = new ArrayList<>();

    private VetHistoryAdapter vetHistoryAdapter;

    // Live refresh
    private final Handler liveHandler = new Handler();
    private Runnable liveRunnable;
    private boolean isPolling = false;

    // Lightweight diff
    private String lastSig = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vet_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        // Bind views
        rvVetHistory  = root.findViewById(R.id.recyclerView);
        pbVetHistory  = root.findViewById(R.id.progressBar_history);
        srlVetHistory = root.findViewById(R.id.swipeRefreshHistory);

        // Recycler
        rvVetHistory.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Request queue
        volleyQueue = Volley.newRequestQueue(requireContext());

        // Logged-in patient
        SharedPreferences sp = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        patientId = sp.getString("patient_id", null);
        if (patientId == null || patientId.trim().isEmpty()) {
            // Silent failure as requested
            return;
        }

        // Endpoint
        vetHistoryUrl = ApiConfig.endpoint("get_vet_history.php", "patient_id", patientId);

        // Adapter
        vetHistoryAdapter = new VetHistoryAdapter(
                requireContext(),
                patientId,
                vetIds,
                vetNames,
                vetSpecialties,
                vetAppointmentDates,
                vetAppointmentFees,
                vetProfilePhotos,
                vetAppointmentIds,
                vetAppointmentStates
        );
        rvVetHistory.setAdapter(vetHistoryAdapter);

        // Section headers via ItemDecoration (no adapter change)
        rvVetHistory.addItemDecoration(new SectionHeaderDecoration(
                sectionHeaders,
                dp(14),  // top margin above header
                dp(28),  // header height
                dp(16),  // left padding for text
                dp(8),   // bottom space under each item
                0xFFEFF2F7, // bg color (light)
                0xFF334155  // text color (slate-ish)
        ));

        // Pull-to-refresh (silent)
        srlVetHistory.setOnRefreshListener(() -> fetchData(false, true));

        // Initial load (silent UI except loader)
        fetchData(true, true);

        // Live runnable
        liveRunnable = new Runnable() {
            @Override public void run() {
                if (!isAdded()) return;
                fetchData(false, true);      // silent periodic refresh
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
        if (volleyQueue != null) volleyQueue.cancelAll(REQ_TAG);
    }

    private void setLoading(boolean show) {
        if (pbVetHistory != null) pbVetHistory.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show && srlVetHistory != null && srlVetHistory.isRefreshing()) srlVetHistory.setRefreshing(false);
    }

    /**
     * @param showLoader  true ⇒ ProgressBar दिखाएं (पहला लोड)
     * @param silent      true ⇒ कोई Toast/Log नहीं (हम यहाँ purely silent रख रहे हैं)
     */
    private void fetchData(boolean showLoader, boolean silent) {
        setLoading(showLoader);

        @SuppressLint("NotifyDataSetChanged")
        StringRequest req = new StringRequest(
                Request.Method.GET,
                vetHistoryUrl,
                resp -> {
                    setLoading(false);
                    try {
                        JSONObject root = new JSONObject(resp);
                        if (!root.optBoolean("success", false)) { applyEmpty(); return; }
                        JSONArray arr = root.optJSONArray("appointments");
                        if (arr == null || arr.length() == 0) { applyEmpty(); return; }

                        // temp
                        List<Integer> tApptIds = new ArrayList<>();
                        List<Integer> tVetIds  = new ArrayList<>();
                        List<String>  tNames   = new ArrayList<>();
                        List<String>  tSpecs   = new ArrayList<>();
                        List<String>  tDates   = new ArrayList<>();
                        List<String>  tFees    = new ArrayList<>();
                        List<String>  tPhotos  = new ArrayList<>();
                        List<String>  tStates  = new ArrayList<>();
                        List<Long>    sortKeys = new ArrayList<>();

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);

                            int apptId = obj.optInt("appointment_id", 0);
                            int vetId  = obj.optInt("doctor_id", 0); // vet == doctor on backend
                            String name = obj.optString("doctor_name", "");
                            String spec = obj.optString("doctor_specialty", obj.optString("specialty",""));
                            String date = obj.optString("appointment_date", "");
                            String time = obj.optString("time_slot", "");
                            String fee  = obj.optString("fee", "0");
                            String photo= obj.optString("doctor_profile_picture", obj.optString("profile_picture",""));
                            String state= obj.optString("status", "");

                            tApptIds.add(apptId);
                            tVetIds.add(vetId);
                            tNames.add(name);
                            tSpecs.add(spec);
                            tDates.add(date);
                            tFees.add(formatFee(fee));
                            tPhotos.add(safePhotoUrl(photo));
                            tStates.add(state);

                            sortKeys.add(buildSortKey(date, time));
                        }

                        // order latest-first
                        List<Integer> order = orderDesc(sortKeys, tApptIds);

                        // final ordered lists
                        List<Integer> fApptIds = new ArrayList<>();
                        List<Integer> fVetIds  = new ArrayList<>();
                        List<String>  fNames   = new ArrayList<>();
                        List<String>  fSpecs   = new ArrayList<>();
                        List<String>  fDates   = new ArrayList<>();
                        List<String>  fFees    = new ArrayList<>();
                        List<String>  fPhotos  = new ArrayList<>();
                        List<String>  fStates  = new ArrayList<>();
                        List<Long>    fEpochs  = new ArrayList<>();

                        for (int idx : order) {
                            fApptIds.add(tApptIds.get(idx));
                            fVetIds.add(tVetIds.get(idx));
                            fNames.add(tNames.get(idx));
                            fSpecs.add(tSpecs.get(idx));
                            fDates.add(tDates.get(idx));
                            fFees.add(tFees.get(idx));
                            fPhotos.add(tPhotos.get(idx));
                            fStates.add(tStates.get(idx));
                            fEpochs.add(sortKeys.get(idx));
                        }

                        // section headers
                        List<String> fHeaders = buildSectionHeaders(fEpochs);

                        boolean changed = applyIfChanged(
                                fVetIds, fNames, fSpecs, fDates, fFees, fPhotos, fApptIds, fStates,
                                fHeaders, fEpochs
                        );

                        if (changed) {
                            rvVetHistory.invalidateItemDecorations();
                            rvVetHistory.setAlpha(0f);
                            rvVetHistory.animate().alpha(1f).setDuration(220).start();
                        }

                    } catch (JSONException ignored) {
                        // silent
                    }
                },
                err -> {
                    setLoading(false);
                    // silent
                }
        );
        req.setTag(REQ_TAG);
        volleyQueue.add(req);
    }

    /* ---------- Apply / Diff ---------- */

    @SuppressLint("NotifyDataSetChanged")
    private boolean applyIfChanged(List<Integer> newVetIds,
                                   List<String> newVetNames,
                                   List<String> newVetSpecialties,
                                   List<String> newAppointmentDates,
                                   List<String> newAppointmentFees,
                                   List<String> newVetPhotos,
                                   List<Integer> newAppointmentIds,
                                   List<String> newAppointmentStates,
                                   List<String> newSectionHeaders,
                                   List<Long>   newEpochs) {

        String sig = buildSig(newAppointmentIds, newAppointmentStates);
        if (sig.equals(lastSig)) return false;

        vetIds.clear();               vetIds.addAll(newVetIds);
        vetNames.clear();             vetNames.addAll(newVetNames);
        vetSpecialties.clear();       vetSpecialties.addAll(newVetSpecialties);
        vetAppointmentDates.clear();  vetAppointmentDates.addAll(newAppointmentDates);
        vetAppointmentFees.clear();   vetAppointmentFees.addAll(newAppointmentFees);
        vetProfilePhotos.clear();     vetProfilePhotos.addAll(newVetPhotos);
        vetAppointmentIds.clear();    vetAppointmentIds.addAll(newAppointmentIds);
        vetAppointmentStates.clear(); vetAppointmentStates.addAll(newAppointmentStates);

        sectionHeaders.clear();       sectionHeaders.addAll(newSectionHeaders);
        epochs.clear();               epochs.addAll(newEpochs);

        if (vetHistoryAdapter != null) vetHistoryAdapter.notifyDataSetChanged();
        lastSig = sig;
        return true;
    }

    private void applyEmpty() {
        vetIds.clear(); vetNames.clear(); vetSpecialties.clear();
        vetAppointmentDates.clear(); vetAppointmentFees.clear();
        vetProfilePhotos.clear(); vetAppointmentIds.clear(); vetAppointmentStates.clear();
        sectionHeaders.clear(); epochs.clear();
        if (vetHistoryAdapter != null) vetHistoryAdapter.notifyDataSetChanged();
        lastSig = "";
        if (rvVetHistory != null) rvVetHistory.invalidateItemDecorations();
    }

    private String buildSig(List<Integer> ids, List<String> statuses) {
        StringBuilder sb = new StringBuilder(ids.size() * 12);
        for (int i = 0; i < ids.size(); i++) {
            sb.append(ids.get(i)).append('|')
                    .append(safe(i < statuses.size() ? statuses.get(i) : "")).append(';');
        }
        return sb.toString();
    }

    /* ---------- Formatting helpers ---------- */

    private String safePhotoUrl(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return ApiConfig.endpoint("doctor_images/default.png");
        }
        return raw;
    }

    private String formatFee(String rawFee) {
        String fee = (rawFee == null || rawFee.trim().isEmpty()) ? "0" : rawFee.trim();
        return "₹ " + fee + " /-";
    }

    /* ---------- Date/Section helpers ---------- */

    private enum Section { TODAY, YESTERDAY, LAST7, EARLIER }

    private List<String> buildSectionHeaders(List<Long> orderedEpochs) {
        List<String> headers = new ArrayList<>(orderedEpochs.size());
        Section prev = null;
        for (int i = 0; i < orderedEpochs.size(); i++) {
            Section s = classifySection(orderedEpochs.get(i));
            if (i == 0 || s != prev) {
                headers.add(sectionLabel(s));
                prev = s;
            } else {
                headers.add("");
            }
        }
        return headers;
    }

    private Section classifySection(long epoch) {
        if (epoch <= 0) return Section.EARLIER;
        long now = System.currentTimeMillis();
        long dayMs = 24L * 60 * 60 * 1000;
        long startToday = startOfDay(now);
        long startYesterday = startToday - dayMs;
        long start7 = startToday - (7L * dayMs);

        if (epoch >= startToday) return Section.TODAY;
        if (epoch >= startYesterday && epoch < startToday) return Section.YESTERDAY;
        if (epoch >= start7 && epoch < startYesterday) return Section.LAST7;
        return Section.EARLIER;
    }

    private String sectionLabel(Section s) {
        switch (s) {
            case TODAY: return "Today";
            case YESTERDAY: return "Yesterday";
            case LAST7: return "Last 7 days";
            default: return "Earlier";
        }
    }

    private long startOfDay(long ts) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Build epoch from date+time with multiple formats. */
    private long buildSortKey(String dateText, String timeText) {
        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd hh:mm a",
                "yyyy-MM-dd",
                "dd-MM-yyyy HH:mm:ss",
                "dd-MM-yyyy hh:mm a",
                "dd-MM-yyyy"
        };
        String candidate = (safe(dateText) + " " + safe(timeText)).trim();
        for (String p : patterns) {
            try {
                SimpleDateFormat df = new SimpleDateFormat(p, Locale.getDefault());
                Date d = df.parse(candidate);
                if (d != null) return d.getTime();
            } catch (ParseException ignored) {}
        }
        // fallback on date only
        for (String p : patterns) {
            if (!p.contains(" ")) {
                try {
                    SimpleDateFormat df = new SimpleDateFormat(p, Locale.getDefault());
                    Date d = df.parse(safe(dateText));
                    if (d != null) return d.getTime();
                } catch (ParseException ignored) {}
            }
        }
        return 0L;
    }

    /** order indices by (epoch desc, id desc) */
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

    private static String safe(String s) { return s == null ? "" : s.trim(); }

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }

    /* ================== Section Header ItemDecoration ================== */

    private static class SectionHeaderDecoration extends RecyclerView.ItemDecoration {
        private final List<String> headers; // per-position header text or ""
        private final int topMargin;
        private final int headerHeight;
        private final int leftPadding;
        private final int bottomSpace;
        private final int bgColor;
        private final int textColor;

        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        SectionHeaderDecoration(List<String> headers,
                                int topMargin, int headerHeight, int leftPadding, int bottomSpace,
                                int bgColor, int textColor) {
            this.headers = headers;
            this.topMargin = topMargin;
            this.headerHeight = headerHeight;
            this.leftPadding = leftPadding;
            this.bottomSpace = bottomSpace;
            this.bgColor = bgColor;
            this.textColor = textColor;

            bgPaint.setColor(bgColor);
            textPaint.setColor(textColor);
            textPaint.setTextSize(40f);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int pos = parent.getChildAdapterPosition(view);
            if (pos == RecyclerView.NO_POSITION) return;

            String h = (pos < headers.size()) ? headers.get(pos) : "";
            if (h != null && !h.isEmpty()) {
                outRect.top = topMargin + headerHeight;
                outRect.bottom = bottomSpace;
            } else {
                outRect.top = 0;
                outRect.bottom = bottomSpace;
            }
        }

        @Override
        public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);
                int pos = parent.getChildAdapterPosition(child);
                if (pos == RecyclerView.NO_POSITION) continue;

                String header = (pos < headers.size()) ? headers.get(pos) : "";
                if (header == null || header.isEmpty()) continue;

                int left = parent.getPaddingLeft();
                int right = parent.getWidth() - parent.getPaddingRight();
                int top = child.getTop() - headerHeight - topMargin;
                int bottom = top + headerHeight;

                // Background strip
                c.drawRect(left, top, right, bottom, bgPaint);

                // Text baseline
                Paint.FontMetrics fm = textPaint.getFontMetrics();
                float textY = top + (headerHeight - fm.bottom + fm.top) / 2f - fm.top;

                c.drawText(header, left + leftPadding, textY, textPaint);
            }
        }
    }
}
