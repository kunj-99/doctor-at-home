package com.infowave.thedoctorathomeuser;

public class Animal {
    private int id;
    private String name;
    private String imageUrl;
    private int drawableRes;
    private double price; // NEW

    public Animal(int id, String name, String imageUrl) {
        this.id = id; this.name = name; this.imageUrl = imageUrl;
    }

    // getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public int getDrawableRes() { return drawableRes; }
    public double getPrice() { return price; } // NEW

    // setters
    public void setDrawableRes(int drawableRes) { this.drawableRes = drawableRes; }
    public void setPrice(double price) { this.price = price; } // NEW
}
