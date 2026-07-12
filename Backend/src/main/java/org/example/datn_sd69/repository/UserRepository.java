package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"role"})
    @Query("""
            SELECT u
            FROM User u
            JOIN FETCH u.role r
            WHERE LOWER(u.email) = LOWER(:email)
            """)
    Optional<User> findWithRoleByEmail(@Param("email") String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("""
            SELECT COUNT(u) > 0
            FROM User u
            WHERE LOWER(u.email) = LOWER(:email)
            AND (:ignoreId IS NULL OR u.id <> :ignoreId)
            """)
    boolean existsEmail(
            @Param("email") String email,
            @Param("ignoreId") Integer ignoreId
    );

    @Query("""
            SELECT COUNT(u) > 0
            FROM User u
            WHERE u.phone = :phone
            AND (:ignoreId IS NULL OR u.id <> :ignoreId)
            """)
    boolean existsPhone(
            @Param("phone") String phone,
            @Param("ignoreId") Integer ignoreId
    );

    boolean existsByEmailAndIdNot(String email, Integer id);

    boolean existsByPhoneAndIdNot(String phone, Integer id);
    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmailIgnoreCase(String email);

}