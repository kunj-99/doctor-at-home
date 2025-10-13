package com.infowave.thedoctorathomeuser;

public class DegreeOption {
    private String primaryTitle;
    private String subtitle;
    private String description;
    private int iconRes;

    public DegreeOption(String primaryTitle, String subtitle, String description, int iconRes) {
        this.primaryTitle = primaryTitle;
        this.subtitle = subtitle;
        this.description = description;
        this.iconRes = iconRes;
    }

    public String getPrimaryTitle() {
        return primaryTitle;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getDescription() {
        return description;
    }

    public int getIconRes() {
        return iconRes;
    }
}