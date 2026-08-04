import api from "@/common/api";
import type { CustomerAddress } from "../types/address.type";

class CustomerAddressService {
  private baseUrl = "/customer/addresses";

  getAddresses(customerId: number | string) {
    const id = Number(customerId);
    return api.get<CustomerAddress[]>(this.baseUrl, { 
      params: { customerId: isNaN(id) ? undefined : id } 
    });
  }

  addAddress(customerId: number | string, data: CustomerAddress) {
    const id = Number(customerId);
    return api.post<CustomerAddress>(this.baseUrl, data, { 
      params: { customerId: isNaN(id) ? undefined : id } 
    });
  }

  updateAddress(customerId: number | string, id: number, data: CustomerAddress) {
    const cId = Number(customerId);
    return api.put<CustomerAddress>(`${this.baseUrl}/${id}`, data, { 
      params: { customerId: isNaN(cId) ? undefined : cId } 
    });
  }

  deleteAddress(customerId: number | string, id: number) {
    const cId = Number(customerId);
    return api.delete(`${this.baseUrl}/${id}`, { 
      params: { customerId: isNaN(cId) ? undefined : cId } 
    });
  }
}

export default new CustomerAddressService();