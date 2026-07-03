import { defineStore } from 'pinia';
import { fragranceFamilyService } from '../services/fragrance-family.service';
import type { FragranceFamily, FragranceFamilyRequest } from '../types/fragrance-family.type';

export const useFragranceFamilyStore = defineStore('fragranceFamilyStore', {
  state: () => ({
    fragranceFamilies: [] as FragranceFamily[],
    isLoading: false,
    currentPage: 0,
    totalPages: 0,  
    pageSize: 5     
  }),
  actions: {
    async fetchFragranceFamilies(keyword: string = '', page: number = 0) {
      this.isLoading = true;
      try {
        const response = await fragranceFamilyService.getAll({ 
          params: { keyword, page, size: this.pageSize } 
        });
        
        const resData = response.data;

        // Trường hợp 1: Dữ liệu phân trang chuẩn (có object 'page' của Spring Boot mới)
        if (resData && resData.content) {
          this.fragranceFamilies = resData.content; 
          // 👇 Đã cập nhật đọc biến page
          this.totalPages = resData.page?.totalPages ?? resData.totalPages ?? 1;
          this.currentPage = resData.page?.number ?? resData.number ?? 0;
        } 
        // Trường hợp 2: Dữ liệu bọc trong class ApiResponse
        else if (resData && resData.data && resData.data.content) {
          this.fragranceFamilies = resData.data.content;
          // 👇 Đã cập nhật đọc biến page
          this.totalPages = resData.data.page?.totalPages ?? resData.data.totalPages ?? 1;
          this.currentPage = resData.data.page?.number ?? resData.data.number ?? 0;
        } 
        // Trường hợp 3: Mảng thuần không phân trang
        else if (Array.isArray(resData)) {
          this.fragranceFamilies = resData;
          this.totalPages = 1;
          this.currentPage = 0;
        } 
        // Trường hợp 4: Mảng thuần nằm trong mục data
        else if (resData && resData.data && Array.isArray(resData.data)) {
          this.fragranceFamilies = resData.data;
          this.totalPages = 1;
          this.currentPage = 0;
        } 
        else {
          this.fragranceFamilies = [];
        }

      } catch (error) {
        console.error("Lỗi khi tải danh sách nhóm hương:", error);
        this.fragranceFamilies = [];
      } finally {
        this.isLoading = false;
      }
    },

    async createFragranceFamily(data: FragranceFamilyRequest) {
      try {
        await fragranceFamilyService.create(data);
        await this.fetchFragranceFamilies('', 0); 
      } catch (error: any) {
        if (error.response && error.response.status === 400) {
          const errorData = error.response.data;
          if (typeof errorData === 'string') {
            throw new Error(errorData); 
          } else if (typeof errorData === 'object') {
            const firstErrorMessage = Object.values(errorData)[0] as string;
            throw new Error(firstErrorMessage);
          }
        }
        throw new Error(error.response?.data?.message || "Lỗi hệ thống khi thêm nhóm hương!");
      }
    },

    async updateFragranceFamily(id: number, data: FragranceFamilyRequest) {
      try {
        await fragranceFamilyService.update(id, data);
        await this.fetchFragranceFamilies('', this.currentPage); 
      } catch (error: any) {
        if (error.response && error.response.status === 400) {
          const errorData = error.response.data;
          if (typeof errorData === 'string') {
            throw new Error(errorData); 
          } else if (typeof errorData === 'object') {
            const firstErrorMessage = Object.values(errorData)[0] as string;
            throw new Error(firstErrorMessage);
          }
        }
        throw new Error(error.response?.data?.message || "Lỗi hệ thống khi cập nhật nhóm hương!");
      }
    },

    async deleteFragranceFamily(id: number) {
      try {
        await fragranceFamilyService.delete(id);
        await this.fetchFragranceFamilies('', this.currentPage); 
      } catch (error) {
         throw new Error("Không thể xóa nhóm hương này!");
      }
    }
  }
});