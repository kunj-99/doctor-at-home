package com.infowave.thedoctorathomeuser.view;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.infowave.thedoctorathomeuser.fragment.BookAppointmentFragment;
import com.infowave.thedoctorathomeuser.fragment.HistoryFragment;
import com.infowave.thedoctorathomeuser.fragment.HomeFragment;
import com.infowave.thedoctorathomeuser.fragment.OngoingAppointmentFragment;

public class view extends FragmentPagerAdapter {

public view(@NonNull FragmentManager fm) {
    super(fm);
}

@NonNull
@Override
public Fragment getItem(int position) {
    switch (position) {
        case 0:
            return new HomeFragment();
        case 1:
            return new BookAppointmentFragment();
        case 2:
            return new OngoingAppointmentFragment();
        default:
            return new HistoryFragment();


    }
}

@Override
public int getCount() {
    return 4;
}

@Nullable
@Override
public CharSequence getPageTitle(int position) {
    switch (position) {
        case 0:
            return "Home";
        case 1:
            return "Book Appointment";
        case 2:
            return "Ongoing Appointment";
        default:
            return  "History";



    }
}
}
