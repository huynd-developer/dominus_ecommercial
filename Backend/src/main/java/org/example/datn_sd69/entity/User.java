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
@Table(name = "Users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class User extends BaseEntity {
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "RoleId", nullable = false)
    private Role role;

    @Nationalized
    @Column(name = "Name", length = 255, nullable = false)
    private String name;

    @Column(name = "Email", length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "PasswordHash", length = 255, nullable = false)
    private String passwordHash;

    @Column(name = "Phone", length = 15, unique = true)
    private String phone;

    @Nationalized
    @Column(name = "Address", length = 500)
    private String address;

    @Column(name = "AvatarUrl", length = 500)
    private String avatarUrl;

    @Column(name = "Status")
    private Integer status = 1;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "IsDeleted")
    private Boolean isDeleted = false;
}