package org.example.datn_sd69;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
public class DatnSd69Application {

    public static void main(String[] args) {
        SpringApplication.run(DatnSd69Application.class, args);
    }

}
