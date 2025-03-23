package com.example.thedoctorathomeuser;

public class TopDoctor {
    private String name;
    private String specialty;
    private int imageResId;

    public TopDoctor(String name, String specialty, int imageResId) {
        this.name = name;
        this.specialty = specialty;
        this.imageResId = imageResId;
    }

    public String getName() {
        return name;
    }

    public String getSpecialty() {
        return specialty;
    }

    public int getImageResId() {
        return imageResId;
    }
}
