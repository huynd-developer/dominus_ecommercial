import { defineStore } from 'pinia';
import { capacityService } from '../services/capacity.service';
import type { Capacity, CapacityRequest } from '../types/capacity.type';

export const useCapacityStore = defineStore('capacityStore', {
  state: () => ({
    capacities: [] as Capacity[],
    isLoading: false,
    currentPage: 0,
    totalPages: 0, 
    pageSize: 5  
  }),
  actions: {
    async fetchCapacities(keyword: string = '', page: number = 0) {
      this.isLoading = true;
      try {
        const response = await capacityService.getAll({ 
          params: { keyword, page, size: this.pageSize } 
        });
        
        const resData = response.data;

        // BÓC TÁCH DỮ LIỆU THÔNG MINH ĐÃ CẬP NHẬT OBJECT "page"
        if (resData && resData.content) {
          this.capacities = resData.content; 
          this.totalPages = resData.page?.totalPages ?? resData.totalPages ?? 1;
          this.currentPage = resData.page?.number ?? resData.number ?? 0;
        } 
        else if (resData && resData.data && resData.data.content) {
          this.capacities = resData.data.content;
          this.totalPages = resData.data.page?.totalPages ?? resData.data.totalPages ?? 1;
          this.currentPage = resData.data.page?.number ?? resData.data.number ?? 0;
        } 
        else if (Array.isArray(resData)) {
          this.capacities = resData;
          this.totalPages = 1;
          this.currentPage = 0;
        } 
        else if (resData && resData.data && Array.isArray(resData.data)) {
          this.capacities = resData.data;
          this.totalPages = 1;
          this.currentPage = 0;
        } 
        else {
          this.capacities = [];
        }
      } catch (error) {
        console.error("Lỗi khi tải danh sách dung tích:", error);
        this.capacities = [];
      } finally {
        this.isLoading = false;
      }
    },

    async createCapacity(data: CapacityRequest) {
      try {
        await capacityService.create(data);
        await this.fetchCapacities('', 0); 
      } catch (error: any) {
        if (error.response && error.response.status === 400) {
          const errorData = error.response.data;
          const targetError = errorData?.message || errorData;
          if (typeof targetError === 'string') throw new Error(targetError);
          if (typeof targetError === 'object') throw new Error(Object.values(targetError)[0] as string);
        }
        throw new Error("Lỗi hệ thống khi thêm dung tích!");
      }
    },

    async updateCapacity(id: number, data: CapacityRequest) {
      try {
        await capacityService.update(id, data);
        await this.fetchCapacities('', this.currentPage); 
      } catch (error: any) {
        if (error.response && error.response.status === 400) {
          const errorData = error.response.data;
          const targetError = errorData?.message || errorData;
          if (typeof targetError === 'string') throw new Error(targetError);
          if (typeof targetError === 'object') throw new Error(Object.values(targetError)[0] as string);
        }
        throw new Error("Lỗi hệ thống khi cập nhật dung tích!");
      }
    },

    async deleteCapacity(id: number) {
      try {
        await capacityService.delete(id);
        await this.fetchCapacities('', this.currentPage);
      } catch (error: any) {
        throw new Error(error.response?.data?.message || error.response?.data || "Không thể xóa dung tích này!");
      }
    }
  }
});