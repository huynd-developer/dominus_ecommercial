package org.example.datn_sd69.modules.employee.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.employee.dto.request.EmployeeCreateRequest;
import org.example.datn_sd69.modules.employee.dto.request.EmployeeUpdateRequest;
import org.example.datn_sd69.modules.employee.dto.response.EmployeeAvatarUploadResponse;
import org.example.datn_sd69.modules.employee.dto.response.EmployeeResponse;
import org.example.datn_sd69.modules.employee.service.EmployeeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/employees")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('OWNER')")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public Page<EmployeeResponse> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(safePage, safeSize);

        return employeeService.getAll(keyword, status, pageable);
    }

    @GetMapping("/{userId}")
    public EmployeeResponse getById(@PathVariable Integer userId) {
        return employeeService.getById(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@Valid @RequestBody EmployeeCreateRequest request) {
        return employeeService.create(request);
    }

    @PutMapping("/{userId}")
    public EmployeeResponse update(
            @PathVariable Integer userId,
            @Valid @RequestBody EmployeeUpdateRequest request
    ) {
        return employeeService.update(userId, request);
    }

    @PatchMapping("/{userId}/lock")
    public EmployeeResponse lock(@PathVariable Integer userId) {
        return employeeService.lock(userId);
    }

    @PatchMapping("/{userId}/unlock")
    public EmployeeResponse unlock(@PathVariable Integer userId) {
        return employeeService.unlock(userId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSoft(@PathVariable Integer userId) {
        employeeService.deleteSoft(userId);
    }

    @PostMapping(
            value = "/upload-avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public EmployeeAvatarUploadResponse uploadAvatar(
            @RequestParam("file") MultipartFile file
    ) {
        return employeeService.uploadAvatar(file);
    }
}