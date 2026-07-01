package org.example.datn_sd69.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.base.BaseEntity;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "Brand")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Brand extends BaseEntity {
    @Nationalized
    @Column(name = "Name", length = 255, nullable = false)
    private String name;

    @Nationalized
    @Column(name = "Description", length = 1000)
    private String description;

    @Column(name = "LogoUrl", length = 500)
    private String logoUrl;

    @Column(name = "Status")
    private Integer status = 1;

    @Column(name = "IsDeleted")
    private Boolean isDeleted = false;
}
