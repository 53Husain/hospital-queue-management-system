package com.shqms.service.impl;

import com.shqms.dto.response.DoctorResponse;
import com.shqms.entity.Doctor;
import com.shqms.repository.DoctorRepository;
import com.shqms.service.DoctorService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;

    public DoctorServiceImpl(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    @Override
    public List<DoctorResponse> getAllDoctors() {

        List<Doctor> doctors = doctorRepository.findAll();

        List<DoctorResponse> response = new ArrayList<>();

        for (Doctor doctor : doctors) {

            response.add(
                    new DoctorResponse(
                            doctor.getId(),
                            doctor.getUser().getFullName(),
                            doctor.getSpecialization(),
                            doctor.getExperience(),
                            doctor.getRating()
                    )
            );
        }

        return response;
    }
}