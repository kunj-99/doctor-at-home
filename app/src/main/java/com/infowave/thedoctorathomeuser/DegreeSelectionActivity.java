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

    public static final String EXTRA_DEGREE_KEY       = "selected_degree_key";
    public static final String EXTRA_CATEGORY_ID      = "category_id";
    public static final String EXTRA_CATEGORY_NAME    = "category_name";
    public static final String EXTRA_CATEGORY_PRICE   = "price";
    public static final String EXTRA_CATEGORY_IMAGE   = "image";
    public static final String EXTRA_CATEGORY_DISEASE = "disease";

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private View statusScrim, navScrim;

    private final List<JSONObject> categories = new ArrayList<>();
    private DegreeAdapter adapter;

    // अपनी API:
    private static final String VET_CATEGORY_URL = ApiConfig.endpoint("Animal/get_vet_categories.php");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_degree_selection);

        Log.d(TAG, "DegreeSelectionActivity.onCreate");

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
                lp.height = sys.top; statusScrim.setLayoutParams(lp);
                statusScrim.setVisibility(sys.top > 0 ? View.VISIBLE : View.GONE);
            }
            if (navScrim != null) {
                ViewGroup.LayoutParams lp = navScrim.getLayoutParams();
                lp.height = sys.bottom; navScrim.setLayoutParams(lp);
                navScrim.setVisibility(sys.bottom > 0 ? View.VISIBLE : View.GONE);
            }
            return insets;
        });

        setupToolbar();
        initRecycler();
        fetchVetCategories();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setTitle("Choose Animal Category");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        toolbar.setNavigationIconTint(Color.WHITE);
    }

    private void initRecycler() {
        recyclerView = findViewById(R.id.recyclerView);
        emptyState   = findViewById(R.id.emptyState);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DegreeAdapter(categories, selected -> {
            int categoryId       = selected.optInt("category_id", 0);
            String categoryName  = selected.optString("category_name", "");
            double price         = selected.optDouble("price", 0.0);
            String image         = selected.optString("image", selected.optString("category_image", ""));
            String disease       = selected.optString("disease", "");

            Log.d(TAG, "CLICK DegreeSelection -> going to VetAnimalsActivity: " +
                    "id=" + categoryId + ", name=" + categoryName +
                    ", price=" + price + ", image=" + image);
            Toast.makeText(this, "Selected: " + categoryName + " (" + categoryId + ")", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, VetAnimalsActivity.class);
            intent.putExtra(EXTRA_DEGREE_KEY, "Vet");
            intent.putExtra(EXTRA_CATEGORY_ID,      categoryId);
            intent.putExtra(EXTRA_CATEGORY_NAME,    categoryName);
            intent.putExtra(EXTRA_CATEGORY_PRICE,   price);
            intent.putExtra(EXTRA_CATEGORY_IMAGE,   image);
            intent.putExtra(EXTRA_CATEGORY_DISEASE, disease);
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
                                Log.d(TAG, "cat[" + i + "] id=" + o.optInt("category_id")
                                        + ", name=" + o.optString("category_name")
                                        + ", price=" + o.optDouble("price", 0.0));
                                categories.add(o);
                            }
                        }
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
