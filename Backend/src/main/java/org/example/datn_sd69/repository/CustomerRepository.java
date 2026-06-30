package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    @Query("""
            SELECT c FROM Customer c
            JOIN FETCH c.user u
            WHERE u.phone = :phone AND u.status = 1
            """)
    Optional<Customer> findByPhone(@Param("phone") String phone);
}