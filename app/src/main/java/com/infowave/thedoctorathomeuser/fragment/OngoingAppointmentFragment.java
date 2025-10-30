package com.infowave.thedoctorathomeuser.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.tabs.TabLayout;
import com.infowave.thedoctorathomeuser.ApiConfig;
import com.infowave.thedoctorathomeuser.MainActivity;
import com.infowave.thedoctorathomeuser.R;
import com.infowave.thedoctorathomeuser.adapter.OngoingAdapter;
import com.infowave.thedoctorathomeuser.adapter.VetOngoingAdapter;
import com.infowave.thedoctorathomeuser.VetAppointment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class OngoingAppointmentFragment extends Fragment {

    private static final String API_URL = ApiConfig.endpoint("getOngoingAppointment.php");

    private ViewPager vp;
    private RecyclerView recyclerView;
    private Button bookAppointment;
    private SwipeRefreshLayout swipeRefresh;
    private TabLayout appointmentTabs;

    private OngoingAdapter humanAdapter;
    private VetOngoingAdapter vetAdapter;

    private String patientId;

    // ---------- Human lists (what your OngoingAdapter expects) ----------
    private final List<String>  doctorNames     = new ArrayList<>();
    private final List<String>  specialties     = new ArrayList<>();
    private final List<String>  hospitals       = new ArrayList<>();
    private final List<Float>   ratings         = new ArrayList<>();
    private final List<String>  profilePictures = new ArrayList<>();
    private final List<Integer> appointmentIds  = new ArrayList<>();
    private final List<String>  statuses        = new ArrayList<>();
    private final List<String>  durations       = new ArrayList<>();
    private final List<Integer> doctorIds       = new ArrayList<>();

    // ---------- Vet list (your VetOngoingAdapter model) ----------
    private final List<VetAppointment> vetItems = new ArrayList<>();

    // ---------- Live refresh (polling) ----------
    private final Handler liveHandler = new Handler();
    private Runnable liveRunnable;
    private static final long POLL_INTERVAL_MS = 3000L; // 3 seconds
    private boolean isPolling = false;

    // For diffing to avoid unnecessary adapter updates/flicker
    private String lastHumanSig = "";
    private String lastVetSig   = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ongoing_appointment, container, false);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getActivity() instanceof MainActivity) {
            vp = ((MainActivity) getActivity()).findViewById(R.id.vp);
        }

        appointmentTabs = view.findViewById(R.id.appointmentTabs);
        swipeRefresh    = view.findViewById(R.id.swipeRefreshOngoing);
        recyclerView    = view.findViewById(R.id.recyclerView);
        bookAppointment = view.findViewById(R.id.bookButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        SharedPreferences sp = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        patientId = sp.getString("patient_id", "");
        if (patientId == null) patientId = "";
        if (patientId.trim().isEmpty()) {
            // Silent fail — as requested, no toast/log. UI stays empty.
            return;
        }

        // human adapter
        humanAdapter = new OngoingAdapter(
                requireContext(),
                doctorNames, specialties, hospitals, ratings, profilePictures,
                appointmentIds, statuses, durations, doctorIds
        );

        // vet adapter
        vetAdapter = new VetOngoingAdapter(requireContext(), vetItems);

        // default tab: Human
        recyclerView.setAdapter(humanAdapter);

        if (appointmentTabs != null) {
            appointmentTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override public void onTabSelected(TabLayout.Tab tab) {
                    if (tab.getPosition() == 0) {
                        recyclerView.setAdapter(humanAdapter);
                    } else {
                        recyclerView.setAdapter(vetAdapter);
                    }
                }
                @Override public void onTabUnselected(TabLayout.Tab tab) {}
                @Override public void onTabReselected(TabLayout.Tab tab) {}
            });
            TabLayout.Tab patientTab = appointmentTabs.getTabAt(0);
            if (patientTab != null) patientTab.select();
        }

        swipeRefresh.setOnRefreshListener(() -> fetchOngoing(true)); // manual pull-to-refresh (silent)

        // initial load (silent visual; spinner set but quickly hidden)
        swipeRefresh.setRefreshing(true);
        fetchOngoing(true);

        bookAppointment.setOnClickListener(v -> { if (vp != null) vp.setCurrentItem(1); });

        // Prepare live runnable
        liveRunnable = new Runnable() {
            @Override public void run() {
                if (!isAdded()) return;
                fetchOngoing(true); // always silent
                // Re-schedule
                liveHandler.postDelayed(this, POLL_INTERVAL_MS);
            }
        };
    }

    private void stopRefreshingUI() {
        if (swipeRefresh != null && swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().getOnBackPressedDispatcher().addCallback(
                this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override public void handleOnBackPressed() {
                        ViewPager vp = requireActivity().findViewById(R.id.vp);
                        if (vp != null && vp.getCurrentItem() > 0) {
                            vp.setCurrentItem(0, true);
                        } else {
                            setEnabled(false);
                            requireActivity().onBackPressed();
                        }
                    }
                }
        );
        startLivePolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLivePolling();
    }

    private void startLivePolling() {
        if (isPolling || liveRunnable == null) return;
        isPolling = true;
        liveHandler.postDelayed(liveRunnable, POLL_INTERVAL_MS);
    }

    private void stopLivePolling() {
        if (!isPolling) return;
        isPolling = false;
        liveHandler.removeCallbacks(liveRunnable);
    }

    // ---- fetch + split + sort ----
    private void fetchOngoing(boolean silent) {
        if (!isAdded()) { stopRefreshingUI(); return; }

        StringRequest req = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    stopRefreshingUI();
                    try {
                        JSONObject json = new JSONObject(response);
                        if (!json.optBoolean("success", false)) {
                            clearAll(); // no items
                            return;
                        }

                        JSONArray arr = json.optJSONArray("appointments");
                        if (arr == null) { clearAll(); return; }

                        // temp lists with sort key
                        List<HumanRow> tmpHuman = new ArrayList<>();
                        List<VetRow>   tmpVet   = new ArrayList<>();

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject a = arr.getJSONObject(i);

                            // normalize fields coming from PHP
                            int apptId   = a.optInt("appointment_id", 0);
                            int docId    = a.optInt("doctor_id", 0);
                            String date  = a.optString("appointment_date", "");  // "yyyy-MM-dd"
                            String time  = a.optString("time_slot", "");         // "hh:mm a"
                            String status= a.optString("status", "");
                            String doctor= a.optString("doctor_name", "");
                            String spec  = a.optString("specialty", "");
                            String hosp  = a.optString("hospital_name", "");
                            float exp    = (float) a.optDouble("experience", 0);
                            String dur   = a.optString("experience_duration", "");
                            String pic   = a.optString("profile_picture", "");

                            // vet specifics (may be empty for human)
                            int isVet    = a.optInt("is_vet_case", 0);
                            String animalName   = a.optString("animal_name", "");
                            String animalBreed  = a.optString("animal_breed", "");
                            String animalAge    = a.optString("animal_age_text", a.optString("animal_age","")); // support either
                            String reason       = a.optString("reason_for_visit", "");
                            String fee          = a.optString("fee_text", a.optString("fee",""));
                            String whenLabel    = a.optString("when_text", ""); // optional pretty date

                            long sortKey = buildSortKey(date, time); // later date/time = larger key

                            if (isVet == 1) {
                                String title = (animalName == null || animalName.isEmpty()) ? "Pet" : animalName;
                                String sub   = (reason == null ? "" : reason);
                                String doc   = doctor;
                                String when  = !isEmpty(whenLabel) ? whenLabel : (date + " • " + time);
                                String price = !isEmpty(fee) ? ("₹" + fee) : "";
                                String statusText = status;

                                tmpVet.add(new VetRow(sortKey, new VetAppointment(
                                        title, sub, doc, when, price, statusText, pic
                                )));
                            } else {
                                tmpHuman.add(new HumanRow(sortKey, new HumanPayload(
                                        doctor, spec, hosp, exp, pic, apptId, status, dur, docId
                                )));
                            }
                        }

                        // sort desc (latest first)
                        Comparator<HasKey> byKeyDesc = (a, b) -> Long.compare(b.key(), a.key());
                        Collections.sort(tmpHuman, (o1, o2) -> byKeyDesc.compare(o1::key, o2::key));
                        Collections.sort(tmpVet,   (o1, o2) -> byKeyDesc.compare(o1::key, o2::key));

                        // Compute signatures to detect actual change
                        String newHumanSig = buildHumanSignature(tmpHuman);
                        String newVetSig   = buildVetSignature(tmpVet);

                        boolean humanChanged = !newHumanSig.equals(lastHumanSig);
                        boolean vetChanged   = !newVetSig.equals(lastVetSig);

                        if (humanChanged) {
                            writeHuman(tmpHuman);
                            lastHumanSig = newHumanSig;
                        }
                        if (vetChanged) {
                            writeVet(tmpVet);
                            lastVetSig = newVetSig;
                        }

                    } catch (JSONException e) {
                        // silent; keep previous UI
                        stopRefreshingUI();
                    }
                },
                error -> {
                    // silent; keep previous UI
                    stopRefreshingUI();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String,String> p = new HashMap<>();
                p.put("patient_id", patientId);
                return p;
            }
        };

        Volley.newRequestQueue(requireContext()).add(req);
    }

    private void clearAll() {
        doctorNames.clear(); specialties.clear(); hospitals.clear(); ratings.clear();
        profilePictures.clear(); appointmentIds.clear(); statuses.clear(); durations.clear(); doctorIds.clear();
        vetItems.clear();
        notifyBoth();
        lastHumanSig = "";
        lastVetSig   = "";
    }

    private void writeHuman(List<HumanRow> rows) {
        doctorNames.clear(); specialties.clear(); hospitals.clear(); ratings.clear();
        profilePictures.clear(); appointmentIds.clear(); statuses.clear(); durations.clear(); doctorIds.clear();

        for (HumanRow r : rows) {
            HumanPayload h = r.payload;
            doctorNames.add(h.doctorName);
            specialties.add(h.specialty);
            hospitals.add(h.hospital);
            ratings.add(h.experience);
            profilePictures.add(h.profilePicture);
            appointmentIds.add(h.appointmentId);
            statuses.add(h.status);
            durations.add(h.duration);
            doctorIds.add(h.doctorId);
        }
        if (humanAdapter != null) humanAdapter.notifyDataSetChanged();
    }

    private void writeVet(List<VetRow> rows) {
        vetItems.clear();
        for (VetRow r : rows) vetItems.add(r.payload);
        if (vetAdapter != null) vetAdapter.notifyDataSetChanged();
    }

    private void notifyBoth() {
        if (humanAdapter != null) humanAdapter.notifyDataSetChanged();
        if (vetAdapter != null) vetAdapter.notifyDataSetChanged();
    }

    // ---- helpers ----
    private static boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }

    private long buildSortKey(String yyyyMmDd, String hhmmAMPM) {
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
            Date d = df.parse((yyyyMmDd == null ? "" : yyyyMmDd) + " " + (hhmmAMPM == null ? "" : hhmmAMPM));
            return (d != null) ? d.getTime() : 0L;
        } catch (ParseException e) {
            return 0L;
        }
    }

    // Signatures to detect meaningful changes without heavy DiffUtil or flicker
    private String buildHumanSignature(List<HumanRow> rows) {
        StringBuilder sb = new StringBuilder(rows.size() * 32);
        for (HumanRow r : rows) {
            HumanPayload h = r.payload;
            // Include fields that affect presentation/order
            sb.append(r.key).append('|')
                    .append(h.appointmentId).append('|')
                    .append(safe(h.status)).append('|')
                    .append(safe(h.doctorName)).append('|')
                    .append(safe(h.specialty)).append('|')
                    .append(safe(h.hospital)).append('|')
                    .append(safe(h.duration)).append('|')
                    .append(h.experience).append('|')
                    .append(h.doctorId).append(';');
        }
        return sb.toString();
    }

    private String buildVetSignature(List<VetRow> rows) {
        StringBuilder sb = new StringBuilder(rows.size() * 32);
        for (VetRow r : rows) {
            VetAppointment v = r.payload;
            sb.append(r.key).append('|')
                    .append(safe(v.getTitle())).append('|')
                    .append(safe(v.getSubtitle())).append('|')
                    .append(safe(v.getDoctorName())).append('|')
                    .append(safe(v.getWhen())).append('|')
                    .append(safe(v.getPrice())).append('|')
                    .append(safe(v.getStatus())).append(';');
        }
        return sb.toString();
    }

    private static String safe(String s) { return s == null ? "" : s; }

    // ---- tiny structs for sorting ----
    private interface HasKey { long key(); }

    private static class HumanPayload {
        final String doctorName, specialty, hospital, profilePicture, status, duration;
        final float experience;
        final int appointmentId, doctorId;
        HumanPayload(String dn, String sp, String ho, float ex, String pic, int apptId, String st, String dur, int docId) {
            doctorName = dn; specialty = sp; hospital = ho; experience = ex; profilePicture = pic;
            appointmentId = apptId; status = st; duration = dur; doctorId = docId;
        }
    }

    private static class HumanRow implements HasKey {
        final long key; final HumanPayload payload;
        HumanRow(long k, HumanPayload p) { key = k; payload = p; }
        @Override public long key() { return key; }
    }

    private static class VetRow implements HasKey {
        final long key; final VetAppointment payload;
        VetRow(long k, VetAppointment p) { key = k; payload = p; }
        @Override public long key() { return key; }
    }
}
