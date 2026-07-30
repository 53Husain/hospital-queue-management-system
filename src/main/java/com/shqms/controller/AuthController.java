package com.shqms.controller;

import com.shqms.dto.request.RegisterRequest;
import com.shqms.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.shqms.dto.request.LoginRequest;
import com.shqms.dto.response.LoginResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(
            AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String>
    register(
            @RequestBody
            RegisterRequest request) {

        return ResponseEntity.ok(
                authService.register(
                        request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                authService.login(request)
        );
    }
}