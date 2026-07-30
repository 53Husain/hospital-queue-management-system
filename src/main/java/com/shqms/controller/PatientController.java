package com.shqms.controller;

import com.shqms.dto.request.UpdateProfileRequest;
import com.shqms.dto.response.ProfileResponse;
import com.shqms.service.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patient")
public class PatientController {

    private final PatientService patientService;

    public PatientController(
            PatientService patientService) {

        this.patientService = patientService;
    }

    @GetMapping("/profile/{patientId}")
    public ResponseEntity<ProfileResponse>
    getProfile(
            @PathVariable Long patientId) {

        return ResponseEntity.ok(
                patientService.getProfile(patientId)
        );
    }

    @PutMapping("/profile/{patientId}")
    public ResponseEntity<String>
    updateProfile(
            @PathVariable Long patientId,
            @RequestBody UpdateProfileRequest request) {

        return ResponseEntity.ok(
                patientService.updateProfile(
                        patientId,
                        request
                )
        );
    }
}