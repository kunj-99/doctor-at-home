package com.infowave.thedoctorathomeuser;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class DegreeSelectionActivity extends AppCompatActivity {

    public static final String EXTRA_DEGREE_KEY = "selected_degree_key";

    private List<DegreeOption> degreeOptions;
    private LinearLayout cardsContainer;
    private int selectedPosition = -1;

    // Scrim Views
    private View statusScrim;
    private View navScrim;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_degree_selection);

        // 1) Enable true edge-to-edge so scrims can occupy system bar areas.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // 2) Force solid black system bars (as a baseline color).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
        }

        // 3) Ensure icons are WHITE on the black bars.
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        // 4) Hook the scrim views and size them exactly to the insets.
        statusScrim = findViewById(R.id.status_bar_scrim);
        navScrim = findViewById(R.id.navigation_bar_scrim);

        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            final Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Top status bar height
            if (statusScrim != null) {
                ViewGroup.LayoutParams lp = statusScrim.getLayoutParams();
                lp.height = sys.top;
                statusScrim.setLayoutParams(lp);
                statusScrim.setVisibility(sys.top > 0 ? View.VISIBLE : View.GONE);
            }

            // Bottom nav/gesture area height (0 on gesture nav -> hide)
            if (navScrim != null) {
                ViewGroup.LayoutParams lp = navScrim.getLayoutParams();
                lp.height = sys.bottom;
                navScrim.setLayoutParams(lp);
                navScrim.setVisibility(sys.bottom > 0 ? View.VISIBLE : View.GONE);
            }

            // IMPORTANT: do NOT consume insets; allow child scrolling behavior, etc.
            return insets;
        });

        initializeViews();
        setupDegreeOptions();
        populateCards();
        setupToolbar();
    }

    private void initializeViews() {
        cardsContainer = findViewById(R.id.cardsContainer);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        toolbar.setNavigationIconTint(Color.WHITE);
        // Toolbar gradient stays separate; system bars are handled by scrims above.
    }

    private void setupDegreeOptions() {
        degreeOptions = new ArrayList<>();

        degreeOptions.add(new DegreeOption(
                "Bachelor Vet Doctor",
                "B.V.Sc & A.H.",
                "Entry to mid-level veterinary physicians with comprehensive animal care knowledge and basic surgical skills.",
                R.drawable.ic_degree_bachelor
        ));

        degreeOptions.add(new DegreeOption(
                "Master Vet Doctor",
                "M.V.Sc",
                "Advanced specialists with deeper expertise in specific animal species and complex medical procedures.",
                R.drawable.ic_degree_master
        ));

        // Add more if needed...
    }

    private void populateCards() {
        cardsContainer.removeAllViews();

        for (int i = 0; i < degreeOptions.size(); i++) {
            DegreeOption option = degreeOptions.get(i);

            // Inflate card layout
            View cardView = LayoutInflater.from(this).inflate(R.layout.item_degree_option, cardsContainer, false);

            // Bind views
            MaterialCardView cardRoot = cardView.findViewById(R.id.cardRoot);
            ImageView ivIcon = cardView.findViewById(R.id.ivIcon);
            TextView tvTitle = cardView.findViewById(R.id.tvTitle);
            TextView tvSubtitle = cardView.findViewById(R.id.tvSubtitle);
            TextView tvDesc = cardView.findViewById(R.id.tvDesc);
            ImageView selectionIndicator = cardView.findViewById(R.id.selectionIndicator);

            // Set data
            tvTitle.setText(option.getPrimaryTitle());
            tvSubtitle.setText(option.getSubtitle());
            tvDesc.setText(option.getDescription());
            ivIcon.setImageResource(option.getIconRes());

            final int position = i;
            cardRoot.setOnClickListener(v -> onDegreeCardClicked(position, cardRoot, selectionIndicator));

            // Add to container
            cardsContainer.addView(cardView);
        }

        // Default (unselected) state
        for (int i = 0; i < cardsContainer.getChildCount(); i++) {
            updateCardAppearance(i, false);
        }
    }

    private void onDegreeCardClicked(int position, MaterialCardView cardRoot, ImageView selectionIndicator) {
        // Reset previous
        if (selectedPosition != -1) {
            updateCardAppearance(selectedPosition, false);
        }

        // New selection
        selectedPosition = position;
        updateCardAppearance(selectedPosition, true);

        // Tiny press animation
        cardRoot.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(80)
                .withEndAction(() -> cardRoot.animate().scaleX(1f).scaleY(1f).setDuration(80).start())
                .start();

        // Navigate a moment later
        cardRoot.postDelayed(() -> {
            DegreeOption selected = degreeOptions.get(position);
            Intent intent = new Intent(this, VetAnimalsActivity.class);
            intent.putExtra(EXTRA_DEGREE_KEY, selected.getPrimaryTitle());
            startActivity(intent);
        }, 150);
    }

    private void updateCardAppearance(int position, boolean isSelected) {
        View cardView = cardsContainer.getChildAt(position);
        if (cardView == null) return;

        MaterialCardView card = cardView.findViewById(R.id.cardRoot);
        ImageView selectionIndicator = cardView.findViewById(R.id.selectionIndicator);

        if (isSelected) {
            card.setStrokeColor(Color.parseColor("#4361EE"));
            card.setStrokeWidth(dpToPx(2));
            if (selectionIndicator != null) {
                selectionIndicator.setImageResource(R.drawable.ic_selected_check);
                selectionIndicator.setColorFilter(Color.parseColor("#4361EE"));
            }
            card.setCardElevation(dpToPx(8));
        } else {
            card.setStrokeColor(Color.parseColor("#F3F4F6"));
            card.setStrokeWidth(dpToPx(1));
            if (selectionIndicator != null) {
                selectionIndicator.setImageResource(R.drawable.ic_unselected_circle);
                selectionIndicator.setColorFilter(Color.parseColor("#D1D5DB"));
            }
            card.setCardElevation(dpToPx(4));
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    // Simple model
    public static class DegreeOption {
        private final String primaryTitle;
        private final String subtitle;
        private final String description;
        private final int iconRes;

        public DegreeOption(String primaryTitle, String subtitle, String description, int iconRes) {
            this.primaryTitle = primaryTitle;
            this.subtitle = subtitle;
            this.description = description;
            this.iconRes = iconRes;
        }

        public String getPrimaryTitle() { return primaryTitle; }
        public String getSubtitle() { return subtitle; }
        public String getDescription() { return description; }
        public int getIconRes() { return iconRes; }
    }
}
