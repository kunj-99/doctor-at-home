package com.infowave.thedoctorathomeuser.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;              // Main pager in Activity (R.id.vp, legacy ViewPager)
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.infowave.thedoctorathomeuser.MainActivity;
import com.infowave.thedoctorathomeuser.R;

/**
 * Ongoing screen — History-style structure (TabLayout + ViewPager2).
 * Tabs:
 *  - 0: HumanOngoingFragment
 *  - 1: VetOngoingFragment
 *
 * Edge-swipe bridge:
 *  - On tab 0, swipe RIGHT  → BookAppointment page (main pager index = 1).
 *  - On tab 1, swipe LEFT   → History page        (main pager index = 3).
 *
 * Adjust PAGE_* indices to match your MainActivity pager order if needed.
 */
public class OngoingAppointmentFragment extends Fragment {

    // ---- MAIN ACTIVITY VIEWPAGER INDEXES (adjust if your order differs) ----
    private static final int PAGE_BOOK_APPOINTMENT = 1; // Book screen
    private static final int PAGE_HISTORY          = 3; // History screen (hosts Human/Vet history tabs)

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Button bookButton;

    // Reference to Activity's main pager (legacy ViewPager)
    private ViewPager mainPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ongoing_appointment, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        tabLayout  = root.findViewById(R.id.appointmentTabs);
        viewPager  = root.findViewById(R.id.viewPagerOngoing);
        bookButton = root.findViewById(R.id.bookButton);

        // Hook main Activity pager (R.id.vp)
        if (getActivity() instanceof MainActivity) {
            mainPager = ((MainActivity) getActivity()).findViewById(R.id.vp);
        }

        // Child ViewPager2: two tabs (Human / Vet)
        viewPager.setOffscreenPageLimit(1);
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return position == 0 ? new HumanOngoingFragment()
                        : new VetOngoingFragment();
            }
            @Override
            public int getItemCount() { return 2; }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Patient Ongoing" : "Vet Ongoing");
        }).attach();

// Read which tab should open (0 = Human, 1 = Vet) from Activity Intent
        int initialTab = requireActivity().getIntent().getIntExtra("ongoing_tab", 0);
        if (initialTab < 0 || initialTab > 1) initialTab = 0;
        viewPager.setCurrentItem(initialTab, false);


        // Bottom CTA → Book page on main pager
        bookButton.setOnClickListener(v -> {
            if (mainPager != null) mainPager.setCurrentItem(PAGE_BOOK_APPOINTMENT, true);
        });

        // ---- Edge-swipe bridge at ends only (keeps intra-tab swiping intact) ----
        viewPager.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;
            final float THRESHOLD = 64f; // pixels

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = e.getX();
                        downY = e.getY();
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getX() - downX;
                        float dy = e.getY() - downY;

                        // consider horizontal intent
                        if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > THRESHOLD) {
                            int cur = viewPager.getCurrentItem();

                            // Tab 0 (Human): swipe RIGHT → Book Appointment (main pager)
                            if (cur == 0 && dx > 0) {
                                if (mainPager != null) mainPager.setCurrentItem(PAGE_BOOK_APPOINTMENT, true);
                                return true; // consume to avoid wobble
                            }
                            // Tab 1 (Vet): swipe LEFT → History (main pager)
                            if (cur == 1 && dx < 0) {
                                if (mainPager != null) mainPager.setCurrentItem(PAGE_HISTORY, true);
                                return true;
                            }
                        }
                        return false;
                    default:
                        return false;
                }
            }
        });
    }
}
