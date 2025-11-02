package com.infowave.thedoctorathomeuser;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
// import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class suppor extends AppCompatActivity {

    // private static final String TAG = "suppor";
    // Replace with your actual API URL where contact_us.php is hosted
    private static final String API_URL = ApiConfig.endpoint("contact_us.php");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suppor);
        setupBarsWithScrimsNoRootPadding();
        // Start fetching data from the API
        new FetchContactDataTask().execute();
    }

    // Helper method to format phone number: first 3 digits in brackets.
    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber != null && phoneNumber.length() >= 3) {
            String firstThree = phoneNumber.substring(0, 3);
            String rest = phoneNumber.substring(3);
            return "(" + firstThree + ")" + rest;
        }
        return phoneNumber;
    }

    private void setupBarsWithScrimsNoRootPadding() {
        // Draw behind bars
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        // Real bars transparent
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        // White icons on black
        WindowInsetsControllerCompat c =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        c.setAppearanceLightStatusBars(false);
        c.setAppearanceLightNavigationBars(false);

        final View root = findViewById(R.id.main);
        final View top = findViewById(R.id.status_bar_scrim);
        final View bottom = findViewById(R.id.navigation_bar_scrim);
        if (root == null || top == null || bottom == null) return;

        // Ensure scrim is physically at the very top (above any header)
        top.bringToFront();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            final Insets s = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            final Insets n = insets.getInsets(WindowInsetsCompat.Type.navigationBars());

            // Size scrims – DO NOT pad the root (that created the gap)
            ViewGroup.LayoutParams lpTop = top.getLayoutParams();
            if (lpTop.height != s.top) {
                lpTop.height = s.top;
                top.setLayoutParams(lpTop);
            }
            top.setVisibility(s.top > 0 ? View.VISIBLE : View.GONE);

            ViewGroup.LayoutParams lpBot = bottom.getLayoutParams();
            if (lpBot.height != n.bottom) {
                lpBot.height = n.bottom;
                bottom.setLayoutParams(lpBot);
            }
            bottom.setVisibility(n.bottom > 0 ? View.VISIBLE : View.GONE);

            return WindowInsetsCompat.CONSUMED;
        });

        root.requestApplyInsets();
    }


    private class FetchContactDataTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            StringBuilder response = new StringBuilder();
            try {
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    return response.toString();
                } else {
                    // Log.e(TAG, "HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                // Log.e(TAG, "Error fetching data", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                try {
                    // Parse the JSON response assuming it's an array of contact objects.
                    JSONArray jsonArray = new JSONArray(result);
                    if (jsonArray.length() > 0) {
                        // For this example, we pick the first contact in the array.
                        JSONObject contact = jsonArray.getJSONObject(0);
                        final String whatsappNumber = contact.getString("Whatsapp_number");
                        final String phoneNumber = contact.getString("phone_number");
                        final String email = contact.getString("email");

                        // Format the phone number with first 3 digits in brackets.
                        final String formattedPhoneNumber = formatPhoneNumber(phoneNumber);

                        // Find the TextViews in the layout.
                        TextView whatsappText = findViewById(R.id.whatsapp_text);
                        TextView phoneText = findViewById(R.id.mobile_text);
                        TextView emailText = findViewById(R.id.email_text);

                        // Set the fetched and formatted data into the TextViews.
                        whatsappText.setText(whatsappNumber);
                        phoneText.setText(formattedPhoneNumber);
                        emailText.setText(email);

                        // Set click listener for email: Opens Gmail's compose window.
                        emailText.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                                emailIntent.setData(Uri.parse("mailto:" + email));
                                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Contact from The Doctor At Home");
                                emailIntent.putExtra(Intent.EXTRA_TEXT, "");
                                startActivity(Intent.createChooser(emailIntent, "Send Email"));
                            }
                        });

                        // Set click listener for phone: Opens the dialer with the formatted phone number.
                        phoneText.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                                dialIntent.setData(Uri.parse("tel:" + formattedPhoneNumber));
                                startActivity(dialIntent);
                            }
                        });

                        // Set click listener for WhatsApp: Opens a chat with the provided number.
                        whatsappText.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // WhatsApp API URL; ensure the number includes the country code.
                                String url = "https://api.whatsapp.com/send?phone=" + whatsappNumber;
                                Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
                                whatsappIntent.setData(Uri.parse(url));
                                startActivity(whatsappIntent);
                            }
                        });
                    }
                } catch (JSONException e) {
                    // Log.e(TAG, "JSON parsing error", e);
                    Toast.makeText(suppor.this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Log.e(TAG, "Result is null. Check your API URL or network connection.");
                Toast.makeText(suppor.this, "Unable to load contact details. Please check your connection.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
