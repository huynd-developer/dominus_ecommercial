package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Integer>, JpaSpecificationExecutor<Review> {

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

    @Query("""
        SELECT COALESCE(AVG(r.rating), 0)
        FROM Review r
        WHERE r.orderItem.productVariant.product.id = :productId
          AND r.isDeleted = false
          AND r.approvalStatus = 1
        """)
    Double findAverageRatingByProductId(@Param("productId") Integer productId);

    @Query("""
        SELECT COUNT(r)
        FROM Review r
        WHERE r.orderItem.productVariant.product.id = :productId
          AND r.isDeleted = false
          AND r.approvalStatus = 1
        """)
    Long countReviewsByProductId(@Param("productId") Integer productId);

    Optional<Review> findByIdAndIsDeletedFalse(Integer id);

    @Query("""
        SELECT r 
        FROM Review r
        WHERE r.orderItem.productVariant.product.id = :productId
          AND r.isDeleted = false
          AND r.approvalStatus = 1
        """)
    Page<Review> findApprovedReviewsByProductId(@Param("productId") Integer productId, Pageable pageable);
}