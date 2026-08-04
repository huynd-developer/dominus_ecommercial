package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CustomerAddress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "CustomerId", nullable = false)
    private Integer customerId;

    @Column(name = "RecipientName", nullable = false)
    private String recipientName;

    @Column(name = "Phone", nullable = false, length = 15)
    private String phone;

    @Column(name = "ProvinceCode", length = 20)
    private String provinceCode;

    @Column(name = "ProvinceName", length = 100)
    private String provinceName;

    @Column(name = "WardCode", length = 20)
    private String wardCode;

    @Column(name = "WardName", length = 100)
    private String wardName;

    @Column(name = "SpecificAddress", nullable = false)
    private String specificAddress;

    @Column(name = "FullAddress", nullable = false)
    private String fullAddress;

    @Column(name = "IsDefault")
    private Boolean isDefault = false;

    @Column(name = "IsDeleted")
    private Boolean isDeleted = false;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();
}
