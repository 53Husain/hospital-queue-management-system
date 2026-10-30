package com.shqms.repository;

import com.shqms.entity.Appointment;
import com.shqms.enums.AppointmentStatus;
import com.shqms.enums.PriorityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

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

    // Locking reads see committed transitions even with an older MySQL snapshot.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.doctor.id = :doctorId and a.status = :status")
    List<Appointment> findQueueForUpdate(@Param("doctorId") Long doctorId,
                                       @Param("status") AppointmentStatus status);

    @Query("select a.doctor.id from Appointment a where a.id = :id")
    Optional<Long> findDoctorId(@Param("id") Long id);

    @Query(value = "select token_number from appointments where doctor_id = :doctorId for update", nativeQuery = true)
    List<Integer> findTokensForUpdate(@Param("doctorId") Long doctorId);

}
