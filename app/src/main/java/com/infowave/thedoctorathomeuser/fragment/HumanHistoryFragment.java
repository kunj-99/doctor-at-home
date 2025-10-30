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
import androidx.core.content.ContextCompat;
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
import com.infowave.thedoctorathomeuser.adapter.DoctorHistoryAdapter;

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

public class HumanHistoryFragment extends Fragment {

    private static final long POLL_INTERVAL_MS = 6000L;
    private static final String REQ_TAG = "history_poll";

    private RecyclerView recyclerView;
    private DoctorHistoryAdapter adapter;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private RequestQueue requestQueue;

    private final List<String> doctorNames = new ArrayList<>();
    private final List<String> doctorSpecialties = new ArrayList<>();
    private final List<String> appointmentDates = new ArrayList<>();
    private final List<String> appointmentPrices = new ArrayList<>();
    private final List<String> doctorProfilePictures = new ArrayList<>();
    private final List<Integer> doctorIds = new ArrayList<>();
    private final List<Integer> appointmentIds = new ArrayList<>();
    private final List<String> appointmentStatuses = new ArrayList<>();

    // सेक्शन-हेडर स्टेट (हर पोज़िशन के लिए header text या "")
    private final List<String> sectionHeaders = new ArrayList<>();
    // ऑर्डरिंग/सेक्शन तय करने के लिए epochs
    private final List<Long> epochs = new ArrayList<>();

    private String apiUrl;
    private String patientId;

    // live refresh
    private final Handler liveHandler = new Handler();
    private Runnable liveRunnable;
    private boolean isPolling = false;

    // lightweight diff
    private String lastSig = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_human_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView  = view.findViewById(R.id.recyclerView);
        progressBar   = view.findViewById(R.id.progressBar_history);
        swipeRefresh  = view.findViewById(R.id.swipeRefreshHistory);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        requestQueue = Volley.newRequestQueue(requireContext());

        SharedPreferences sp = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");
        if (patientId == null) patientId = "";
        if (patientId.trim().isEmpty()) return;

        apiUrl = ApiConfig.endpoint("get_history.php", "patient_id", patientId);

        adapter = new DoctorHistoryAdapter(
                requireContext(),
                patientId,
                doctorIds,
                doctorNames,
                doctorSpecialties,
                appointmentDates,
                appointmentPrices,
                doctorProfilePictures,
                appointmentIds,
                appointmentStatuses
        );
        recyclerView.setAdapter(adapter);

        // सेक्शन-हेडर डेकोरेशन (एडॉप्टर में बदलाव नहीं)
        recyclerView.addItemDecoration(new SectionHeaderDecoration(
                sectionHeaders,
                dp(14),           // शीर्ष मार्जिन
                dp(28),           // हेडर ऊँचाई
                dp(16),           // बायां पैडिंग
                dp(8),            // नीचे स्पेस
                0xFFEFF2F7,       // हल्का बैकग्राउंड (कार्ड जैसा)
                0xFF334155        // गहरा टेक्स्ट (slate-700)
        ));

        swipeRefresh.setOnRefreshListener(() -> fetchData(false, true)); // manual silent
        fetchData(true, true); // first load, silent

