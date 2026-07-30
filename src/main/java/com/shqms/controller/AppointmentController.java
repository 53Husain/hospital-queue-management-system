package com.shqms.controller;

import com.shqms.dto.request.AppointmentRequest;
import com.shqms.dto.response.AppointmentDetailsResponse;
import com.shqms.dto.response.AppointmentResponse;
import com.shqms.service.AppointmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(
            AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse>
    bookAppointment(
            @RequestBody
            AppointmentRequest request) {

        return ResponseEntity.ok(
                appointmentService
                        .bookAppointment(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getAppointment(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                appointmentService.getAppointment(id)
        );
    }


    @GetMapping
    public String test() {
        return "Appointment API is working";
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<AppointmentDetailsResponse>> getAppointments(
            @PathVariable Long patientId) {

        return ResponseEntity.ok(
                appointmentService.getAppointments(patientId)
        );
    }


}