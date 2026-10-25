package com.shqms.controller;

import com.shqms.dto.response.DashboardResponse;
import com.shqms.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final com.shqms.service.AuthService authService;
    private final AdminService adminService;

    public AdminController(AdminService adminService, com.shqms.service.AuthService authService) {
        this.authService = authService;
        this.adminService = adminService;
    }

    @PostMapping("/users")
    public ResponseEntity<String> createStaff(@jakarta.validation.Valid @RequestBody com.shqms.dto.request.RegisterRequest request) {
        return ResponseEntity.ok(authService.registerStaff(request));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard() {

        return ResponseEntity.ok(
                adminService.getDashboard()
        );
    }
}
