package com.example.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.firebase.FirebaseApp;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import com.example.thedoctorathomeuser.Fragment.HomeFragment;
import com.example.thedoctorathomeuser.view.view;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private ViewPager vp;
    private BottomNavigationView btn;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    private LinearLayout profileSection;
    private TextView toolbarTitle; // To dynamically update the title

    @SuppressLint("MissingInflatedId")
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
                intent = new Intent(MainActivity.this, HomeFragment.class);
            } else if (item.getItemId() == R.id.records1) {
                intent = new Intent(MainActivity.this, HomeFragment.class);
            } else if (item.getItemId() == R.id.policy) {
                intent = new Intent(MainActivity.this, HomeFragment.class);
            } else if (item.getItemId() == R.id.notification) {
                intent = new Intent(MainActivity.this, HomeFragment.class);
            } else if (item.getItemId() == R.id.support1) {
                intent = new Intent(MainActivity.this, HomeFragment.class);
            } else if (item.getItemId() == R.id.shareapp1) {
                intent = new Intent(MainActivity.this, share_app.class);
            } else if (item.getItemId() == R.id.aboutus1) {
                intent = new Intent(MainActivity.this, HomeFragment.class);
            } else if (item.getItemId() == R.id.settings1) {
                intent = new Intent(MainActivity.this, HomeFragment.class);
            } else if (item.getItemId() == R.id.rateapp) {
                intent = new Intent(MainActivity.this, Rate_app.class);
            } else if (item.getItemId() == R.id.logout) {
                // Handle logout logic here
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

        // Handle profile section click
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
}
