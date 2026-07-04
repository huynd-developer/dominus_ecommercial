package org.example.datn_sd69.modules.employee.service;

import org.example.datn_sd69.modules.employee.dto.request.EmployeeCreateRequest;
import org.example.datn_sd69.modules.employee.dto.request.EmployeeUpdateRequest;
import org.example.datn_sd69.modules.employee.dto.response.EmployeeAvatarUploadResponse;
import org.example.datn_sd69.modules.employee.dto.response.EmployeeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface EmployeeService {

    Page<EmployeeResponse> getAll(String keyword, Integer status, Pageable pageable);

    EmployeeResponse getById(Integer userId);

    EmployeeResponse create(EmployeeCreateRequest request);

    EmployeeResponse update(Integer userId, EmployeeUpdateRequest request);

    EmployeeResponse lock(Integer userId);

    EmployeeResponse unlock(Integer userId);

    void deleteSoft(Integer userId);

    EmployeeAvatarUploadResponse uploadAvatar(MultipartFile file);
}