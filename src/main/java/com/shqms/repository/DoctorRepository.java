package com.shqms.repository;

import com.shqms.entity.Doctor;
import com.shqms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUser(User user);

    // Scalar locking read avoids locking the doctor's eagerly loaded relations.
    // Call only inside a transaction; all queue writers take this lock first.
    @Query(value = "select id from doctors where id = :id for update", nativeQuery = true)
    Optional<Long> lockQueue(@Param("id") Long id);

}
