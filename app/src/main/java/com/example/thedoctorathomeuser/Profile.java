package com.example.thedoctorathomeuser;

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
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
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
    private static final String TAG = "Profile";
    private CircleImageView civProfile;
    private EditText etFullName, etDOB, etAddress, etMobile, etEmail, etMedicalHistory,
            etAllergies, etCurrentMedications, etEmergencyName, etEmergencyNumber;
    private Spinner spinnerGender, spinnerBloodGroup;
    private Button btnUpdate;
    private ProgressDialog progressDialog;
    private RequestQueue requestQueue;
    private int patientId;

    // Endpoints
    private static final String GET_PROFILE_URL = "http://sxm.a58.mytemp.website/get_profile.php?patient_id=";
    private static final String UPDATE_PROFILE_URL = "http://sxm.a58.mytemp.website/update_profile.php";

    // Member variable to store the selected profile image bitmap
    private Bitmap selectedBitmap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        SharedPreferences sp = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String patientIdStr = sp.getString("patient_id", "");
        if (patientIdStr.isEmpty()) {
            Log.e(TAG, "Patient ID not found in SharedPreferences");
            finish();
            return;
        }
        patientId = Integer.parseInt(patientIdStr);
        Log.d(TAG, "Patient ID: " + patientId);

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
                this,
                R.array.gender_array,
                android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);

        ArrayAdapter<CharSequence> bloodGroupAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.blood_group_array,
                android.R.layout.simple_spinner_item);
        bloodGroupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(bloodGroupAdapter);

        // Make DOB field non-editable so that DatePickerDialog is used
        etDOB.setFocusable(false);
        etDOB.setClickable(true);
        etDOB.setOnClickListener(v -> showDatePicker());

        civProfile.setOnClickListener(v -> openGallery());
        btnUpdate.setOnClickListener(v -> {
            Log.d(TAG, "Update button clicked");
            try {
                updateProfile();
            } catch (AuthFailureError e) {
                Log.e(TAG, "AuthFailureError in updateProfile", e);
                throw new RuntimeException(e);
            }
        });
        fetchProfile();
    }

    public void openGallery() {
        Log.d(TAG, "Opening gallery to select image");
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                civProfile.setImageBitmap(bitmap);
                selectedBitmap = bitmap; // Store selected bitmap for upload
                Log.d(TAG, "Image selected successfully. Bitmap size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            } catch (IOException e) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading image", e);
            }
        }
    }

    private void showDatePicker() {
        // Use current date as default
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        Log.d(TAG, "Showing DatePicker with default date: " + day + "/" + (month + 1) + "/" + year);
        DatePickerDialog datePickerDialog = new DatePickerDialog(Profile.this,
                (DatePicker view, int selectedYear, int selectedMonth, int selectedDay) -> {
                    // Format date as dd/MM/yyyy
                    String formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d",
                            selectedDay, selectedMonth + 1, selectedYear);
                    etDOB.setText(formattedDate);
                    Log.d(TAG, "Date selected: " + formattedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void fetchProfile() {
        progressDialog.show();
        String url = GET_PROFILE_URL + patientId;
        Log.d(TAG, "Fetching profile from URL: " + url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressDialog.dismiss();
                    Log.d(TAG, "Profile fetch response: " + response.toString());
                    try {
                        if(response.getString("status").equals("success")){
                            JSONObject data = response.getJSONObject("data");

                            String fullName = data.optString("full_name", "");
                            if("null".equalsIgnoreCase(fullName)) { fullName = ""; }
                            etFullName.setText(fullName);

                            String dob = data.optString("date_of_birth", "");
                            if("null".equalsIgnoreCase(dob)) { dob = ""; }
                            etDOB.setText(dob);

                            String gender = data.optString("gender", "");
                            if("null".equalsIgnoreCase(gender)) { gender = ""; }
                            setSpinnerSelection(spinnerGender, gender);

                            String bloodGroup = data.optString("blood_group", "");
                            if("null".equalsIgnoreCase(bloodGroup)) { bloodGroup = ""; }
                            setSpinnerSelection(spinnerBloodGroup, bloodGroup);

                            String address = data.optString("address", "");
                            if("null".equalsIgnoreCase(address)) { address = ""; }
                            etAddress.setText(address);

                            String mobile = data.optString("mobile", "");
                            if("null".equalsIgnoreCase(mobile)) { mobile = ""; }
                            etMobile.setText(mobile);

                            String email = data.optString("email", "");
                            if("null".equalsIgnoreCase(email)) { email = ""; }
                            etEmail.setText(email);

                            String emergencyName = data.optString("emergency_contact_name", "");
                            if("null".equalsIgnoreCase(emergencyName)) { emergencyName = ""; }
                            etEmergencyName.setText(emergencyName);

                            String emergencyNumber = data.optString("emergency_contact_number", "");
                            if("null".equalsIgnoreCase(emergencyNumber)) { emergencyNumber = ""; }
                            etEmergencyNumber.setText(emergencyNumber);

                            String medicalHistory = data.optString("medical_history", "");
                            if("null".equalsIgnoreCase(medicalHistory)) { medicalHistory = ""; }
                            etMedicalHistory.setText(medicalHistory);

                            String allergies = data.optString("allergies", "");
                            if("null".equalsIgnoreCase(allergies)) { allergies = ""; }
                            etAllergies.setText(allergies);

                            String currentMedications = data.optString("current_medications", "");
                            if("null".equalsIgnoreCase(currentMedications)) { currentMedications = ""; }
                            etCurrentMedications.setText(currentMedications);

                            // Load profile picture if available
                            String profilePictureUrl = data.optString("profile_picture", "");
                            if (!TextUtils.isEmpty(profilePictureUrl)) {
                                Log.d(TAG, "Loading profile picture from URL: " + profilePictureUrl);
                                Glide.with(this)
                                        .load(profilePictureUrl)
                                        .into(civProfile);
                            } else {
                                Log.d(TAG, "No profile picture URL available");
                            }
                        } else {
                            Toast.makeText(Profile.this, "Profile not found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(Profile.this, "Parsing error", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "JSON parsing error", e);
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    Toast.makeText(Profile.this, "Error fetching profile", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Volley error: " + error.getMessage());
                });
        requestQueue.add(jsonObjectRequest);
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        if (adapter != null) {
            int count = adapter.getCount();
            for (int i = 0; i < count; i++) {
                if (adapter.getItem(i).toString().equalsIgnoreCase(value)) {
                    spinner.setSelection(i);
                    Log.d(TAG, "Spinner " + spinner.getId() + " set to: " + value);
                    break;
                }
            }
        }
    }

    private void updateProfile() throws AuthFailureError {
        if (TextUtils.isEmpty(etFullName.getText().toString())) {
            etFullName.setError("Full Name is required");
            return;
        }
        progressDialog.show();

        // Log current field values for debugging
        Log.d(TAG, "Updating profile with details:");
        Log.d(TAG, "Full Name: " + etFullName.getText().toString());
        Log.d(TAG, "DOB: " + etDOB.getText().toString());
        Log.d(TAG, "Gender: " + spinnerGender.getSelectedItem().toString());
        Log.d(TAG, "Blood Group: " + spinnerBloodGroup.getSelectedItem().toString());
        Log.d(TAG, "Address: " + etAddress.getText().toString());
        Log.d(TAG, "Mobile: " + etMobile.getText().toString());
        Log.d(TAG, "Email: " + etEmail.getText().toString());
        // ... (other fields)

        // If a new profile image is selected, use multipart request
        if (selectedBitmap != null) {
            Log.d(TAG, "New image selected, preparing multipart request");
            byte[] imageData = getFileDataFromDrawable(selectedBitmap);
            Log.d(TAG, "Converted image data length: " + imageData.length);

            VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, UPDATE_PROFILE_URL,
                    response -> {
                        progressDialog.dismiss();
                        try {
                            JSONObject jsonResponse = new JSONObject(new String(response.data));
                            Log.d(TAG, "Multipart response: " + jsonResponse.toString());
                            if (jsonResponse.getString("status").equals("success")) {
                                Toast.makeText(Profile.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(Profile.this, "Update failed: " + jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(Profile.this, "Response parsing error", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "JSON parse error", e);
                        }
                    },
                    error -> {
                        progressDialog.dismiss();
                        Toast.makeText(Profile.this, "Error updating profile", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Multipart Volley error: " + error.getMessage());
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("patient_id", String.valueOf(patientId));
                    params.put("full_name", etFullName.getText().toString());

                    // Handle DOB conversion only if input contains a slash.
                    String dobInput = etDOB.getText().toString().trim();
                    if (!dobInput.isEmpty()) {
                        if (dobInput.contains("/")) {
                            try {
                                SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                Date dobDate = inputFormat.parse(dobInput);
                                dobInput = outputFormat.format(dobDate);
                                Log.d(TAG, "Converted DOB (multipart): " + dobInput);
                            } catch (ParseException e) {
                                Log.e(TAG, "DOB conversion error (multipart)", e);
                            }
                        } else {
                            Log.d(TAG, "DOB already in expected format (multipart): " + dobInput);
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
                    Log.d(TAG, "Multipart Params: " + params.toString());
                    return params;
                }

                @Override
                protected Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();
                    byte[] imageData = getFileDataFromDrawable(selectedBitmap);
                    params.put("profile_image", new DataPart("profile.jpg", imageData, "image/jpeg"));
                    Log.d(TAG, "Multipart Byte Data set for profile_image");
                    return params;
                }
            };
            requestQueue.add(multipartRequest);
        } else {
            Log.d(TAG, "No new image selected; using standard request");
            StringRequest stringRequest = new StringRequest(Request.Method.POST, UPDATE_PROFILE_URL,
                    response -> {
                        progressDialog.dismiss();
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            Log.d(TAG, "StringRequest response: " + jsonResponse.toString());
                            if (jsonResponse.getString("status").equals("success")) {
                                Toast.makeText(Profile.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(Profile.this, "Update failed: " + jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(Profile.this, "Response parsing error", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "JSON parsing error", e);
                        }
                    },
                    error -> {
                        progressDialog.dismiss();
                        Toast.makeText(Profile.this, "Error updating profile", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "StringRequest Volley error: " + error.getMessage());
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("patient_id", String.valueOf(patientId));
                    params.put("full_name", etFullName.getText().toString());

                    // Handle DOB conversion only if input contains a slash.
                    String dobInput = etDOB.getText().toString().trim();
                    if (!dobInput.isEmpty()) {
                        if (dobInput.contains("/")) {
                            try {
                                SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                Date dobDate = inputFormat.parse(dobInput);
                                dobInput = outputFormat.format(dobDate);
                                Log.d(TAG, "Converted DOB (StringRequest): " + dobInput);
                            } catch (ParseException e) {
                                Log.e(TAG, "DOB conversion error (StringRequest)", e);
                            }
                        } else {
                            Log.d(TAG, "DOB already in expected format (StringRequest): " + dobInput);
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
                    Log.d(TAG, "StringRequest Params: " + params.toString());
                    return params;
                }
            };
            requestQueue.add(stringRequest);
        }
    }

    // Helper method to convert bitmap to byte array
    public byte[] getFileDataFromDrawable(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // Compress the image to JPEG with 80% quality (adjust as needed)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] imageData = byteArrayOutputStream.toByteArray();
        Log.d(TAG, "getFileDataFromDrawable: byte array length = " + imageData.length);
        return imageData;
    }

    // Inner class for multipart requests
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
            Log.d(TAG, "VolleyMultipartRequest initialized with boundary: " + boundary);
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
                // Text parameters
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    bos.write((twoHyphens + boundary + lineEnd).getBytes());
                    bos.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + lineEnd).getBytes());
                    bos.write(("Content-Type: text/plain; charset=UTF-8" + lineEnd).getBytes());
                    bos.write(lineEnd.getBytes());
                    bos.write(entry.getValue().getBytes());
                    bos.write(lineEnd.getBytes());
                }
                // File data
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
            byte[] multipartBody = bos.toByteArray();
            Log.d(TAG, "Multipart body length: " + multipartBody.length);
            return multipartBody;
        }
    }

    // DataPart class to hold file data for multipart requests
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
