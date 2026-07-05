package org.example.datn_sd69.modules.report.projection;

import java.math.BigDecimal;

public interface BestSellingProductProjection {

    Integer getProductId();

    String getProductName();

    String getBrandName();

    Long getTotalSold();

    BigDecimal getRevenue();

    String getImageUrl();
}