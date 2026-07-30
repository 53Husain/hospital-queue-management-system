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
    public AppointmentResponse bookAppointment(
            AppointmentRequest request) {

        Patient patient =
                patientRepository.findById(request.getPatientId())
                        .orElseThrow(() ->
                                new RuntimeException("Patient not found"));

        Doctor doctor =
                doctorRepository.findById(request.getDoctorId())
                        .orElseThrow(() ->
                                new RuntimeException("Doctor not found"));

        long count =
                appointmentRepository.countByDoctorId(
                        doctor.getId());

        Appointment appointment = new Appointment();

        appointment.setPatient(patient);
        appointment.setDoctor(doctor);

        appointment.setTokenNumber((int) count + 1);

        appointment.setPriority(request.getPriority());

        appointment.setStatus(
                AppointmentStatus.BOOKED);

        appointment.setAppointmentTime(
                LocalDateTime.now());

        appointment.setEstimatedWaitingTime(
                ((int) count) * 15);

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