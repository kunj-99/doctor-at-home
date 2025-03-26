// TeamMember.java
package com.example.thedoctorathomeuser;

public class TeamMember {
    private String name;
    private String role;
    private int photoRes;

    public TeamMember(String name, String role, int photoRes) {
        this.name = name;
        this.role = role;
        this.photoRes = photoRes;
    }

    public String getName() { return name; }
    public String getRole() { return role; }
    public int getPhotoRes() { return photoRes; }
}