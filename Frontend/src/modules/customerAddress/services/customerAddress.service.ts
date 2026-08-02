import api from "@/common/api";
import type { CustomerAddress } from "../types/address.type";

class CustomerAddressService {
  private baseUrl = "/api/customer/addresses";

  getAddresses(customerId: number) {
    return api.get<CustomerAddress[]>(this.baseUrl, { params: { customerId } });
  }

  addAddress(customerId: number, data: CustomerAddress) {
    return api.post<CustomerAddress>(this.baseUrl, data, { params: { customerId } });
  }

  updateAddress(customerId: number, id: number, data: CustomerAddress) {
    return api.put<CustomerAddress>(`${this.baseUrl}/${id}`, data, { params: { customerId } });
  }

  deleteAddress(customerId: number, id: number) {
    return api.delete(`${this.baseUrl}/${id}`, { params: { customerId } });
  }
}

export default new CustomerAddressService();