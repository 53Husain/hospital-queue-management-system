package com.shqms.dto.response;
public class QueueEntryResponse {

    private Long appointmentId;
    private Integer tokenNumber;
    private String patientName;
    private String priority;
    private String status;

    public QueueEntryResponse() {
    }

    public QueueEntryResponse(
            Long appointmentId,
            Integer tokenNumber,
            String patientName,
            String priority,
            String status) {

        this.appointmentId = appointmentId;
        this.tokenNumber = tokenNumber;
        this.patientName = patientName;
        this.priority = priority;
        this.status = status;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public Integer getTokenNumber() {
        return tokenNumber;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getPriority() {
        return priority;
    }

    public String getStatus() {
        return status;
    }
}