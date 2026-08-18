package org.example.datn_sd69.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InsurancePolicyRepository
        extends JpaRepository<InsurancePolicy, Long> {

    Optional<InsurancePolicy> findByPolicyNumber(String policyNumber);
}