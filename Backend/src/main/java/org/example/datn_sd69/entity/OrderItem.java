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

import java.math.BigDecimal;

@Entity
@Table(name = "OrderItem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "OrderId", nullable = false)
    private Order order;

    /**
     * DB đang để ProductVariantId NULL ON DELETE SET NULL.
     * Vì vậy entity không được nullable = false.
     * Khi sản phẩm/biến thể bị xóa, đơn hàng cũ vẫn còn snapshot bên dưới:
     * productName, sku, capacityName, bottleTypeName, image, giá...
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "ProductVariantId", nullable = true)
    private ProductVariant productVariant;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    /**
     * Giá gốc của 1 sản phẩm tại thời điểm mua.
     * Không nhân quantity.
     */
    @Column(name = "OriginalPrice", nullable = false, precision = 18, scale = 2)
    private BigDecimal originalPrice = BigDecimal.ZERO;

    /**
     * Số tiền giảm Flash Sale của 1 sản phẩm.
     * Không chứa voucher toàn đơn.
     */
    @Column(name = "DiscountAmount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * Giá sau giảm của 1 sản phẩm.
     * Công thức đúng:
     * FinalPrice = OriginalPrice - DiscountAmount
     *
     * Tổng dòng = FinalPrice * Quantity
     */
    @Column(name = "FinalPrice", nullable = false, precision = 18, scale = 2)
    private BigDecimal finalPrice = BigDecimal.ZERO;

    @Nationalized
    @Column(name = "Note", length = 255)
    private String note;

    @Column(name = "Image", length = 500)
    private String image;

    /**
     * Snapshot thông tin sản phẩm tại thời điểm mua.
     * Dùng để đơn hàng cũ vẫn hiển thị được dù ProductVariant bị xóa/sửa.
     */
    @Nationalized
    @Column(name = "ProductName", length = 255)
    private String productName;

    @Column(name = "Sku", length = 100)
    private String sku;

    @Nationalized
    @Column(name = "CapacityName", length = 50)
    private String capacityName;

    @Nationalized
    @Column(name = "BottleTypeName", length = 255)
    private String bottleTypeName;

    /**
     * Snapshot thông tin logistics tại thời điểm đặt hàng.
     */
    @Column(name = "WeightGram")
    private Integer weightGram;

    @Column(name = "LengthCm", precision = 8, scale = 2)
    private BigDecimal lengthCm;

    @Column(name = "WidthCm", precision = 8, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "HeightCm", precision = 8, scale = 2)
    private BigDecimal heightCm;
}