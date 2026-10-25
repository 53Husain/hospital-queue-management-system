package com.shqms.service.impl;

import com.shqms.dto.request.RegisterRequest;
import com.shqms.entity.Doctor;
import com.shqms.entity.Patient;
import com.shqms.entity.User;
import com.shqms.enums.UserRole;
import com.shqms.repository.DoctorRepository;
import com.shqms.repository.PatientRepository;
import com.shqms.repository.UserRepository;
import com.shqms.service.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import com.shqms.dto.request.LoginRequest;
import com.shqms.dto.response.LoginResponse;

@Service
public class AuthServiceImpl implements AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AuthServiceImpl(
            UserRepository userRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository, PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;

        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    @Override
    @Transactional
    public String register(RegisterRequest request) {
        if (request.getRole() != null && !"PATIENT".equalsIgnoreCase(request.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Public registration is patient-only");
        }
        return createUser(request, UserRole.PATIENT);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public String registerStaff(RegisterRequest request) {
        UserRole role;
        try { role = UserRole.valueOf(request.getRole()); }
        catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use DOCTOR or ADMIN");
        }
        if (role == UserRole.PATIENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use public patient registration");
        }
        return createUser(request, role);
    }

    private String createUser(RegisterRequest request, UserRole role) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = new User();

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        if (request.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must not exceed 72 UTF-8 bytes");
        }
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());

        user.setRole(role);

        user = userRepository.save(user);
        if (user.getRole() == UserRole.PATIENT) {

            Patient patient = new Patient();

            patient.setUser(user);
            patient.setAge(0);
            patient.setGender("");
            patient.setBloodGroup("");

            patientRepository.save(patient);
        }
        if (user.getRole() == UserRole.DOCTOR) {

            Doctor doctor = new Doctor();

            doctor.setUser(user);
            doctor.setSpecialization("");
            doctor.setExperience(0);
            doctor.setRating(0.0);

            doctorRepository.save(doctor);
        }

        return "User Registered Successfully";
    }

    @Override
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (request.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        Long patientId = null;
        Long doctorId = null;

        if (user.getRole() == UserRole.PATIENT) {

            Patient patient = patientRepository.findByUser(user)
                    .orElseThrow(() ->
                            new RuntimeException("Patient not found"));

            patientId = patient.getId();
        }

        if (user.getRole() == UserRole.DOCTOR) {

            Doctor doctor = doctorRepository.findByUser(user)
                    .orElseThrow(() ->
                            new RuntimeException("Doctor not found"));

            doctorId = doctor.getId();
        }

        return new LoginResponse(
                user.getId(),
                patientId,
                doctorId,
                user.getFullName(),
                user.getRole().name(),
                "Login Successful"
        );
    }
}
