package org.example.datn_sd69.modules.customerAddress.service.impl;


import org.example.datn_sd69.entity.CustomerAddress;
import org.example.datn_sd69.modules.customerAddress.dto.CustomerAddressRequest;
import org.example.datn_sd69.modules.customerAddress.service.CustomerAddressService;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.repository.CustomerAddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerAddressServiceImpl implements CustomerAddressService {

    private final CustomerAddressRepository addressRepository;

    @Override
    public List<CustomerAddress> getAddressesByCustomerId(Integer customerId) {
        return addressRepository.findByCustomerIdAndIsDeletedFalseOrderByIsDefaultDescCreatedAtDesc(customerId);
    }

    @Override
    @Transactional
    public CustomerAddress addAddress(Integer customerId, CustomerAddressRequest request) {
        List<CustomerAddress> existing = addressRepository.findByCustomerIdAndIsDeletedFalseOrderByIsDefaultDescCreatedAtDesc(customerId);

        boolean shouldBeDefault = existing.isEmpty() || Boolean.TRUE.equals(request.getIsDefault());

        if (shouldBeDefault && !existing.isEmpty()) {
            existing.forEach(a -> {
                a.setIsDefault(false);
                addressRepository.save(a);
            });
        }

        String fullAddress = String.format("%s, %s, %s",
                request.getSpecificAddress(),
                request.getWardName(),
                request.getProvinceName());

        CustomerAddress address = CustomerAddress.builder()
                .customerId(customerId)
                .recipientName(request.getRecipientName())
                .phone(request.getPhone())
                .provinceCode(request.getProvinceCode())
                .provinceName(request.getProvinceName())
                .wardCode(request.getWardCode())
                .wardName(request.getWardName())
                .specificAddress(request.getSpecificAddress())
                .fullAddress(fullAddress)
                .isDefault(shouldBeDefault)
                .isDeleted(false)
                .build();

        return addressRepository.save(address);
    }

    @Override
    @Transactional
    public CustomerAddress updateAddress(Integer customerId, Integer addressId, CustomerAddressRequest request) {
        CustomerAddress address = addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            List<CustomerAddress> existing = addressRepository.findByCustomerIdAndIsDeletedFalseOrderByIsDefaultDescCreatedAtDesc(customerId);
            existing.forEach(a -> {
                a.setIsDefault(false);
                addressRepository.save(a);
            });
        }

        String fullAddress = String.format("%s, %s, %s",
                request.getSpecificAddress(),
                request.getWardName(),
                request.getProvinceName());

        address.setRecipientName(request.getRecipientName());
        address.setPhone(request.getPhone());
        address.setProvinceCode(request.getProvinceCode());
        address.setProvinceName(request.getProvinceName());
        address.setWardCode(request.getWardCode());
        address.setWardName(request.getWardName());
        address.setSpecificAddress(request.getSpecificAddress());
        address.setFullAddress(fullAddress);
        address.setIsDefault(request.getIsDefault());

        return addressRepository.save(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Integer customerId, Integer addressId) {
        CustomerAddress address = addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));
        address.setIsDeleted(true);
        addressRepository.save(address);
    }
}