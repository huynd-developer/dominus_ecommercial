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
@Table(name = "Roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role extends BaseEntity {
    @Nationalized
    @Column(name = "Name", length = 100, nullable = false, unique = true)
    private String name;

    @Nationalized
    @Column(name = "Description", length = 255)
    private String description;

    @Column(name = "IsDeleted")
    private Boolean isDeleted = false;
}
