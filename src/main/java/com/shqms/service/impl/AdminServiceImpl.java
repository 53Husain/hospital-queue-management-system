package com.shqms.service.impl;

import com.shqms.dto.response.DashboardResponse;
import com.shqms.enums.AppointmentStatus;
import com.shqms.enums.PriorityType;
import com.shqms.repository.AppointmentRepository;
import com.shqms.repository.DoctorRepository;
import com.shqms.repository.PatientRepository;
import com.shqms.service.AdminService;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl implements AdminService {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    public AdminServiceImpl(
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            AppointmentRepository appointmentRepository) {

        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public DashboardResponse getDashboard() {

        return new DashboardResponse(

                patientRepository.count(),

                doctorRepository.count(),

                appointmentRepository.count(),

                appointmentRepository.countByStatus(
                        AppointmentStatus.COMPLETED
                ),

                appointmentRepository.countByStatus(
                        AppointmentStatus.BOOKED
                ),

                appointmentRepository.countByPriority(
                        PriorityType.EMERGENCY
                )
        );
    }
}