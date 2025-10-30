package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * AnimalReportViewerActivity — Null-safe rendering (shows "N/A" for any null-ish values)
 */
public class AnimalReportViewerActivity extends AppCompatActivity {

    private static final String TAG = "AnimalReportViewer";

    // Loader
    private View loader;

    // Header + Attachment
    private ImageButton btnBack;
    private TextView tvReportTitle, tvReportDate;
    private ImageView ivReportPhoto;

    // Animal & Owner
    private TextView tvAnimalName, tvSpeciesBreed, tvAnimalSex, tvAnimalAge, tvAnimalWeight;
    private TextView tvOwnerAddress, tvNextVisitDate, tvIsFollowup;

    // Vitals & Clinical
    private TextView tvTemperatureC, tvPulseBpm, tvSpo2Pct, tvBpMmhg, tvRespiratoryRateBpm;
    private TextView tvPainScore, tvHydrationStatus, tvMucousMembranes, tvCrtSec;

    // Findings
    private TextView tvSymptoms, tvBehaviorGait, tvSkinCoat, tvRespiratorySystem, tvReasons;

    // Investigations
    private TextView tvRequiresInvestigation, tvInvestigationNotes;

    // Medications
    private TableLayout tblMeds;

    // Vaccination
    private View cardVaccination;
    private TextView tvVaccinationName, tvVaccinationNotes;

    // Doctor & System
    private TextView tvDoctorSignature;
    private TextView tvCreatedAt, tvUpdatedAt;

    // Actions
    private Button btnDownload;

