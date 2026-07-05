import { defineStore } from "pinia";
import { employeeService } from "../services/employee.service";
import type {
  EmployeeCreateRequest,
  EmployeeResponse,
  EmployeeStatus,
  EmployeeUpdateRequest,
} from "../types/employee.type";

function getApiErrorMessage(error: any): string {
  const data = error?.response?.data;

  if (data?.errors) {
    return Object.values(data.errors).join("\n");
  }

  if (data?.message) {
    return data.message;
  }

  return "Có lỗi xảy ra. Vui lòng thử lại.";
}

export const useEmployeeStore = defineStore("employeeStore", {
  state: () => ({
    employees: [] as EmployeeResponse[],
    loading: false,
    error: "",

    keyword: "",
    status: null as EmployeeStatus | null,

    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  }),

  actions: {
    async fetchEmployees() {
      this.loading = true;
      this.error = "";

      try {
        const res = await employeeService.getAll({
          keyword: this.keyword,
          status: this.status,
          page: this.page,
          size: this.size,
        });

        const data: any = res.data;

        /**
         * BE Spring Page trả dạng:
         * {
         *   content: [],
         *   number: 0,
         *   size: 10,
         *   totalElements: 5,
         *   totalPages: 1
         * }
         *
         * Nhưng nếu BE trả List thường thì vẫn không làm FE bị undefined.
         */
        this.employees = Array.isArray(data) ? data : data.content || [];

        this.page = Number(data.number ?? this.page ?? 0);
        this.size = Number(data.size ?? this.size ?? 10);
        this.totalElements = Number(data.totalElements ?? this.employees.length ?? 0);
        this.totalPages = Number(data.totalPages ?? 1);

        if (this.totalPages < 1) {
          this.totalPages = 1;
        }
      } catch (error: any) {
        this.error = getApiErrorMessage(error);
        this.employees = [];
        this.totalElements = 0;
        this.totalPages = 1;
        throw error;
      } finally {
        this.loading = false;
      }
    },

    async createEmployee(payload: EmployeeCreateRequest) {
      this.loading = true;
      this.error = "";

      try {
        await employeeService.create(payload);
        await this.fetchEmployees();
      } catch (error: any) {
        this.error = getApiErrorMessage(error);
        throw error;
      } finally {
        this.loading = false;
      }
    },

    async updateEmployee(userId: number, payload: EmployeeUpdateRequest) {
      this.loading = true;
      this.error = "";

      try {
        await employeeService.update(userId, payload);
        await this.fetchEmployees();
      } catch (error: any) {
        this.error = getApiErrorMessage(error);
        throw error;
      } finally {
        this.loading = false;
      }
    },

    async lockEmployee(userId: number) {
      this.loading = true;
      this.error = "";

      try {
        await employeeService.lock(userId);
        await this.fetchEmployees();
      } catch (error: any) {
        this.error = getApiErrorMessage(error);
        throw error;
      } finally {
        this.loading = false;
      }
    },

    async unlockEmployee(userId: number) {
      this.loading = true;
      this.error = "";

      try {
        await employeeService.unlock(userId);
        await this.fetchEmployees();
      } catch (error: any) {
        this.error = getApiErrorMessage(error);
        throw error;
      } finally {
        this.loading = false;
      }
    },

    async deleteEmployee(userId: number) {
      this.loading = true;
      this.error = "";

      try {
        await employeeService.deleteSoft(userId);

        if (this.employees.length === 1 && this.page > 0) {
          this.page -= 1;
        }

        await this.fetchEmployees();
      } catch (error: any) {
        this.error = getApiErrorMessage(error);
        throw error;
      } finally {
        this.loading = false;
      }
    },

    setPage(page: number) {
      this.page = Math.max(Number(page) || 0, 0);
    },

    setKeyword(keyword: string) {
      this.keyword = keyword || "";
      this.page = 0;
    },

    setStatus(status: EmployeeStatus | null) {
      this.status = status;
      this.page = 0;
    },
  },
});