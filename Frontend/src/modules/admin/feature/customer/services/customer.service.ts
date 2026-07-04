import api from "@/common/api";

import type {
  CustomerResponse,
  CustomerSearchParams,
  PageResponse,
} from "../types/customer.types";

const buildParams = (params: CustomerSearchParams) => {
  const result: Record<string, string | number> = {};

  if (params.keyword && params.keyword.trim() !== "") {
    result.keyword = params.keyword.trim();
  }

  if (params.status !== null && params.status !== undefined) {
    result.status = params.status;
  }

  result.page = params.page ?? 0;
  result.size = params.size ?? 10;

  return result;
};

export const customerService = {
  getCustomers(params: CustomerSearchParams) {
    return api.get<PageResponse<CustomerResponse>>("/admin/customers", {
      params: buildParams(params),
    });
  },

  getCustomerById(userId: number) {
    return api.get<CustomerResponse>(`/admin/customers/${userId}`);
  },

  updateStatus(userId: number, status: number) {
    return api.patch<CustomerResponse>(
      `/admin/customers/${userId}/status`,
      null,
      {
        params: { status },
      }
    );
  },
};