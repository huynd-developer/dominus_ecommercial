package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    boolean existsByUser_IdAndOrderItem_IdAndIsDeletedFalse(
            Integer userId,
            Integer orderItemId
    );

    Optional<Review> findByUser_IdAndOrderItem_IdAndIsDeletedFalse(
            Integer userId,
            Integer orderItemId
    );

    List<Review> findByUser_IdAndIsDeletedFalseOrderByCreatedAtDesc(
            Integer userId
    );

    List<Review> findByIsDeletedFalseOrderByCreatedAtDesc();
}