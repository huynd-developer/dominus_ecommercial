package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Integer> {

    Optional<ReturnRequest> findTopByOrder_IdOrderByCreatedAtDesc(Integer orderId);
}
