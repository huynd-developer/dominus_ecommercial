package org.example.datn_sd69.modules.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterCustomerRequest {
    private String name;
    private String email;
    private String password;
    private String phone;
}
