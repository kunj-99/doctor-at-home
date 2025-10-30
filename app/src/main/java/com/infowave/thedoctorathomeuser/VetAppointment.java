package com.infowave.thedoctorathomeuser;

public class VetAppointment {

    // Core fields (same as your original)
    private final String petTitle;   // e.g., "Bruno (Dog)"
    private final String reason;     // e.g., "Skin allergy consultation"
    private final String vetName;    // e.g., "Dr. K. Desai"
    private final String when;       // e.g., "Thu, 09 Oct • 02:15 PM"
    private final String amount;     // e.g., "₹650"
    private final String status;     // e.g., "Ongoing" | "Scheduled"
    private final String imageUrl;   // optional avatar URL

    public VetAppointment(String petTitle,
                          String reason,
                          String vetName,
                          String when,
                          String amount,
                          String status,
                          String imageUrl) {
        // Null-safety: empty string fallback to avoid NPEs in adapters
        this.petTitle = safe(petTitle);
        this.reason   = safe(reason);
        this.vetName  = safe(vetName);
        this.when     = safe(when);
        this.amount   = safe(amount);
        this.status   = safe(status);
        this.imageUrl = safe(imageUrl);
    }

    // -------- Original getters (backward compatible) --------
    public String getPetTitle() { return petTitle; }
    public String getReason()   { return reason; }
    public String getVetName()  { return vetName; }
    public String getWhen()     { return when; }
    public String getAmount()   { return amount; }
    public String getStatus()   { return status; }
    public String getImageUrl() { return imageUrl; }

    // -------- Convenience getters (used by new fragment/adapters) --------
    public String getTitle()        { return petTitle; }   // maps to petTitle
    public String getSubtitle()     { return reason; }     // maps to reason
    public String getDoctorName()   { return vetName; }    // maps to vetName
    public String getPrice()        { return amount; }     // maps to amount

    // -------- Utility --------
    private static String safe(String s) { return s == null ? "" : s; }
}
