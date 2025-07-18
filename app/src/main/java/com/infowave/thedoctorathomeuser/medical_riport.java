package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
//import android.util.Log;
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

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class medical_riport extends AppCompatActivity {

    private static final String GET_REPORT_URL = "https://thedoctorathome.in/get_medical_report.php?appointment_id=";
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
//        Log.d(TAG, "Received appointment ID: " + appointmentId);
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

        Glide.with(this)
                .asGif()
                .load(R.drawable.loader)
                .into(ivLoader);

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

    private void fetchMedicalReport() {
        String url = GET_REPORT_URL + appointmentId;
//        Log.d(TAG, "Fetching report from URL: " + url);

        @SuppressLint("SetTextI18n") JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
//                    Log.d(TAG, "API Response received: " + response);
                    try {
                        String status = response.optString("status", "");
                        if (!status.equalsIgnoreCase("success")) {
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
                                if (child.getId() != R.id.btn_download) {
                                    child.setVisibility(View.GONE);
                                }
                            }
                            btnDownload.setVisibility(View.VISIBLE);

                            Glide.with(medical_riport.this)
                                    .load(photoUrl)
                                    .error(R.drawable.error)
                                    .into(ivReportPhoto);

                        } else {
                            tvPatientName.setText("Name: " + data.optString("patient_name", "N/A"));
                            tvPatientAddress.setText("Address: " + data.optString("patient_address", "N/A"));
                            tvVisitDate.setText("Date: " + data.optString("visit_date", "N/A"));
                            tvTemperature.setText("Temperature: " + data.optString("temperature", "N/A"));
                            tvPatientAge.setText("Age: " + data.optString("age", "N/A") + " Years");
                            tvPatientWeight.setText("Weight: " + data.optString("weight", "N/A") + " kg");
                            tvPatientSex.setText("Sex: " + data.optString("sex", "N/A"));
                            tvPulse.setText("Pulse: " + data.optString("pulse", "N/A"));
                            tvSpo2.setText("SP02: " + data.optString("spo2", "N/A"));
                            tvBloodPressure.setText("Blood Pressure: " + data.optString("blood_pressure", "N/A"));
                            tvRespiratory.setText("Respiratory: " + data.optString("respiratory_system", "N/A"));
                            tvSymptoms.setText("Symptoms: " + data.optString("symptoms", "N/A"));
                            tvInvestigations.setText("Investigations: " + data.optString("investigations", "N/A"));
                            tvDoctorName.setText("Doctor: " + data.optString("doctor_name", "N/A"));

                            // Table for medications
                            String medicationsStr = data.optString("medications", "");
                            String dosageStr = data.optString("dosage", "");
                            String[] medicationsArray = medicationsStr.split("\\n");
                            String[] dosageArray = dosageStr.split("\\n");
                            List<String> medList = new ArrayList<>();
                            for (String med : medicationsArray) {
                                med = med.trim();
                                if (!med.isEmpty()) {
                                    if (med.endsWith(",")) med = med.substring(0, med.length() - 1);
                                    medList.add(med);
                                }
                            }
                            List<String> dosageList = new ArrayList<>();
                            for (String dos : dosageArray) {
                                dos = dos.trim();
                                if (!dos.isEmpty()) {
                                    if (dos.endsWith(",")) dos = dos.substring(0, dos.length() - 1);
                                    dosageList.add(dos);
                                }
                            }

                            int rowCount = Math.max(medList.size(), dosageList.size());
                            TableLayout tableMedications = findViewById(R.id.table_medications);
                            if (tableMedications.getChildCount() > 1) {
                                tableMedications.removeViews(1, tableMedications.getChildCount() - 1);
                            }
                            for (int i = 0; i < rowCount; i++) {
                                TableRow row = new TableRow(medical_riport.this);
                                TextView tvNo = new TextView(medical_riport.this);
                                tvNo.setText(String.valueOf(i + 1));
                                tvNo.setTextSize(16);
                                tvNo.setPadding(4, 4, 4, 4);

                                TextView tvMedName = new TextView(medical_riport.this);
                                tvMedName.setText(i < medList.size() ? medList.get(i) : "");
                                tvMedName.setTextSize(16);
                                tvMedName.setPadding(4, 4, 4, 4);

                                TextView tvDosage = new TextView(medical_riport.this);
                                tvDosage.setText(i < dosageList.size() ? dosageList.get(i) : "");
                                tvDosage.setTextSize(16);
                                tvDosage.setPadding(4, 4, 4, 4);

                                row.addView(tvNo);
                                row.addView(tvMedName);
                                row.addView(tvDosage);
                                tableMedications.addView(row);
                            }
                        }
                    } catch (Exception e) {
//                        Log.e(TAG, "Exception during parsing: " + e.getMessage(), e);
                        Toast.makeText(medical_riport.this, "Something went wrong while loading the report.", Toast.LENGTH_SHORT).show();
                    } finally {
                        new Handler().postDelayed(() -> loader.setVisibility(View.GONE), 1000);
                    }
                },
                error -> {
//                    Log.e(TAG, "Volley error: " + error.getMessage(), error);
                    Toast.makeText(medical_riport.this, "Unable to load your report. Please check your internet connection.", Toast.LENGTH_SHORT).show();
                    new Handler().postDelayed(() -> loader.setVisibility(View.GONE), 1000);
                }
        );
        requestQueue.add(request);
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
        if (bgDrawable != null)
            bgDrawable.draw(canvas);
        else
            canvas.drawColor(Color.WHITE);
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
