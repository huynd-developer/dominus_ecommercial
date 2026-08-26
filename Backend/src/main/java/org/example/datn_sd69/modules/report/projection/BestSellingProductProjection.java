package org.example.datn_sd69.modules.report.projection;

import java.math.BigDecimal;

public interface BestSellingProductProjection {

    Integer getProductId();

    String getProductName();

    String getBrandName();

    String getCapacityName(); // <-- Bổ sung dòng này để JPA Projection nhận diện được cột capacityName từ SQL

    Long getTotalSold();

    BigDecimal getRevenue();

    String getImageUrl();
}