package com.infowave.thedoctorathomeuser;

public class VetAppointment {
    private final String petTitle;  // "Bruno (Dog)"
    private final String reason;    // "Skin allergy consultation"
    private final String vetName;   // "Dr. K. Desai"
    private final String when;      // "Thu, 09 Oct • 02:15 PM"
    private final String amount;    // "₹650"
    private final String status;    // "Ongoing" | "Scheduled"
    private final String imageUrl;  // optional avatar

    public VetAppointment(String petTitle, String reason, String vetName,
                          String when, String amount, String status, String imageUrl) {
        this.petTitle = petTitle;
        this.reason = reason;
        this.vetName = vetName;
        this.when = when;
        this.amount = amount;
        this.status = status;
        this.imageUrl = imageUrl;
    }

    public String getPetTitle() { return petTitle; }
    public String getReason() { return reason; }
    public String getVetName() { return vetName; }
    public String getWhen() { return when; }
    public String getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getImageUrl() { return imageUrl; }
}
