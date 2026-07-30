package com.shqms.dto.response;

public class AppointmentResponse {

    private Long appointmentId;
    private Integer tokenNumber;
    private Integer estimatedWaitingTime;
    private String status;

    public AppointmentResponse() {
    }

    public AppointmentResponse(Long appointmentId,
                               Integer tokenNumber,
                               Integer estimatedWaitingTime,
                               String status) {
        this.appointmentId = appointmentId;
        this.tokenNumber = tokenNumber;
        this.estimatedWaitingTime = estimatedWaitingTime;
        this.status = status;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public Integer getTokenNumber() {
        return tokenNumber;
    }

    public Integer getEstimatedWaitingTime() {
        return estimatedWaitingTime;
    }

    public String getStatus() {
        return status;
    }
}