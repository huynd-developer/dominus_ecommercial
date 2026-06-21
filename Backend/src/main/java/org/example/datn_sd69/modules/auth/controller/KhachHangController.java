package org.example.datn_sd69.modules.auth.controller;

import org.example.datn_sd69.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.example.datn_sd69.modules.auth.dto.AccountUpdateRequest;
import org.example.datn_sd69.modules.auth.dto.CustomerProfileResponse;
import org.example.datn_sd69.modules.auth.service.KhachHangServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/account")
@CrossOrigin(origins = "*")
public class KhachHangController {

    @Autowired
    private KhachHangServices khachHangService;

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateAccount(
            @Valid @RequestBody AccountUpdateRequest request) {

        CustomerProfileResponse responseData = khachHangService.updateProfileAndPass(request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin khách hàng Dominus thành công!", responseData));
    }
}
