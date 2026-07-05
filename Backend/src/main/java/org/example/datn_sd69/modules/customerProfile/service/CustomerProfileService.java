package org.example.datn_sd69.modules.customerProfile.service;

import org.example.datn_sd69.modules.customerProfile.dto.ChangePasswordRequest;
import org.example.datn_sd69.modules.customerProfile.dto.CustomerProfileResponse;
import org.example.datn_sd69.modules.customerProfile.dto.UpdateCustomerProfileRequest;
import org.springframework.web.multipart.MultipartFile;

public interface CustomerProfileService {

    CustomerProfileResponse getProfile();

    CustomerProfileResponse updateProfile(UpdateCustomerProfileRequest request);

    CustomerProfileResponse uploadAvatar(MultipartFile file);

    void changePassword(ChangePasswordRequest request);
}