        liveRunnable = new Runnable() {
            @Override public void run() {
                if (!isAdded()) return;
                fetchData(false, true);
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
        if (requestQueue != null) requestQueue.cancelAll(REQ_TAG);
    }

    private void startLoader(boolean show) {
        if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void stopSwipe() {
        if (swipeRefresh != null && swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
    }

    /**
     * @param showLoader  true ⇒ ProgressBar
     * @param silent      true ⇒ कोई Toast/Log नहीं
     */
    private void fetchData(boolean showLoader, boolean silent) {
        startLoader(showLoader);

        @SuppressLint("NotifyDataSetChanged")
        StringRequest request = new StringRequest(Request.Method.GET, apiUrl,
                response -> {
                    startLoader(false);
                    stopSwipe();
                    try {
                        JSONObject root = new JSONObject(response);
                        if (!root.optBoolean("success", false)) {
                            applyEmpty();
                            return;
                        }

                        JSONArray arr = root.optJSONArray("appointments");
                        if (arr == null || arr.length() == 0) {
                            applyEmpty();
                            return;
                        }

                        // temp store
                        List<Integer> tApptIds = new ArrayList<>();
                        List<Integer> tDocIds  = new ArrayList<>();
                        List<String>  tDocNames= new ArrayList<>();
                        List<String>  tSpecs   = new ArrayList<>();
                        List<String>  tDates   = new ArrayList<>();
                        List<String>  tPrices  = new ArrayList<>();
                        List<String>  tPics    = new ArrayList<>();
                        List<String>  tStats   = new ArrayList<>();
                        List<Long>    sortKeys = new ArrayList<>();

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.getJSONObject(i);

                            int apptId = o.optInt("appointment_id", 0);
                            int docId  = o.optInt("doctor_id", 0);
                            String dnm = o.optString("doctor_name", "");
                            String spc = o.optString("specialty", "");
                            String dt  = o.optString("appointment_date", "");
                            String fee = o.optString("fee", "0");
                            String pic = o.optString("profile_picture", "");
                            String sts = o.optString("status", "");
                            String tms = o.optString("time_slot", "");

                            if (pic == null || pic.trim().isEmpty()) {
                                pic = ApiConfig.endpoint("doctor_images/default.png");
                            }

                            tApptIds.add(apptId);
                            tDocIds.add(docId);
                            tDocNames.add(dnm);
                            tSpecs.add(spc);
                            tDates.add(dt);
                            tPrices.add("₹ " + fee + " /-");
                            tPics.add(pic);
                            tStats.add(sts);

                            sortKeys.add(buildSortKey(dt, tms));
                        }

                        // order indices: latest first
                        List<Integer> order = orderDesc(sortKeys, tApptIds);

                        // final lists (ordered)
                        List<Integer> fApptIds = new ArrayList<>();
                        List<Integer> fDocIds  = new ArrayList<>();
                        List<String>  fDocNames= new ArrayList<>();
                        List<String>  fSpecs   = new ArrayList<>();
                        List<String>  fDates   = new ArrayList<>();
                        List<String>  fPrices  = new ArrayList<>();
                        List<String>  fPics    = new ArrayList<>();
                        List<String>  fStats   = new ArrayList<>();
                        List<Long>    fEpochs  = new ArrayList<>();

                        for (int idx : order) {
                            fApptIds.add(tApptIds.get(idx));
                            fDocIds.add(tDocIds.get(idx));
                            fDocNames.add(tDocNames.get(idx));
                            fSpecs.add(tSpecs.get(idx));
                            fDates.add(tDates.get(idx));
                            fPrices.add(tPrices.get(idx));
                            fPics.add(tPics.get(idx));
                            fStats.add(tStats.get(idx));
                            fEpochs.add(sortKeys.get(idx));
                        }

                        // सेक्शन हेडर्स बनाएं
                        List<String> fHeaders = buildSectionHeaders(fEpochs);

                        boolean changed = applyIfChanged(
                                fDocIds, fDocNames, fSpecs, fDates, fPrices, fPics, fApptIds, fStats,
                                fHeaders, fEpochs
                        );

                        if (changed) {
                            recyclerView.invalidateItemDecorations();
                            recyclerView.setAlpha(0f);
                            recyclerView.animate().alpha(1f).setDuration(220).start();
                        }

                    } catch (JSONException ignored) {
                        // silent
                    }
                },
                error -> {
                    startLoader(false);
                    stopSwipe();
                }) {
            @Override public Priority getPriority() { return Priority.LOW; }
        };

        request.setTag(REQ_TAG);
        requestQueue.add(request);
    }

    /* ---- Helpers ---- */

    private void applyEmpty() {
        doctorIds.clear(); doctorNames.clear(); doctorSpecialties.clear();
        appointmentDates.clear(); appointmentPrices.clear();
        doctorProfilePictures.clear(); appointmentIds.clear();
        appointmentStatuses.clear(); sectionHeaders.clear(); epochs.clear();
        if (adapter != null) adapter.notifyDataSetChanged();
        lastSig = "";
        recyclerView.invalidateItemDecorations();
    }

    private enum Section { TODAY, YESTERDAY, LAST7, EARLIER }

    private List<String> buildSectionHeaders(List<Long> orderedEpochs) {
        List<String> headers = new ArrayList<>(orderedEpochs.size());
        Section prev = null;
        for (int i = 0; i < orderedEpochs.size(); i++) {
            Section s = classifySection(orderedEpochs.get(i));
            if (i == 0 || s != prev) {
                headers.add(sectionLabel(s)); // header text at this position
                prev = s;
            } else {
                headers.add(""); // no header for this item
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

    @SuppressLint("NotifyDataSetChanged")
    private boolean applyIfChanged(List<Integer> newDoctorIds,
                                   List<String> newDoctorNames,
                                   List<String> newDoctorSpecialties,
                                   List<String> newAppointmentDates,
                                   List<String> newAppointmentPrices,
                                   List<String> newDoctorProfilePictures,
                                   List<Integer> newAppointmentIds,
                                   List<String> newAppointmentStatuses,
                                   List<String> newSectionHeaders,
                                   List<Long>   newEpochs) {

        String sig = buildSig(newAppointmentIds, newAppointmentStatuses);
        if (sig.equals(lastSig)) return false;

        doctorIds.clear();              doctorIds.addAll(newDoctorIds);
        doctorNames.clear();            doctorNames.addAll(newDoctorNames);
        doctorSpecialties.clear();      doctorSpecialties.addAll(newDoctorSpecialties);
        appointmentDates.clear();       appointmentDates.addAll(newAppointmentDates);
        appointmentPrices.clear();      appointmentPrices.addAll(newAppointmentPrices);
        doctorProfilePictures.clear();  doctorProfilePictures.addAll(newDoctorProfilePictures);
        appointmentIds.clear();         appointmentIds.addAll(newAppointmentIds);
        appointmentStatuses.clear();    appointmentStatuses.addAll(newAppointmentStatuses);

        sectionHeaders.clear();         sectionHeaders.addAll(newSectionHeaders);
        epochs.clear();                 epochs.addAll(newEpochs);

        if (adapter != null) adapter.notifyDataSetChanged();
        lastSig = sig;
        return true;
    }

    private String buildSig(List<Integer> ids, List<String> statuses) {
        StringBuilder sb = new StringBuilder(ids.size() * 12);
        for (int i = 0; i < ids.size(); i++) {
            sb.append(ids.get(i)).append('|')
                    .append(safe(i < statuses.size() ? statuses.get(i) : "")).append(';');
        }
        return sb.toString();
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
            textPaint.setTextSize(40f); // scaled by density automatically
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int pos = parent.getChildAdapterPosition(view);
            if (pos == RecyclerView.NO_POSITION) return;

            String h = (pos < headers.size()) ? headers.get(pos) : "";
            if (h != null && !h.isEmpty()) {
                // Add space for header above this item
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
