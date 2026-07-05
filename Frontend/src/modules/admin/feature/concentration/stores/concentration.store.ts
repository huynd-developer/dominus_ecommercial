import { defineStore } from 'pinia';
import { concentrationService } from '../services/concentration.service';
import type { Concentration, ConcentrationRequest } from '../types/concentration.type';

export const useConcentrationStore = defineStore('concentrationStore', {
  state: () => ({
    concentrations: [] as Concentration[],
    isLoading: false,
    currentPage: 0,
    totalPages: 0,  
    pageSize: 5     
  }),
  actions: {
    async fetchConcentrations(keyword: string = '', page: number = 0) {
      this.isLoading = true;
      try {
        const response = await concentrationService.getAll({ 
          params: { keyword, page, size: this.pageSize } 
        });
        
        const resData = response.data;

        // Trường hợp 1: Data chuẩn không bị bọc (Cấu trúc trong ảnh F12 của bạn)
        if (resData && resData.content) {
          this.concentrations = resData.content; 
          // 👇 ĐÃ CHỈNH: Trỏ vào resData.page để lấy thông số phân trang
          this.totalPages = resData.page?.totalPages ?? resData.totalPages ?? 1;
          this.currentPage = resData.page?.number ?? resData.number ?? 0;
        } 
        // Trường hợp 2: Dữ liệu bị bọc bởi class ApiResponse (đề phòng module khác có dùng)
        else if (resData && resData.data && resData.data.content) {
          this.concentrations = resData.data.content;
          // 👇 ĐÃ CHỈNH: Trỏ vào resData.data.page
          this.totalPages = resData.data.page?.totalPages ?? resData.data.totalPages ?? 1;
          this.currentPage = resData.data.page?.number ?? resData.data.number ?? 0;
        } 
        // Trường hợp 3: Trả về một mảng thuần không phân trang
        else if (Array.isArray(resData)) {
          this.concentrations = resData;
          this.totalPages = 1;
          this.currentPage = 0;
        } 
        // Trường hợp 4: Mảng thuần nằm trong mục data
        else if (resData && resData.data && Array.isArray(resData.data)) {
          this.concentrations = resData.data;
          this.totalPages = 1;
          this.currentPage = 0;
        } 
        else {
          this.concentrations = [];
        }
      } catch (error) {
        console.error("Lỗi khi tải danh sách nồng độ:", error);
        this.concentrations = [];
      } finally {
        this.isLoading = false;
      }
    },
    async createConcentration(data: ConcentrationRequest) {
      try {
        await concentrationService.create(data);
        await this.fetchConcentrations('', 0); 
      } catch (error: any) {
        this.handleError(error, "Lỗi hệ thống khi thêm nồng độ!");
      }
    },

    async updateConcentration(id: number, data: ConcentrationRequest) {
      try {
        await concentrationService.update(id, data);
        await this.fetchConcentrations('', this.currentPage); 
      } catch (error: any) {
        this.handleError(error, "Lỗi hệ thống khi cập nhật nồng độ!");
      }
    },

    async deleteConcentration(id: number) {
      try {
        await concentrationService.delete(id);
        await this.fetchConcentrations('', this.currentPage); 
      } catch (error) {
         throw new Error("Không thể xóa nồng độ này!");
      }
    },

    handleError(error: any, defaultMessage: string) {
      if (error.response && error.response.status === 400) {
        const errorData = error.response.data;
        if (typeof errorData === 'string') {
          throw new Error(errorData); 
        } else if (typeof errorData === 'object') {
          const firstErrorMessage = Object.values(errorData)[0] as string;
          throw new Error(firstErrorMessage);
        }
      }
      throw new Error(error.response?.data?.message || defaultMessage);
    }
  }
});