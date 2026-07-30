package com.shqms.dto.response;

public class DashboardResponse {

    private long totalPatients;
    private long totalDoctors;
    private long totalAppointments;
    private long completedAppointments;
    private long bookedAppointments;
    private long emergencyAppointments;

    public DashboardResponse() {
    }

    public DashboardResponse(
            long totalPatients,
            long totalDoctors,
            long totalAppointments,
            long completedAppointments,
            long bookedAppointments,
            long emergencyAppointments) {

        this.totalPatients = totalPatients;
        this.totalDoctors = totalDoctors;
        this.totalAppointments = totalAppointments;
        this.completedAppointments = completedAppointments;
        this.bookedAppointments = bookedAppointments;
        this.emergencyAppointments = emergencyAppointments;
    }

    public long getTotalPatients() {
        return totalPatients;
    }

    public long getTotalDoctors() {
        return totalDoctors;
    }

    public long getTotalAppointments() {
        return totalAppointments;
    }

    public long getCompletedAppointments() {
        return completedAppointments;
    }

    public long getBookedAppointments() {
        return bookedAppointments;
    }

    public long getEmergencyAppointments() {
        return emergencyAppointments;
    }
}