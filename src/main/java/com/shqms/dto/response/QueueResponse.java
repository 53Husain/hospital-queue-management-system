package com.shqms.dto.response;

public class QueueResponse {

    private Integer tokenNumber;
    private String patientName;
    private String priority;
    private String status;

    public QueueResponse(Integer tokenNumber,
                         String patientName,
                         String priority,
                         String status) {
        this.tokenNumber = tokenNumber;
        this.patientName = patientName;
        this.priority = priority;
        this.status = status;
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