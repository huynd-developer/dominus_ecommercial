package org.example.datn_sd69;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;


@SpringBootApplication
@EnableMethodSecurity
// Bật cấu hình VIA_DTO: Ép Spring Boot trả về cấu trúc phân trang (Pagination)
// dưới dạng DTO ổn định, Frontend sẽ không bao giờ lo bị lỗi vặt khi parse JSON nữa.
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class DatnSd69Application {

    public static void main(String[] args) {
        SpringApplication.run(DatnSd69Application.class, args);
    }

}