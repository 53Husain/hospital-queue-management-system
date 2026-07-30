package com.shqms.controller;

import com.shqms.dto.response.QueueEntryResponse;
import com.shqms.dto.response.QueueResponse;
import com.shqms.service.QueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/queue")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @PostMapping("/next/{doctorId}")
    public ResponseEntity<QueueResponse> callNextPatient(
            @PathVariable Long doctorId) {

        return ResponseEntity.ok(
                queueService.callNextPatient(doctorId)
        );
    }

    @GetMapping("/{doctorId}")
    public ResponseEntity<List<QueueEntryResponse>> getQueue(
            @PathVariable Long doctorId) {

        return ResponseEntity.ok(
                queueService.getQueue(doctorId)
        );
    }

    @GetMapping("/position/{appointmentId}")
    public ResponseEntity<Integer> getPosition(
            @PathVariable Long appointmentId) {

        return ResponseEntity.ok(
                queueService.getQueuePosition(appointmentId)
        );
    }

    @PutMapping("/complete/{appointmentId}")
    public ResponseEntity<String> completeAppointment(
            @PathVariable Long appointmentId) {

        queueService.completeAppointment(appointmentId);

        return ResponseEntity.ok(
                "Appointment completed successfully"
        );
    }
}