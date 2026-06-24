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

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Employee")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Employee {
    @Id
    @Column(name = "UserId")
    private Integer userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "UserId")
    private User user;

    @Column(name = "EmployeeCode", length = 50, nullable = false, unique = true)
    private String employeeCode;

    @Column(name = "CitizenId", length = 12, nullable = false, unique = true)
    private String citizenId;

    @Nationalized
    @Column(name = "JobTitle", length = 100)
    private String jobTitle;

    @Column(name = "Salary")
    private BigDecimal salary;

    @Column(name = "HireDate")
    private LocalDate hireDate = LocalDate.now();
}