package com.shqms.config;

import com.shqms.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Checks database ownership; client-supplied IDs and localStorage are never trusted. */
@Component("recordAccess")
public class RecordAccess {
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;

    public RecordAccess(PatientRepository patients, DoctorRepository doctors, AppointmentRepository appointments) {
        this.patients = patients;
        this.doctors = doctors;
        this.appointments = appointments;
    }
    private boolean role(Authentication auth, String role) {
        return auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
    private boolean owner(Authentication auth, Long userId) {
        return userId != null && userId.toString().equals(auth.getName());
    }
    public boolean patient(Authentication auth, Long id) {
        return id != null && role(auth, "PATIENT") && patients.findById(id)
            .map(p -> owner(auth, p.getUser().getId())).orElse(false);
    }
    public boolean doctor(Authentication auth, Long id) {
        return id != null && role(auth, "DOCTOR") && doctors.findById(id)
            .map(d -> owner(auth, d.getUser().getId())).orElse(false);
    }
    public boolean appointment(Authentication auth, Long id) {
        return id != null && appointments.findById(id).map(a ->
            (role(auth, "PATIENT") && owner(auth, a.getPatient().getUser().getId())) ||
            (role(auth, "DOCTOR") && owner(auth, a.getDoctor().getUser().getId()))).orElse(false);
    }
    public boolean manageAppointment(Authentication auth, Long id) {
        return id != null && role(auth, "DOCTOR") && appointments.findById(id)
            .map(a -> owner(auth, a.getDoctor().getUser().getId())).orElse(false);
    }
}
