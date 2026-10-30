package com.shqms.service.impl;

import com.shqms.dto.request.AppointmentRequest;
import com.shqms.dto.response.AppointmentResponse;
import com.shqms.entity.Appointment;
import com.shqms.entity.Doctor;
import com.shqms.entity.Patient;
import com.shqms.enums.AppointmentStatus;
import com.shqms.repository.AppointmentRepository;
import com.shqms.repository.DoctorRepository;
import com.shqms.repository.PatientRepository;
import com.shqms.service.AppointmentService;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.shqms.dto.response.AppointmentDetailsResponse;
import java.util.ArrayList;
import java.util.List;

import java.time.LocalDateTime;

@Service
public class AppointmentServiceImpl
        implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentServiceImpl(
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository) {

        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    @Override
    @Transactional
    public AppointmentResponse bookAppointment(
            AppointmentRequest request) {

        if (request.getPatientId() == null || request.getPatientId() <= 0
                || request.getDoctorId() == null || request.getDoctorId() <= 0
                || request.getPriority() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient, doctor and priority are required");
        }

        doctorRepository.lockQueue(request.getDoctorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));

        Patient patient =
                patientRepository.findById(request.getPatientId())
                        .orElseThrow(() ->
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));

        Doctor doctor =
                doctorRepository.findById(request.getDoctorId())
                        .orElseThrow(() ->
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));

        List<Integer> tokens = appointmentRepository.findTokensForUpdate(doctor.getId());
        int nextToken = Math.addExact(tokens.stream().filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue).max().orElse(0), 1);

        Appointment appointment = new Appointment();

        appointment.setPatient(patient);
        appointment.setDoctor(doctor);

        appointment.setTokenNumber(nextToken);

        appointment.setPriority(request.getPriority());

        appointment.setStatus(
                AppointmentStatus.BOOKED);

        appointment.setAppointmentTime(
                LocalDateTime.now());

        appointment.setEstimatedWaitingTime(
                tokens.size() * 15);

        appointment =
                appointmentRepository.save(appointment);

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getTokenNumber(),
                appointment.getEstimatedWaitingTime(),
                appointment.getStatus().name()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentDetailsResponse> getAppointments(Long patientId) {

        List<Appointment> appointments =
                appointmentRepository.findByPatientId(patientId);

        List<AppointmentDetailsResponse> response =
                new ArrayList<>();

        for (Appointment appointment : appointments) {

            response.add(

                    new AppointmentDetailsResponse(

                            appointment.getId(),

                            appointment.getDoctor()
                                    .getUser()
                                    .getFullName(),

                            appointment.getPriority()
                                    .name(),

                            appointment.getTokenNumber(),

                            appointment.getEstimatedWaitingTime(),

                            appointment.getStatus()
                                    .name()

                    )

            );

        }

        return response;
    }
    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointment(Long id) {

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Appointment not found"));

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getTokenNumber(),
                appointment.getEstimatedWaitingTime(),
                appointment.getStatus().name()
        );
    }
}
