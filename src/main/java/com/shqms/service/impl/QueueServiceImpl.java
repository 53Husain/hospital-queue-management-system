package com.shqms.service.impl;

import com.shqms.dto.response.QueueEntryResponse;
import com.shqms.dto.response.QueueResponse;
import com.shqms.dsa.queue.QueueManager;
import com.shqms.entity.Appointment;
import com.shqms.enums.AppointmentStatus;
import com.shqms.repository.AppointmentRepository;
import com.shqms.repository.DoctorRepository;
import com.shqms.service.QueueService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class QueueServiceImpl implements QueueService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final EntityManager entityManager;

    public QueueServiceImpl(
            AppointmentRepository appointmentRepository,
            DoctorRepository doctorRepository,
            EntityManager entityManager) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public QueueResponse callNextPatient(Long doctorId) {

        lockDoctor(doctorId);

        // Check if a patient is already being consulted
        List<Appointment> inProgressAppointments =
                appointmentRepository.findQueueForUpdate(
                        doctorId,
                        AppointmentStatus.IN_PROGRESS
                );

        if (!inProgressAppointments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Please complete the current appointment before calling the next patient."
            );
        }

        // Get all booked appointments
        List<Appointment> appointments =
                appointmentRepository.findQueueForUpdate(
                        doctorId,
                        AppointmentStatus.BOOKED
                );

        QueueManager queue = new QueueManager();

        for (Appointment appointment : appointments) {
            queue.add(appointment);
        }

        if (queue.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No patients in queue");
        }

        Appointment nextPatient = queue.poll();

        // Ownership checks/earlier reads may already have cached this entity.
        entityManager.refresh(nextPatient, LockModeType.PESSIMISTIC_WRITE);

        nextPatient.setStatus(AppointmentStatus.IN_PROGRESS);

        appointmentRepository.save(nextPatient);

        return new QueueResponse(
                nextPatient.getTokenNumber(),
                nextPatient.getPatient().getUser().getFullName(),
                nextPatient.getPriority().name(),
                nextPatient.getStatus().name()
        );
    }

    @Override
    public List<QueueEntryResponse> getQueue(Long doctorId) {

        List<Appointment> appointments =
                appointmentRepository.findByDoctorIdAndStatusIn(
                        doctorId,
                        List.of(
                                AppointmentStatus.BOOKED,
                                AppointmentStatus.IN_PROGRESS
                        )
                );

        QueueManager queue = new QueueManager();

        for (Appointment appointment : appointments) {
            queue.add(appointment);
        }

        List<QueueEntryResponse> response =
                new ArrayList<>();

        while (!queue.isEmpty()) {

            Appointment appointment = queue.poll();

            response.add(
                    new QueueEntryResponse(
                            appointment.getId(),
                            appointment.getTokenNumber(),
                            appointment.getPatient().getUser().getFullName(),
                            appointment.getPriority().name(),
                            appointment.getStatus().name()
                    )
            );
        }

        return response;
    }

    @Override
    public int getQueuePosition(Long appointmentId) {

        Appointment appointment =
                appointmentRepository.findById(appointmentId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Appointment not found"
                                )
                        );

        List<Appointment> appointments =
                appointmentRepository.findByDoctorIdAndStatus(
                        appointment.getDoctor().getId(),
                        AppointmentStatus.BOOKED
                );

        QueueManager queue = new QueueManager();

        for (Appointment a : appointments) {
            queue.add(a);
        }

        int position = 1;

        while (!queue.isEmpty()) {

            Appointment current = queue.poll();

            if (current.getId().equals(appointmentId)) {
                return position;
            }

            position++;
        }

        return -1;
    }

    @Override
    @Transactional
    public void completeAppointment(Long appointmentId) {

        // Read only the immutable doctor ID before taking the doctor lock.
        // Always acquire doctor -> appointment, never the reverse.
        Long doctorId = appointmentRepository.findDoctorId(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        lockDoctor(doctorId);

        Appointment appointment =
                appointmentRepository.findById(appointmentId)
                        .orElseThrow(
                                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                        "Appointment not found"
                                )
                        );

        entityManager.refresh(appointment, LockModeType.PESSIMISTIC_WRITE);
        if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only an in-progress appointment can be completed");
        }

        appointment.setStatus(
                AppointmentStatus.COMPLETED
        );

        appointmentRepository.save(appointment);
    }

    private void lockDoctor(Long doctorId) {
        // Keep earlier changes if several service calls share one transaction.
        entityManager.flush();
        doctorRepository.lockQueue(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));
    }
}
