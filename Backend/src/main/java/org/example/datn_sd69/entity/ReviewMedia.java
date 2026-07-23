package org.example.datn_sd69.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "ReviewMedia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReviewId", nullable = false)
    private Review review;

    @Column(name = "MediaUrl", nullable = false, length = 500)
    private String mediaUrl;

    @Column(name = "MediaType", length = 50)
    private String mediaType = "image";

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}