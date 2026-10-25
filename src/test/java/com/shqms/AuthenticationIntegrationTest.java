package com.shqms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shqms.entity.*;
import com.shqms.enums.*;
import com.shqms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTest {
    private static final String PASSWORD = "test-password-123";
    private static final String HASH = new BCryptPasswordEncoder(4).encode(PASSWORD);
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PatientRepository patients;
    @Autowired DoctorRepository doctors;
    @Autowired AppointmentRepository appointments;
    @Autowired PasswordEncoder encoder;
    Patient patient, otherPatient;
    Doctor doctor, otherDoctor;
    Appointment appointment;

    @BeforeEach
    void setup() {
        appointments.deleteAll(); patients.deleteAll(); doctors.deleteAll(); users.deleteAll();
        patient = patient("patient@example.com"); otherPatient = patient("other@example.com");
        doctor = doctor("doctor@example.com"); otherDoctor = doctor("other-doctor@example.com");
        user("admin@example.com", UserRole.ADMIN);
        appointment = new Appointment();
        appointment.setPatient(patient); appointment.setDoctor(doctor);
        appointment.setPriority(PriorityType.NORMAL); appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setTokenNumber(1); appointment.setEstimatedWaitingTime(0);
        appointment = appointments.save(appointment);
    }
    User user(String email, UserRole role) {
        return users.save(new User(null, "Demo User", email, HASH, "0000000000", role));
    }
    Patient patient(String email) {
        Patient p = new Patient(); p.setUser(user(email, UserRole.PATIENT)); return patients.save(p);
    }
    Doctor doctor(String email) {
        Doctor d = new Doctor(); d.setUser(user(email, UserRole.DOCTOR)); return doctors.save(d);
    }
    String credentials(String email, String password) throws Exception {
        return json.writeValueAsString(Map.of("email", email, "password", password));
    }
    String registration(String email, String role) throws Exception {
        return json.writeValueAsString(Map.of("fullName", "New User", "email", email,
            "password", PASSWORD, "phoneNumber", "0000000000", "role", role));
    }
    MockHttpSession login(String email) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
            .contentType("application/json").content(credentials(email, PASSWORD)))
            .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    @Test void anonymousCannotReadPrivateApis() throws Exception {
        for (String path : new String[]{"/api/doctors", "/api/admin/dashboard",
                "/api/patient/profile/" + patient.getId(), "/api/queue/" + doctor.getId(),
                "/api/appointments/" + appointment.getId()}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/pages/login.html")).andExpect(status().isOk());
    }
    @Test void publicRegistrationHashesPasswordAndCreatesPatient() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
            .content(registration("new@example.com", "PATIENT"))).andExpect(status().isOk());
        User user = users.findByEmail("new@example.com").orElseThrow();
        assertThat(user.getRole()).isEqualTo(UserRole.PATIENT);
        assertThat(user.getPassword()).isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, user.getPassword())).isTrue();
        assertThat(patients.findByUser(user)).isPresent();
        login("new@example.com");
    }
    @Test void publicRegistrationCannotCreateStaff() throws Exception {
        for (String role : new String[]{"ADMIN", "DOCTOR", "admin", "UNKNOWN"}) {
            mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                .content(registration("attacker@example.com", role))).andExpect(status().isForbidden());
        }
        assertThat(users.findByEmail("attacker@example.com")).isEmpty();
    }
    @Test void invalidRegistrationAndDuplicateEmailAreRejected() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
            .content("{}" )).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
            .content(registration("patient@example.com", "PATIENT"))).andExpect(status().isConflict());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
            .content(registration("new@example.com", "PATIENT").replace(PASSWORD, "é".repeat(40))))
            .andExpect(status().isBadRequest());
        assertThat(users.findByEmail("new@example.com")).isEmpty();
    }
    @Test void badCredentialsAndLegacyPlaintextAreRejected() throws Exception {
        for (String email : new String[]{"patient@example.com", "missing@example.com"}) {
            mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
                .content(credentials(email, "wrong"))).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }
        User legacy = patient.getUser(); legacy.setPassword(PASSWORD); users.save(legacy);
        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
            .content(credentials(legacy.getEmail(), PASSWORD))).andExpect(status().isUnauthorized());
    }
    @Test void realCsrfSessionRotationAndLogoutLifecycle() throws Exception {
        MvcResult tokenResult = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        MockHttpSession before = (MockHttpSession) tokenResult.getRequest().getSession(false);
        JsonNode token = json.readTree(tokenResult.getResponse().getContentAsString());
        mvc.perform(post("/api/auth/login").session(before).contentType("application/json")
            .content(credentials("patient@example.com", PASSWORD))).andExpect(status().isForbidden());
        MvcResult result = mvc.perform(post("/api/auth/login").session(before)
            .header(token.get("headerName").asText(), token.get("token").asText())
            .contentType("application/json").content(credentials("patient@example.com", PASSWORD)))
            .andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(before.isInvalid()).isTrue();
        assertThat(session.getId()).isNotEqualTo(before.getId());
        mvc.perform(get("/api/doctors").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").session(session)
            .header(token.get("headerName").asText(), token.get("token").asText()))
            .andExpect(status().isForbidden());
        JsonNode fresh = json.readTree(mvc.perform(get("/api/auth/csrf").session(session))
            .andReturn().getResponse().getContentAsString());
        mvc.perform(post("/api/auth/logout").session(session)
            .header(fresh.get("headerName").asText(), fresh.get("token").asText()))
            .andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/doctors")).andExpect(status().isUnauthorized());
    }
    @Test void patientCanOnlyReadAndUpdateOwnProfile() throws Exception {
        MockHttpSession session = login("patient@example.com");
        mvc.perform(get("/api/patient/profile/" + patient.getId()).session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/patient/profile/" + otherPatient.getId()).session(session)).andExpect(status().isForbidden());
        String profile = "{\"phoneNumber\":\"123\",\"age\":20,\"gender\":\"Other\",\"bloodGroup\":\"O+\"}";
        mvc.perform(put("/api/patient/profile/" + otherPatient.getId()).session(session).with(csrf())
            .contentType("application/json").content(profile)).andExpect(status().isForbidden());
        mvc.perform(put("/api/patient/profile/" + patient.getId()).session(session).with(csrf())
            .contentType("application/json").content(profile)).andExpect(status().isOk());
        assertThat(patients.findById(otherPatient.getId()).orElseThrow().getAge()).isZero();
    }
    @Test void patientCanOnlyBookAndReadOwnAppointments() throws Exception {
        MockHttpSession session = login("patient@example.com");
        mvc.perform(get("/api/appointments/patient/" + patient.getId()).session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/appointments/patient/" + otherPatient.getId()).session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/appointments/" + appointment.getId()).session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/queue/position/" + appointment.getId()).session(session)).andExpect(status().isOk());
        for (Patient target : new Patient[]{otherPatient, patient}) {
            mvc.perform(post("/api/appointments").session(session).with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(Map.of("patientId", target.getId(), "doctorId", doctor.getId(), "priority", "NORMAL"))))
                .andExpect(target == patient ? status().isOk() : status().isForbidden());
        }
        assertThat(appointments.findByPatientId(otherPatient.getId())).isEmpty();
        MockHttpSession other = login("other@example.com");
        mvc.perform(get("/api/appointments/" + appointment.getId()).session(other)).andExpect(status().isForbidden());
        mvc.perform(get("/api/queue/position/" + appointment.getId()).session(other)).andExpect(status().isForbidden());
    }
    @Test void patientCannotReadQueueOrOperateDoctorEndpoints() throws Exception {
        MockHttpSession session = login("patient@example.com");
        mvc.perform(get("/api/queue/" + doctor.getId()).session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/queue/next/" + doctor.getId()).session(session).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(put("/api/queue/complete/" + appointment.getId()).session(session).with(csrf())).andExpect(status().isForbidden());
        assertThat(appointments.findById(appointment.getId()).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.BOOKED);
    }
    @Test void doctorCanOnlyManageAssignedQueueAndAppointments() throws Exception {
        MockHttpSession session = login("doctor@example.com");
        mvc.perform(get("/api/queue/" + doctor.getId()).session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/queue/" + otherDoctor.getId()).session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/queue/next/" + otherDoctor.getId()).session(session).with(csrf())).andExpect(status().isForbidden());
        MockHttpSession other = login("other-doctor@example.com");
        mvc.perform(get("/api/appointments/" + appointment.getId()).session(other)).andExpect(status().isForbidden());
        mvc.perform(put("/api/queue/complete/" + appointment.getId()).session(other).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/queue/next/" + doctor.getId()).session(session).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/appointments/" + appointment.getId()).session(session)).andExpect(status().isOk());
        mvc.perform(put("/api/queue/complete/" + appointment.getId()).session(session).with(csrf())).andExpect(status().isOk());
        assertThat(appointments.findById(appointment.getId()).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }
    @Test void onlyAdminCanReadDashboardAndProvisionStaff() throws Exception {
        for (String email : new String[]{"patient@example.com", "doctor@example.com"}) {
            MockHttpSession session = login(email);
            mvc.perform(get("/api/admin/dashboard").session(session)).andExpect(status().isForbidden());
            mvc.perform(post("/api/admin/users").session(session).with(csrf()).contentType("application/json")
                .content(registration("staff@example.com", "DOCTOR"))).andExpect(status().isForbidden());
        }
        MockHttpSession admin = login("admin@example.com");
        mvc.perform(get("/api/admin/dashboard").session(admin)).andExpect(status().isOk());
        mvc.perform(post("/api/admin/users").session(admin).with(csrf()).contentType("application/json")
            .content(registration("staff@example.com", "DOCTOR"))).andExpect(status().isOk());
        User staff = users.findByEmail("staff@example.com").orElseThrow();
        assertThat(doctors.findByUser(staff)).isPresent();
        assertThat(encoder.matches(PASSWORD, staff.getPassword())).isTrue();
        login("staff@example.com");
        mvc.perform(get("/api/patient/profile/" + patient.getId()).session(admin)).andExpect(status().isForbidden());
    }
    @Test void csrfRequiredForRegistrationAndAuthenticatedWrites() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json")
            .content(registration("new@example.com", "PATIENT"))).andExpect(status().isForbidden());
        MockHttpSession session = login("doctor@example.com");
        mvc.perform(post("/api/queue/next/" + doctor.getId()).session(session)).andExpect(status().isForbidden());
        mvc.perform(put("/api/queue/complete/" + appointment.getId()).session(session)).andExpect(status().isForbidden());
        assertThat(appointments.findById(appointment.getId()).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.BOOKED);
    }
}
