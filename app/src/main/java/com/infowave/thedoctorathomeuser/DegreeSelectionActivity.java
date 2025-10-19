package com.infowave.thedoctorathomeuser;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.infowave.thedoctorathomeuser.adapter.DegreeAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DegreeSelectionActivity extends AppCompatActivity {

    private static final String TAG = "VET_FLOW";

    public static final String EXTRA_DEGREE_KEY        = "selected_degree_key";
    public static final String EXTRA_VET_CATEGORY_ID   = "vet_category_id";
    public static final String EXTRA_VET_CATEGORY_NAME = "vet_category_name";
    public static final String EXTRA_DOCTOR_TYPE       = "doctor_type";

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private View statusScrim, navScrim;

    private final List<JSONObject> categories = new ArrayList<>();
    private DegreeAdapter adapter;

    private static final String VET_CATEGORY_URL = ApiConfig.endpoint("Animal/get_vet_categories.php");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_degree_selection);

        Log.d(TAG, "DegreeSelectionActivity.onCreate");

        setupEdgeToEdge();
        setupToolbar();
        initRecycler();
        fetchVetCategories();
    }

    private void setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.BLACK);
            getWindow().setNavigationBarColor(Color.BLACK);
        }
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        statusScrim = findViewById(R.id.status_bar_scrim);
        navScrim    = findViewById(R.id.navigation_bar_scrim);

        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            final Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (statusScrim != null) {
                ViewGroup.LayoutParams lp = statusScrim.getLayoutParams();
                lp.height = sys.top;
                statusScrim.setLayoutParams(lp);
                statusScrim.setVisibility(sys.top > 0 ? View.VISIBLE : View.GONE);
            }
            if (navScrim != null) {
                ViewGroup.LayoutParams lp = navScrim.getLayoutParams();
                lp.height = sys.bottom;
                navScrim.setLayoutParams(lp);
                navScrim.setVisibility(sys.bottom > 0 ? View.VISIBLE : View.GONE);
            }
            return insets;
        });
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setTitle("Choose Veterinary Degree");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        toolbar.setNavigationIconTint(Color.WHITE);
    }

    private void initRecycler() {
        recyclerView = findViewById(R.id.recyclerView);
        emptyState   = findViewById(R.id.emptyState);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DegreeAdapter(categories, selected -> {
            int categoryId      = selected.optInt("category_id", 0);
            String categoryName = selected.optString("category_name", "");
            String doctorType   = selected.optString("doctor_type", "general"); // Always from API!

            Log.d(TAG, "CLICK DegreeSelection → VetAnimalsActivity: id=" + categoryId + ", name=" + categoryName + ", doctor_type=" + doctorType);

            Toast.makeText(this, "Selected: " + categoryName + " (" + doctorType + ")", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, VetAnimalsActivity.class);
            intent.putExtra(EXTRA_DEGREE_KEY, "Vet");
            intent.putExtra(EXTRA_VET_CATEGORY_ID, categoryId);
            intent.putExtra(EXTRA_VET_CATEGORY_NAME, categoryName);
            intent.putExtra(EXTRA_DOCTOR_TYPE, doctorType); // 🔥 Correct pass
            Log.d(TAG, "Sent Intent → vet_category_id: " + categoryId + ", doctor_type: " + doctorType);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void fetchVetCategories() {
        Log.d(TAG, "GET " + VET_CATEGORY_URL);
        RequestQueue q = Volley.newRequestQueue(this);

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                VET_CATEGORY_URL,
                null,
                resp -> {
                    try {
                        boolean ok = resp.optBoolean("success", false);
                        JSONArray arr = resp.optJSONArray("data");
                        Log.d(TAG, "Categories success=" + ok + ", count=" + (arr==null?0:arr.length()));
                        categories.clear();
                        if (ok && arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.getJSONObject(i);

                                Log.d(TAG, String.format("Loaded category: id=%d, name=%s, doctor_type=%s",
                                        o.optInt("category_id", 0),
                                        o.optString("category_name", ""),
                                        o.optString("doctor_type", "")));

                                JSONObject full = new JSONObject();
                                full.put("category_id",   o.optInt("category_id", 0));
                                full.put("category_name", o.optString("category_name", ""));
                                full.put("doctor_type",   o.optString("doctor_type", "general"));
                                full.put("price",         o.optDouble("price", 0.0));
                                full.put("disease",       o.optString("disease", ""));
                                full.put("image",         o.optString("image", ""));

                                categories.add(full);
                            }
                        }
                        // Ensure cards ordered by category_id ascending (lowest id first)
                        categories.sort((a, b) -> Integer.compare(
                                a.optInt("category_id", 0), b.optInt("category_id", 0))
                        );
                        adapter.notifyDataSetChanged();
                        showEmpty(categories.isEmpty());
                    } catch (Exception e) {
                        Log.e(TAG, "Category parse error", e);
                        Toast.makeText(this, "Parse error", Toast.LENGTH_SHORT).show();
                        showEmpty(true);
                    }
                },
                err -> {
                    Log.e(TAG, "Category network error", err);
                    Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
                    showEmpty(true);
                }
        );
        q.add(req);
    }

    private void showEmpty(boolean show) {
        if (emptyState != null) emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onBackPressed() { super.onBackPressed(); }
}
