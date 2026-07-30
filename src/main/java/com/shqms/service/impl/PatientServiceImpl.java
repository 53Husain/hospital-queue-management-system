package com.shqms.service.impl;

import com.shqms.dto.request.UpdateProfileRequest;
import com.shqms.dto.response.ProfileResponse;
import com.shqms.entity.Patient;
import com.shqms.entity.User;
import com.shqms.repository.PatientRepository;
import com.shqms.repository.UserRepository;
import com.shqms.service.PatientService;
import org.springframework.stereotype.Service;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    public PatientServiceImpl(
            PatientRepository patientRepository,
            UserRepository userRepository) {

        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ProfileResponse getProfile(Long patientId) {

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() ->
                        new RuntimeException("Patient not found"));

        User user = patient.getUser();

        return new ProfileResponse(

                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),

                patient.getAge(),
                patient.getGender(),
                patient.getBloodGroup()
        );
    }

    @Override
    public String updateProfile(Long patientId,
                                UpdateProfileRequest request) {

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() ->
                        new RuntimeException("Patient not found"));

        User user = patient.getUser();

        user.setPhoneNumber(request.getPhoneNumber());

        patient.setAge(request.getAge());
        patient.setGender(request.getGender());
        patient.setBloodGroup(request.getBloodGroup());

        userRepository.save(user);
        patientRepository.save(patient);

        return "Profile Updated Successfully";
    }

}