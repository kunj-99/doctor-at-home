package com.infowave.thedoctorathomeuser;

public class HealthTip {
    private String title;
    private String description;

    // NEW: URL from API
    private String imageUrl;

    // OLD: local drawable support (for fallback)
    private int imageResId;

    // New constructor: use URL
    public HealthTip(String title, String description, String imageUrl) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.imageResId = 0;
    }

    // Old constructor (keep for backward compatibility)
    public HealthTip(String title, String description, int imageResId) {
        this.title = title;
        this.description = description;
        this.imageResId = imageResId;
        this.imageUrl = null;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }

    public String getImageUrl() { return imageUrl; }
    public int getImageResId() { return imageResId; }

    public boolean hasImageUrl() {
        return imageUrl != null && !imageUrl.trim().isEmpty();
    }
}
