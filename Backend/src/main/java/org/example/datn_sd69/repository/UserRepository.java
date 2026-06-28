package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    // Hàm cũ: Vẫn giữ nguyên để không ảnh hưởng đến các logic khác
    Optional<User> findByEmail(String email);

    // HÀM MỚI CHUYÊN DỤNG CHO SECURITY:
    // @EntityGraph sẽ bảo Hibernate "kéo" theo cái role lên cùng lúc chỉ trong hàm này
    @EntityGraph(attributePaths = {"role"})
    Optional<User> findWithRoleByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}