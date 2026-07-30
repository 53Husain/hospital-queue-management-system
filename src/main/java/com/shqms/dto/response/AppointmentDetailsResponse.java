package com.shqms.dto.response;

public class AppointmentDetailsResponse {

    private Long appointmentId;
    private String doctorName;
    private String priority;
    private Integer tokenNumber;
    private Integer estimatedWaitingTime;
    private String status;

    public AppointmentDetailsResponse() {
    }

    public AppointmentDetailsResponse(
            Long appointmentId,
            String doctorName,
            String priority,
            Integer tokenNumber,
            Integer estimatedWaitingTime,
            String status) {

        this.appointmentId = appointmentId;
        this.doctorName = doctorName;
        this.priority = priority;
        this.tokenNumber = tokenNumber;
        this.estimatedWaitingTime = estimatedWaitingTime;
        this.status = status;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public String getPriority() {
        return priority;
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