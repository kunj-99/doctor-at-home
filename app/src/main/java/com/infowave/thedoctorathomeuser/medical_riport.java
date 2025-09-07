package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class medical_riport extends AppCompatActivity {

    private static final String GET_REPORT_URL = "http://sxm.a58.mytemp.website/get_medical_report.php?appointment_id=";
    private String appointmentId;
    private String reportPhotoUrl = "";

    private TextView tvHospitalName, tvHospitalAddress;
    private TextView tvPatientName, tvPatientAddress, tvVisitDate;
    private TextView tvPatientAge, tvPatientWeight, tvPatientSex;
    private TextView tvTemperature, tvPulse, tvSpo2, tvBloodPressure, tvRespiratory;
    private TextView tvSymptoms, tvInvestigations;
    private TextView tvDoctorName;
    private ImageButton btnBack;
    private ImageView ivReportPhoto;
    private Button btnDownload;

    private View loader;
    private ImageView ivLoader;

    private RequestQueue requestQueue;
    private static final String TAG = "MedicalReport";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appointmentId = getIntent().getStringExtra("appointment_id");
        if (appointmentId == null || appointmentId.isEmpty()) {
            Toast.makeText(this, "Invalid appointment. Please try again.", Toast.LENGTH_SHORT).show();
            redirectToHistoryFragment();
            return;
        }

        setContentView(R.layout.activity_medical_riport);

        tvHospitalName = findViewById(R.id.tv_hospital_name);
        tvHospitalAddress = findViewById(R.id.tv_hospital_address);
        tvPatientName = findViewById(R.id.tv_patient_name);
        tvPatientAddress = findViewById(R.id.tv_patient_address);
        tvVisitDate = findViewById(R.id.tv_visit_date);
        tvPatientAge = findViewById(R.id.tv_patient_age);
        tvPatientSex = findViewById(R.id.tv_patient_sex);
        tvPatientWeight = findViewById(R.id.tv_patient_weight);
        tvTemperature = findViewById(R.id.tv_temperature);
        tvPulse = findViewById(R.id.tv_pulse);
        tvSpo2 = findViewById(R.id.tv_spo2);
        tvBloodPressure = findViewById(R.id.tv_blood_pressure);
        tvRespiratory = findViewById(R.id.tv_respiratory);
        tvSymptoms = findViewById(R.id.tv_symptoms);
        tvInvestigations = findViewById(R.id.tv_investigations_content);
        tvDoctorName = findViewById(R.id.tv_doctor_name);
        ivReportPhoto = findViewById(R.id.iv_report_photo);
        btnBack = findViewById(R.id.btn_back);
        btnDownload = findViewById(R.id.btn_download);

        loader = findViewById(R.id.loader);
        ivLoader = findViewById(R.id.iv_loader);
        loader.setVisibility(View.VISIBLE);

        Glide.with(this).asGif().load(R.drawable.loader).into(ivLoader);

        btnBack.setOnClickListener(v -> finish());

        tvHospitalName.setText("VRAJ HOSPITAL");
        tvHospitalAddress.setText("150 Feet Ring Road, Rajkot - 360 004");

        requestQueue = Volley.newRequestQueue(this);
        fetchMedicalReport();
        btnDownload.setBackgroundColor(getResources().getColor(R.color.navy_blue));

        btnDownload.setOnClickListener(v -> {
            if (ivReportPhoto.getVisibility() == View.VISIBLE && !reportPhotoUrl.isEmpty()) {
                downloadImage(reportPhotoUrl);
            } else {
                generatePdf();
            }
        });
    }
    /** Returns a clean, user-facing value. Converts null/empty/"null" to "N/A". */
    private String safe(String s) {
        return (s == null || s.trim().isEmpty() || "null".equalsIgnoreCase(s.trim())) ? "N/A" : s.trim();
    }

    private void fetchMedicalReport() {
        String url = GET_REPORT_URL + appointmentId;

        @SuppressLint("SetTextI18n")
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String status = response.optString("status", "");
                        if (!"success".equalsIgnoreCase(status)) {
                            Toast.makeText(medical_riport.this, "Report not found.", Toast.LENGTH_SHORT).show();
                            redirectToHistoryFragment();
                            return;
                        }

                        JSONObject data = response.optJSONObject("data");
                        if (data == null) {
                            Toast.makeText(medical_riport.this, "Report not found.", Toast.LENGTH_SHORT).show();
                            redirectToHistoryFragment();
                            return;
                        }

                        String photoUrl = data.optString("report_photo", "");
                        if (!photoUrl.isEmpty()) {
                            reportPhotoUrl = photoUrl;
                            ivReportPhoto.setVisibility(View.VISIBLE);
                            LinearLayout contentContainer = findViewById(R.id.content_container);
                            for (int i = 0; i < contentContainer.getChildCount(); i++) {
                                View child = contentContainer.getChildAt(i);
                                if (child.getId() != R.id.btn_download) child.setVisibility(View.GONE);
                            }
                            btnDownload.setVisibility(View.VISIBLE);

                            Glide.with(medical_riport.this)
                                    .load(photoUrl)
                                    .error(R.drawable.error)
                                    .into(ivReportPhoto);

                        } else {
                            // Basic details
                            tvPatientName.setText("Name: " + safe(data.optString("patient_name", "")));
                            tvPatientAddress.setText("Address: " + safe(data.optString("patient_address", "")));
                            tvVisitDate.setText("Date: " + safe(data.optString("visit_date", "")));
                            tvTemperature.setText("Temperature: " + safe(data.optString("temperature", "")));
                            tvPatientAge.setText("Age: " + safe(data.optString("age", "")) + (isEmpty(data.optString("age", "")) ? "" : " Years"));
                            tvPatientWeight.setText("Weight: " + safe(data.optString("weight", "")) + (isEmpty(data.optString("weight", "")) ? "" : " kg"));
                            tvPatientSex.setText("Sex: " + safe(data.optString("sex", "")));
                            tvPulse.setText("Pulse: " + safe(data.optString("pulse", "")));
                            tvSpo2.setText("SP02: " + safe(data.optString("spo2", "")));
                            tvBloodPressure.setText("Blood Pressure: " + safe(data.optString("blood_pressure", "")));
                            tvRespiratory.setText("Respiratory: " + safe(data.optString("respiratory_system", "")));

                            // Pretty multiline for comma-separated text
                            tvSymptoms.setText(prettyMultiline("Symptoms", data.optString("symptoms", "")));
                            tvInvestigations.setText(prettyMultiline("Investigations", data.optString("investigations", "")));

                            tvDoctorName.setText("Doctor: " + safe(data.optString("doctor_name", "")));
                            // Replace hospital header with doctor’s name
                            String doctorName = safe(data.optString("doctor_name", ""));
                            tvHospitalName.setText("Dr. " + doctorName);
                            tvHospitalAddress.setVisibility(View.GONE);

                            //tvHospitalAddress.setText("");  // optional: remove address line


                            // ===== Medications Table =====
                            String rawMeds = data.optString("medications", "");
                            String rawDosage = data.optString("dosage", "");

                            List<String> meds = parseToList(rawMeds);
                            List<String> doses = parseToList(rawDosage);

                            // If backend sent a single long comma string, split as fallback
                            if (meds.isEmpty() && !isEmpty(rawMeds)) meds = splitCommaFallback(rawMeds);
                            if (doses.isEmpty() && !isEmpty(rawDosage)) doses = splitCommaFallback(rawDosage);

                            buildMedicationTable(meds, doses);
                        }
                    } catch (Exception e) {
                        Toast.makeText(medical_riport.this, "Something went wrong while loading the report.", Toast.LENGTH_SHORT).show();
                    } finally {
                        new Handler().postDelayed(() -> loader.setVisibility(View.GONE), 500);
                    }
                },
                error -> {
                    Toast.makeText(medical_riport.this, "Unable to load your report. Please check your internet connection.", Toast.LENGTH_SHORT).show();
                    new Handler().postDelayed(() -> loader.setVisibility(View.GONE), 500);
                }
        );
        requestQueue.add(request);
    }

    // --- Helpers ---

    /** Convert JSON array string (e.g., ["PCM","dolo"]) to clean list. */
    private List<String> parseToList(String input) {
        List<String> out = new ArrayList<>();
        if (isEmpty(input)) return out;

        try {
            // Sanitize common backend variations
            String s = input.trim();
            // fix single quotes -> double quotes; stray whitespace; trailing commas
            s = s.replace('\'', '"');
            if (!s.startsWith("[")) s = "[" + s + "]";       // if "PCM","dolo"
            s = s.replaceAll(",\\s*]", "]");

            JSONArray arr = new JSONArray(s);
            for (int i = 0; i < arr.length(); i++) {
                String v = String.valueOf(arr.get(i));
                v = cleanItem(v);
                if (!v.isEmpty()) out.add(v);
            }
        } catch (Exception ignore) {
            // Will fallback to comma split by caller if needed
        }
        return out;
    }

    /** Fallback when backend sends comma-separated text, not JSON. */
    private List<String> splitCommaFallback(String s) {
        List<String> list = new ArrayList<>();
        for (String part : s.split(",")) {
            String v = cleanItem(part);
            if (!v.isEmpty()) list.add(v);
        }
        return list;
    }

    /** Remove quotes/brackets and extra spaces; normalize internal multiple spaces/commas. */
    private String cleanItem(String v) {
        if (v == null) return "";
        v = v.replace("[", "").replace("]", "").replace("\"", "").trim();
        v = v.replaceAll("\\s{2,}", " ");
        v = v.replaceAll("\\s*,\\s*", ", ");
        return v;
    }

    /** Build the medication table rows professionally. */
    private void buildMedicationTable(List<String> meds, List<String> doses) {
        TableLayout table = findViewById(R.id.table_medications);
        // Remove old rows except header (index 0)
        if (table.getChildCount() > 1) table.removeViews(1, table.getChildCount() - 1);

        int rows = Math.max(meds.size(), doses.size());
        if (rows == 0) {
            // Show a single row with "None"
            TableRow row = new TableRow(this);
            row.addView(colText("#", true));
            row.addView(colText("None", false));
            row.addView(colText("-", false));
            table.addView(row);
            addDivider(table);
            return;
        }

        for (int i = 0; i < rows; i++) {
            TableRow row = new TableRow(this);
            row.setPadding(0, dp(6), 0, dp(6));

            TextView c1 = colText(String.valueOf(i + 1), true);
            TextView c2 = colText(i < meds.size() ? meds.get(i) : "", false);
            TextView c3 = colText(i < doses.size() ? doses.get(i) : "", false);

            row.addView(c1);
            row.addView(c2);
            row.addView(c3);
            table.addView(row);
            addDivider(table);
        }
    }

    /** Styled column text. */
    private TextView colText(String s, boolean center) {
        TextView tv = new TextView(this);
        tv.setText(isEmpty(s) ? "-" : s);
        tv.setTextSize(16);
        tv.setPadding(dp(4), dp(2), dp(4), dp(2));
        tv.setSingleLine(false);
        tv.setMaxLines(3);
        tv.setEllipsize(null);
        if (center) tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        return tv;
    }

    /** Thin divider between rows for a cleaner table look. */
    private void addDivider(TableLayout table) {
        View divider = new View(this);
        divider.setLayoutParams(new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT, dp(1)));
        divider.setBackgroundColor(Color.parseColor("#1F000000"));
        table.addView(divider);
    }

    /** Make "Label: value" with value split to new lines by comma (no brackets/quotes). */
    private String prettyMultiline(String label, String raw) {
        if (isEmpty(raw) || "null".equalsIgnoreCase(raw) || "[]".equals(raw)) {
            return label + ": None";
        }
        // If backend already sends JSON-like array use parser
        List<String> parts = parseToList(raw);
        if (parts.isEmpty()) parts = splitCommaFallback(raw);

        StringBuilder b = new StringBuilder(label).append(": ");
        for (int i = 0; i < parts.size(); i++) {
            b.append(parts.get(i));
            if (i < parts.size() - 1) b.append("\n");
        }
        return b.toString();
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty() || "null".equalsIgnoreCase(s.trim());
    }

    private int dp(int v) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (v * scale + 0.5f);
    }

    private void downloadImage(String url) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "medical_report.jpg");
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);
        Toast.makeText(this, "Your report image is downloading...", Toast.LENGTH_SHORT).show();
    }

    private void generatePdf() {
        View content = findViewById(R.id.content_container);
        btnDownload.setVisibility(View.GONE);
        Bitmap bitmap = getBitmapFromView(content);
        btnDownload.setVisibility(View.VISIBLE);

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        canvas.drawBitmap(bitmap, 0, 0, null);
        document.finishPage(page);

        String fileName = "medical_report.pdf";
        File pdfFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
        try {
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();
            Toast.makeText(this, "PDF saved in your Downloads folder.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to generate PDF. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap getBitmapFromView(View view) {
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null) bgDrawable.draw(canvas);
        else canvas.drawColor(Color.WHITE);
        view.draw(canvas);
        return returnedBitmap;
    }

    private void redirectToHistoryFragment() {
        Intent intent = new Intent(medical_riport.this, MainActivity.class);
        intent.putExtra("open_fragment", 3);
        startActivity(intent);
        overridePendingTransition(1, 1);
        finish();
    }
}
