package com.infowave.thedoctorathomeuser;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.infowave.thedoctorathomeuser.adapter.FeatureAdapter;
import com.infowave.thedoctorathomeuser.adapter.StepAdapter;
import com.infowave.thedoctorathomeuser.adapter.TeamAdapter;

import java.util.ArrayList;
import java.util.List;

public class aboutus extends AppCompatActivity {

    private ImageView backbutton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aboutus);

        // ---- Make bars black with white icons + set scrim heights via insets ----
        try {
            getWindow().setStatusBarColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);

            WindowInsetsControllerCompat controller =
                    new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
            controller.setAppearanceLightStatusBars(false);      // white icons for status bar
            controller.setAppearanceLightNavigationBars(false);  // white icons for nav bar

            final View statusScrim = findViewById(R.id.status_bar_scrim);
            final View navScrim = findViewById(R.id.navigation_bar_scrim);

            View root = findViewById(android.R.id.content);
            if (root != null) {
                ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                    Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());

                    if (statusScrim != null) {
                        ViewGroup.LayoutParams lp = statusScrim.getLayoutParams();
                        lp.height = sys.top; // status bar height
                        statusScrim.setLayoutParams(lp);
                        statusScrim.setVisibility(sys.top > 0 ? View.VISIBLE : View.GONE);
                    }

                    if (navScrim != null) {
                        ViewGroup.LayoutParams lp = navScrim.getLayoutParams();
                        lp.height = sys.bottom; // nav bar height (0 on gesture)
                        navScrim.setLayoutParams(lp);
                        navScrim.setVisibility(sys.bottom > 0 ? View.VISIBLE : View.GONE);
                    }
                    return insets;
                });
            }
        } catch (Throwable ignored) { }
        // -----------------------------------------------------------------------

        backbutton = findViewById(R.id.backButton);
        if (backbutton != null) {
            backbutton.setOnClickListener(v -> {
                startActivity(new Intent(aboutus.this, MainActivity.class));
                finish();
            });
        }

        setupFeatures();
        setupTeam();
        setupSteps();
        setupContactInfo();
    }

    private void setupFeatures() {
        RecyclerView featuresRecycler = findViewById(R.id.featuresRecycler);
        if (featuresRecycler == null) return;

        List<AppFeature> features = new ArrayList<>();
        features.add(new AppFeature(R.drawable.ic_secure, "Secure", "Bank-grade security"));
        features.add(new AppFeature(R.drawable.ic_location, "Location", "User can see doctor location"));
        features.add(new AppFeature(R.drawable.payment, "Payment", "Two modes of payments online and offline"));
        features.add(new AppFeature(R.drawable.ic_support, "24/7 Support", "Always available"));

        FeatureAdapter adapter = new FeatureAdapter(features);
        featuresRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        featuresRecycler.setAdapter(adapter);
    }

    private void setupTeam() {
        RecyclerView teamRecycler = findViewById(R.id.teamRecycler);
        if (teamRecycler == null) return;

        List<TeamMember> team = new ArrayList<>();
        team.add(new TeamMember("Alice Smith", "Lead Surgeon ", R.drawable.doctor_avatar));
        team.add(new TeamMember("Bob Johnson", "Expert in Medicines", R.drawable.doctor_avatar));
        team.add(new TeamMember("Charlie Brown", "general Doctor", R.drawable.doctor_avatar));

        TeamAdapter adapter = new TeamAdapter(team);
        teamRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        teamRecycler.setAdapter(adapter);
    }

    private void setupSteps() {
        RecyclerView stepsRecycler = findViewById(R.id.stepsRecycler);
        if (stepsRecycler == null) return;

        List<Step> steps = new ArrayList<>();
        steps.add(new Step(1, "Search for doctors by specialty or name"));
        steps.add(new Step(2, "Select a doctor and fill the form with detail"));
        steps.add(new Step(3, "Confirm your booking details"));
        steps.add(new Step(4, "After booking select a payment mode"));
        steps.add(new Step(5, "Make payment"));
        steps.add(new Step(6, "Receive confirmation of your appointment"));

        StepAdapter adapter = new StepAdapter(steps);
        stepsRecycler.setLayoutManager(new LinearLayoutManager(this));
        stepsRecycler.setAdapter(adapter);
    }

    private void setupContactInfo() {
        TextView email = findViewById(R.id.contactEmail);
        TextView contact = findViewById(R.id.contact);
        if (email != null) email.setText("thedoctorathome2025@gmail.com");
        if (contact != null) contact.setText("+91 6354355617");
    }

    public void openFacebook(View view) { openUrl("https://facebook.com/yourpage"); }
    public void openTwitter(View view)  { openUrl("https://twitter.com/yourhandle"); }

    private void openUrl(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) { Toast.makeText(this, R.string.link_error, Toast.LENGTH_SHORT).show(); }
    }
}
