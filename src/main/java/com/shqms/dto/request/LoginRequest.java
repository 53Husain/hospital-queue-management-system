package com.shqms.dto.request;

import jakarta.validation.constraints.*;

public class LoginRequest {

    @NotBlank @Email @Size(max = 254)
    private String email;
    @NotBlank @Size(min = 1, max = 72)
    private String password;

    public LoginRequest() {
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
}
