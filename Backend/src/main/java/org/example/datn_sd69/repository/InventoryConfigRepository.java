package org.example.datn_sd69.repository;

import jakarta.persistence.LockModeType;
import org.example.datn_sd69.entity.InventoryConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryConfigRepository
        extends JpaRepository<InventoryConfig, Byte> {

    /**
     * Khóa đúng dòng cấu hình kho trong lúc UPDATE để hai request đồng thời
     * không cùng đọc một state cũ rồi ghi đè lẫn nhau.
     *
     * Không ảnh hưởng các API GET vì findById(...) cũ vẫn giữ nguyên.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c
            FROM InventoryConfig c
            WHERE c.id = :id
            """)
    Optional<InventoryConfig> findByIdForUpdate(
            @Param("id") Byte id
    );
}