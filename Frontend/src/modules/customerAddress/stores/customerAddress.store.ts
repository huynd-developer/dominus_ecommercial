import { defineStore } from "pinia";
import { ref } from "vue";
import customerAddressService from "../services/customerAddress.service";
import type { CustomerAddress } from "../types/address.type";

export const useCustomerAddressStore = defineStore("customerAddress", () => {
  const addresses = ref<CustomerAddress[]>([]);
  const loading = ref(false);

  const fetchAddresses = async (customerId: number) => {
    try {
      loading.value = true;
      const res = await customerAddressService.getAddresses(customerId);
      addresses.value = Array.isArray(res.data) ? res.data : [];
    } catch (error) {
      console.error("Lỗi tải danh sách địa chỉ:", error);
      addresses.value = [];
    } finally {
      loading.value = false;
    }
  };

  const saveAddressData = async (customerId: number, data: CustomerAddress, id: number | null = null) => {
    try {
      loading.value = true;
      if (id) {
        await customerAddressService.updateAddress(customerId, id, data);
      } else {
        await customerAddressService.addAddress(customerId, data);
      }
      await fetchAddresses(customerId);
      return true;
    } catch (error) {
      console.error("Lỗi lưu địa chỉ:", error);
      return false;
    } finally {
      loading.value = false;
    }
  };

  const removeAddressData = async (customerId: number, id: number) => {
    try {
      loading.value = true;
      await customerAddressService.deleteAddress(customerId, id);
      addresses.value = addresses.value.filter((a) => a.id !== id);
      return true;
    } catch (error) {
      console.error("Lỗi xóa địa chỉ:", error);
      return false;
    } finally {
      loading.value = false;
    }
  };

  return {
    addresses,
    loading,
    fetchAddresses,
    saveAddressData,
    removeAddressData,
  };
});