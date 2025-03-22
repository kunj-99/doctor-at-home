package com.example.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log; // For debugging
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.view.GravityCompat;

import com.bumptech.glide.Glide; // Using Glide for image loading
import com.google.firebase.FirebaseApp;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import com.example.thedoctorathomeuser.Fragment.HomeFragment;
import com.example.thedoctorathomeuser.view.view;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    // URL for fetching profile details
    private static final String GET_PROFILE_URL = "http://sxm.a58.mytemp.website/get_profile.php?patient_id=";
    private static final String TAG = "GetProfileTask";

    private ViewPager vp;
    private BottomNavigationView btn;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    private LinearLayout profileSection;
    private TextView toolbarTitle; // To dynamically update the title

    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);

        // Initialize UI components
        vp = findViewById(R.id.vp);
        btn = findViewById(R.id.bottem);
        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.tb);
        navigationView = findViewById(R.id.nev_view);
        toolbarTitle = findViewById(R.id.toolbar_title); // Toolbar title TextView

        // Reference the profile section from the navigation header
        View headerView = navigationView.getHeaderView(0);
        profileSection = headerView.findViewById(R.id.ll_profile_section);
        // Additional header view references for profile details (from appbar_header.xml)
        CircleImageView civProfile = headerView.findViewById(R.id.civ_profile);
        TextView tvProfileName = headerView.findViewById(R.id.tv_profile_name);
        TextView tvProfileEmail = headerView.findViewById(R.id.tv_profile_email);

        // Fetch profile details from remote server using GET_PROFILE_URL
        SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String patientId = userPrefs.getString("patient_id", "");
        Log.d(TAG, "Patient ID from SharedPreferences: " + patientId);
        if (!patientId.isEmpty()) {
            new GetProfileTask(civProfile, tvProfileName, tvProfileEmail).execute(patientId);
        } else {
            Log.d(TAG, "No patient ID found in SharedPreferences.");
        }

        // Set up the toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Set up the Drawer Toggle
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.black));
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Set up the ViewPager with a custom adapter
        view adapter = new view(getSupportFragmentManager());
        vp.setAdapter(adapter);

        // Set up BottomNavigationView
        int fragmentToOpen = getIntent().getIntExtra("open_fragment", 0);
        // Set the correct fragment and update Bottom Navigation selection
        vp.setCurrentItem(fragmentToOpen);
        if (fragmentToOpen == 2) {
            btn.setSelectedItemId(R.id.page_3); // ✅ Set the correct bottom navigation icon
        }
        btn.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.page_1) {
                vp.setCurrentItem(0);
                toolbarTitle.setText("The Doctor At Home");
            } else if (item.getItemId() == R.id.page_2) {
                vp.setCurrentItem(1);
                toolbarTitle.setText(" Book Appointments");
            } else if (item.getItemId() == R.id.page_3) {
                vp.setCurrentItem(2);
                toolbarTitle.setText(" Ongoing Appointments");
            } else if (item.getItemId() == R.id.page_4) {
                vp.setCurrentItem(3);
                toolbarTitle.setText("History");
            }
            return true;
        });

        // Synchronize ViewPager and BottomNavigationView
        vp.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @SuppressLint("SetTextI18n")
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    btn.setSelectedItemId(R.id.page_1);
                    toolbarTitle.setText("The Doctor At Home");
                } else if (position == 1) {
                    btn.setSelectedItemId(R.id.page_2);
                    toolbarTitle.setText("Book Appointments");
                } else if (position == 2) {
                    btn.setSelectedItemId(R.id.page_3);
                    toolbarTitle.setText("Ongoing Appointments");
                } else if (position == 3) {
                    btn.setSelectedItemId(R.id.page_4);
                    toolbarTitle.setText("History");
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        // Set up Navigation Drawer item click listener
        navigationView.setNavigationItemSelectedListener(item -> {
            Intent intent = null;
            if (item.getItemId() == R.id.payments1) {
                intent = new Intent(MainActivity.this, payments.class);
            } else if (item.getItemId() == R.id.records1) {
                intent = new Intent(MainActivity.this, pathology_test.class);
            } else if (item.getItemId() == R.id.policy) {
                intent = new Intent(MainActivity.this, policy.class);
            } else if (item.getItemId() == R.id.notification) {
                intent = new Intent(MainActivity.this, tarmsandcondition.class);
            } else if (item.getItemId() == R.id.support1) {
                intent = new Intent(MainActivity.this, suppor.class);
            } else if (item.getItemId() == R.id.shareapp1) {
                intent = new Intent(MainActivity.this, share_app.class);
            } else if (item.getItemId() == R.id.aboutus1) {
                intent = new Intent(MainActivity.this, aboutus.class);
            } else if (item.getItemId() == R.id.settings1) {
                intent = new Intent(MainActivity.this, settings.class);
            } else if (item.getItemId() == R.id.rateapp) {
                intent = new Intent(MainActivity.this, Rate_app.class);
            } else if (item.getItemId() == R.id.logout) {
                // Clear UserPrefs SharedPreferences
                SharedPreferences userPrefsLogout = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor userEditor = userPrefsLogout.edit();
                userEditor.clear();
                userEditor.apply();

                // Clear ReviewPrefs SharedPreferences
                SharedPreferences reviewPrefs = getSharedPreferences("ReviewPrefs", MODE_PRIVATE);
                SharedPreferences.Editor reviewEditor = reviewPrefs.edit();
                reviewEditor.clear();
                reviewEditor.apply();

                // Redirect to Login activity
                intent = new Intent(MainActivity.this, login.class);
                startActivity(intent);
                finish();

                return true;
            }

            if (intent != null) {
                startActivity(intent);
                drawerLayout.closeDrawers();
                return true;
            }
            return false;
        });

        // Adjust the navigation drawer width to 70% of screen width
        adjustNavigationDrawerWidth();

        // Handle profile section click; this should launch your Profile activity.
        profileSection.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Profile.class);
            startActivity(intent);
        });
    }

    private void adjustNavigationDrawerWidth() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        ViewGroup.LayoutParams params = navigationView.getLayoutParams();
        params.width = (int) (width * 0.7);
        navigationView.setLayoutParams(params);
    }

    // AsyncTask to fetch profile details from the remote server
    private class GetProfileTask extends AsyncTask<String, Void, ProfileData> {

        private CircleImageView civProfile;
        private TextView tvProfileName, tvProfileEmail;

        public GetProfileTask(CircleImageView civProfile, TextView tvProfileName, TextView tvProfileEmail) {
            this.civProfile = civProfile;
            this.tvProfileName = tvProfileName;
            this.tvProfileEmail = tvProfileEmail;
        }


        @Override
        protected ProfileData doInBackground(String... params) {
            String patientId = params[0];
            String urlString = GET_PROFILE_URL + patientId;
            Log.d(TAG, "Request URL: " + urlString);
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(7000);
                connection.setReadTimeout(7000);
                connection.connect();

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response code: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    Log.d(TAG, "Response: " + response.toString());

                    // Parse JSON from the "data" object
                    JSONObject jsonObject = new JSONObject(response.toString());
                    if (jsonObject.has("data")) {
                        JSONObject dataObj = jsonObject.getJSONObject("data");
                        String name = dataObj.optString("full_name");
                        String mobile = dataObj.optString("mobile");
                        String imageUrl = dataObj.optString("profile_picture");
                        Log.d(TAG, "Parsed values - Name: " + name + ", Mobile: " + mobile + ", ImageUrl: " + imageUrl);
                        return new ProfileData(name, mobile, imageUrl);
                    } else {
                        Log.e(TAG, "JSON does not contain 'data' field");
                    }
                } else {
                    Log.e(TAG, "Server returned non-OK status: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception in GetProfileTask: ", e);
            }
            return null;
        }




        @Override
        protected void onPostExecute(ProfileData profileData) {
            if (profileData != null) {
                Log.d(TAG, "ProfileData received - Name: " + profileData.getName() +
                        ", Mobile: " + profileData.getMobileNumber() +
                        ", ImageUrl: " + profileData.getImageName());
                tvProfileName.setText(profileData.getName());
                tvProfileEmail.setText(profileData.getMobileNumber());
                // Use Glide to load the image from URL
                Glide.with(MainActivity.this)
                        .load(profileData.getImageName())
                        .placeholder(R.drawable.pr_ic_profile_placeholder) // Optional placeholder
                        .into(civProfile);
            } else {
                Log.e(TAG, "ProfileData is null");
            }
        }
    }

    // Model class for profile data (renamed to avoid conflict with your Profile activity)
    private class ProfileData {
        private String name;
        private String mobileNumber;
        private String imageName; // Now contains the full URL

        public ProfileData(String name, String mobileNumber, String imageName) {
            this.name = name;
            this.mobileNumber = mobileNumber;
            this.imageName = imageName;
        }

        public String getName() {
            return name;
        }

        public String getMobileNumber() {
            return mobileNumber;
        }

        public String getImageName() {
            return imageName;
        }
    }
}
