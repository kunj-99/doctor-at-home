package com.infowave.thedoctorathomeuser; // Replace with your package name

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.infowave.thedoctorathomeuser.adapter.TeamAdapter;
import com.infowave.thedoctorathomeuser.adapter.FeatureAdapter;

import java.util.ArrayList;

import com.infowave.thedoctorathomeuser.adapter.StepAdapter;

import java.util.List;

public class aboutus extends AppCompatActivity {
ImageView backbutton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aboutus);
    backbutton = findViewById(R.id.backButton);
    backbutton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent in = new Intent(aboutus.this, MainActivity.class);
            startActivity(in);
        }
    });
        setupFeatures();
        setupTeam();
        setupSteps();
        setupContactInfo();
    }

    // In your AboutUsActivity.java
    private void setupFeatures() {
        RecyclerView featuresRecycler = findViewById(R.id.featuresRecycler);

        // Explicitly declare as List<AppFeature>
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
        List<TeamMember> team = new ArrayList<>();
        team.add(new TeamMember("Alice Smith", "Lead Developer", R.drawable.doctor_avatar));
        team.add(new TeamMember("Bob Johnson", "UI Designer", R.drawable.doctor_avatar));
        team.add(new TeamMember("Charlie Brown", "QA Engineer", R.drawable.doctor_avatar));

        TeamAdapter adapter = new TeamAdapter(team);
        teamRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        teamRecycler.setAdapter(adapter);
    }

    private void setupContactInfo() {
        TextView email = findViewById(R.id.contactEmail);
        TextView contact = findViewById(R.id.contact);
        email.setText("support@company.com");
        contact.setText("+91 9876543210");
    }
    private void setupSteps() {
        RecyclerView stepsRecycler = findViewById(R.id.stepsRecycler);
        List<Step> steps = new ArrayList<>();
        steps.add(new Step(1, "Search for doctors by specialty or name"));
        steps.add(new Step(2, "Select a doctor and fill the form with detail"));
        steps.add(new Step(3, "Confirm your booking details"));
        steps.add(new Step(4, "After booking select a payment mode "));
        steps.add(new Step(5, "Make payment"));
        steps.add(new Step(6, "Receive confirmation of your appointment"));

        StepAdapter adapter = new StepAdapter(steps);
        stepsRecycler.setAdapter(adapter);
    }
    public void openFacebook(View view) {
        openUrl("https://facebook.com/yourpage");
    }

    public void openTwitter(View view) {
        openUrl("https://twitter.com/yourhandle");
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.link_error, Toast.LENGTH_SHORT).show();
        }
    }
}