package com.shqms.dto.request;

import com.shqms.enums.PriorityType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AppointmentRequest {

    @NotNull @Positive private Long patientId;
    @NotNull @Positive private Long doctorId;
    @NotNull private PriorityType priority;

    public AppointmentRequest() {
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public PriorityType getPriority() {
        return priority;
    }

    public void setPriority(PriorityType priority) {
        this.priority = priority;
    }
}
