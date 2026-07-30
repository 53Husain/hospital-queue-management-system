package com.shqms.dto.response;

public class ProfileResponse {

    private String fullName;
    private String email;
    private String phoneNumber;
    private int age;
    private String gender;
    private String bloodGroup;

    public ProfileResponse() {
    }

    public ProfileResponse(String fullName,
                           String email,
                           String phoneNumber,
                           int age,
                           String gender,
                           String bloodGroup) {

        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.age = age;
        this.gender = gender;
        this.bloodGroup = bloodGroup;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public int getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }
}