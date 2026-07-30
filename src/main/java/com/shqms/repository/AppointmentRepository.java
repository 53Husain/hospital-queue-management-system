package com.shqms.repository;

import com.shqms.entity.Appointment;
import com.shqms.enums.AppointmentStatus;
import com.shqms.enums.PriorityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentRepository
        extends JpaRepository<Appointment, Long> {

    long countByDoctorId(Long doctorId);
    long countByStatus(AppointmentStatus status);

    long countByPriority(PriorityType priority);

    List<Appointment> findByDoctorIdAndStatus(
            Long doctorId,
            AppointmentStatus status);

    List<Appointment> findByDoctorIdAndStatusIn(
            Long doctorId,
            List<AppointmentStatus> statuses);

    List<Appointment> findByPatientId(Long patientId);

}