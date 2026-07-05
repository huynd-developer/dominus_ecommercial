package org.example.datn_sd69.modules.customerProfile.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerProfileResponse(
        Integer userId,
        String name,
        String email,
        String phone,
        String address,
        String avatarUrl,
        Integer status,
        LocalDateTime createdAt,

        String customerRank,
        Integer loyaltyPoints,
        LocalDate dateOfBirth,
        Integer gender
) {
}