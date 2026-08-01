package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.ReturnRequestMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnRequestMediaRepository extends JpaRepository<ReturnRequestMedia, Integer> {
    List<ReturnRequestMedia> findByReturnRequest_Id(Integer returnRequestId);
}