    // State
    private String appointmentId;
    private String attachmentUrl; // For image download/open

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animal_report_viewer);

        bindViews();

        btnBack.setOnClickListener(v -> onBackPressed());
        btnDownload.setOnClickListener(v -> downloadAction());

        appointmentId = getIntent().getStringExtra("appointment_id");
        Log.d(TAG, "onCreate: appointment_id (intent) = " + appointmentId);

        if (TextUtils.isEmpty(appointmentId)) {
            Toast.makeText(this, "Invalid appointment.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Invalid appointment_id: null/empty");
            finish();
            return;
        }

        // NOTE: ensure you already fixed the server path to /Users/get_vet_report.php
        String url = ApiConfig.endpoint("get_vet_report.php", "appointment_id", appointmentId);
        Log.d(TAG, "Request URL = " + url);
        fetchReport(url);
    }

    /* -------------------- Bind Views -------------------- */
    private void bindViews() {
        loader = findViewById(R.id.loader);

        // Header
        btnBack = findViewById(R.id.btn_back);
        tvReportTitle = findViewById(R.id.tv_report_title);
        tvReportDate = findViewById(R.id.tv_report_date);
        ivReportPhoto = findViewById(R.id.iv_report_photo);

        // Animal & Owner
        tvAnimalName = findViewById(R.id.tv_animal_name);
        tvSpeciesBreed = findViewById(R.id.tv_species_breed);
        tvAnimalSex = findViewById(R.id.tv_animal_sex);
        tvAnimalAge = findViewById(R.id.tv_animal_age);
        tvAnimalWeight = findViewById(R.id.tv_animal_weight);
        tvOwnerAddress = findViewById(R.id.tv_owner_address);
        tvNextVisitDate = findViewById(R.id.tv_next_visit_date);
        tvIsFollowup = findViewById(R.id.tv_is_followup);

        // Vitals & Clinical
        tvTemperatureC = findViewById(R.id.tv_temperature_c);
        tvPulseBpm = findViewById(R.id.tv_pulse_bpm);
        tvSpo2Pct = findViewById(R.id.tv_spo2_pct);
        tvBpMmhg = findViewById(R.id.tv_bp_mmhg);
        tvRespiratoryRateBpm = findViewById(R.id.tv_respiratory_rate_bpm);
        tvPainScore = findViewById(R.id.tv_pain_score_0_10);
        tvHydrationStatus = findViewById(R.id.tv_hydration_status);
        tvMucousMembranes = findViewById(R.id.tv_mucous_membranes);
        tvCrtSec = findViewById(R.id.tv_crt_sec);

        // Findings
        tvSymptoms = findViewById(R.id.tv_symptoms);
        tvBehaviorGait = findViewById(R.id.tv_behavior_gait);
        tvSkinCoat = findViewById(R.id.tv_skin_coat);
        tvRespiratorySystem = findViewById(R.id.tv_respiratory_system);
        tvReasons = findViewById(R.id.tv_reasons);

        // Investigations
        tvRequiresInvestigation = findViewById(R.id.tv_requires_investigation);
        tvInvestigationNotes = findViewById(R.id.tv_investigation_notes);

        // Medications
        tblMeds = findViewById(R.id.table_medications);

        // Vaccination
        cardVaccination = findViewById(R.id.card_vaccination);
        tvVaccinationName = findViewById(R.id.tv_vaccination_name);
        tvVaccinationNotes = findViewById(R.id.tv_vaccination_notes);

        // Doctor & System
        tvDoctorSignature = findViewById(R.id.tv_doctor_signature);
        tvCreatedAt = findViewById(R.id.tv_created_at);
        tvUpdatedAt = findViewById(R.id.tv_updated_at);

        // Actions
        btnDownload = findViewById(R.id.btn_download);
    }

    private void setLoading(boolean loading) {
        Log.d(TAG, "setLoading: " + loading);
        loader.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    /* -------------------- Network -------------------- */
    private void fetchReport(String url) {
        setLoading(true);
        RequestQueue q = Volley.newRequestQueue(this);
        StringRequest req = new StringRequest(
                Request.Method.GET,
                url,
                resp -> {
                    setLoading(false);
                    Log.d(TAG, "Volley success. Raw response length=" + (resp != null ? resp.length() : 0));
                    Log.v(TAG, "Response payload: " + truncate(resp, 4000));

                    try {
                        // Guard: if server returned HTML by mistake
                        if (resp == null || !resp.trim().startsWith("{")) {
                            Log.e(TAG, "Server did not return JSON. First 200 chars: " + truncate(resp, 200));
                            Toast.makeText(this, "Server error: invalid response.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JSONObject root = new JSONObject(resp);
                        boolean ok = root.optBoolean("success", false);
                        Log.d(TAG, "Parsed success=" + ok);
                        if (!ok) {
                            String msg = root.optString("message");
                            Log.w(TAG, "API success=false, message=" + msg);
                            Toast.makeText(this, TextUtils.isEmpty(msg) ? "Report not available yet." : msg, Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        JSONObject r = root.optJSONObject("report");
                        if (r == null) {
                            Log.e(TAG, "report object is null");
                            Toast.makeText(this, "Invalid report data.", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        Log.v(TAG, "Report JSON: " + truncate(r.toString(), 3000));
                        bindReport(r);
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parse error", e);
                        Toast.makeText(this, "Failed to parse report.", Toast.LENGTH_SHORT).show();
                    }
                },
                err -> {
                    setLoading(false);
                    int code = -1;
                    String body = null;
                    if (err.networkResponse != null) {
                        code = err.networkResponse.statusCode;
                        if (err.networkResponse.data != null) {
                            body = new String(err.networkResponse.data, StandardCharsets.UTF_8);
                        }
                    }
                    Log.e(TAG, "Volley error. status=" + code + " body=" + truncate(body, 2000), err);
                    Toast.makeText(this, "Unable to load report. Please check your internet.", Toast.LENGTH_SHORT).show();
                }
        );
        req.setShouldCache(false);
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1.0f));
        q.add(req);
    }

    /* -------------------- Bind Report -------------------- */
    @SuppressLint("SetTextI18n")
    private void bindReport(JSONObject r) {
        // Header
        String title = sanitize(r.optString("report_title", null), "Veterinary Report");
        String date = sanitize(r.optString("report_date", null), "N/A");
        tvReportTitle.setText(title);
        tvReportDate.setText("Date: " + date);
        Log.d(TAG, "Bind header: title=" + title + ", date=" + date);

        // Attachment
        String rawAttachment = r.optString("attachment_url", null);
        attachmentUrl = isNullish(rawAttachment) ? "" : rawAttachment.trim();
        Log.d(TAG, "attachment_url=" + attachmentUrl);
        if (!TextUtils.isEmpty(attachmentUrl)) {
            ivReportPhoto.setVisibility(View.VISIBLE);
            try {
                Glide.with(this)
                        .load(attachmentUrl)
                        .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                Log.e(TAG, "Glide load FAILED for: " + model, e);
                                return false;
                            }
                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                Log.d(TAG, "Glide load OK: " + model + ", source=" + dataSource);
                                return false;
                            }
                        })
                        .into(ivReportPhoto);
            } catch (Exception e) {
                Log.e(TAG, "Glide exception", e);
            }
        } else {
            ivReportPhoto.setVisibility(View.GONE);
        }

        // Animal & Owner
        setAndLog(tvAnimalName, "Animal: ", sanitize(r.optString("animal_name", null), "N/A"));
        setAndLog(tvSpeciesBreed, "Species/Breed: ", sanitize(r.optString("species_breed", null), "N/A"));
        setAndLog(tvAnimalSex, "Sex: ", sanitize(r.optString("sex", null), "N/A"));
        setAndLog(tvAnimalAge, "Age (years): ", sanitize(numStr(r, "age_years"), "N/A"));
        setAndLog(tvAnimalWeight, "Weight (kg): ", sanitize(numStr(r, "weight_kg"), "N/A"));
        setAndLog(tvOwnerAddress, "Owner Address: ", sanitize(r.optString("owner_address", null), "N/A"));

        String nextVisit = sanitize(r.optString("next_visit_date", null), "");
        tvNextVisitDate.setVisibility(TextUtils.isEmpty(nextVisit) ? View.GONE : View.VISIBLE);
        tvNextVisitDate.setText("Next Visit: " + (TextUtils.isEmpty(nextVisit) ? "N/A" : nextVisit));
        Log.d(TAG, "next_visit_date=" + nextVisit + ", visible=" + (tvNextVisitDate.getVisibility() == View.VISIBLE));

        boolean isFollowup = r.optBoolean("is_followup", false);
        tvIsFollowup.setText("Follow-up: " + (isFollowup ? "Yes" : "No"));
        Log.d(TAG, "is_followup=" + isFollowup);

        // Vitals & Clinical
        setAndLog(tvTemperatureC, "Temperature (°C): ", sanitize(numStr(r, "temperature_c"), "N/A"));
        setAndLog(tvPulseBpm, "Pulse (bpm): ", sanitize(numStr(r, "pulse_bpm"), "N/A"));
        setAndLog(tvSpo2Pct, "SpO₂ (%): ", sanitize(numStr(r, "spo2_pct"), "N/A"));
        setAndLog(tvBpMmhg, "Blood Pressure (mmHg): ", sanitize(r.optString("bp_mmhg", null), "N/A"));
        setAndLog(tvRespiratoryRateBpm, "Respiratory Rate (bpm): ", sanitize(numStr(r, "respiratory_rate_bpm"), "N/A"));

        setAndLog(tvPainScore, "Pain Score (0–10): ", sanitize(numStr(r, "pain_score_0_10"), "N/A"));
        setAndLog(tvHydrationStatus, "Hydration: ", sanitize(r.optString("hydration_status", null), "N/A"));
        setAndLog(tvMucousMembranes, "Mucous Membranes: ", sanitize(r.optString("mucous_membranes", null), "N/A"));
        setAndLog(tvCrtSec, "CRT (sec): ", sanitize(numStr(r, "crt_sec"), "N/A"));

        // Findings
        setAndLog(tvSymptoms, "Symptoms: ", sanitize(r.optString("symptoms", null), "N/A"));
        setAndLog(tvBehaviorGait, "Behavior/Gait: ", sanitize(r.optString("behavior_gait", null), "N/A"));
        setAndLog(tvSkinCoat, "Skin/Coat: ", sanitize(r.optString("skin_coat", null), "N/A"));
        setAndLog(tvRespiratorySystem, "Respiratory System: ", sanitize(r.optString("respiratory_system", null), "N/A"));
        setAndLog(tvReasons, "Reasons/Notes: ", sanitize(r.optString("reasons", null), "N/A"));

        // Investigations
        boolean reqInv = r.optBoolean("requires_investigation", false);
        tvRequiresInvestigation.setText("Requires Investigation: " + (reqInv ? "Yes" : "No"));
        setAndLog(tvInvestigationNotes, "Notes: ", sanitize(r.optString("investigation_notes", null), "N/A"));
        Log.d(TAG, "requires_investigation=" + reqInv);

        // Doctor & System
        setAndLog(tvDoctorSignature, "Signature/Name: ", sanitize(r.optString("doctor_signature", null), "N/A"));
        setAndLog(tvCreatedAt, "Created: ", sanitize(r.optString("created_at", null), "N/A"));
        setAndLog(tvUpdatedAt, "Updated: ", sanitize(r.optString("updated_at", null), "N/A"));

        // Medications (combined → fallback to raw arrays)
        JSONArray medsCombined = r.optJSONArray("medications");
        if (medsCombined != null && medsCombined.length() > 0) {
            Log.d(TAG, "Using combined medications array, count=" + medsCombined.length());
            inflateMedicationsFromCombined(medsCombined);
        } else {
            String medsRaw = r.optString("medications_json", "[]");
            String dosesRaw = r.optString("dosage_json", "[]");
            Log.d(TAG, "Combined meds empty. Fallback raw arrays. meds_json=" + truncate(medsRaw, 500) +
                    ", dosage_json=" + truncate(dosesRaw, 500));
            try {
                JSONArray names = new JSONArray(medsRaw);
                JSONArray doses = new JSONArray(dosesRaw);
                inflateMedicationsFromRawArrays(names, doses);
            } catch (JSONException e) {
                Log.e(TAG, "Raw meds JSON arrays parse error", e);
            }
        }

        // Vaccination – show only if present
        String vaccName = sanitize(r.optString("vaccination_name", null), "");
        String vaccNotes = sanitize(r.optString("vaccination_notes", null), "");
        boolean showVacc = !(TextUtils.isEmpty(vaccName) && TextUtils.isEmpty(vaccNotes));
        if (cardVaccination != null) cardVaccination.setVisibility(showVacc ? View.VISIBLE : View.GONE);
        Log.d(TAG, "Vaccination visible=" + showVacc + " name=" + vaccName + " notes=" + vaccNotes);
        if (showVacc) {
            tvVaccinationName.setText("Name: " + (TextUtils.isEmpty(vaccName) ? "N/A" : vaccName));
            tvVaccinationNotes.setText("Notes: " + (TextUtils.isEmpty(vaccNotes) ? "N/A" : vaccNotes));
        }
    }

    /* -------------------- Medications helpers -------------------- */

    /** For combined structure: [{name, dosage}] */
    private void inflateMedicationsFromCombined(JSONArray meds) {
        clearMedicationRows();
        for (int i = 0; i < meds.length(); i++) {
            JSONObject m = meds.optJSONObject(i);
            if (m == null) continue;
            String name = sanitize(m.optString("name", null), "N/A");
            String dose = sanitize(m.optString("dosage", null), "N/A");
            String multiName = splitByCommaNewLines(name);
            String multiDose = splitByCommaNewLines(dose);
            Log.v(TAG, "Med #" + (i + 1) + " name=" + name + " dose=" + dose + " -> rows");
            addMedicationRow(i + 1, multiName, multiDose);
        }
    }

    /** Fallback: separate arrays for names and doses */
    private void inflateMedicationsFromRawArrays(JSONArray names, JSONArray doses) {
        clearMedicationRows();
        int max = Math.max(names != null ? names.length() : 0, doses != null ? doses.length() : 0);
        Log.d(TAG, "inflateMedicationsFromRawArrays: max=" + max);
        for (int i = 0; i < max; i++) {
            String name = "N/A";
            String dose = "N/A";
            if (names != null && i < names.length()) name = sanitize(names.optString(i, null), "N/A");
            if (doses != null && i < doses.length()) dose = sanitize(doses.optString(i, null), "N/A");

            String multiName = splitByCommaNewLines(name);
            String multiDose = splitByCommaNewLines(dose);
            Log.v(TAG, "Raw med #" + (i + 1) + " name=" + name + " dose=" + dose);
            addMedicationRow(i + 1, multiName, multiDose);
        }
    }

    private void clearMedicationRows() {
        int childCount = tblMeds.getChildCount();
        Log.d(TAG, "clearMedicationRows: existingRows=" + childCount);
        if (childCount > 1) tblMeds.removeViews(1, childCount - 1); // keep header
    }

    private void addMedicationRow(int index, String nameText, String doseText) {
        TableRow row = new TableRow(this);
        TextView tvNo = makeCell(String.valueOf(index), true);
        TextView tvName = makeCell(nameText, false);
        TextView tvDose = makeCell(doseText, false);
        row.addView(tvNo);
        row.addView(tvName);
        row.addView(tvDose);
        tblMeds.addView(row);
    }

    /** Turn "Paracetamol, Azithromycin" -> "Paracetamol\nAzithromycin"; null-ish -> "N/A" */
    private String splitByCommaNewLines(String s) {
        if (isNullish(s)) return "N/A";
        if (!s.contains(",")) return sanitize(s, "N/A");
        String[] parts = s.split(",");
        StringBuilder b = new StringBuilder();
        for (String p : parts) {
            String t = sanitize(p, ""); // treat each token
            if (t.isEmpty() || "N/A".equals(t)) continue;
            if (b.length() > 0) b.append('\n');
            b.append(t);
        }
        return b.length() == 0 ? "N/A" : b.toString();
    }

    private TextView makeCell(String text, boolean center) {
        TextView tv = new TextView(this);
        tv.setText(sanitize(text, "N/A"));
        tv.setTextSize(16f);
        int pad = (int) (getResources().getDisplayMetrics().density * 8);
        tv.setPadding(pad, pad + 4, pad, pad + 4);
        if (center) tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        return tv;
    }

    /* -------------------- Utilities (NULL-SAFE) -------------------- */

    /** Treat any null-ish value as empty; return fallback (default "N/A") */
    private String sanitize(String s, String fallback) {
        if (s == null) return fallback == null ? "N/A" : fallback;
        String t = s.trim();
        if (t.isEmpty()) return fallback == null ? "N/A" : fallback;
        String low = t.toLowerCase(Locale.ROOT);
        if (low.equals("null") || low.equals("undefined") || low.equals("n/a") || low.equals("na")) {
            return fallback == null ? "N/A" : fallback;
        }
        return t;
    }

    private boolean isNullish(String s) {
        if (s == null) return true;
        String t = s.trim();
        if (t.isEmpty()) return true;
        String low = t.toLowerCase(Locale.ROOT);
        return low.equals("null") || low.equals("undefined");
    }

    /** Reads number-like fields safely; returns null for absent/JSONObject.NULL */
    private String numStr(JSONObject obj, String key) {
        if (obj == null || key == null) return null;
        if (!obj.has(key)) return null;
        Object v = obj.opt(key);
        if (v == null || v == JSONObject.NULL) return null;
        String s = String.valueOf(v);
        return isNullish(s) ? null : s;
    }

    private void setAndLog(TextView tv, String prefix, String value) {
        String val = sanitize(value, "N/A");
        tv.setText(prefix + val);
        Log.d(TAG, "Bind " + prefix + " = " + val);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max) + "… (+" + (s.length() - max) + " chars)";
    }

    /* -------------------- Download / Share -------------------- */
    private void downloadAction() {
        Log.d(TAG, "downloadAction: attachmentUrl=" + attachmentUrl);
        if (!TextUtils.isEmpty(attachmentUrl)) {
            try {
                DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                DownloadManager.Request r = new DownloadManager.Request(Uri.parse(attachmentUrl));
                r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                String fileName = String.format(Locale.getDefault(), "vet_report_%s.jpg", appointmentId);
                r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                long id = dm.enqueue(r);
                Log.d(TAG, "Download enqueued. id=" + id + " fileName=" + fileName);
                Toast.makeText(this, "Downloading report image…", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "DownloadManager failed, fallback open in browser", e);
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(attachmentUrl)));
                } catch (ActivityNotFoundException ex) {
                    Log.e(TAG, "No handler for ACTION_VIEW", ex);
                }
            }
        } else {
            String share = buildShareSummary();
            Log.v(TAG, "Sharing text summary length=" + share.length());
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, "Veterinary Report");
            i.putExtra(Intent.EXTRA_TEXT, share);
            startActivity(Intent.createChooser(i, "Share Report"));
        }
    }

    private String buildShareSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(tvReportTitle.getText()).append("\n")
                .append(tvReportDate.getText()).append("\n\n")
                .append(tvAnimalName.getText()).append("\n")
                .append(tvSpeciesBreed.getText()).append("\n")
                .append(tvAnimalSex.getText()).append("\n")
                .append(tvAnimalAge.getText()).append("\n")
                .append(tvAnimalWeight.getText()).append("\n")
                .append(tvOwnerAddress.getText()).append("\n")
                .append(tvIsFollowup.getText()).append("\n")
                .append(tvNextVisitDate.getText()).append("\n\n")
                .append(tvTemperatureC.getText()).append("\n")
                .append(tvPulseBpm.getText()).append("\n")
                .append(tvSpo2Pct.getText()).append("\n")
                .append(tvBpMmhg.getText()).append("\n")
                .append(tvRespiratoryRateBpm.getText()).append("\n")
                .append(tvPainScore.getText()).append("\n")
                .append(tvHydrationStatus.getText()).append("\n")
                .append(tvMucousMembranes.getText()).append("\n")
                .append(tvCrtSec.getText()).append("\n\n")
                .append(tvSymptoms.getText()).append("\n")
                .append(tvBehaviorGait.getText()).append("\n")
                .append(tvSkinCoat.getText()).append("\n")
                .append(tvRespiratorySystem.getText()).append("\n")
                .append(tvReasons.getText()).append("\n\n")
                .append(tvRequiresInvestigation.getText()).append("\n")
                .append(tvInvestigationNotes.getText()).append("\n")
                .append(tvDoctorSignature.getText()).append("\n")
                .append(tvCreatedAt.getText()).append("\n")
                .append(tvUpdatedAt.getText());
        return sb.toString();
    }
}
