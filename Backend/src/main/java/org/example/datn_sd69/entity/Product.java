package org.example.datn_sd69.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.base.BaseEntity;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Product")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Product extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BrandId", nullable = false)
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ConcentrationId", nullable = false)
    private Concentration concentration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategoryId", nullable = false)
    private Category category;

    @Nationalized
    @Column(name = "Name", length = 255, nullable = false)
    private String name;

    @Column(name = "Gender")
    private Integer gender;

    @Nationalized
    @Column(name = "Description", length = 2000)
    private String description;

    @Column(name = "IsNiche")
    private Boolean isNiche = false;

    @Column(name = "Status")
    private Integer status = 1;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Mapping bảng trung gian ProductFragrance không cần trường phụ
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "ProductFragrance",
            joinColumns = @JoinColumn(name = "ProductId"),
            inverseJoinColumns = @JoinColumn(name = "FragranceFamilyId")
    )
    private Set<FragranceFamily> fragranceFamilies = new HashSet<>();
}
