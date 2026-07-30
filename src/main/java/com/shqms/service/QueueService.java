package com.shqms.service;

import com.shqms.dto.response.QueueEntryResponse;
import com.shqms.dto.response.QueueResponse;

import java.util.List;

public interface QueueService {

    QueueResponse callNextPatient(Long doctorId);

    List<QueueEntryResponse> getQueue(Long doctorId);

    int getQueuePosition(Long appointmentId);

    void completeAppointment(Long appointmentId);
}