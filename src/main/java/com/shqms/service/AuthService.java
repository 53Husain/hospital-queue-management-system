package com.shqms.service;

import com.shqms.dto.request.LoginRequest;
import com.shqms.dto.request.RegisterRequest;
import com.shqms.dto.response.LoginResponse;

public interface AuthService {

    String register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}