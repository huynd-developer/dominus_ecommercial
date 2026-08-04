package org.example.datn_sd69.modules.customerAddress.controller;


import org.example.datn_sd69.entity.CustomerAddress;
import org.example.datn_sd69.modules.customerAddress.dto.CustomerAddressRequest;
import org.example.datn_sd69.modules.customerAddress.service.CustomerAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/addresses")
@RequiredArgsConstructor
public class CustomerAddressController {

    private final CustomerAddressService addressService;

    @GetMapping
    public ResponseEntity<List<CustomerAddress>> getAddresses(@RequestParam Integer customerId) {
        return ResponseEntity.ok(addressService.getAddressesByCustomerId(customerId));
    }

    @PostMapping
    public ResponseEntity<CustomerAddress> addAddress(
            @RequestParam Integer customerId,
            @RequestBody CustomerAddressRequest request) {
        return ResponseEntity.ok(addressService.addAddress(customerId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerAddress> updateAddress(
            @RequestParam Integer customerId,
            @PathVariable Integer id,
            @RequestBody CustomerAddressRequest request) {
        return ResponseEntity.ok(addressService.updateAddress(customerId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(
            @RequestParam Integer customerId,
            @PathVariable Integer id) {
        addressService.deleteAddress(customerId, id);
        return ResponseEntity.ok().build();
    }
}