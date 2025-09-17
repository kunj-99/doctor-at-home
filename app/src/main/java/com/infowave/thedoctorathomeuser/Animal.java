package com.infowave.thedoctorathomeuser;

public class Animal {
    private final String name;
    private final int drawableRes;

    public Animal(String name, int drawableRes) {
        this.name = name;
        this.drawableRes = drawableRes;
    }

    public String getName() { return name; }
    public int getDrawableRes() { return drawableRes; }
}
