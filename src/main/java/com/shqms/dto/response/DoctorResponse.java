package com.shqms.dto.response;

public class DoctorResponse {

    private Long id;
    private String name;
    private String specialization;
    private int experience;
    private double rating;

    public DoctorResponse(Long id,
                          String name,
                          String specialization,
                          int experience,
                          double rating) {

        this.id = id;
        this.name = name;
        this.specialization = specialization;
        this.experience = experience;
        this.rating = rating;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSpecialization() {
        return specialization;
    }

    public int getExperience() {
        return experience;
    }

    public double getRating() {
        return rating;
    }
}