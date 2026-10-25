package com.shqms.repository;

import com.shqms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    boolean existsByRole(com.shqms.enums.UserRole role);

    Optional<User> findByEmail(String email);
}
