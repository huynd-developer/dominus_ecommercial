package org.example.datn_sd69.modules.employee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class EmployeeAvatarUploadResponse {

    private String url;

    private String publicId;
}