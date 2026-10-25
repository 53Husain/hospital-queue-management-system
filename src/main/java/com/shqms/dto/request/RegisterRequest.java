package com.shqms.dto.request;

import jakarta.validation.constraints.*;

public class RegisterRequest {

    @NotBlank @Size(max = 100)
    private String fullName;
    @NotBlank @Email @Size(max = 254)
    private String email;
    @NotBlank @Size(min = 12, max = 72)
    private String password;
    @Size(max = 30)
    private String phoneNumber;
    private String role;

    public RegisterRequest() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
