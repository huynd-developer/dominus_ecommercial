package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
}
