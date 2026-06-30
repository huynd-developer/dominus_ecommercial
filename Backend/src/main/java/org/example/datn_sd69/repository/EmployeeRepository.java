package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    @Query("""
            SELECT e FROM Employee e
            JOIN FETCH e.user u
            WHERE u.email = :email
            """)
    Optional<Employee> findByUserEmail(@Param("email") String email);
}