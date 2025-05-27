package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log; // For debugging
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.GravityCompat;

import com.bumptech.glide.Glide; // Using Glide for image loading
import com.google.firebase.FirebaseApp;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import com.infowave.thedoctorathomeuser.view.*;
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

    // Loader-related fields
    private final Handler loaderHandler = new Handler();
    private Runnable loaderRunnable;
    private boolean networkResponseReceived = false;

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
            // Setup delayed loader trigger (300ms delay)
            networkResponseReceived = false;
            loaderRunnable = new Runnable() {
                @Override
                public void run() {
                    // If the network response isn't received yet, show the custom loader using loaderutil
                    if (!networkResponseReceived) {
                        loaderutil.showLoader(MainActivity.this);
                    }
                }
            };
            loaderHandler.postDelayed(loaderRunnable, 300);

            // Execute the AsyncTask to fetch profile details
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

        toggle.setDrawerIndicatorEnabled(false);
        toolbar.setNavigationIcon(R.drawable.burger_menu);  // Replace with your custom icon drawable

        // Load your custom icon drawable
        Drawable customIcon = ContextCompat.getDrawable(this, R.drawable.burger_menu);

        if (customIcon != null) {
            // Wrap the drawable so that it can be tinted
            customIcon = DrawableCompat.wrap(customIcon);
            // Set the tint color to white
            DrawableCompat.setTint(customIcon, ContextCompat.getColor(this, android.R.color.white));
        }

        // Set the tinted icon as the navigation icon
        toolbar.setNavigationIcon(customIcon);

        // Add click listener for the navigation icon to open/close the drawer
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });

        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.black));
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Set up the ViewPager with a custom adapter
        view adapter = new view(getSupportFragmentManager());
        vp.setAdapter(adapter);

        // Set up BottomNavigationView
        int fragmentToOpen = getIntent().getIntExtra("open_fragment", 0);
        vp.setCurrentItem(fragmentToOpen);
        if (fragmentToOpen == 2) {
            btn.setSelectedItemId(R.id.page_3);
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
                intent = new Intent(MainActivity.this,  payments.class);
            } else if (item.getItemId() == R.id.records1) {
                intent = new Intent(MainActivity.this, pathology_test.class);
            } else if (item.getItemId() == R.id.policy) {
                intent = new Intent(MainActivity.this, policy.class);
            } else if (item.getItemId() == R.id.notification) {
                intent = new Intent(MainActivity.this, tarmsandcondition.class);
            } else if (item.getItemId() == R.id.support1) {
                intent = new Intent(MainActivity.this, suppor.class);
            } else if (item.getItemId() == R.id.shareapp1) {
                shareApp();  // Directly share the app
                return true;
            } else if (item.getItemId() == R.id.aboutus1) {
                intent = new Intent(MainActivity.this, aboutus.class);
            } else if (item.getItemId() == R.id.settings1) {
                intent = new Intent(MainActivity.this, settings.class);
            } else if (item.getItemId() == R.id.rateapp) {
                rateApp();
                return true;
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

        MyFirebaseMessagingService.requestNotificationPermissionIfNeeded(this);
        MyFirebaseMessagingService.refreshTokenIfNeeded(this);

    }

    private void rateApp() {
        String appPackageName = getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareBody = "Check out this cool app: [Your App Link Here]";
        String shareSubject = "The Doctor At Home App";
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, shareSubject);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (vp.getCurrentItem() != 0) {
            vp.setCurrentItem(0);
        } else {
            super.onBackPressed();
        }
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
    @SuppressLint("StaticFieldLeak")
    private class GetProfileTask extends AsyncTask<String, Void, ProfileData> {

        private final CircleImageView civProfile;
        private final TextView tvProfileName;
        private final TextView tvProfileEmail;

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
                    Log.d(TAG, "Response: " + response);

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
            // Cancel the loader since the network response is received
            MainActivity.this.networkResponseReceived = true;
            loaderHandler.removeCallbacks(loaderRunnable);
            loaderutil.hideLoader();

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

    // Model class for profile data
    private class ProfileData {
        private final String name;
        private final String mobileNumber;
        private final String imageName; // Contains the full URL

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
