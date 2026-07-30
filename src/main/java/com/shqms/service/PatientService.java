package com.shqms.service;

import com.shqms.dto.request.UpdateProfileRequest;
import com.shqms.dto.response.ProfileResponse;

public interface PatientService {

    ProfileResponse getProfile(Long patientId);

    String updateProfile(Long patientId,
                         UpdateProfileRequest request);

}