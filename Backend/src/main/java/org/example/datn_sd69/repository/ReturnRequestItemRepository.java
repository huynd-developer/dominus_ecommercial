package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.ReturnRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnRequestItemRepository extends JpaRepository<ReturnRequestItem, Integer> {

    List<ReturnRequestItem> findByReturnRequest_Id(Integer returnRequestId);

    boolean existsByReturnRequest_Order_IdAndStatus(Integer orderId, Integer status);

    List<ReturnRequestItem> findByReturnRequest_Order_IdAndStatus(Integer orderId, Integer status);
}
