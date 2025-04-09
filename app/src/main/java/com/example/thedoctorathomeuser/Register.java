package com.example.thedoctorathomeuser;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.Html;
import android.text.method.LinkMovementMethod;
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

        // ✅ Set clickable HTML links in checkbox
        String html = "I agree with the <a href='http://sxm.a58.mytemp.website/term.html'>Terms of Service</a> and <a href='http://sxm.a58.mytemp.website/policy.html'>Privacy Policy</a>.";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cbTerms.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
        }
        cbTerms.setMovementMethod(LinkMovementMethod.getInstance());
        cbTerms.setLinksClickable(true);

        // "Log in" TextView
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

        // Sign Up button click
        signup.setOnClickListener(v -> {
            if (validateInputs()) {
                registerUser();
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
                            Intent intent = new Intent(Register.this, login.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Log.e(TAG, "Registration Failed: " + message);
                            Toast.makeText(Register.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Parsing Error: " + e.getMessage(), e);
                    }
                    signup.setEnabled(true);
                    signup.setText("Sign Up");
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
}
