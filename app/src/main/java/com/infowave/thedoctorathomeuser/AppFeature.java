// AppFeature.java in your model package
package com.infowave.thedoctorathomeuser;

public class AppFeature {
    private int iconRes;
    private String title;
    private String description;

    public AppFeature(int iconRes, String title, String description) {
        this.iconRes = iconRes;
        this.title = title;
        this.description = description;
    }

    // Getters
    public int getIconRes() { return iconRes; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
}