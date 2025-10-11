package com.infowave.thedoctorathomeuser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.thedoctorathomeuser.adapter.DiseaseAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Diseases screen with PERFECT true-black system bars via scrim Views.
 * Layout must contain:
 *  - root_container           (LinearLayout root)
 *  - status_bar_scrim         (View at top, height 0dp)
 *  - navigation_bar_scrim     (View at bottom, height 0dp)
 */
public class diseases extends AppCompatActivity {

    private static final String API_PATH = "fetch_diseases.php";

    private RecyclerView recyclerView;
    private DiseaseAdapter adapter;
    private final List<String> diseaseList = new ArrayList<>();
    private Button confirm;
    private String categoryId, categoryName;
    private TextView title;

    private RequestQueue requestQueue;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diseases);

        // 1) Edge-to-edge: our scrim views occupy bar areas
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // 2) We draw system bars and force them to true black; icons white
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        WindowInsetsControllerCompat wic =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        wic.setAppearanceLightStatusBars(false);     // white status icons
        wic.setAppearanceLightNavigationBars(false); // white nav icons

        // 3) Remove OEM overlays that can make bottom look gray
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().setNavigationBarDividerColor(Color.BLACK);
        }

        // 4) Size scrims from real system bar insets
        final View root = findViewById(R.id.root_container);
        final View statusScrim = findViewById(R.id.status_bar_scrim);
        final View navScrim = findViewById(R.id.navigation_bar_scrim);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            final WindowInsetsCompat in = insets;
            final int top = in.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars()).top;
            final int bottom = in.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars()).bottom;

            if (statusScrim != null) {
                ViewGroup.LayoutParams lpTop = statusScrim.getLayoutParams();
                lpTop.height = top;
                statusScrim.setLayoutParams(lpTop);
                statusScrim.setVisibility(top > 0 ? View.VISIBLE : View.GONE);
            }

            if (navScrim != null) {
                ViewGroup.LayoutParams lpBot = navScrim.getLayoutParams();
                lpBot.height = bottom; // 0 on gesture navigation
                navScrim.setLayoutParams(lpBot);
                navScrim.setVisibility(bottom > 0 ? View.VISIBLE : View.GONE);
            }

            // Do NOT add extra paddings here – the scrim views are part of layout.
            return insets; // not consumed
        });

        // ---- UI refs ----
        title = findViewById(R.id.title);
        confirm = findViewById(R.id.confirm_button);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DiseaseAdapter(diseaseList);
        recyclerView.setAdapter(adapter);

        // ---- Intent extras ----
        categoryId = getIntent().getStringExtra("category_id");
        categoryName = getIntent().getStringExtra("category_name");

        if (categoryName != null && !categoryName.trim().isEmpty()) {
            title.setText(categoryName + " – General Physician");
        } else {
            title.setText("Select a Category");
        }

        // ---- Networking ----
        requestQueue = Volley.newRequestQueue(this);

        if (categoryId != null && !categoryId.trim().isEmpty()) {
            fetchDiseases(categoryId.trim());
        } else {
            Toast.makeText(this, "Invalid category. Please go back and select again.", Toast.LENGTH_SHORT).show();
        }

        // ---- Confirm ----
        confirm.setOnClickListener(v -> {
            Intent intent = new Intent(diseases.this, available_doctor.class);
            intent.putExtra("category_id", categoryId);
            intent.putExtra("category_name", categoryName);
            startActivity(intent);
        });
    }

    private void fetchDiseases(String categoryId) {
        final String url = ApiConfig.endpoint(API_PATH, "category_id", categoryId);

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    diseaseList.clear();

                    boolean ok = response.optBoolean("success", false);
                    if (ok) {
                        JSONArray arr = response.optJSONArray("diseases");
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.optJSONObject(i);
                                if (obj == null) continue;
                                String name = obj.optString("disease_name", "").trim();
                                if (!name.isEmpty()) diseaseList.add(name);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        if (diseaseList.isEmpty()) {
                            Toast.makeText(this, "No diseases found for this category.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        String msg = response.optString("message", "No diseases found.");
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                        adapter.notifyDataSetChanged();
                    }
                },
                error -> Toast.makeText(this, "Network error. Please check your internet.", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(req);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) requestQueue.stop();
    }
}
