package com.infowave.thedoctorathomeuser;

public class VetAppointment {

    private final String petTitle;
    private final String reason;
    private final String vetName;
    private final String when;
    private final String amount;
    private final String status;
    private final String imageUrl;

    // NEW: ids used for tracking
    private final String doctorId;
    private final String appointmentId;

    public VetAppointment(String petTitle,
                          String reason,
                          String vetName,
                          String when,
                          String amount,
                          String status,
                          String imageUrl,
                          String doctorId,
                          String appointmentId) {
        this.petTitle = petTitle;
        this.reason = reason;
        this.vetName = vetName;
        this.when = when;
        this.amount = amount;
        this.status = status;
        this.imageUrl = imageUrl;
        this.doctorId = doctorId;
        this.appointmentId = appointmentId;
    }

    public String getPetTitle() { return petTitle; }
    public String getReason() { return reason; }
    public String getVetName() { return vetName; }
    public String getWhen() { return when; }
    public String getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getImageUrl() { return imageUrl; }

    // NEW getters
    public String getDoctorId() { return doctorId; }
    public String getAppointmentId() { return appointmentId; }
}
