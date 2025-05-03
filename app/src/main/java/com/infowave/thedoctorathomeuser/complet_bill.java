package com.infowave.thedoctorathomeuser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class complet_bill extends AppCompatActivity {

    Button Download;
    ScrollView billLayout;
    int appointmentId;

    private static final String TAG = "CompleteBill";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complet_bill);

        appointmentId = getIntent().getIntExtra("appointment_id", -1);
        Log.d(TAG, "Received appointment_id: " + appointmentId);

        if (appointmentId == -1) {
            Toast.makeText(this, "Invalid appointment ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Download = findViewById(R.id.download_button);
        billLayout = findViewById(R.id.bill_layout);

        Download.setOnClickListener(v -> {
            if (checkPermissions()) {
                generatePDF();
            } else {
                requestPermissions();
            }
        });

        fetchBillDetails(appointmentId);
    }

    private void fetchBillDetails(int appointmentId) {
        String url = "http://sxm.a58.mytemp.website/fetch_user_payment_history.php?appointment_id=" + appointmentId;
        Log.d(TAG, "Fetching bill from URL: " + url);

        // Show loader
        loaderutil.showLoader(this);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    loaderutil.hideLoader(); // Hide loader on success
                    try {
                        Log.d(TAG, "Response received: " + response);
                        if (response.getBoolean("success")) {
                            JSONArray data = response.getJSONArray("data");
                            if (data.length() > 0) {
                                JSONObject bill = data.getJSONObject(0);
                                Log.d(TAG, "Parsed bill data: " + bill);
                                populateBillUI(bill);
                            } else {
                                Toast.makeText(this, "No bill found.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "No bill found.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response: " + e.getMessage());
                        e.printStackTrace();
                    }
                },
                error -> {
                    loaderutil.hideLoader(); // Hide loader on error
                    Log.e(TAG, "Volley error: " + error);
                    Toast.makeText(this, "Error fetching bill", Toast.LENGTH_SHORT).show();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

    @SuppressLint("SetTextI18n")
    private void populateBillUI(JSONObject bill) {
        try {
            setTextSafe(R.id.tv_bill_date, bill.getString("created_at"));
            setTextSafe(R.id.tv_bill_patient_name, bill.getString("patient_name"));
            setTextSafe(R.id.tv_bill_doctor_name, bill.optString("doctor_name", "N/A"));

            setTextSafe(R.id.tv_appointment_charge, "₹ " + bill.getString("amount"));
            setTextSafe(R.id.tv_consultation_fee, "₹ " + bill.getString("consultation_fee"));
            setTextSafe(R.id.tv_deposit, "₹ " + bill.getString("deposit"));
            setTextSafe(R.id.tv_distance_km_value, bill.getString("distance"));
            setTextSafe(R.id.tv_distance_charge_value, "₹ " + bill.getString("distance_charge"));
            setTextSafe(R.id.tv_gst_value, "₹ " + bill.getString("gst"));
            setTextSafe(R.id.tv_total_paid_value, "₹ " + bill.getString("total_payment"));

            setTextSafe(R.id.tv_payment_status, bill.optString("payment_status", "N/A"));
            setTextSafe(R.id.tv_refund_status, bill.optString("refund_status", "N/A"));
            setTextSafe(R.id.tv_notes, bill.optString("notes", "-"));
            setTextSafe(R.id.tv_payment_method, bill.optString("payment_method", "N/A")); // ✅ NEW

            Log.i(TAG, "UI populated successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error populating UI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setTextSafe(int id, String value) {
        TextView tv = findViewById(id);
        if (tv != null) {
            tv.setText(value);
        } else {
            Log.e(TAG, "Missing TextView with ID: " + getResources().getResourceEntryName(id));
        }
    }

    private boolean checkPermissions() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            generatePDF();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            generatePDF();
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void generatePDF() {
        Download.setVisibility(View.GONE); // Hide button during generation

        PdfDocument pdfDocument = new PdfDocument();
        Bitmap bitmap = getBitmapFromView(billLayout);

        if (bitmap == null) {
            Toast.makeText(this, "Error capturing layout", Toast.LENGTH_SHORT).show();
            Download.setVisibility(View.VISIBLE);
            return;
        }

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        canvas.drawPaint(paint);
        canvas.drawBitmap(bitmap, 0, 0, null);
        pdfDocument.finishPage(page);

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String fileName = "Your_Bill_" + date + ".pdf";

        try {
            OutputStream outputStream;
            Uri pdfUri;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                pdfUri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
                if (pdfUri == null) {
                    Toast.makeText(this, "Failed to create file", Toast.LENGTH_SHORT).show();
                    Download.setVisibility(View.VISIBLE);
                    return;
                }
                outputStream = getContentResolver().openOutputStream(pdfUri);
            } else {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
                outputStream = new FileOutputStream(file);
                pdfUri = Uri.fromFile(file);
            }

            pdfDocument.writeTo(outputStream);
            outputStream.close();
            pdfDocument.close();

            Toast.makeText(this, "PDF Downloaded: " + fileName, Toast.LENGTH_LONG).show();
            Log.i(TAG, "PDF saved as: " + fileName);

            // Open the PDF
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);

        } catch (IOException e) {
            Log.e(TAG, "Error creating PDF: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error creating PDF", Toast.LENGTH_SHORT).show();
        } finally {
            Download.setVisibility(View.VISIBLE); // Show button again in all cases
        }
    }



    private Bitmap getBitmapFromView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }
}
