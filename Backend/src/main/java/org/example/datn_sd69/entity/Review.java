//package org.example.datn_sd69.entity;
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//import org.example.datn_sd69.entity.base.BaseEntity;
//import org.hibernate.annotations.Nationalized;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//@Entity
//@Table(name = "Review")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//public class Review extends BaseEntity {
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "UserId", nullable = false)
//    private User user;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "OrderItemId", nullable = false)
//    private OrderItem orderItem;
//
//    @Column(name = "Rating", nullable = false)
//    private Integer rating;
//
//    @Nationalized
//    @Column(name = "Comment", length = 1000)
//    private String comment;
//
//    // 👇 1. BỔ SUNG CÁC TRƯỜNG PHỤC VỤ LUỒNG DUYỆT ĐÁNH GIÁ
//    @Column(name = "ApprovalStatus", nullable = false)
//    private Integer approvalStatus = 1; // 0: Chờ duyệt, 1: Đã duyệt, 2: Từ chối, 3: Đã ẩn
//
//    @Column(name = "ApprovedAt")
//    private LocalDateTime approvedAt;
//
//    @Column(name = "RejectedAt")
//    private LocalDateTime rejectedAt;
//
//    @Nationalized
//    @Column(name = "RejectedReason", length = 255)
//    private String rejectedReason;
//
//    // 👇 2. BỔ SUNG CÁC TRƯỜNG PHỤC VỤ LỊCH SỬ CHỈNH SỬA
//    @Column(name = "EditedAt")
//    private LocalDateTime editedAt;
//
//    @Column(name = "EditCount", nullable = false)
//    private Integer editCount = 0;
//
//    @Column(name = "CreatedAt")
//    private LocalDateTime createdAt = LocalDateTime.now();
//
//    @Column(name = "IsDeleted")
//    private Boolean isDeleted = false;
//
//    @PrePersist
//    public void prePersist() {
//        if (createdAt == null) {
//            createdAt = LocalDateTime.now();
//        }
//
//        if (isDeleted == null) {
//            isDeleted = false;
//        }
//
//        if (approvalStatus == null) {
//            approvalStatus = 1;
//        }
//
//        if (editCount == null) {
//            editCount = 0;
//        }
//    }
//
//    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
//    private List<ReviewMedia> reviewMedias = new ArrayList<>();
//}
package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.datn_sd69.entity.base.BaseEntity;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Review")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderItemId", nullable = false)
    private OrderItem orderItem;

    @Column(name = "Rating", nullable = false)
    private Integer rating;

    @Nationalized
    @Column(name = "Comment", length = 1000)
    private String comment;

    @Column(name = "ApprovalStatus", nullable = false)
    private Integer approvalStatus = 1; // 0: Chờ duyệt, 1: Đã duyệt, 2: Từ chối, 3: Đã ẩn

    @Column(name = "ApprovedAt")
    private LocalDateTime approvedAt;

    @Column(name = "RejectedAt")
    private LocalDateTime rejectedAt;

    @Nationalized
    @Column(name = "RejectedReason", length = 255)
    private String rejectedReason;

    @Column(name = "EditedAt")
    private LocalDateTime editedAt;

    @Column(name = "EditCount", nullable = false)
    private Integer editCount = 0;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "IsDeleted")
    private Boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (isDeleted == null) isDeleted = false;
        if (approvalStatus == null) approvalStatus = 1;
        if (editCount == null) editCount = 0;
    }

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ReviewMedia> reviewMedias = new ArrayList<>();
}