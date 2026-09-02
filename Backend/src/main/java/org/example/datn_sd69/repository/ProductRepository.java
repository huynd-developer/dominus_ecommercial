package org.example.datn_sd69.repository;

import jakarta.persistence.LockModeType;
import org.example.datn_sd69.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, Integer> {

    Page<Product> findByIsDeletedFalse(
            Pageable pageable);

    /*
     * Chỉ dùng cho màn/tab sản phẩm đã xóa mềm.
     * Không ảnh hưởng danh sách sản phẩm đang hoạt động hiện tại.
     */
    Page<Product> findByIsDeletedTrue(
            Pageable pageable);

    Page<Product> findByStatusAndIsDeletedFalse(
            Integer status,
            Pageable pageable);

    boolean existsByBrandIdAndIsDeletedFalse(Integer brandId);

    boolean existsByCategoryIdAndIsDeletedFalse(Integer categoryId);

    boolean existsByConcentrationIdAndIsDeletedFalse(Integer concentrationId);

    /**
     * Row lock chung cho mutation Product/SKU và trạng thái ảnh của cùng Product.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM Product p
        WHERE p.id = :id
    """)
    Optional<Product> findByIdForUpdate(
            @Param("id") Integer id
    );

    // Kiểm tra xem có sản phẩm nào đang sử dụng nhóm hương này không
    // (bỏ qua sản phẩm đã xóa)
    boolean existsByFragranceFamilies_IdAndIsDeletedFalse(
            Integer fragranceFamilyId
    );

    boolean existsByConcentrationId(Integer concentrationId);

    /**
     * Kiểm tra tên sản phẩm đã được sử dụng bởi một Product chưa xóa mềm hay chưa.
     *
     * Không quan tâm Status = đang bán / ngừng bán.
     * Chỉ cần IsDeleted = false thì vẫn được xem là Product đang tồn tại trong hệ thống.
     */
    @Query("""
    SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
    FROM Product p
    WHERE p.isDeleted = false
      AND LOWER(TRIM(p.name)) = LOWER(TRIM(:name))
""")
    boolean existsActiveByName(
            @Param("name") String name
    );

    /**
     * Dùng cho UPDATE / RESTORE.
     *
     * Loại trừ chính Product đang thao tác để tránh trường hợp
     * Product tự trùng tên với chính nó.
     */
    @Query("""
    SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
    FROM Product p
    WHERE p.isDeleted = false
      AND p.id <> :excludeId
      AND LOWER(TRIM(p.name)) = LOWER(TRIM(:name))
""")
    boolean existsOtherActiveByName(
            @Param("name") String name,
            @Param("excludeId") Integer excludeId
    );
}