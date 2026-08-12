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
@Table(name = "InventoryLotLockHistory")
public class InventoryLotLockHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "InventoryLotId", nullable = false)
    private InventoryLot inventoryLot;

    /**
     * 1 = LOCK
     * 2 = UNLOCK
     */
    @Column(name = "ActionType", nullable = false)
    private Byte actionType;

    @Column(name = "Reason", length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ActionBy", nullable = false)
    private User actionBy;

    @Column(name = "ActionAt", nullable = false)
    private LocalDateTime actionAt;
}