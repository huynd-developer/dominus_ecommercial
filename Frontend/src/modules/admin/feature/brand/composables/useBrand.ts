// import { ref, computed } from 'vue';
// import { storeToRefs } from 'pinia'; // Dùng để trích xuất state từ store mà vẫn giữ tính reactivity
// import { useBrandStore } from '../stores/brand.store';
// import { brandApi } from '../services/brand.api';
// import type { Brand } from '../types/brand.type';

// export function useBrand() {
//   const store = useBrandStore();
  
//   // Trích xuất các biến state từ Store ra để View dễ gọi
//   const { brands, loading, currentPage, totalPages, totalElements } = storeToRefs(store);
  
//   const searchQuery = ref('');

//   // LƯU Ý: Lọc cục bộ này chỉ hiệu quả trên dữ liệu của 1 trang hiện tại (5 bản ghi).
//   const filteredBrands = computed(() => {
//     if (!searchQuery.value) return brands.value;
//     return brands.value.filter(b => 
//       b.name.toLowerCase().includes(searchQuery.value.toLowerCase())
//     );
//   });

//   // --- CÁC HÀM XỬ LÝ SỰ KIỆN ---

//   const handleToggleStatus = async (brand: Brand) => {
//     const oldStatus = brand.status;
//     brand.status = oldStatus === 1 ? 0 : 1;
//     try {
//       await brandApi.update(brand.id!, brand);
//     } catch {
//       brand.status = oldStatus;
//       alert('Lỗi cập nhật trạng thái');
//     }
//   };

//   const handleDelete = async (id?: number) => {
//     if (!id) return;
//     if (confirm('Bạn muốn xóa thương hiệu này? (Chỉ Owner mới có quyền)')) {
//       await brandApi.delete(id);
//       // Xóa xong thì load lại ĐÚNG trang hiện tại đang đứng
//       await store.fetchBrands(currentPage.value);
//     }
//   };

//   const handleSave = async (data: Brand, isEditing: boolean) => {
//     if (isEditing && data.id) {
//       await brandApi.update(data.id, data);
//       // Sửa xong thì load lại ĐÚNG trang hiện tại
//       await store.fetchBrands(currentPage.value);
//     } else {
//       await brandApi.create(data);
//       // Thêm mới thì reset về trang 0 (trang đầu tiên) để thấy bản ghi vừa thêm
//       await store.fetchBrands(0);
//     }
//   };

//   // Hàm chuyển trang
//   const changePage = (page: number) => {
//     store.changePage(page);
//   };

//   return {
//     // Thuộc tính (State)
//     store,
//     brands,
//     currentPage,
//     totalPages,
//     totalElements,
//     loading,
//     searchQuery,
//     filteredBrands,
    
//     // Hành động (Actions)
//     changePage,
//     handleToggleStatus,
//     handleDelete,
//     handleSave,
//     fetchBrands: store.fetchBrands
//   };
// }