package org.example.datn_sd69.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.base.BaseEntity;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "ReturnRequestMedia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestMedia extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReturnRequestId", nullable = false)
    private ReturnRequest returnRequest;

    /**
     * 1 = Ảnh
     * 2 = Video
     */
    @Column(name = "MediaType", nullable = false)
    private Integer mediaType;

    @Nationalized
    @Column(name = "MediaUrl", nullable = false, length = 500)
    private String mediaUrl;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}