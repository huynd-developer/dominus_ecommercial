import { defineStore } from 'pinia';
import { brandService } from '../services/brand.service';
import type { Brand, BrandRequest } from '../types/brand.type';

export const useBrandStore = defineStore('brandStore', {
  state: () => ({
    brands: [] as Brand[],
    isLoading: false,
    currentPage: 0, // Trang hiện tại (Spring Boot bắt đầu từ 0)
    totalPages: 0,  // Tổng số trang
    pageSize: 5     // Số item trên 1 trang
  }),
  actions: {
    // Nhận thêm tham số page và keyword để tìm kiếm + phân trang
    // Nhận thêm tham số page và keyword để tìm kiếm + phân trang
    async fetchBrands(keyword: string = '', page: number = 0) {
      this.isLoading = true;
      try {
        const response = await brandService.getAll({ 
          params: { keyword, page, size: this.pageSize } 
        });
        
        const resData = response.data; // Lấy dữ liệu axios trả về

        // BÓC TÁCH DỮ LIỆU THÔNG MINH
        if (resData && resData.content) {
          // Trường hợp 1: Trả về Page chuẩn Spring Boot
          this.brands = resData.content; 
          this.totalPages = resData.totalPages || 1;
          this.currentPage = resData.number || 0;
        } 
        else if (Array.isArray(resData)) {
          // Trường hợp 2: Trả về trực tiếp một Mảng (List)
          this.brands = resData;
          this.totalPages = 1;
          this.currentPage = 0;
        } 
        else if (resData && resData.data && Array.isArray(resData.data)) {
          // Trường hợp 3: Bị bọc trong Object { data: [...] }
          this.brands = resData.data;
          this.totalPages = 1;
          this.currentPage = 0;
        } 
        else if (resData && resData.data && resData.data.content) {
          // Trường hợp 4: Bọc Page bên trong { data: { content: [...] } }
          this.brands = resData.data.content;
          this.totalPages = resData.data.totalPages || 1;
          this.currentPage = resData.data.number || 0;
        } 
        else {
          // Nếu vẫn không lọt vào trường hợp nào, in ra để kiểm tra
          console.log("Cấu trúc dữ liệu API lạ, hãy kiểm tra:", resData);
          this.brands = [];
        }

      } catch (error) {
        console.error("Lỗi khi tải danh sách thương hiệu:", error);
        this.brands = [];
      } finally {
        this.isLoading = false;
      }
    },

    // Xử lý thêm mới kèm bắt lỗi validation 400
    async createBrand(data: BrandRequest) {
      try {
        await brandService.create(data);
        await this.fetchBrands('', 0); // Thêm thành công reset về trang đầu
      } catch (error: any) {
        if (error.response && error.response.status === 400) {
          const errorData = error.response.data;
          if (typeof errorData === 'string') {
            throw new Error(errorData); // Trả về text lỗi: "Tên thương hiệu đã tồn tại"
          } else if (typeof errorData === 'object') {
            const firstErrorMessage = Object.values(errorData)[0] as string;
            throw new Error(firstErrorMessage); // Trả về lỗi validate trống
          }
        }
        throw new Error("Lỗi hệ thống khi thêm thương hiệu!");
      }
    },

    // Xử lý cập nhật kèm bắt lỗi validation 400
    async updateBrand(id: number, data: BrandRequest) {
      try {
        await brandService.update(id, data);
        await this.fetchBrands('', this.currentPage); // Cập nhật xong giữ nguyên trang hiện tại
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
        throw new Error("Lỗi hệ thống khi cập nhật thương hiệu!");
      }
    },

    // Xử lý xóa thương hiệu
    async deleteBrand(id: number) {
      try {
        await brandService.delete(id);
        await this.fetchBrands('', this.currentPage); 
      } catch (error) {
         console.error("Lỗi khi xóa thương hiệu:", error);
         throw new Error("Không thể xóa thương hiệu này!");
      }
    }
  }
});