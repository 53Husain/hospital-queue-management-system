package com.shqms.integration;

import com.shqms.dto.request.AppointmentRequest;
import com.shqms.dto.response.AppointmentResponse;
import com.shqms.entity.*;
import com.shqms.enums.*;
import com.shqms.repository.*;
import com.shqms.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
@Testcontainers
class QueueConcurrencyIT {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withCommand("--innodb-lock-wait-timeout=10");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
    @Autowired QueueService queue;
    @Autowired AppointmentService booking;
    @Autowired AppointmentRepository appointments;
    @Autowired DoctorRepository doctors;
    @Autowired PatientRepository patients;
    @Autowired UserRepository users;
    @Autowired PlatformTransactionManager transactions;
    @Autowired MockMvc mvc;
    Doctor doctor;
    Patient patient;
    Appointment normal, emergency;

    @BeforeEach void fixture() {
        appointments.deleteAll(); patients.deleteAll(); doctors.deleteAll(); users.deleteAll();
        patient = new Patient(); patient.setUser(createUser("patient", UserRole.PATIENT)); patient = patients.save(patient);
        doctor = doctor("doctor");
        normal = appointment(doctor, 1, PriorityType.NORMAL);
        emergency = appointment(doctor, 2, PriorityType.EMERGENCY);
    }
    User createUser(String name, UserRole role) {
        return users.save(new User(null, name, name + "@example.com", "unused-test-hash", "0000000000", role));
    }
    Doctor doctor(String name) {
        Doctor d = new Doctor(); d.setUser(createUser(name, UserRole.DOCTOR)); return doctors.save(d);
    }
    Appointment appointment(Doctor d, int token, PriorityType priority) {
        Appointment a = new Appointment(); a.setDoctor(d); a.setPatient(patient);
        a.setTokenNumber(token); a.setPriority(priority); a.setStatus(AppointmentStatus.BOOKED);
        a.setAppointmentTime(LocalDateTime.now()); a.setEstimatedWaitingTime(0);
        return appointments.save(a);
    }
    AppointmentRequest request() {
        AppointmentRequest r = new AppointmentRequest(); r.setPatientId(patient.getId());
        r.setDoctorId(doctor.getId()); r.setPriority(PriorityType.NORMAL); return r;
    }
    void assertActive(int count) {
        assertEquals(count, appointments.findByDoctorIdAndStatus(doctor.getId(), AppointmentStatus.IN_PROGRESS).size());
    }
    void assertStatus(Appointment a, AppointmentStatus status) {
        assertEquals(status, appointments.findById(a.getId()).orElseThrow().getStatus());
    }
    boolean next() {
        return attempt(() -> queue.callNextPatient(doctor.getId()));
    }
    boolean attempt(Runnable action) {
        try { action.run(); return true; }
        catch (ResponseStatusException ex) { assertEquals(409, ex.getStatusCode().value()); return false; }
    }

    @Test void simultaneousNextRequestsStartExactlyOneConsultation() throws Exception {
        List<Boolean> outcomes = concurrently(this::next, this::next);
        assertEquals(1, outcomes.stream().filter(Boolean::booleanValue).count());
        assertActive(1); assertStatus(emergency, AppointmentStatus.IN_PROGRESS);
        assertStatus(normal, AppointmentStatus.BOOKED);
    }

