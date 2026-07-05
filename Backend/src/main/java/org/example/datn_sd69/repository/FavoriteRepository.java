package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Integer> {

    List<Favorite> findByCustomer_UserIdOrderByCreatedAtDesc(Integer customerId);

    boolean existsByCustomer_UserIdAndProductVariant_Id(Integer customerId, Integer productVariantId);

    Optional<Favorite> findByIdAndCustomer_UserId(Integer favoriteId, Integer customerId);

    Optional<Favorite> findByCustomer_UserIdAndProductVariant_Id(Integer customerId, Integer productVariantId);


}