package org.example.datn_sd69.modules.customer.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class CustomerResponse {

    private Integer userId;

    private String name;

    private String email;

    private String phone;

    private String address;

    private String avatarUrl;

    private String customerRank;

    private Integer loyaltyPoints;

    private LocalDate dateOfBirth;

    private Integer gender;

    private String genderText;

    private Integer status;

    private String statusText;

    private LocalDateTime createdAt;
}
