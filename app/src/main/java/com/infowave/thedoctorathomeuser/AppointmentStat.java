package com.infowave.thedoctorathomeuser;

public class AppointmentStat {
    private String title;
    private int count;
    private int iconResId;

    public AppointmentStat(String title, int count, int iconResId) {
        this.title = title;
        this.count = count;
        this.iconResId = iconResId;
    }

    public String getTitle() {
        return title;
    }

    public int getCount() {
        return count;
    }

    public int getIconResId() {
        return iconResId;
    }
}
