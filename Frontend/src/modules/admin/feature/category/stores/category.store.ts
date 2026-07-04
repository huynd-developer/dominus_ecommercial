import { defineStore } from 'pinia';
import { categoryService } from '../services/category.service';
import type { Category, CategoryRequest } from '../types/category.type';

export const useCategoryStore = defineStore('categoryStore', {
  state: () => ({
    categories: [] as Category[],
    isLoading: false,
    currentPage: 0,
    totalPages: 0, 
    pageSize: 5  
  }),
  actions: {
    async fetchCategories(keyword: string = '', page: number = 0) {
      this.isLoading = true;
      try {
        const response = await categoryService.getAll({ 
          params: { keyword, page, size: this.pageSize } 
        });
        
        const backendResult = response.data?.data ? response.data.data : response.data;
        
        if (backendResult && backendResult.content) {
          this.categories = backendResult.content; 
          
          // 🎯 ĐIỂM MẤU CHỐT: Lấy thông tin từ object "page"
          this.totalPages = backendResult.page?.totalPages || backendResult.totalPages || 1;
          this.currentPage = backendResult.page?.number || backendResult.number || 0;
          
        } else if (Array.isArray(backendResult)) {
          this.categories = backendResult;
          this.totalPages = 1;
          this.currentPage = 0;
        } else {
          this.categories = [];
        }
      } catch (error) {
        console.error("Lỗi khi tải danh mục:", error);
        this.categories = [];
      } finally {
        this.isLoading = false;
      }
    },
    async createCategory(data: CategoryRequest) {
      try {
        await categoryService.create(data);
        // ĐÃ XÓA: dòng fetchCategories tự động để View tự quản lý luồng dữ liệu mượt hơn
      } catch (error: any) {
        if (error.response && error.response.status === 400) {
          const errorData = error.response.data;
          const targetError = errorData?.message || errorData;
          if (typeof targetError === 'string') throw new Error(targetError);
          if (typeof targetError === 'object') {
            const firstErrorMessage = Object.values(targetError)[0] as string;
            throw new Error(firstErrorMessage);
          }
        }
        throw new Error("Lỗi hệ thống khi thêm danh mục!");
      }
    },

    async updateCategory(id: number, data: CategoryRequest) {
      try {
        await categoryService.update(id, data);
        // ĐÃ XÓA: dòng fetchCategories tự động nhằm tránh ghi đè mất từ khóa tìm kiếm hiện tại
      } catch (error: any) {
        if (error.response && error.response.status === 400) {
          const errorData = error.response.data;
          const targetError = errorData?.message || errorData;
          if (typeof targetError === 'string') throw new Error(targetError);
          if (typeof targetError === 'object') {
            const firstErrorMessage = Object.values(targetError)[0] as string;
            throw new Error(firstErrorMessage);
          }
        }
        throw new Error("Lỗi hệ thống khi cập nhật danh mục!");
      }
    },

    async deleteCategory(id: number) {
      try {
        await categoryService.delete(id);
      } catch (error: any) {
        console.error("Lỗi khi xóa danh mục:", error);
        if (error.response && error.response.data) {
          throw new Error(error.response.data.message || error.response.data);
        }
        throw new Error("Không thể xóa danh mục này!");
      }
    }
  }
});