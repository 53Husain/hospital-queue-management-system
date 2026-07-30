package com.shqms.dto.response;

public class LoginResponse {

    private Long userId;
    private Long patientId;
    private Long doctorId;

    private String fullName;
    private String role;
    private String message;

    public LoginResponse() {
    }

    public LoginResponse(Long userId,
                         Long patientId,
                         Long doctorId,
                         String fullName,
                         String role,
                         String message) {

        this.userId = userId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.fullName = fullName;
        this.role = role;
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}