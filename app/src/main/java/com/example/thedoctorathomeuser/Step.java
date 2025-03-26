package com.example.thedoctorathomeuser;

public class Step {
    private int stepNumber;
    private String description;

    public Step(int stepNumber, String description) {
        this.stepNumber = stepNumber;
        this.description = description;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public String getDescription() {
        return description;
    }
}