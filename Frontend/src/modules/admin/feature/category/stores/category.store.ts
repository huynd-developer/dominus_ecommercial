import { defineStore } from 'pinia';
import { categoryService } from '../services/category.service';
import type { Category, CategoryRequest } from '../types/category.type';

export const useCategoryStore = defineStore('categoryStore', {
  state: () => ({
    categories: [] as Category[],
    isLoading: false,
    currentPage: 0, // Trang hiện tại (Spring Boot bắt đầu từ 0)
    totalPages: 0,  // Tổng số trang
    pageSize: 5     // Số item trên 1 trang
  }),
  actions: {
    // Nhận thêm tham số page
    async fetchCategories(keyword: string = '', page: number = 0) {
      this.isLoading = true;
      try {
        const response = await categoryService.getAll({ 
          params: { keyword, page, size: this.pageSize } 
        });
        
        if (response.data && response.data.content) {
          // 1. Nếu Backend chuẩn chỉnh trả về Page (có content)
          this.categories = response.data.content; 
          this.totalPages = response.data.totalPages || 1;
          this.currentPage = response.data.number || 0;
        } else if (Array.isArray(response.data)) {
          // 2. Nếu Backend cũ vẫn đang trả về 1 mảng List bình thường
          this.categories = response.data;
          this.totalPages = 1;
          this.currentPage = 0;
        } else {
          // 3. Fallback an toàn (tránh lỗi Undefined)
          this.categories = [];
        }

      } catch (error) {
        console.error("Lỗi khi tải danh mục:", error);
        this.categories = []; // Lỗi cũng trả về mảng rỗng để không bị sập UI
      } finally {
        this.isLoading = false;
      }
    },

    // --- SỬA Ở ĐÂY: Bổ sung try...catch và bắt lỗi 400 ---
    async createCategory(data: CategoryRequest) {
      try {
        await categoryService.create(data);
        await this.fetchCategories('', 0); // Thêm xong về trang 1
      } catch (error: any) {
        // Bóc tách lỗi 400 từ Backend trả về
        if (error.response && error.response.status === 400) {
          const errorData = error.response.data;
          if (typeof errorData === 'string') {
            throw new Error(errorData); // Trả về text: "Tên danh mục đã tồn tại"
          } else if (typeof errorData === 'object') {
            const firstErrorMessage = Object.values(errorData)[0] as string;
            throw new Error(firstErrorMessage); // Trả về lỗi validation (VD: "Không để trống")
          }
        }
        throw new Error("Lỗi hệ thống khi thêm danh mục!");
      }
    },

    // --- SỬA Ở ĐÂY: Bổ sung try...catch và bắt lỗi 400 ---
    async updateCategory(id: number, data: CategoryRequest) {
      try {
        await categoryService.update(id, data);
        await this.fetchCategories('', this.currentPage); // Cập nhật xong giữ nguyên trang
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
        throw new Error("Lỗi hệ thống khi cập nhật danh mục!");
      }
    },

    async deleteCategory(id: number) {
      try {
        await categoryService.delete(id);
        await this.fetchCategories('', this.currentPage); 
      } catch (error) {
         console.error("Lỗi khi xóa:", error);
         throw new Error("Không thể xóa danh mục này!");
      }
    }
  }
});