package com.example.thedoctorathomeuser;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class Profile extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private CircleImageView civProfile;
    private EditText etFullName, etDOB, etAddress, etMobile, etEmail, etMedicalHistory,
            etAllergies, etCurrentMedications, etEmergencyName, etEmergencyNumber;
    private Spinner spinnerGender, spinnerBloodGroup;
    private Button btnUpdate;
    private ProgressDialog progressDialog;
    private RequestQueue requestQueue;
    // For demo, patient id is hard-coded; ideally, you would get this from login/session management
    private int patientId = 1;

    // Replace these URLs with your own server endpoints
    private static final String GET_PROFILE_URL = "http://sxm.a58.mytemp.website/get_profile.php?patient_id=";
    private static final String UPDATE_PROFILE_URL = "http://sxm.a58.mytemp.website/update_profile.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile); // Ensure this layout contains the required fields

        // Initialize UI elements
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

        // Initialize Spinners using string arrays defined in res/values/strings.xml
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

        // Set listener for profile image click (to pick an image)
        civProfile.setOnClickListener(v -> openGallery());

        // Set listener for update button to trigger profile update
        btnUpdate.setOnClickListener(v -> updateProfile());

        // Fetch and display profile data when the activity starts
        fetchProfile();
    }

    /**
     * Open gallery to pick an image.
     */
    public void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    /**
     * Handle result from image picker.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                civProfile.setImageBitmap(bitmap); // Display the selected image
                // To upload the image, consider converting it to Base64 or using a multipart request.
            } catch (IOException e) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    /**
     * Fetch the profile data from the server using a GET request.
     */
    private void fetchProfile() {
        progressDialog.show();

        String url = GET_PROFILE_URL + patientId;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressDialog.dismiss();
                    try {
                        if(response.getString("status").equals("success")){
                            JSONObject data = response.getJSONObject("data");
                            // Populate fields with fetched data (adjust keys as per your API response)
                            etFullName.setText(data.optString("full_name", ""));
                            etDOB.setText(data.optString("date_of_birth", ""));
                            // Set spinner selections based on API response.
                            String gender = data.optString("gender", "");
                            setSpinnerSelection(spinnerGender, gender);

                            String bloodGroup = data.optString("blood_group", "");
                            setSpinnerSelection(spinnerBloodGroup, bloodGroup);

                            etAddress.setText(data.optString("address", ""));
                            etMobile.setText(data.optString("mobile", ""));
                            etEmail.setText(data.optString("email", ""));
                            etEmergencyName.setText(data.optString("emergency_contact_name", ""));
                            etEmergencyNumber.setText(data.optString("emergency_contact_number", ""));
                            etMedicalHistory.setText(data.optString("medical_history", ""));
                            etAllergies.setText(data.optString("allergies", ""));
                            etCurrentMedications.setText(data.optString("current_medications", ""));
                            // Optionally load profile picture if URL is provided.
                        } else {
                            Toast.makeText(Profile.this, "Profile not found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(Profile.this, "Parsing error", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    Toast.makeText(Profile.this, "Error fetching profile", Toast.LENGTH_SHORT).show();
                    Log.e("Profile", "Volley error: " + error.getMessage());
                });
        requestQueue.add(jsonObjectRequest);
    }

    /**
     * Utility method to set the selection of a spinner based on a value.
     */
    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        if (adapter != null) {
            int count = adapter.getCount();
            for (int i = 0; i < count; i++) {
                if (adapter.getItem(i).toString().equalsIgnoreCase(value)) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }

    /**
     * Update the profile data on the server using a POST request.
     */
    private void updateProfile() {
        // Basic validation; add more as needed
        if(TextUtils.isEmpty(etFullName.getText().toString())){
            etFullName.setError("Full Name is required");
            return;
        }

        progressDialog.show();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, UPDATE_PROFILE_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressDialog.dismiss();
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if(jsonResponse.getString("status").equals("success")){
                                Toast.makeText(Profile.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(Profile.this, "Update failed: " + jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(Profile.this, "Response parsing error", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        Toast.makeText(Profile.this, "Error updating profile", Toast.LENGTH_SHORT).show();
                        Log.e("Profile", "Volley error: " + error.getMessage());
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                // Prepare parameters for the POST request
                Map<String, String> params = new HashMap<>();
                params.put("patient_id", String.valueOf(patientId));
                params.put("full_name", etFullName.getText().toString());
                params.put("date_of_birth", etDOB.getText().toString());
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
        };

        requestQueue.add(stringRequest);
    }
}
