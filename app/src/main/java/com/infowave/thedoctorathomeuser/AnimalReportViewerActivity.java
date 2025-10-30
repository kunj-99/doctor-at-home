package com.infowave.thedoctorathomeuser;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class AnimalReportViewerActivity extends AppCompatActivity {

    // Views
    private View loader;
    private ImageView ivReportPhoto;
    private View contentContainer;

    private ImageButton btnBack;
    private TextView tvHospitalName, tvHospitalAddress;
    private TextView tvOwnerName, tvAnimalAge, tvAnimalSex, tvAnimalWeight, tvAddress, tvDate;
    private TextView tvTemp, tvPulse, tvSpo2, tvBp, tvRespiratory;
    private TextView tvSymptoms, tvInvestigations, tvDoctorName, tvReportType;
    private TableLayout tblMeds;
    private Button btnDownload;

    // State
    private String appointmentId;
    private String attachmentUrl; // if direct image was uploaded, we show & download this

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animal_report_viewer);

        bindViews();

        btnBack.setOnClickListener(v -> onBackPressed());
        btnDownload.setOnClickListener(v -> downloadAction());

        appointmentId = getIntent().getStringExtra("appointment_id");
        if (TextUtils.isEmpty(appointmentId)) {
            Toast.makeText(this, "Invalid appointment.", Toast.LENGTH_SHORT).show();
        }

        // API endpoint (patient side)
        // Expecting JSON like:
        // { success:true, report:{ clinic_name, clinic_address, attachment_url, doctor_name, report_type, ...
        //   owner_name, date, address, animal_age, animal_sex, animal_weight, temperature, pulse, spo2, bp, respiratory, symptoms, investigations, medications:[{name,dosage},...] } }
        String url = ApiConfig.endpoint("get_vet_report.php", "appointment_id", appointmentId);

        fetchReport(url);
    }

    private void bindViews() {
        loader           = findViewById(R.id.loader);
        ivReportPhoto    = findViewById(R.id.iv_report_photo);
        contentContainer = findViewById(R.id.content_container);

        btnBack          = findViewById(R.id.btn_back);
        tvHospitalName   = findViewById(R.id.tv_hospital_name);
        tvHospitalAddress= findViewById(R.id.tv_hospital_address);

        tvOwnerName      = findViewById(R.id.tv_patient_name);
        tvAnimalAge      = findViewById(R.id.tv_patient_age);
        tvAnimalSex      = findViewById(R.id.tv_patient_sex);
        tvAnimalWeight   = findViewById(R.id.tv_patient_weight);
        tvAddress        = findViewById(R.id.tv_patient_address);
        tvDate           = findViewById(R.id.tv_visit_date);

        tvTemp           = findViewById(R.id.tv_temperature);
        tvPulse          = findViewById(R.id.tv_pulse);
        tvSpo2           = findViewById(R.id.tv_spo2);
        tvBp             = findViewById(R.id.tv_blood_pressure);
        tvRespiratory    = findViewById(R.id.tv_respiratory);

        tvSymptoms       = findViewById(R.id.tv_symptoms);
        tvInvestigations = findViewById(R.id.tv_investigations_content);
        tvDoctorName     = findViewById(R.id.tv_doctor_name);
        tvReportType     = findViewById(R.id.tv_report_type);
        tblMeds          = findViewById(R.id.table_medications);

        btnDownload      = findViewById(R.id.btn_download);
    }

    private void setLoading(boolean loading) {
        loader.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void fetchReport(String url) {
        setLoading(true);

        RequestQueue q = Volley.newRequestQueue(this);
        StringRequest req = new StringRequest(
                Request.Method.GET,
                url,
                resp -> {
                    setLoading(false);
                    try {
                        JSONObject root = new JSONObject(resp);
                        if (!root.optBoolean("success", false)) {
                            Toast.makeText(this, "Report not available yet.", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        JSONObject r = root.optJSONObject("report");
                        if (r == null) {
                            Toast.makeText(this, "Invalid report data.", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        bindReport(r);
                    } catch (JSONException e) {
                        Toast.makeText(this, "Failed to parse report.", Toast.LENGTH_SHORT).show();
                     //   finish();
                    }
                },
                err -> {
                    setLoading(false);
                    Toast.makeText(this, "Unable to load report. Please check your internet.", Toast.LENGTH_SHORT).show();
                 //   finish();
                }
        );
        q.add(req);
    }

    private void bindReport(JSONObject r) {
        // Clinic
        tvHospitalName.setText(nonEmpty(r.optString("clinic_name"), "Clinic / Hospital"));
        tvHospitalAddress.setText(nonEmpty(r.optString("clinic_address"), "-"));

        // Attachment (direct image path) — from doctor direct upload flow
        attachmentUrl = r.optString("attachment_url", "");
        boolean hasImage = !TextUtils.isEmpty(attachmentUrl);

        if (hasImage) {
            ivReportPhoto.setVisibility(View.VISIBLE);
            contentContainer.setVisibility(View.GONE); // image-only style
            try {
                Glide.with(this).load(attachmentUrl).into(ivReportPhoto);
            } catch (Exception ignored) {}
            return; // if direct image, we don’t show the form sections below
        }

        // Otherwise: Virtual Report fields
        ivReportPhoto.setVisibility(View.GONE);
        contentContainer.setVisibility(View.VISIBLE);

        tvDoctorName.setText(nonEmpty(r.optString("doctor_name"), "-"));
        tvReportType.setText("Report Type: " + nonEmpty(r.optString("report_type"), "Initial"));

        // Owner / Animal
        tvOwnerName.setText("Owner: " + nonEmpty(r.optString("owner_name"), "-"));
        tvDate.setText("Date: " + nonEmpty(r.optString("date"), "-"));
        tvAddress.setText("Address: " + nonEmpty(r.optString("address"), "-"));

        tvAnimalAge.setText("Animal Age: " + nonEmpty(r.optString("animal_age"), "-"));
        tvAnimalSex.setText("Animal Sex: " + nonEmpty(r.optString("animal_sex"), "-"));
        tvAnimalWeight.setText("Animal Weight: " + nonEmpty(r.optString("animal_weight"), "-"));

        // Vitals
        tvTemp.setText("Temperature: " + nonEmpty(r.optString("temperature"), "-"));
        tvPulse.setText("Pulse: " + nonEmpty(r.optString("pulse"), "-"));
        tvSpo2.setText("SPO2: " + nonEmpty(r.optString("spo2"), "-"));
        tvBp.setText("Blood Pressure: " + nonEmpty(r.optString("bp"), "-"));
        tvRespiratory.setText("Respiratory Rate: " + nonEmpty(r.optString("respiratory"), "-"));

        // Symptoms & Investigations
        tvSymptoms.setText(nonEmpty(r.optString("symptoms"), "-"));
        tvInvestigations.setText(nonEmpty(r.optString("investigations"), "-"));

        // Medications array: [{name:"", dosage:""}]
        JSONArray meds = r.optJSONArray("medications");
        inflateMedications(meds);
    }

    private void inflateMedications(@Nullable JSONArray meds) {
        // Remove old dynamic rows (keep header at index 0)
        int childCount = tblMeds.getChildCount();
        if (childCount > 1) tblMeds.removeViews(1, childCount - 1);

        if (meds == null || meds.length() == 0) return;

        for (int i = 0; i < meds.length(); i++) {
            JSONObject m = meds.optJSONObject(i);
            if (m == null) continue;

            String name = nonEmpty(m.optString("name"), "-");
            String dose = nonEmpty(m.optString("dosage"), "-");

            TableRow row = new TableRow(this);

            TextView tvNo = makeCell(String.valueOf(i + 1), true);
            TextView tvName = makeCell(name, false);
            TextView tvDose = makeCell(dose, false);

            row.addView(tvNo);
            row.addView(tvName);
            row.addView(tvDose);

            tblMeds.addView(row);
        }
    }

    private TextView makeCell(String text, boolean center) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16f);
        tv.setPadding(8, 12, 8, 12);
        if (center) tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        return tv;
    }

    private String nonEmpty(String s, String fallback) {
        if (s == null) return fallback;
        s = s.trim();
        return s.isEmpty() ? fallback : s;
    }

    private void downloadAction() {
        if (!TextUtils.isEmpty(attachmentUrl)) {
            // Download the image directly
            try {
                DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                DownloadManager.Request r = new DownloadManager.Request(Uri.parse(attachmentUrl));
                r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                String fileName = String.format(Locale.getDefault(), "vet_report_%s.jpg", appointmentId);
                r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                dm.enqueue(r);
                Toast.makeText(this, "Downloading report image…", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                // Fallback open in browser
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(attachmentUrl)));
                } catch (ActivityNotFoundException ignored) {
                }
            }
        } else {
            // No attachment image; open system share with a text summary
            String share = buildShareSummary();
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, "Animal Report");
            i.putExtra(Intent.EXTRA_TEXT, share);
            startActivity(Intent.createChooser(i, "Share Report"));
        }
    }

    private String buildShareSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(tvHospitalName.getText()).append("\n")
                .append(tvHospitalAddress.getText()).append("\n\n")
                .append(tvDoctorName.getText()).append("\n")
                .append(tvReportType.getText()).append("\n\n")
                .append(tvOwnerName.getText()).append("\n")
                .append(tvAnimalAge.getText()).append("\n")
                .append(tvAnimalSex.getText()).append("\n")
                .append(tvAnimalWeight.getText()).append("\n")
                .append(tvAddress.getText()).append("\n")
                .append(tvDate.getText()).append("\n\n")
                .append(tvTemp.getText()).append("\n")
                .append(tvPulse.getText()).append("\n")
                .append(tvSpo2.getText()).append("\n")
                .append(tvBp.getText()).append("\n")
                .append(tvRespiratory.getText()).append("\n\n")
                .append("Symptoms: ").append(tvSymptoms.getText()).append("\n")
                .append("Investigations: ").append(tvInvestigations.getText());
        return sb.toString();
    }
}
