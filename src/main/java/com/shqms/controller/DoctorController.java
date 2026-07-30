package com.shqms.controller;

import com.shqms.dto.response.DoctorResponse;
import com.shqms.service.DoctorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    public List<DoctorResponse> getDoctors() {
        return doctorService.getAllDoctors();
    }
}