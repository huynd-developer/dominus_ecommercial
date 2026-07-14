package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    /*
     * Dùng cho POS tìm khách theo SĐT.
     * Chỉ lấy khách đang hoạt động, chưa bị xóa mềm.
     */
    @EntityGraph(attributePaths = {"user", "user.role"})
    @Query("""
            SELECT c
            FROM Customer c
            JOIN c.user u
            WHERE u.phone = :phone
            AND u.status = 1
            AND (u.isDeleted = false OR u.isDeleted IS NULL)
            """)
    Optional<Customer> findByUserPhone(@Param("phone") String phone);

    /*
     * Dùng cho POS xử lý case:
     * - Không tìm thấy theo SĐT
     * - Nhưng email đã thuộc khách hàng cũ
     * => dùng lại customer đó thay vì báo Email đã tồn tại.
     */
    @EntityGraph(attributePaths = {"user", "user.role"})
    @Query("""
            SELECT c
            FROM Customer c
            JOIN c.user u
            WHERE LOWER(u.email) = LOWER(:email)
            AND u.status = 1
            AND (u.isDeleted = false OR u.isDeleted IS NULL)
            """)
    Optional<Customer> findByUserEmailIgnoreCase(@Param("email") String email);

    /*
     * Giữ lại cho code cũ nếu còn gọi findByPhone().
     */
    @EntityGraph(attributePaths = {"user", "user.role"})
    @Query("""
            SELECT c
            FROM Customer c
            JOIN c.user u
            WHERE u.phone = :phone
            AND u.status = 1
            AND (u.isDeleted = false OR u.isDeleted IS NULL)
            """)
    Optional<Customer> findByPhone(@Param("phone") String phone);

    @EntityGraph(attributePaths = {"user", "user.role"})
    @Query("""
            SELECT c
            FROM Customer c
            WHERE (c.user.isDeleted = false OR c.user.isDeleted IS NULL)
            AND (:status IS NULL OR c.user.status = :status)
            AND (
                :keyword IS NULL OR :keyword = ''
                OR LOWER(c.user.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.user.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR c.user.phone LIKE CONCAT('%', :keyword, '%')
                OR LOWER(c.customerRank) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<Customer> searchCustomers(
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user", "user.role"})
    @Query("""
            SELECT c
            FROM Customer c
            WHERE c.userId = :userId
            AND (c.user.isDeleted = false OR c.user.isDeleted IS NULL)
            """)
    Optional<Customer> findActiveByUserId(@Param("userId") Integer userId);

    @EntityGraph(attributePaths = {"user", "user.role"})
    Optional<Customer> findByUserId(Integer userId);
}