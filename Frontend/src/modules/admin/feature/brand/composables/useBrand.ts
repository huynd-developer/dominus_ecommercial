import { ref, computed } from 'vue';
import { useBrandStore } from '../stores/brand.store';
import { brandApi } from '../services/brand.api';
import type { Brand } from '../types/brand.type';

export function useBrand() {
  const store = useBrandStore();
  const searchQuery = ref('');

  const filteredBrands = computed(() => {
    if (!searchQuery.value) return store.brands;
    return store.brands.filter(b => 
      b.name.toLowerCase().includes(searchQuery.value.toLowerCase())
    );
  });

  const handleToggleStatus = async (brand: Brand) => {
    const oldStatus = brand.status;
    brand.status = oldStatus === 1 ? 0 : 1;
    try {
      await brandApi.update(brand.id!, brand);
    } catch {
      brand.status = oldStatus;
      alert('Lỗi cập nhật trạng thái');
    }
  };

  const handleDelete = async (id?: number) => {
    if (!id) return;
    if (confirm('Bạn muốn xóa thương hiệu này? (Chỉ Owner)')) {
      await brandApi.delete(id);
      await store.fetchBrands();
    }
  };

  const handleSave = async (data: Brand, isEditing: boolean) => {
    if (isEditing && data.id) {
      await brandApi.update(data.id, data);
    } else {
      await brandApi.create(data);
    }
    await store.fetchBrands();
  };

  return {
    store,
    searchQuery,
    filteredBrands,
    handleToggleStatus,
    handleDelete,
    handleSave
  };
}