    @Test void oldTransactionSnapshotCannotStartAnotherConsultation() throws Exception {
        CountDownLatch loaded = new CountDownLatch(2), firstGo = new CountDownLatch(1), secondGo = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = pool.submit(() -> transaction().execute(status -> {
                assertEquals(2, appointments.findByDoctorIdAndStatus(doctor.getId(), AppointmentStatus.BOOKED).size());
                loaded.countDown(); await(firstGo); return next();
            }));
            Future<Boolean> second = pool.submit(() -> {
                try {
                    transaction().executeWithoutResult(status -> {
                        assertEquals(2, appointments.findByDoctorIdAndStatus(doctor.getId(), AppointmentStatus.BOOKED).size());
                        loaded.countDown(); await(secondGo); queue.callNextPatient(doctor.getId());
                    });
                    return true;
                } catch (ResponseStatusException ex) { assertEquals(409, ex.getStatusCode().value()); return false; }
            });
            assertTrue(loaded.await(10, TimeUnit.SECONDS)); firstGo.countDown();
            assertTrue(first.get(30, TimeUnit.SECONDS)); secondGo.countDown();
            assertFalse(second.get(30, TimeUnit.SECONDS)); assertActive(1);
        } finally { firstGo.countDown(); secondGo.countDown(); stop(pool); }
    }

    @Test void completionAndNextAreSerializedUntilCommit() throws Exception {
        queue.callNextPatient(doctor.getId());
        CountDownLatch completedInTransaction = new CountDownLatch(1), commit = new CountDownLatch(1), nextStarted = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> complete = pool.submit(() -> transaction().executeWithoutResult(status -> {
                queue.completeAppointment(emergency.getId()); completedInTransaction.countDown(); await(commit);
            }));
            assertTrue(completedInTransaction.await(10, TimeUnit.SECONDS));
            Future<?> next = pool.submit(() -> { nextStarted.countDown(); return queue.callNextPatient(doctor.getId()); });
            assertTrue(nextStarted.await(10, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> next.get(300, TimeUnit.MILLISECONDS));
            commit.countDown(); complete.get(30, TimeUnit.SECONDS); next.get(30, TimeUnit.SECONDS);
            assertStatus(emergency, AppointmentStatus.COMPLETED);
            assertStatus(normal, AppointmentStatus.IN_PROGRESS); assertActive(1);
        } finally { commit.countDown(); stop(pool); }
    }

    @Test void concurrentCompletionOnlySucceedsOnce() throws Exception {
        queue.callNextPatient(doctor.getId());
        List<Boolean> outcomes = concurrently(
                () -> attempt(() -> queue.completeAppointment(emergency.getId())),
                () -> attempt(() -> queue.completeAppointment(emergency.getId())));
        assertEquals(1, outcomes.stream().filter(Boolean::booleanValue).count());
        assertStatus(emergency, AppointmentStatus.COMPLETED); assertActive(0);
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"BOOKED", "IN_QUEUE", "CANCELLED", "COMPLETED"})
    void onlyInProgressAppointmentsCanComplete(AppointmentStatus prior) {
        emergency.setStatus(prior); appointments.save(emergency);
        assertFalse(attempt(() -> queue.completeAppointment(emergency.getId())));
        assertStatus(emergency, prior);
    }

    @Test void emergencyThenFifoAndEmptyQueueConflict() {
        Appointment laterEmergency = appointment(doctor, 3, PriorityType.EMERGENCY);
        assertEquals(2, queue.callNextPatient(doctor.getId()).getTokenNumber());
        queue.completeAppointment(emergency.getId());
        assertEquals(3, queue.callNextPatient(doctor.getId()).getTokenNumber());
        queue.completeAppointment(laterEmergency.getId());
        assertEquals(1, queue.callNextPatient(doctor.getId()).getTokenNumber());
        queue.completeAppointment(normal.getId());
        assertFalse(next()); assertActive(0);
    }

    @Test void failedTransactionRollsBackTransitionAndAllowsRetry() {
        transaction().executeWithoutResult(status -> {
            queue.callNextPatient(doctor.getId()); status.setRollbackOnly();
        });
        assertActive(0); assertStatus(emergency, AppointmentStatus.BOOKED);
        assertEquals(2, queue.callNextPatient(doctor.getId()).getTokenNumber());
    }

    @Test void concurrentBookingsGetDistinctTokens() throws Exception {
        List<Integer> tokens = concurrently(() -> booking.bookAppointment(request()).getTokenNumber(),
                () -> booking.bookAppointment(request()).getTokenNumber());
        assertEquals(List.of(3, 4), tokens.stream().sorted().toList());
        assertEquals(4, appointments.countByDoctorId(doctor.getId()));
    }

    @Test void bookingWithOldSnapshotUsesLatestToken() throws Exception {
        CountDownLatch cached = new CountDownLatch(1), otherBooked = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<AppointmentResponse> later = pool.submit(() -> transaction().execute(status -> {
                assertEquals(2, appointments.countByDoctorId(doctor.getId()));
                cached.countDown(); await(otherBooked); return booking.bookAppointment(request());
            }));
            assertTrue(cached.await(10, TimeUnit.SECONDS));
            assertEquals(3, booking.bookAppointment(request()).getTokenNumber());
            otherBooked.countDown(); assertEquals(4, later.get(30, TimeUnit.SECONDS).getTokenNumber());
        } finally { otherBooked.countDown(); stop(pool); }
    }

    @Test void otherDoctorCanAdvanceWhileFirstDoctorsTransactionIsOpen() throws Exception {
        Doctor other = doctor("other-doctor"); appointment(other, 1, PriorityType.NORMAL);
        CountDownLatch holding = new CountDownLatch(1), finish = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = pool.submit(() -> transaction().executeWithoutResult(status -> {
                queue.callNextPatient(doctor.getId()); holding.countDown(); await(finish);
            }));
            assertTrue(holding.await(10, TimeUnit.SECONDS));
            Future<?> second = pool.submit(() -> queue.callNextPatient(other.getId()));
            second.get(5, TimeUnit.SECONDS); finish.countDown(); first.get(30, TimeUnit.SECONDS);
        } finally { finish.countDown(); stop(pool); }
    }

    @Test void bookingAndTransitionHttpContracts() throws Exception {
        String body = "{\"patientId\":" + patient.getId() + ",\"doctorId\":" + doctor.getId() + ",\"priority\":\"NORMAL\"}";
        mvc.perform(post("/api/appointments").with(user(patient.getUser().getId().toString()).roles("PATIENT"))
                .with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenNumber").value(3))
                .andExpect(jsonPath("$.appointmentId").isNumber()).andExpect(jsonPath("$.status").value("BOOKED"));
        mvc.perform(post("/api/appointments").with(user(patient.getUser().getId().toString()).roles("PATIENT"))
                .with(csrf()).contentType("application/json").content(body.replace(",\"priority\":\"NORMAL\"", "")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/queue/complete/" + normal.getId()).with(user(doctor.getUser().getId().toString()).roles("DOCTOR"))
                .with(csrf())).andExpect(status().isConflict()).andExpect(jsonPath("$.message").exists());
        mvc.perform(post("/api/queue/next/" + doctor.getId()).with(user(doctor.getUser().getId().toString()).roles("DOCTOR"))
                .with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/api/queue/next/" + doctor.getId()).with(user(doctor.getUser().getId().toString()).roles("DOCTOR"))
                .with(csrf())).andExpect(status().isConflict());
    }

    TransactionTemplate transaction() {
        TransactionTemplate template = new TransactionTemplate(transactions); template.setTimeout(25); return template;
    }
    <T> List<T> concurrently(Supplier<T> left, Supplier<T> right) throws Exception {
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<T> a = pool.submit(() -> { ready.countDown(); await(start); return left.get(); });
            Future<T> b = pool.submit(() -> { ready.countDown(); await(start); return right.get(); });
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            return List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        } finally { start.countDown(); stop(pool); }
    }
    static void await(CountDownLatch latch) {
        try { assertTrue(latch.await(15, TimeUnit.SECONDS), "Concurrent transaction timed out"); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new AssertionError(ex); }
    }
    static void stop(ExecutorService pool) throws InterruptedException {
        pool.shutdownNow(); assertTrue(pool.awaitTermination(15, TimeUnit.SECONDS), "Worker did not finish");
    }
}
