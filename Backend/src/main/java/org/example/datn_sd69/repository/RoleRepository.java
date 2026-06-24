package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(String name); // Ví dụ: "ROLE_CUSTOMER", "ROLE_ADMIN"
}
