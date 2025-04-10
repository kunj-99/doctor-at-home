package com.example.thedoctorathomeuser;

import android.annotation.SuppressLint;
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
    TextView tvTerms, tvPolicy;

    @SuppressLint("SetTextI18n")
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
        tvTerms = findViewById(R.id.tv_terms);
        tvPolicy = findViewById(R.id.tv_policy);

        cbTerms.setText("I agree with the Terms of Service and Privacy Policy");

        // Terms and Policy Clicks
        tvTerms.setOnClickListener(v -> {
            Intent intent = new Intent(Register.this, tarmsandcondition.class);
            startActivity(intent);
        });

        tvPolicy.setOnClickListener(v -> {
            Intent intent = new Intent(Register.this, policy.class);
            startActivity(intent);
        });

        // "Log in" TextView click
        TextView tvLogin = findViewById(R.id.tv_login);
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(Register.this, login.class);
            startActivity(intent);
            finish();
        });

        // AutoCompleteTextView for City
        String[] cities = {"Rajkot", "Ahemdabad", "Surat", "Mumbai", "Junagadh"};
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, cities);
        actvCity.setAdapter(cityAdapter);

        // AutoCompleteTextView for Age
        String[] ages = new String[100];
        for (int i = 1; i <= 100; i++) {
            ages[i - 1] = String.valueOf(i);
        }
        ArrayAdapter<String> ageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, ages);
        actvAge.setAdapter(ageAdapter);

        // Sign Up button click event
        signup.setOnClickListener(v -> {
            if (validateInputs()) {
                registerUser();
            }
        });
    }

    // Validate input fields in the register form
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
        if (Integer.parseInt(actvAge.getText().toString()) < 18) {
            actvAge.setError("You must be 18 or older to use this app");
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

    // Register user by calling register.php endpoint
    private void registerUser() {
        signup.setEnabled(false);
        signup.setText("Processing...");

        String name = etName.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String city = actvCity.getText().toString().trim();
        String age = actvAge.getText().toString().trim();
        String pincode = etPincode.getText().toString().trim();
        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        String gender = selectedGenderId != -1 ? ((RadioButton) findViewById(selectedGenderId)).getText().toString() : "Not Specified";

        String URL = "http://sxm.a58.mytemp.website/register.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        String message = jsonObject.getString("message");
                        boolean success = jsonObject.getBoolean("success");

                        if (success) {
                            Toast.makeText(Register.this, message, Toast.LENGTH_SHORT).show();
                            // After registration, call login.php so that the OTP is sent.
                            callLoginForOtp(mobile);
                        } else {
                            Log.e(TAG, "Registration Failed: " + message);
                            Toast.makeText(Register.this, message, Toast.LENGTH_SHORT).show();
                            signup.setEnabled(true);
                            signup.setText("Sign Up");
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Parsing Error: " + e.getMessage(), e);
                        signup.setEnabled(true);
                        signup.setText("Sign Up");
                    }
                },
                error -> {
                    if (error.networkResponse != null) {
                        String responseBody = new String(error.networkResponse.data);
                        Log.e(TAG, "Server Error: " + responseBody);
                    } else {
                        Log.e(TAG, "Volley error: " + error.getMessage(), error);
                    }
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
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    // Call the login.php endpoint to trigger OTP generation
    private void callLoginForOtp(String mobile) {
        String URL = "http://sxm.a58.mytemp.website/login.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean success = jsonObject.getBoolean("success");
                        String message = jsonObject.getString("message");

                        if (success) {
                            Toast.makeText(Register.this, "OTP sent! Proceeding to OTP verification.", Toast.LENGTH_SHORT).show();
                            // Pass the mobile number to the OTP verification activity
                            Intent intent = new Intent(Register.this, otp_verification.class);
                            intent.putExtra("mobile", mobile);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(Register.this, message, Toast.LENGTH_SHORT).show();
                            signup.setEnabled(true);
                            signup.setText("Sign Up");
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Parsing Error: " + e.getMessage(), e);
                        signup.setEnabled(true);
                        signup.setText("Sign Up");
                    }
                },
                error -> {
                    Log.e(TAG, "Volley Error: " + error.getMessage(), error);
                    Toast.makeText(Register.this, "Server error! Try again.", Toast.LENGTH_SHORT).show();
                    signup.setEnabled(true);
                    signup.setText("Sign Up");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("mobile", mobile);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(Register.this);
        requestQueue.add(stringRequest);
    }
}
