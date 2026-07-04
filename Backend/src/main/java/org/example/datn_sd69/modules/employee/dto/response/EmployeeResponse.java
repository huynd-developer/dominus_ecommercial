package org.example.datn_sd69.modules.employee.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class EmployeeResponse {

    private Integer userId;

    private String employeeCode;

    private String roleName;

    private String name;

    private String email;

    private String phone;

    private String address;

    private String avatarUrl;

    private Integer status;

    private Boolean isDeleted;

    private LocalDateTime createdAt;

    private String citizenId;

    private String jobTitle;

    private BigDecimal salary;

    private LocalDate hireDate;
}