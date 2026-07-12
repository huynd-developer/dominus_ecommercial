package org.example.datn_sd69.modules.favorite.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddFavoriteRequest(

        @NotNull(message = "productVariantId không được để trống")
        @Positive(message = "productVariantId phải là số nguyên dương")
        Integer productVariantId
) {
}