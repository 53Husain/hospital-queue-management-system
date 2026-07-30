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
import com.shqms.dto.request.LoginRequest;
import com.shqms.dto.response.LoginResponse;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AuthServiceImpl(
            UserRepository userRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository) {

        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    @Override
    public String register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setPhoneNumber(request.getPhoneNumber());

        user.setRole(
                UserRole.valueOf(
                        request.getRole().toUpperCase()
                )
        );

        user = userRepository.save(user);
        System.out.println(user.getRole());
        if (user.getRole() == UserRole.PATIENT) {

            Patient patient = new Patient();

            patient.setUser(user);
            patient.setAge(0);
            patient.setGender("");
            patient.setBloodGroup("");

            patientRepository.save(patient);
        }
        System.out.println(user.getRole());
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
                        new RuntimeException("Invalid Email"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Invalid Password");
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