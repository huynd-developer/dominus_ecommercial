package org.example.datn_sd69.modules.product.service;

import org.example.datn_sd69.modules.product.dto.response.AiProductCompareResponse;

import java.util.List;

public interface AiProductCompareService {

    AiProductCompareResponse compareProducts(
            List<Integer> productIds
    );
}