package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.OrderDeliveryEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderDeliveryEvidenceRepository extends JpaRepository<OrderDeliveryEvidence, Integer> {

    List<OrderDeliveryEvidence> findByOrder_IdAndEvidenceTypeOrderByCreatedAtAsc(
            Integer orderId,
            Integer evidenceType
    );

    long countByOrder_IdAndEvidenceType(Integer orderId, Integer evidenceType);
}