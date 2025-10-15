package com.infowave.thedoctorathomeuser;

public class VetDoctor {
    private int id;
    private String name;
    private String specialization;
    private String imageUrl;
    private double rating;
    private int experience;
    private String location;
    private String zipCode;
    private String education;
    private int consultationFee;
    private boolean isAvailable;

    public VetDoctor(int id, String name, String specialization, String imageUrl,
                     double rating, int experience, String location, String zipCode,
                     String education, int consultationFee, boolean isAvailable) {
        this.id = id;
        this.name = name;
        this.specialization = specialization;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.experience = experience;
        this.location = location;
        this.zipCode = zipCode;
        this.education = education;
        this.consultationFee = consultationFee;
        this.isAvailable = isAvailable;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getSpecialization() { return specialization; }
    public String getImageUrl() { return imageUrl; }
    public double getRating() { return rating; }
    public int getExperience() { return experience; }
    public String getLocation() { return location; }
    public String getZipCode() { return zipCode; }
    public String getEducation() { return education; }
    public int getConsultationFee() { return consultationFee; }
    public boolean isAvailable() { return isAvailable; }
}