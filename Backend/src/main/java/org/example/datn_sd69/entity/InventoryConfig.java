package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "InventoryConfig")
public class InventoryConfig {

    @Id
    @Column(name = "Id")
    private Byte id;

    @Column(name = "ExpiryWarningDays", nullable = false)
    private Short expiryWarningDays;
}