package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.base.BaseEntity;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "GoodsReceiptApprovalHistory")
public class GoodsReceiptApprovalHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GoodsReceiptId", nullable = false)
    private GoodsReceipt goodsReceipt;

    @Column(name = "FromStatus")
    private Byte fromStatus;

    @Column(name = "ToStatus", nullable = false)
    private Byte toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ActionBy", nullable = false)
    private User actionBy;

    @Column(name = "Reason", length = 500)
    private String reason;

    @Column(name = "ActionAt", nullable = false)
    private LocalDateTime actionAt;
}