import { ref, computed } from 'vue';
// Sử dụng đường dẫn tương đối chính xác đi từ thư mục hiện tại của useFilter
import { mockProductList } from '@/modules/shop/feature/product/services/mockData';

// Trạng thái các bộ lọc đang được chọn
const selectedGenders = ref<string[]>([]);
const selectedBrands = ref<string[]>([]);
const selectedPriceRanges = ref<string[]>([]);

export function useFilter() {
  // Hàm reset bộ lọc về rỗng
  const resetFilters = () => {
    selectedGenders.value = [];
    selectedBrands.value = [];
    selectedPriceRanges.value = [];
  };

  // Logic lọc sản phẩm tự động chạy
  const filteredProducts = computed(() => {
    // Ép kiểu sang any[] để TypeScript không bắt bẻ các trường ẩn như gender, brand
    return (mockProductList as any[]).filter((product: any) => {
      
      // 1. Lọc theo giới tính
      if (selectedGenders.value.length > 0) {
        const productGender = product.gender || '';
        const productName = product.name || '';
        const genderMatch = selectedGenders.value.some(g => 
          productName.toLowerCase().includes(g.toLowerCase()) || 
          productGender.toLowerCase() === g.toLowerCase()
        );
        if (!genderMatch) return false;
      }

      // 2. Lọc theo thương hiệu (Brand)
      if (selectedBrands.value.length > 0) {
        if (!product.brand || !selectedBrands.value.includes(product.brand)) {
          return false;
        }
      }

      // 3. Lọc theo khoảng giá (Hỗ trợ cả giá dạng Số lẫn dạng Chuỗi có chữ đ)
      if (selectedPriceRanges.value.length > 0) {
        let productPrice = 0;
        
        if (typeof product.price === 'number') {
          productPrice = product.price;
        } else if (typeof product.price === 'string') {
          productPrice = parseInt(product.price.replace(/[^0-9]/g, ''), 10) || 0;
        }
        
        const priceMatch = selectedPriceRanges.value.some(range => {
          if (range === 'Dưới 1 triệu') return productPrice < 1000000;
          if (range === '1 - 3 triệu') return productPrice >= 1000000 && productPrice <= 3000000;
          if (range === '3 - 5 triệu') return productPrice > 3000000 && productPrice <= 5000000;
          if (range === 'Trên 5 triệu') return productPrice > 5000000;
          return true;
        });

        if (!priceMatch) return false;
      }

      return true;
    });
  });

  return {
    selectedGenders,
    selectedBrands,
    selectedPriceRanges,
    filteredProducts,
    resetFilters
  };
}