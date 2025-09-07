package com.infowave.thedoctorathomeuser;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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

import de.hdodenhof.circleimageview.CircleImageView;

public class Profile extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    // private static final String TAG = "Profile"; // Commented for production

    private ImageView civProfile;
    private EditText etFullName, etDOB, etAddress, etMobile, etEmail, etMedicalHistory,
            etAllergies, etCurrentMedications, etEmergencyName, etEmergencyNumber;
    private Spinner spinnerGender, spinnerBloodGroup;
    private Button btnUpdate;

    private ProgressDialog progressDialog;
    private RequestQueue requestQueue;
    private int patientId;

    private static final String GET_PROFILE_URL = "http://sxm.a58.mytemp.website/get_profile.php?patient_id=";
    private static final String UPDATE_PROFILE_URL = "http://sxm.a58.mytemp.website/update_profile.php";

    private Bitmap selectedBitmap = null;
    private String oldImageUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        SharedPreferences sp = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String patientIdStr = sp.getString("patient_id", "");
        if (patientIdStr.isEmpty()) {
            // Log.e(TAG, "Patient ID not found");
            finish();
            return;
        }

        ImageView btnBack = findViewById(R.id.iv_back_arrow);
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(Profile.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        patientId = Integer.parseInt(patientIdStr);

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
            } catch (AuthFailureError e) {
                // Log.e(TAG, "AuthFailureError in updateProfile", e);
            }
        });
        fetchProfile();
    }

    public void openGallery() {
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
                // Log.e(TAG, "Error loading image", e);
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
                calendar.setTime(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(Profile.this,
                (DatePicker view, int selectedYear, int selectedMonth, int selectedDay) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay);
                    Calendar currentCalendar = Calendar.getInstance();
                    if (selectedCalendar.after(currentCalendar)) {
                        Toast.makeText(Profile.this, "Birth date cannot be in the future.", Toast.LENGTH_SHORT).show();
                    } else {
                        String formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d",
                                selectedDay, selectedMonth + 1, selectedYear);
                        etDOB.setText(formattedDate);
                    }
                }, year, month, day);

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void fetchProfile() {
        progressDialog.show();
        String url = GET_PROFILE_URL + patientId;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressDialog.dismiss();
                    try {
                        if ("success".equals(response.getString("status"))) {
                            JSONObject data = response.getJSONObject("data");
                            etFullName.setText(data.optString("full_name", ""));
                            etDOB.setText(data.optString("date_of_birth", ""));
                            setSpinnerSelection(spinnerGender, data.optString("gender", ""));
                            setSpinnerSelection(spinnerBloodGroup, data.optString("blood_group", ""));
                            etAddress.setText(data.optString("address", ""));
                            etMobile.setText(data.optString("mobile", ""));
                            etEmail.setText(data.optString("email", ""));
                            etEmergencyName.setText(data.optString("emergency_contact_name", ""));
                            etEmergencyNumber.setText(data.optString("emergency_contact_number", ""));
                            etMedicalHistory.setText(data.optString("medical_history", ""));
                            etAllergies.setText(data.optString("allergies", ""));
                            etCurrentMedications.setText(data.optString("current_medications", ""));

                            String profilePictureUrl = data.optString("profile_picture", "");
                            if (!TextUtils.isEmpty(profilePictureUrl)) {
                                Glide.with(this)
                                        .load(profilePictureUrl)
                                        .into(civProfile);
                                int lastSlash = profilePictureUrl.lastIndexOf('/');
                                if (lastSlash != -1) {
                                    oldImageUrl = profilePictureUrl.substring(lastSlash + 1);
                                } else {
                                    oldImageUrl = profilePictureUrl;
                                }
                                // Log.d(TAG, "Fetched old image name: " + oldImageUrl);
                            } else {
                                // Log.d(TAG, "No profile image found in DB.");
                            }
                        } else {
                            Toast.makeText(Profile.this, "Profile not found.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(Profile.this, "Could not load your profile. Please try again.", Toast.LENGTH_SHORT).show();
                        // Log.e(TAG, "JSON parsing error", e);
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    Toast.makeText(Profile.this, "Unable to connect. Please check your internet and try again.", Toast.LENGTH_SHORT).show();
                    // Log.e(TAG, "Fetch profile error: " + error.getMessage());
                });
        requestQueue.add(jsonObjectRequest);
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
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
            return;
        }
        progressDialog.show();

        if (selectedBitmap != null) {
            VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, UPDATE_PROFILE_URL,
                    response -> {
                        progressDialog.dismiss();
                        try {
                            JSONObject jsonResponse = new JSONObject(new String(response.data));
                            if ("success".equals(jsonResponse.getString("status"))) {
                                Toast.makeText(Profile.this, "Your profile was updated successfully!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(Profile.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(Profile.this, "Could not update your profile. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(Profile.this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        progressDialog.dismiss();
                        Toast.makeText(Profile.this, "Unable to update profile. Please check your internet and try again.", Toast.LENGTH_SHORT).show();
                        // Log.e(TAG, "Multipart update error: " + error.getMessage());
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("patient_id", String.valueOf(patientId));
                    params.put("full_name", etFullName.getText().toString());
                    String dobInput = etDOB.getText().toString().trim();
                    if (!dobInput.isEmpty() && dobInput.contains("/")) {
                        try {
                            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            Date dobDate = inputFormat.parse(dobInput);
                            dobInput = outputFormat.format(dobDate);
                        } catch (ParseException e) {
                            // Log.e(TAG, "DOB conversion error (multipart)", e);
                        }
                    }
                    params.put("date_of_birth", dobInput);
                    params.put("gender", spinnerGender.getSelectedItem().toString());
                    params.put("blood_group", spinnerBloodGroup.getSelectedItem().toString());
                    params.put("address", etAddress.getText().toString());
                    params.put("mobile", etMobile.getText().toString());
                    params.put("email", etEmail.getText().toString());
                    params.put("emergency_contact_name", etEmergencyName.getText().toString());
                    params.put("emergency_contact_number", etEmergencyNumber.getText().toString());
                    params.put("medical_history", etMedicalHistory.getText().toString());
                    params.put("allergies", etAllergies.getText().toString());
                    params.put("current_medications", etCurrentMedications.getText().toString());
                    return params;
                }

                @Override
                protected Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();
                    params.put("profile_image", new DataPart("profile.jpg", getFileDataFromDrawable(selectedBitmap), "image/jpeg"));
                    return params;
                }
            };
            requestQueue.add(multipartRequest);
        } else {
            // Log.d(TAG, "Updating profile with old image name: " + oldImageUrl);
            StringRequest stringRequest = new StringRequest(Request.Method.POST, UPDATE_PROFILE_URL,
                    response -> {
                        progressDialog.dismiss();
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if ("success".equals(jsonResponse.getString("status"))) {
                                Toast.makeText(Profile.this, "Your profile was updated successfully!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(Profile.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(Profile.this, "Could not update your profile. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(Profile.this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        progressDialog.dismiss();
                        Toast.makeText(Profile.this, "Unable to update profile. Please check your internet and try again.", Toast.LENGTH_SHORT).show();
                        // Log.e(TAG, "StringRequest update error: " + error.getMessage());
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("patient_id", String.valueOf(patientId));
                    params.put("full_name", etFullName.getText().toString());
                    String dobInput = etDOB.getText().toString().trim();
                    if (!dobInput.isEmpty() && dobInput.contains("/")) {
                        try {
                            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            Date dobDate = inputFormat.parse(dobInput);
                            dobInput = outputFormat.format(dobDate);
                        } catch (ParseException e) {
                            // Log.e(TAG, "DOB conversion error (StringRequest)", e);
                        }
                    }
                    params.put("date_of_birth", dobInput);
                    params.put("gender", spinnerGender.getSelectedItem().toString());
                    params.put("blood_group", spinnerBloodGroup.getSelectedItem().toString());
                    params.put("address", etAddress.getText().toString());
                    params.put("mobile", etMobile.getText().toString());
                    params.put("email", etEmail.getText().toString());
                    params.put("emergency_contact_name", etEmergencyName.getText().toString());
                    params.put("emergency_contact_number", etEmergencyNumber.getText().toString());
                    params.put("medical_history", etMedicalHistory.getText().toString());
                    params.put("allergies", etAllergies.getText().toString());
                    params.put("current_medications", etCurrentMedications.getText().toString());

                    params.put("profile_image", oldImageUrl);
                    return params;
                }
            };
            requestQueue.add(stringRequest);
        }
    }

    public byte[] getFileDataFromDrawable(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

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

        private byte[] buildMultipartBody(Map<String, String> params, Map<String, DataPart> dataParts) throws AuthFailureError {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            try {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    bos.write((twoHyphens + boundary + lineEnd).getBytes());
                    bos.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + lineEnd).getBytes());
                    bos.write(("Content-Type: text/plain; charset=UTF-8" + lineEnd).getBytes());
                    bos.write(lineEnd.getBytes());
                    bos.write(entry.getValue().getBytes());
                    bos.write(lineEnd.getBytes());
                }
                for (Map.Entry<String, DataPart> entry : dataParts.entrySet()) {
                    DataPart dataPart = entry.getValue();
                    bos.write((twoHyphens + boundary + lineEnd).getBytes());
                    bos.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"; filename=\"" + dataPart.getFileName() + "\"" + lineEnd).getBytes());
                    bos.write(("Content-Type: " + dataPart.getType() + lineEnd).getBytes());
                    bos.write(lineEnd.getBytes());
                    bos.write(dataPart.getContent());
                    bos.write(lineEnd.getBytes());
                }
                bos.write((twoHyphens + boundary + twoHyphens + lineEnd).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bos.toByteArray();
        }
    }

    public class DataPart {
        private String fileName;
        private byte[] content;
        private String type;

        public DataPart(String fileName, byte[] content, String type) {
            this.fileName = fileName;
            this.content = content;
            this.type = type;
        }

        public String getFileName() {
            return fileName;
        }

        public byte[] getContent() {
            return content;
        }

        public String getType() {
            return type;
        }
    }
}
