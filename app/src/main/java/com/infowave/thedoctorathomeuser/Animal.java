package com.infowave.thedoctorathomeuser;

public class Animal {
    private final String name;
    private final int drawableRes;
    private final String imageUrl;  // ✅ added for backend images
    private int id;                 // ✅ optional (for category_id)

    // For local drawable images (existing constructor)
    public Animal(String name, int drawableRes) {
        this.name = name;
        this.drawableRes = drawableRes;
        this.imageUrl = null;
    }

    // For backend images (new constructor)
    public Animal(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.drawableRes = 0;
    }

    // Optional constructor if you want both
    public Animal(int id, String name, String imageUrl) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.drawableRes = 0;
    }

    public String getName() {
        return name;
    }

    public int getDrawableRes() {
        return drawableRes;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
