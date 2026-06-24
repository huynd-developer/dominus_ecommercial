package org.example.datn_sd69.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.base.BaseEntity;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDate;

@Entity
@Table(name = "Customer")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Customer {
    @Id
    @Column(name = "UserId")
    private Integer userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "UserId")
    private User user;

    @Column(name = "CustomerRank", length = 50)
    private String customerRank = "Bronze";

    @Column(name = "LoyaltyPoints")
    private Integer loyaltyPoints = 0;

    @Column(name = "DateOfBirth")
    private LocalDate dateOfBirth;

    @Column(name = "Gender")
    private Integer gender; // 0, 1, 2
}
