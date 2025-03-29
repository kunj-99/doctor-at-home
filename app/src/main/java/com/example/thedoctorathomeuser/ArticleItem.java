package com.example.thedoctorathomeuser;

public class ArticleItem {
    private int id;
    private String title;
    private String subtitle;
    private String cover; // Full URL for the cover image
    private String pdf;   // Full URL for the PDF document

    public ArticleItem(int id, String title, String subtitle, String cover, String pdf) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.cover = cover;
        this.pdf = pdf;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getCover() {
        return cover;
    }

    public String getPdf() {
        return pdf;
    }
}
