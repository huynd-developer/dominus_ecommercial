import api from "@/common/api";
import type { Order } from "../types/order";
import type { OrderDetail } from "../types/OrderDetail";

const API = "/admin/orders";

export interface OrderPage {
  content: Order[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface SearchOrderParams {
  keyword?: string;
  status?: number;
  orderType?: string;
  page?: number;
  size?: number;
}

class OrderAdminService {

  search(params: SearchOrderParams) {
    return api.get<OrderPage>(API,{
    params
});
  }

  getDetail(id: number) {
    return api.get<OrderDetail>(`${API}/${id}`);
  }

  updateStatus(id: number, status: number) {
  return api.put(
    `${API}/${id}/status`,
    {
      status
    }
  );
}

cancel(id: number) {
  return api.put(
    `${API}/${id}/cancel`
  );
}

}

export default new OrderAdminService();