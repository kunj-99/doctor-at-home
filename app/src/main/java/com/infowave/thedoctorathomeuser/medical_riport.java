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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class medical_riport extends AppCompatActivity {

    private static final String TAG = "MedicalReport";

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
        setupSystemBarsBlackWithScrims();
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
        try { Glide.with(this).asGif().load(R.drawable.loader).into(ivLoader); } catch (Throwable ignored) {}

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
        String url = ApiConfig.endpoint("get_medical_report.php", "appointment_id", appointmentId);
        Log.d(TAG, "fetchMedicalReport → " + url);

        @SuppressLint("SetTextI18n")
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d(TAG, "Volley success, response: " + response);
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
                        Log.d(TAG, "report_photo: " + photoUrl);

                        if (!isEmpty(photoUrl)) {
                            // IMAGE MODE — keep loader visible until Glide finishes
                            reportPhotoUrl = photoUrl;
                            ivReportPhoto.setVisibility(View.VISIBLE);

                            // Hide other content (except Download button)
                            LinearLayout contentContainer = findViewById(R.id.content_container);
                            for (int i = 0; i < contentContainer.getChildCount(); i++) {
                                View child = contentContainer.getChildAt(i);
                                if (child.getId() != R.id.btn_download && child.getId() != R.id.iv_report_photo) {
                                    child.setVisibility(View.GONE);
                                }
                            }
                            btnDownload.setVisibility(View.VISIBLE);

                            loadReportPhotoWithBlockingLoader(photoUrl);

                        } else {
                            // TEXT MODE — fill all fields then hide loader immediately after binding
                            bindTextReport(data);
                            hideLoader();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Exception parsing response", e);
                        Toast.makeText(medical_riport.this, "Something went wrong while loading the report.", Toast.LENGTH_SHORT).show();
                        hideLoader();
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error: " + error);
                    Toast.makeText(medical_riport.this, "Unable to load your report. Please check your internet connection.", Toast.LENGTH_SHORT).show();
                    hideLoader();
                }
        );
        requestQueue.add(request);
    }

    /**
     * Loads the report image and ONLY hides the loader when the image either:
     *  - successfully loads (onResourceReady), or
     *  - fails (onLoadFailed).
     * Also adds a safety timeout to avoid infinite spinner.
     */
    private void loadReportPhotoWithBlockingLoader(String url) {
        Log.d(TAG, "Glide start → " + url);

        // Safety timeout (e.g., if the connection stalls); adjust as needed
        final long SAFETY_TIMEOUT_MS = 45000L; // 45s
        final Handler watchdog = new Handler();
        final Runnable timeout = () -> {
            Log.w(TAG, "Glide safety timeout reached → hiding loader, showing image view anyway");
            hideLoader();
        };
        watchdog.postDelayed(timeout, SAFETY_TIMEOUT_MS);

        Glide.with(medical_riport.this)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .dontAnimate()
                .placeholder(R.drawable.plasholder)      // remains under loader
                .error(R.drawable.error)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        Log.e(TAG, "Glide onLoadFailed", e);
                        hideLoader();
                        watchdog.removeCallbacks(timeout);
                        return false; // let Glide set error drawable
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.d(TAG, "Glide onResourceReady, dataSource=" + dataSource);
                        hideLoader();
                        watchdog.removeCallbacks(timeout);
                        return false; // continue with normal set
                    }
                })
                .into(ivReportPhoto);
    }

    @SuppressLint("SetTextI18n")
    private void bindTextReport(JSONObject data) {
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

        // Pretty multiline for comma-/array text
        tvSymptoms.setText(prettyMultiline("Symptoms", data.optString("symptoms", "")));
        tvInvestigations.setText(prettyMultiline("Investigations", data.optString("investigations", "")));

        tvDoctorName.setText("Doctor: " + safe(data.optString("doctor_name", "")));
        String doctorName = safe(data.optString("doctor_name", ""));
        tvHospitalName.setText("Dr. " + doctorName);
        tvHospitalAddress.setVisibility(View.GONE);

        // ===== Medications Table =====
        String rawMeds = data.optString("medications", "");
        String rawDosage = data.optString("dosage", "");

        List<String> meds = parseToList(rawMeds);
        List<String> doses = parseToList(rawDosage);

        if (meds.isEmpty() && !isEmpty(rawMeds)) meds = splitCommaFallback(rawMeds);
        if (doses.isEmpty() && !isEmpty(rawDosage)) doses = splitCommaFallback(rawDosage);

        buildMedicationTable(meds, doses);
    }

    private void hideLoader() {
        if (loader != null && loader.getVisibility() == View.VISIBLE) {
            Log.d(TAG, "hideLoader()");
            loader.setVisibility(View.GONE);
        }
    }

    // --- Helpers ---

    /** Convert JSON array string (e.g., ["PCM","dolo"]) to clean list. */
    private List<String> parseToList(String input) {
        List<String> out = new ArrayList<>();
        if (isEmpty(input)) return out;

        try {
            String s = input.trim();
            s = s.replace('\'', '"');
            if (!s.startsWith("[")) s = "[" + s + "]";
            s = s.replaceAll(",\\s*]", "]");

            JSONArray arr = new JSONArray(s);
            for (int i = 0; i < arr.length(); i++) {
                String v = String.valueOf(arr.get(i));
                v = cleanItem(v);
                if (!v.isEmpty()) out.add(v);
            }
        } catch (Exception ignore) { }
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

    /** Remove quotes/brackets and extra spaces; normalize internal spaces/commas. */
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
        if (table.getChildCount() > 1) table.removeViews(1, table.getChildCount() - 1);

        int rows = Math.max(meds.size(), doses.size());
        if (rows == 0) {
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
            Log.e(TAG, "PDF generation error", e);
            Toast.makeText(this, "Unable to generate PDF. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
    private void setupSystemBarsBlackWithScrims() {
        // 1) Draw behind system bars so our scrim Views can occupy those areas
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        // 2) Make *real* bars black (hard guarantee) and keep light icons over black
        window.setStatusBarColor(Color.BLACK);
        window.setNavigationBarColor(Color.BLACK);
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(false);      // light icons on dark bg
        controller.setAppearanceLightNavigationBars(false);  // light icons on dark bg

        // 3) Find your scrim Views (must exist in the layout)
        final View statusScrim = findViewById(R.id.status_bar_scrim);
        final View navScrim    = findViewById(R.id.navigation_bar_scrim);
        if (statusScrim == null || navScrim == null) return;

        // 4) Use the actual root view Android inflated for this content
        final ViewGroup content = findViewById(android.R.id.content);
        final View root = (content != null && content.getChildCount() > 0)
                ? content.getChildAt(0)
                : window.getDecorView();

        // 5) Size scrims to EXACT system bar heights (works with notches & any nav mode)
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets navBars    = insets.getInsets(WindowInsetsCompat.Type.navigationBars());

            // TOP scrim = status bar height
            ViewGroup.LayoutParams topLp = statusScrim.getLayoutParams();
            if (topLp.height != statusBars.top) {
                topLp.height = statusBars.top;
                statusScrim.setLayoutParams(topLp);
            }
            statusScrim.setBackgroundColor(Color.BLACK);
            statusScrim.setVisibility(statusBars.top > 0 ? View.VISIBLE : View.GONE);

            // BOTTOM scrim = nav bar height (0 on gesture nav without 3-button bar)
            ViewGroup.LayoutParams botLp = navScrim.getLayoutParams();
            if (botLp.height != navBars.bottom) {
                botLp.height = navBars.bottom;
                navScrim.setLayoutParams(botLp);
            }
            navScrim.setBackgroundColor(Color.BLACK);
            navScrim.setVisibility(navBars.bottom > 0 ? View.VISIBLE : View.GONE);

            // We handled insets via scrims.
            return WindowInsetsCompat.CONSUMED;
        });

        // 6) Trigger initial inset dispatch
        root.requestApplyInsets();
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
