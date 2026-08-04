package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Integer> {
    List<CustomerAddress> findByCustomerIdAndIsDeletedFalseOrderByIsDefaultDescCreatedAtDesc(Integer customerId);
    Optional<CustomerAddress> findByIdAndCustomerId(Integer id, Integer customerId);
}
