package org.example.datn_sd69.modules.customer.service;

import org.example.datn_sd69.modules.customer.dto.response.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    Page<CustomerResponse> getCustomers(String keyword, Integer status, Pageable pageable);

    CustomerResponse getCustomerById(Integer userId);

    CustomerResponse updateStatus(Integer userId, Integer status);
}
