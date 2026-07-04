import { defineStore } from "pinia";
import { customerService } from "../services/customer.service";
import type { CustomerResponse } from "../types/customer.types";

interface CustomerState {
  customers: CustomerResponse[];
  selectedCustomer: CustomerResponse | null;

  keyword: string;
  status: number | null;

  page: number;
  size: number;
  totalPages: number;
  totalElements: number;

  isLoading: boolean;
  isDetailLoading: boolean;
  errorMessage: string;
}

const getErrorMessage = (error: unknown): string => {
  const err = error as {
    response?: {
      data?: {
        message?: string;
      };
    };
    message?: string;
  };

  return (
    err.response?.data?.message ||
    err.message ||
    "Có lỗi xảy ra, vui lòng thử lại"
  );
};

export const useCustomerStore = defineStore("customerStore", {
  state: (): CustomerState => ({
    customers: [],
    selectedCustomer: null,

    keyword: "",
    status: null,

    page: 0,

    // Để dễ thấy phân trang, để 5.
    // Nếu muốn mặc định 10 dòng/trang thì đổi lại thành 10.
    size: 5,

    totalPages: 0,
    totalElements: 0,

    isLoading: false,
    isDetailLoading: false,
    errorMessage: "",
  }),

  actions: {
    async fetchCustomers() {
      this.isLoading = true;
      this.errorMessage = "";

      try {
        const response = await customerService.getCustomers({
          keyword: this.keyword,
          status: this.status,
          page: this.page,
          size: this.size,
        });

        const pageData = response.data;

        this.customers = Array.isArray(pageData.content)
          ? pageData.content
          : [];

        this.totalElements =
          pageData.totalElements ?? this.customers.length;

        this.totalPages =
          pageData.totalPages ??
          Math.ceil(this.totalElements / this.size);

        this.page = pageData.number ?? this.page;
        this.size = pageData.size ?? this.size;
      } catch (error) {
        this.customers = [];
        this.totalPages = 0;
        this.totalElements = 0;
        this.errorMessage = getErrorMessage(error);
      } finally {
        this.isLoading = false;
      }
    },

    async searchCustomers() {
      this.page = 0;
      await this.fetchCustomers();
    },

    async resetFilter() {
      this.keyword = "";
      this.status = null;
      this.page = 0;
      await this.fetchCustomers();
    },

    async changePage(page: number) {
      if (page < 0 || page >= this.totalPages || page === this.page) {
        return;
      }

      this.page = page;
      await this.fetchCustomers();
    },

    async changeSize(size: number) {
      this.size = size;
      this.page = 0;
      await this.fetchCustomers();
    },

    async fetchCustomerById(userId: number) {
      this.isDetailLoading = true;
      this.errorMessage = "";

      try {
        const response = await customerService.getCustomerById(userId);
        this.selectedCustomer = response.data;
      } catch (error) {
        this.selectedCustomer = null;
        this.errorMessage = getErrorMessage(error);
      } finally {
        this.isDetailLoading = false;
      }
    },

    async updateStatus(userId: number, status: number) {
      this.isLoading = true;
      this.errorMessage = "";

      try {
        const response = await customerService.updateStatus(userId, status);

        this.customers = this.customers.map((customer) =>
          customer.userId === userId ? response.data : customer
        );

        if (this.selectedCustomer?.userId === userId) {
          this.selectedCustomer = response.data;
        }
      } catch (error) {
        this.errorMessage = getErrorMessage(error);
        throw error;
      } finally {
        this.isLoading = false;
      }
    },

    clearSelectedCustomer() {
      this.selectedCustomer = null;
    },
  },
});