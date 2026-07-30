package com.shqms.service;

import com.shqms.dto.request.AppointmentRequest;
import com.shqms.dto.response.AppointmentDetailsResponse;
import com.shqms.dto.response.AppointmentResponse;

import java.util.List;

public interface AppointmentService {

    AppointmentResponse bookAppointment(
            AppointmentRequest request);

    AppointmentResponse getAppointment(Long id);

    List<AppointmentDetailsResponse> getAppointments(Long patientId);

}