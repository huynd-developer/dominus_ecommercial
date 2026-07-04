package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    @EntityGraph(attributePaths = {"user", "user.role"})
    @Query("""
            SELECT e
            FROM Employee e
            WHERE LOWER(e.user.email) = LOWER(:email)
            AND (e.user.isDeleted = false OR e.user.isDeleted IS NULL)
            """)
    Optional<Employee> findByUserEmail(@Param("email") String email);

    @EntityGraph(attributePaths = {"user", "user.role"})
    @Query("""
            SELECT e
            FROM Employee e
            WHERE (e.user.isDeleted = false OR e.user.isDeleted IS NULL)
            AND (:status IS NULL OR e.user.status = :status)
            AND (
                :keyword IS NULL OR :keyword = ''
                OR LOWER(e.user.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(e.user.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR e.user.phone LIKE CONCAT('%', :keyword, '%')
                OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR e.citizenId LIKE CONCAT('%', :keyword, '%')
                OR LOWER(e.jobTitle) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<Employee> searchEmployees(
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user", "user.role"})
    @Query("""
            SELECT e
            FROM Employee e
            WHERE e.userId = :userId
            AND (e.user.isDeleted = false OR e.user.isDeleted IS NULL)
            """)
    Optional<Employee> findActiveByUserId(@Param("userId") Integer userId);

    @Query("""
            SELECT COUNT(e) > 0
            FROM Employee e
            WHERE e.citizenId = :citizenId
            AND (:ignoreUserId IS NULL OR e.userId <> :ignoreUserId)
            """)
    boolean existsCitizenId(
            @Param("citizenId") String citizenId,
            @Param("ignoreUserId") Integer ignoreUserId
    );
}