package com.infowave.thedoctorathomeuser;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Profile extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;

    private ImageView civProfile;
    private EditText etFullName, etDOB, etAddress, etMobile, etEmail, etMedicalHistory,
            etAllergies, etCurrentMedications, etEmergencyName, etEmergencyNumber;
    private Spinner spinnerGender, spinnerBloodGroup;
    private Button btnUpdate;

    private ProgressDialog progressDialog;
    private RequestQueue requestQueue;
    private int patientId;

    private static final String UPDATE_PROFILE_URL = ApiConfig.endpoint("update_profile.php");

    private Bitmap selectedBitmap = null;
    private String oldImageUrl = "";

    // ==== System bar & layout refs (no fragments) ====
    private View root;                // R.id.profile_root
    private View statusBarScrim;      // R.id.status_bar_scrim
    private View navBarScrim;         // R.id.navigation_bar_scrim
    private ScrollView scrollView;    // R.id.profile_scroll

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // ===== System bars via views =====
        root = findViewById(R.id.profile_root);
        statusBarScrim = findViewById(R.id.status_bar_scrim);
        navBarScrim = findViewById(R.id.navigation_bar_scrim);
        scrollView = findViewById(R.id.profile_scroll);

        // Draw behind bars so scrims can cover them
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        // White icons on black bars
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), root);
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        // Ensure scrims are above all content
        statusBarScrim.bringToFront();
        navBarScrim.bringToFront();

        // Size scrims and pad content based on real insets + keyboard
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            setHeight(statusBarScrim, sys.top);
            setHeight(navBarScrim, sys.bottom);

            int bottomPad = Math.max(sys.bottom, ime.bottom);
            scrollView.setPadding(
                    scrollView.getPaddingLeft(),
                    sys.top, // push content below status bar
                    scrollView.getPaddingRight(),
                    bottomPad + dp(24) // keep your original 24dp space
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        // Keep adjustResize for smooth form behavior
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                        | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );

        // ===== Your existing logic (unchanged) =====
        SharedPreferences sp = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String patientIdStr = sp.getString("patient_id", "");
        if (patientIdStr.isEmpty()) {
            finish();
            return;
        }
        patientId = Integer.parseInt(patientIdStr);

        ImageView btnBack = findViewById(R.id.iv_back_arrow);
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(Profile.this, MainActivity.class));
            finish();
        });

        civProfile = findViewById(R.id.civ_profile);
        etFullName = findViewById(R.id.et_full_name);
        etDOB = findViewById(R.id.et_date_of_birth);
        spinnerGender = findViewById(R.id.spinner_gender);
        spinnerBloodGroup = findViewById(R.id.spinner_blood_group);
        etAddress = findViewById(R.id.et_address);
        etMobile = findViewById(R.id.et_mobile);
        etEmail = findViewById(R.id.et_email);
        etEmergencyName = findViewById(R.id.et_emergency_contact_name);
        etEmergencyNumber = findViewById(R.id.et_emergency_contact_number);
        etMedicalHistory = findViewById(R.id.et_medical_history);
        etAllergies = findViewById(R.id.et_allergies);
        etCurrentMedications = findViewById(R.id.et_current_medications);
        btnUpdate = findViewById(R.id.btn_update_profile);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        requestQueue = Volley.newRequestQueue(this);

        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(
                this, R.array.gender_array, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);

        ArrayAdapter<CharSequence> bloodGroupAdapter = ArrayAdapter.createFromResource(
                this, R.array.blood_group_array, android.R.layout.simple_spinner_item);
        bloodGroupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(bloodGroupAdapter);

        etDOB.setFocusable(false);
        etDOB.setClickable(true);
        etDOB.setOnClickListener(v -> showDatePicker());

        civProfile.setOnClickListener(v -> openGallery());
        btnUpdate.setOnClickListener(v -> {
            try {
                updateProfile();
            } catch (AuthFailureError ignored) {}
        });

        attachFocusAutoScroll(
                etFullName, etDOB, etAddress, etMobile, etEmail,
                etEmergencyName, etEmergencyNumber, etMedicalHistory,
                etAllergies, etCurrentMedications
        );

        fetchProfile();
    }

    // ===== System bar helpers =====
    private void setHeight(View v, int h) {
        if (v == null) return;
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp.height != h) {
            lp.height = h;
            v.setLayoutParams(lp);
        }
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    // ===== Keyboard auto-scroll helpers =====
    private void attachFocusAutoScroll(EditText... fields) {
        for (EditText f : fields) {
            f.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) scrollIntoView((EditText) v);
            });
        }
    }

    private ScrollView findNearestScroll(View v) {
        ViewParent p = v.getParent();
        while (p instanceof View) {
            if (p instanceof ScrollView) return (ScrollView) p;
            p = p.getParent();
        }
        return null;
    }

    private void scrollIntoView(EditText field) {
        ScrollView sv = findNearestScroll(field);
        if (sv != null) {
            sv.post(() -> {
                int y = field.getBottom() + sv.getPaddingBottom();
                sv.smoothScrollTo(0, Math.max(0, y - sv.getHeight()));
            });
        } else {
            root.post(() -> field.getParent().requestChildFocus(field, field));
        }
    }

    // ===== Gallery & Date Picker =====
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                civProfile.setImageBitmap(bitmap);
                selectedBitmap = bitmap;
            } catch (IOException e) {
                Toast.makeText(this, "Could not load image. Please try a different photo.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        String dobText = etDOB.getText().toString();
        if (!dobText.isEmpty()) {
            try {
                Date date = sdf.parse(dobText);
                if (date != null) calendar.setTime(date);
            } catch (ParseException ignored) {}
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dlg = new DatePickerDialog(this,
                (DatePicker view, int Y, int M, int D) -> {
                    Calendar sel = Calendar.getInstance();
                    sel.set(Y, M, D);
                    if (sel.after(Calendar.getInstance())) {
                        Toast.makeText(this, "Birth date cannot be in the future.", Toast.LENGTH_SHORT).show();
                    } else {
                        etDOB.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", D, M + 1, Y));
                    }
                }, year, month, day);

        dlg.getDatePicker().setMaxDate(System.currentTimeMillis());
        dlg.show();
    }

    // ===== Networking: fetch & update profile =====
    private void fetchProfile() {
        progressDialog.show();

        String url = ApiConfig.endpoint("get_profile.php", "patient_id", String.valueOf(patientId));
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressDialog.dismiss();
                    try {
                        if ("success".equals(response.optString("status"))) {
                            JSONObject data = response.optJSONObject("data");
                            if (data == null) {
                                Toast.makeText(this, "Profile not found.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            setTextNoNull(etFullName,           clean(data.optString("full_name", "")));
                            setTextNoNull(etDOB,                clean(data.optString("date_of_birth", "")));
                            setSpinnerSelection(spinnerGender,  clean(data.optString("gender", "")));
                            setSpinnerSelection(spinnerBloodGroup, clean(data.optString("blood_group", "")));
                            setTextNoNull(etAddress,            clean(data.optString("address", "")));
                            setTextNoNull(etMobile,             clean(data.optString("mobile", "")));
                            setTextNoNull(etEmail,              clean(data.optString("email", "")));
                            setTextNoNull(etEmergencyName,      clean(data.optString("emergency_contact_name", "")));
                            setTextNoNull(etEmergencyNumber,    clean(data.optString("emergency_contact_number", "")));
                            setTextNoNull(etMedicalHistory,     clean(data.optString("medical_history", "")));
                            setTextNoNull(etAllergies,          clean(data.optString("allergies", "")));
                            setTextNoNull(etCurrentMedications, clean(data.optString("current_medications", "")));

                            String profilePictureUrl = clean(data.optString("profile_picture", ""));
                            if (!TextUtils.isEmpty(profilePictureUrl)) {
                                Glide.with(this).load(profilePictureUrl).into(civProfile);
                                int lastSlash = profilePictureUrl.lastIndexOf('/');
                                oldImageUrl = (lastSlash != -1)
                                        ? profilePictureUrl.substring(lastSlash + 1)
                                        : profilePictureUrl;
                            } else {
                                oldImageUrl = "";
                            }
                        } else {
                            Toast.makeText(this, "Profile not found.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not load your profile. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Unable to connect. Please check your internet and try again.", Toast.LENGTH_SHORT).show();
                });
        requestQueue.add(req);
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (TextUtils.isEmpty(value)) return;
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        if (adapter != null) {
            for (int i = 0, count = adapter.getCount(); i < count; i++) {
                if (adapter.getItem(i).toString().equalsIgnoreCase(value)) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }

    private void updateProfile() throws AuthFailureError {
        if (TextUtils.isEmpty(etFullName.getText().toString())) {
            etFullName.setError("Please enter your full name.");
            etFullName.requestFocus();
            return;
        }
        progressDialog.show();

        if (selectedBitmap != null) {
            VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(
                    Request.Method.POST, UPDATE_PROFILE_URL,
                    response -> {
                        progressDialog.dismiss();
                        try {
                            JSONObject jsonResponse = new JSONObject(new String(response.data));
                            if ("success".equals(jsonResponse.optString("status"))) {
                                Toast.makeText(this, "Your profile was updated successfully!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            } else {
                                Toast.makeText(this, "Could not update your profile. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Unable to update profile. Please check your internet and try again.", Toast.LENGTH_SHORT).show();
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    return buildCommonParams();
                }

                @Override
                protected Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();
                    params.put("profile_image", new DataPart("profile.jpg",
                            getFileDataFromDrawable(selectedBitmap), "image/jpeg"));
                    return params;
                }
            };
            requestQueue.add(multipartRequest);
        } else {
            StringRequest stringRequest = new StringRequest(Request.Method.POST, UPDATE_PROFILE_URL,
                    response -> {
                        progressDialog.dismiss();
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if ("success".equals(jsonResponse.optString("status"))) {
                                Toast.makeText(this, "Your profile was updated successfully!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            } else {
                                Toast.makeText(this, "Could not update your profile. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Unable to update profile. Please check your internet and try again.", Toast.LENGTH_SHORT).show();
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> p = buildCommonParams();
                    p.put("profile_image", oldImageUrl); // keep existing if not replaced
                    return p;
                }
            };
            requestQueue.add(stringRequest);
        }
    }

    private Map<String, String> buildCommonParams() {
        Map<String, String> params = new HashMap<>();
        params.put("patient_id", String.valueOf(patientId));
        params.put("full_name", etFullName.getText().toString().trim());

        String dobInput = etDOB.getText().toString().trim();
        if (!dobInput.isEmpty() && dobInput.contains("/")) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date dobDate = inputFormat.parse(dobInput);
                if (dobDate != null) dobInput = outputFormat.format(dobDate);
            } catch (ParseException ignored) {}
        }
        params.put("date_of_birth", dobInput);

        params.put("gender", String.valueOf(spinnerGender.getSelectedItem()));
        params.put("blood_group", String.valueOf(spinnerBloodGroup.getSelectedItem()));
        params.put("address", etAddress.getText().toString().trim());
        params.put("mobile", etMobile.getText().toString().trim());
        params.put("email", etEmail.getText().toString().trim());
        params.put("emergency_contact_name", etEmergencyName.getText().toString().trim());
        params.put("emergency_contact_number", etEmergencyNumber.getText().toString().trim());
        params.put("medical_history", etMedicalHistory.getText().toString().trim());
        params.put("allergies", etAllergies.getText().toString().trim());
        params.put("current_medications", etCurrentMedications.getText().toString().trim());
        return params;
    }

    private void setTextNoNull(EditText et, String value) {
        et.setText(value == null ? "" : value);
    }

    /** Convert null/"null"/"undefined"/whitespace → "" else trimmed value */
    private String clean(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.isEmpty()) return "";
        String low = t.toLowerCase(Locale.ROOT);
        if (low.equals("null") || low.equals("undefined")) return "";
        return t;
    }

    public byte[] getFileDataFromDrawable(Bitmap bitmap) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
        return bos.toByteArray();
    }

    // ===== Multipart helper classes (unchanged pattern) =====
    public abstract class VolleyMultipartRequest extends com.android.volley.Request<NetworkResponse> {
        private final Response.Listener<NetworkResponse> mListener;
        private final Map<String, String> mParams;
        private final Map<String, DataPart> mByteData;
        private final String boundary = "apiclient-" + System.currentTimeMillis();

        public VolleyMultipartRequest(int method, String url,
                                      Response.Listener<NetworkResponse> listener,
                                      Response.ErrorListener errorListener) throws AuthFailureError {
            super(method, url, errorListener);
            this.mListener = listener;
            this.mParams = getParams() != null ? getParams() : new HashMap<>();
            this.mByteData = getByteData() != null ? getByteData() : new HashMap<>();
        }

        @Override
        public String getBodyContentType() {
            return "multipart/form-data;boundary=" + boundary;
        }

        @Override
        public byte[] getBody() throws AuthFailureError {
            return buildMultipartBody(mParams, mByteData);
        }

        protected abstract Map<String, DataPart> getByteData();

        @Override
        protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
            return Response.success(response, null);
        }

        @Override
        protected void deliverResponse(NetworkResponse response) {
            mListener.onResponse(response);
        }

        private byte[] buildMultipartBody(Map<String, String> params, Map<String, DataPart> dataParts) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                String lineEnd = "\r\n";
                String twoHyphens = "--";

                for (Map.Entry<String, String> entry : params.entrySet()) {
                    bos.write((twoHyphens + boundary + lineEnd).getBytes());
                    bos.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + lineEnd).getBytes());
                    bos.write(("Content-Type: text/plain; charset=UTF-8" + lineEnd).getBytes());
                    bos.write(lineEnd.getBytes());
                    bos.write((entry.getValue() == null ? "" : entry.getValue()).getBytes());
                    bos.write(lineEnd.getBytes());
                }

                for (Map.Entry<String, DataPart> entry : dataParts.entrySet()) {
                    DataPart dp = entry.getValue();
                    bos.write((twoHyphens + boundary + lineEnd).getBytes());
                    bos.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"; filename=\"" + dp.getFileName() + "\"" + lineEnd).getBytes());
                    bos.write(("Content-Type: " + dp.getType() + lineEnd).getBytes());
                    bos.write(lineEnd.getBytes());
                    bos.write(dp.getContent());
                    bos.write(lineEnd.getBytes());
                }

                bos.write((twoHyphens + boundary + twoHyphens + lineEnd).getBytes());
                return bos.toByteArray();
            } catch (IOException e) {
                return new byte[0];
            }
        }
    }

    public class DataPart {
        private final String fileName;
        private final byte[] content;
        private final String type;

        public DataPart(String fileName, byte[] content, String type) {
            this.fileName = fileName;
            this.content = content;
            this.type = type;
        }
        public String getFileName() { return fileName; }
        public byte[] getContent() { return content; }
        public String getType() { return type; }
    }
}
