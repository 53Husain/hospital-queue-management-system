package com.shqms.service;

import com.shqms.dto.response.DoctorResponse;

import java.util.List;

public interface DoctorService {

    List<DoctorResponse> getAllDoctors();

}