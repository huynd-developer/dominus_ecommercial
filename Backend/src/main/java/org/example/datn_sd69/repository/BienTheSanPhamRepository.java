package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.BienTheSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BienTheSanPhamRepository extends JpaRepository<BienTheSanPham, Long> {
    // Hiện tại chỉ cần các hàm CRUD cơ bản (như findById) có sẵn từ JpaRepository là đủ dùng cho Service rồi.
    // Sau này nếu cần tìm biến thể theo mã, kích thước, hoặc theo sản phẩm cha, bạn sẽ viết thêm ở đây.
}
