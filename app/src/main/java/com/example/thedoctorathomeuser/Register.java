package com.example.thedoctorathomeuser;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";

    Button signup;
    EditText etName, etMobile, etEmail, etPincode;
    AutoCompleteTextView actvCity, actvAge;
    RadioGroup rgGender;
    CheckBox cbTerms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize views
        etName = findViewById(R.id.et_name);
        etMobile = findViewById(R.id.et_mobile);
        etEmail = findViewById(R.id.et_email);
        actvCity = findViewById(R.id.actv_city);
        actvAge = findViewById(R.id.actv_age);
        etPincode = findViewById(R.id.pincod);
        rgGender = findViewById(R.id.rg_gender);
        cbTerms = findViewById(R.id.cb_terms);
        signup = findViewById(R.id.btn_signup);

        // "Log in" TextView
        TextView tvLogin = findViewById(R.id.tv_login);
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(Register.this, login.class);
            startActivity(intent);
            finish();
        });

        // Set up AutoCompleteTextView for City
        String[] cities = {"Rajkot", "Ahemdabad", "Surat", "Mumbai", "Junagadh"};
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, cities);
        actvCity.setAdapter(cityAdapter);

        // Set up AutoCompleteTextView for Age
        String[] ages = new String[100];
        for (int i = 1; i <= 100; i++) {
            ages[i - 1] = String.valueOf(i);
        }
        ArrayAdapter<String> ageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, ages);
        actvAge.setAdapter(ageAdapter);

        // Set up Sign Up button click
        signup.setOnClickListener(v -> {
            if (validateInputs()) {
                registerOrUpdateUser(); // Send data to the server
            }
        });
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(etName.getText().toString())) {
            etName.setError("Name is required");
            return false;
        }
        if (TextUtils.isEmpty(etMobile.getText().toString()) || etMobile.getText().toString().length() != 10) {
            etMobile.setError("Enter a valid 10-digit mobile number");
            return false;
        }
        if (TextUtils.isEmpty(etEmail.getText().toString()) ||
                !android.util.Patterns.EMAIL_ADDRESS.matcher(etEmail.getText().toString()).matches()) {
            etEmail.setError("Enter a valid email");
            return false;
        }
        if (TextUtils.isEmpty(actvCity.getText().toString())) {
            actvCity.setError("City is required");
            return false;
        }
        if (TextUtils.isEmpty(actvAge.getText().toString()) || !actvAge.getText().toString().matches("\\d+")) {
            actvAge.setError("Enter a valid age");
            return false;
        }
        if (TextUtils.isEmpty(etPincode.getText().toString()) || etPincode.getText().toString().length() != 6) {
            etPincode.setError("Enter a valid 6-digit pincode");
            return false;
        }
        if (rgGender.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "You must agree to the Terms of Service", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void registerOrUpdateUser() {
        String name = etName.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String city = actvCity.getText().toString().trim();
        String age = actvAge.getText().toString().trim();
        String pincode = etPincode.getText().toString().trim();

        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        String gender = selectedGenderId != -1
                ? ((RadioButton) findViewById(selectedGenderId)).getText().toString()
                : "";

        // API URL (Replace with your actual domain)
        String URL = "http://sxm.a58.mytemp.website/register.php";

        // Disable the button while sending request and log the attempt
        signup.setEnabled(false);
        signup.setText("Please wait...");
        Log.d(TAG, "Attempting to register user: " + name + ", " + mobile + ", " + email);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL,
                response -> {
                    Log.d(TAG, "Server response: " + response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        String message = jsonObject.getString("message");
                        Toast.makeText(Register.this, message, Toast.LENGTH_SHORT).show();

                        if (jsonObject.getBoolean("success")) {
                            // If success, move to OTP screen
                            Intent intent = new Intent(Register.this, login.class);
                            intent.putExtra("name", name);
                            intent.putExtra("mobile", mobile);
                            intent.putExtra("email", email);
                            intent.putExtra("city", city);
                            intent.putExtra("age", age);
                            intent.putExtra("pincode", pincode);
                            intent.putExtra("gender", gender);
                            startActivity(intent);
                            finish();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Parsing Error: " + e.getMessage(), e);
                        Toast.makeText(Register.this, "JSON Parsing Error", Toast.LENGTH_SHORT).show();
                    }
                    // Re-enable the button after response
                    signup.setEnabled(true);
                    signup.setText("Sign Up");
                },
                error -> {
                    Log.e(TAG, "Volley error: " + error.getMessage(), error);
                    Toast.makeText(Register.this, "Failed to connect. Check your internet!", Toast.LENGTH_SHORT).show();
                    // Re-enable the button on error
                    signup.setEnabled(true);
                    signup.setText("Sign Up");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("name", name);
                params.put("mobile", mobile);
                params.put("email", email);
                params.put("city", city);
                params.put("age", age);
                params.put("pincode", pincode);
                params.put("gender", gender);
                Log.d(TAG, "Request parameters: " + params.toString());
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }
}
