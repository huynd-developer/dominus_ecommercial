package org.example.datn_sd69.modules.customerProfile.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.customerProfile.dto.ChangePasswordRequest;
import org.example.datn_sd69.modules.customerProfile.dto.UpdateCustomerProfileRequest;
import org.example.datn_sd69.modules.customerProfile.service.CustomerProfileService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/customer/profile")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER')")
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    @GetMapping
    public ResponseEntity<?> getProfile() {
        return ResponseEntity.ok(customerProfileService.getProfile());
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(
            @Valid @RequestBody UpdateCustomerProfileRequest request
    ) {
        return ResponseEntity.ok(customerProfileService.updateProfile(request));
    }

    @PutMapping(
            value = "/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadAvatar(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(customerProfileService.uploadAvatar(file));
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        customerProfileService.changePassword(request);

        return ResponseEntity.ok(Map.of(
                "message", "Đổi mật khẩu thành công"
        ));
    }
}