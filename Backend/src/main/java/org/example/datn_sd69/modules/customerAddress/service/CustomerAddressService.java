package org.example.datn_sd69.modules.customerAddress.service;


import org.example.datn_sd69.entity.CustomerAddress;
import org.example.datn_sd69.modules.customerAddress.dto.CustomerAddressRequest;

import java.util.List;

public interface CustomerAddressService {
    List<CustomerAddress> getAddressesByCustomerId(Integer customerId);
    CustomerAddress addAddress(Integer customerId, CustomerAddressRequest request);
    CustomerAddress updateAddress(Integer customerId, Integer addressId, CustomerAddressRequest request);
    void deleteAddress(Integer customerId, Integer addressId);
}