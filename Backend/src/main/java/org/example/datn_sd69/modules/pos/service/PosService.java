package org.example.datn_sd69.modules.pos.service;

import org.example.datn_sd69.modules.pos.dto.CustomerPosResponse;
import org.example.datn_sd69.modules.pos.dto.PosCheckoutRequest;
import org.example.datn_sd69.modules.pos.dto.PosOrderResponse;
import org.example.datn_sd69.modules.pos.dto.ProductVariantPosResponse;

public interface PosService {

    ProductVariantPosResponse findVariantBySku(String sku);

    CustomerPosResponse findCustomerByPhone(String phone);

    PosOrderResponse checkout(PosCheckoutRequest request, String cashierEmail, String clientIp);
}