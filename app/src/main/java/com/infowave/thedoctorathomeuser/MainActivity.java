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
//import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.GravityCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseApp;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
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
    private static final String TAG = "GetProfileTask";

    private ViewPager vp;
    private BottomNavigationView btn;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    private LinearLayout profileSection;
    private TextView toolbarTitle;

    private final Handler loaderHandler = new Handler();
    private Runnable loaderRunnable;
    private boolean networkResponseReceived = false;

    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);
        // ✅ Set system bar colors and icons
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, android.R.color.black));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, android.R.color.black));

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        // ✅ Views for black overlays
        final View statusScrim = findViewById(R.id.status_bar_scrim);
        final View navScrim = findViewById(R.id.navigation_bar_scrim);

        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_container), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Set height of black scrims
            if (statusScrim != null) {
                ViewGroup.LayoutParams lp = statusScrim.getLayoutParams();
                lp.height = sys.top;
                statusScrim.setLayoutParams(lp);
                statusScrim.setVisibility(View.VISIBLE);
            }

            if (navScrim != null) {
                ViewGroup.LayoutParams lp = navScrim.getLayoutParams();
                lp.height = sys.bottom;
                navScrim.setLayoutParams(lp);
                navScrim.setVisibility(View.VISIBLE);
            }

            // Drawer padding for edges
            View drawer = findViewById(R.id.drawer_layout);
            if (drawer != null) {
                drawer.setPadding(sys.left, 0, sys.right, 0);
            }
            return insets;
        });


        // Initialize UI components
        vp = findViewById(R.id.vp);
        btn = findViewById(R.id.bottem);
        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.tb);
        navigationView = findViewById(R.id.nev_view);
        toolbarTitle = findViewById(R.id.toolbar_title);

        // Reference the profile section from the navigation header
        View headerView = navigationView.getHeaderView(0);
        profileSection = headerView.findViewById(R.id.ll_profile_section);
        CircleImageView civProfile = headerView.findViewById(R.id.civ_profile);
        TextView tvProfileName = headerView.findViewById(R.id.tv_profile_name);
        TextView tvProfileEmail = headerView.findViewById(R.id.tv_profile_email);

        // Fetch profile details from remote server
        SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String patientId = userPrefs.getString("patient_id", "");
        if (!patientId.isEmpty()) {
            networkResponseReceived = false;
            loaderRunnable = () -> {
                if (!networkResponseReceived) {
                    loaderutil.showLoader(MainActivity.this);
                }
            };
            loaderHandler.postDelayed(loaderRunnable, 300);
            new GetProfileTask(civProfile, tvProfileName, tvProfileEmail).execute(patientId);
        }

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Drawer Toggle setup
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);

        toggle.setDrawerIndicatorEnabled(false);
        toolbar.setNavigationIcon(R.drawable.burger_menu);

        Drawable customIcon = ContextCompat.getDrawable(this, R.drawable.burger_menu);
        if (customIcon != null) {
            customIcon = DrawableCompat.wrap(customIcon);
            DrawableCompat.setTint(customIcon, ContextCompat.getColor(this, android.R.color.white));
        }
        toolbar.setNavigationIcon(customIcon);

        toolbar.setNavigationOnClickListener(view -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.black));
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // ViewPager and Adapter
        view adapter = new view(getSupportFragmentManager());
        vp.setAdapter(adapter);

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

        vp.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

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
            @Override public void onPageScrollStateChanged(int state) {}
        });

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
                shareApp();
                return true;
            } else if (item.getItemId() == R.id.aboutus1) {
                intent = new Intent(MainActivity.this, aboutus.class);
            } else if (item.getItemId() == R.id.settings1) {
                intent = new Intent(MainActivity.this, settings.class);
            } else if (item.getItemId() == R.id.rateapp) {
                rateApp();
                return true;
            } else if (item.getItemId() == R.id.logout) {
                SharedPreferences userPrefsLogout = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor userEditor = userPrefsLogout.edit();
                userEditor.clear();
                userEditor.apply();

                SharedPreferences reviewPrefs = getSharedPreferences("ReviewPrefs", MODE_PRIVATE);
                SharedPreferences.Editor reviewEditor = reviewPrefs.edit();
                reviewEditor.clear();
                reviewEditor.apply();

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

        // Adjust navigation drawer width
        adjustNavigationDrawerWidth();

        // Open profile on click
        profileSection.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Profile.class);
            startActivity(intent);
        });

        MyFirebaseMessagingService.requestNotificationPermissionIfNeeded(this);
        FcmTokenHelper.ensureTokenSynced(this);



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
        // 1) Close navigation drawer if open
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        // 2) If ViewPager exists and not on 0th tab, go to 0th (with smooth scroll)
        if (vp != null) {
            int current = vp.getCurrentItem();
            if (current > 0) {
                vp.setCurrentItem(0, true); // true = smooth animation
                return;
            }
        }

        // 3) Otherwise, default back behavior
        super.onBackPressed();
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
            String urlString = ApiConfig.endpoint("get_profile.php", "patient_id", patientId);
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(7000);
                connection.setReadTimeout(7000);
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonObject = new JSONObject(response.toString());
                    if (jsonObject.has("data")) {
                        JSONObject dataObj = jsonObject.getJSONObject("data");
                        String name = dataObj.optString("full_name");
                        String mobile = dataObj.optString("mobile");
                        String imageUrl = dataObj.optString("profile_picture");
                        return new ProfileData(name, mobile, imageUrl);
                    }
                }
            } catch (Exception e) {
                // Logging error only, no technical error message shown to user
            }
            return null;
        }

        @Override
        protected void onPostExecute(ProfileData profileData) {
            MainActivity.this.networkResponseReceived = true;
            loaderHandler.removeCallbacks(loaderRunnable);
            loaderutil.hideLoader();

            if (profileData != null) {
                tvProfileName.setText(profileData.getName());
                tvProfileEmail.setText(profileData.getMobileNumber());
                Glide.with(MainActivity.this)
                        .load(profileData.getImageName())
                        .placeholder(R.drawable.pr_ic_profile_placeholder)
                        .into(civProfile);
            }
        }
    }

    // Model class for profile data
    private static class ProfileData {
        private final String name;
        private final String mobileNumber;
        private final String imageName;

        public ProfileData(String name, String mobileNumber, String imageName) {
            this.name = name;
            this.mobileNumber = mobileNumber;
            this.imageName = imageName;
        }

        public String getName() { return name; }
        public String getMobileNumber() { return mobileNumber; }
        public String getImageName() { return imageName; }
    }
}
