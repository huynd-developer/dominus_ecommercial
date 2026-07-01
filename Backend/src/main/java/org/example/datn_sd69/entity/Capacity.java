package org.example.datn_sd69.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.base.BaseEntity;

@Entity
@Table(name = "Capacity")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Capacity extends BaseEntity {

    @Column(name = "Value", nullable = false, unique = true)
    private Double value;

    @Column(name = "Status")
    private Integer status = 1;
}