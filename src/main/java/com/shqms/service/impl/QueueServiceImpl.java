package com.shqms.service.impl;

import com.shqms.dto.response.QueueEntryResponse;
import com.shqms.dto.response.QueueResponse;
import com.shqms.dsa.queue.QueueManager;
import com.shqms.entity.Appointment;
import com.shqms.enums.AppointmentStatus;
import com.shqms.repository.AppointmentRepository;
import com.shqms.service.QueueService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class QueueServiceImpl implements QueueService {

    private final AppointmentRepository appointmentRepository;

    public QueueServiceImpl(
            AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public QueueResponse callNextPatient(Long doctorId) {

        // Check if a patient is already being consulted
        List<Appointment> inProgressAppointments =
                appointmentRepository.findByDoctorIdAndStatus(
                        doctorId,
                        AppointmentStatus.IN_PROGRESS
                );

        if (!inProgressAppointments.isEmpty()) {
            throw new RuntimeException(
                    "Please complete the current appointment before calling the next patient."
            );
        }

        // Get all booked appointments
        List<Appointment> appointments =
                appointmentRepository.findByDoctorIdAndStatus(
                        doctorId,
                        AppointmentStatus.BOOKED
                );

        QueueManager queue = new QueueManager();

        for (Appointment appointment : appointments) {
            queue.add(appointment);
        }

        if (queue.isEmpty()) {
            throw new RuntimeException("No patients in queue");
        }

        Appointment nextPatient = queue.poll();

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
    public void completeAppointment(Long appointmentId) {

        Appointment appointment =
                appointmentRepository.findById(appointmentId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Appointment not found"
                                )
                        );

        appointment.setStatus(
                AppointmentStatus.COMPLETED
        );

        appointmentRepository.save(appointment);
    }
}