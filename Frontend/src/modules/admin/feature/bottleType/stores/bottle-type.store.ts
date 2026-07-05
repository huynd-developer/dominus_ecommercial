import { defineStore } from 'pinia';
import { bottleTypeService } from '../services/bottle-type.service';
import type { BottleType, BottleTypeRequest } from '../types/bottle-type.type';

export const useBottleTypeStore = defineStore('bottleTypeStore', {
  state: () => ({
    bottleTypes: [] as BottleType[],
    isLoading: false,
    currentPage: 0, // Spring Boot bắt đầu từ 0
    totalPages: 0,  
    pageSize: 5     // Số item trên 1 trang
  }),
  actions: {
    // ĐÃ SỬA: Nhận đầy đủ tham số keyword và page để phục vụ tìm kiếm
    async fetchBottleTypes(keyword: string = '', page: number = 0) {
      this.isLoading = true;
      try {
        const response = await bottleTypeService.getAll({ 
          // ĐÃ SỬA: Gửi tham số keyword lên Backend
          params: { keyword, page, size: this.pageSize } 
        });
        
        const resData = response.data;

        // BÓC TÁCH DỮ LIỆU THÔNG MINH (Đã cập nhật đọc chuẩn object "page" của Spring Boot mới)
        if (resData && resData.content) {
          this.bottleTypes = resData.content; 
          this.totalPages = resData.page?.totalPages ?? resData.totalPages ?? 1;
          this.currentPage = resData.page?.number ?? resData.number ?? 0;
        } 
        else if (resData && resData.data && resData.data.content) {
          this.bottleTypes = resData.data.content;
          this.totalPages = resData.data.page?.totalPages ?? resData.data.totalPages ?? 1;
          this.currentPage = resData.data.page?.number ?? resData.data.number ?? 0;
        } 
        else if (Array.isArray(resData)) {
          this.bottleTypes = resData;
          this.totalPages = 1;
          this.currentPage = 0;
        } 
        else if (resData && resData.data && Array.isArray(resData.data)) {
          this.bottleTypes = resData.data;
          this.totalPages = 1;
          this.currentPage = 0;
        } 
        else {
          console.log("Cấu trúc dữ liệu API lạ, hãy kiểm tra:", resData);
          this.bottleTypes = [];
        }

      } catch (error) {
        console.error("Lỗi khi tải danh sách loại chai:", error);
        this.bottleTypes = [];
      } finally {
        this.isLoading = false;
      }
    },

    // Thêm mới loại chai + validation 400
    async createBottleType(data: BottleTypeRequest) {
      try {
        await bottleTypeService.create(data);
        // ĐÃ SỬA: Truyền keyword là chuỗi rỗng '' khi gọi lại fetch sau khi thêm
        await this.fetchBottleTypes('', 0); 
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
        throw new Error(error.response?.data?.message || "Lỗi hệ thống khi thêm loại chai!");
      }
    },

    // Cập nhật loại chai + validation 400
    async updateBottleType(id: number, data: BottleTypeRequest) {
      try {
        await bottleTypeService.update(id, data);
        // ĐÃ SỬA: Truyền keyword là chuỗi rỗng '' để fetch đúng signature
        await this.fetchBottleTypes('', this.currentPage); 
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
        throw new Error(error.response?.data?.message || "Lỗi hệ thống khi cập nhật loại chai!");
      }
    },

    // Xóa loại chai
    async deleteBottleType(id: number) {
      try {
        await bottleTypeService.delete(id);
        // ĐÃ SỬA: Truyền keyword là chuỗi rỗng ''
        await this.fetchBottleTypes('', this.currentPage); 
      } catch (error) {
         console.error("Lỗi khi xóa loại chai:", error);
         throw new Error("Không thể xóa loại chai này!");
      }
    }
  }
});