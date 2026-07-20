import { defineStore } from 'pinia';

export const useOrderAdminStore = defineStore('orderAdmin', {
  state: () => ({
    currentFilters: { keyword: '', status: '', orderType: '', page: 0, size: 10 }
  }),
  actions: {
    setFilters(filters: any) { this.currentFilters = { ...filters }; },
    resetFilters() { this.currentFilters = { keyword: '', status: '', orderType: '', page: 0, size: 10 }; }
  }
});