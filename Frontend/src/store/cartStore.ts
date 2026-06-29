import { defineStore } from 'pinia'

// Định nghĩa kiểu dữ liệu cho sản phẩm
export interface Product {
  id: number | string;
  name: string;
  price: number;
  image?: string;
}

// Khai báo Pinia Store
export const useCartStore = defineStore('cart', {
  state: () => ({
    items: [] as Array<{ product: Product; quantity: number }>,
    isOpen: false,
  }),

  getters: {
    cartTotal: (state) => state.items.reduce((total, item) => total + (item.product.price * item.quantity), 0),
    cartCount: (state) => state.items.reduce((count, item) => count + item.quantity, 0),
  },

  actions: {
    toggleCart() {
      this.isOpen = !this.isOpen
    },

    // Thêm hàm này vào
  updateQuantity(cartItemId: number, currentQty: number, change: number, stockQty: number) {
    const newQty = currentQty + change;
    
    // Logic chặn không cho vượt quá tồn kho
    if (newQty < 1) return;
    if (newQty > stockQty) {
      alert(`Sản phẩm này chỉ còn tối đa ${stockQty} cái trong kho!`);
      return;
    }

    // Tìm item trong giỏ để cập nhật
    const item = this.items.find(i => i.product.id === cartItemId);
    if (item) {
      item.quantity = newQty;
    }
  },

    addToCart(product: Product, quantity: number) {
      const existingItem = this.items.find(item => item.product.id === product.id)
      if (existingItem) {
        existingItem.quantity += quantity
      } else {
        this.items.push({ product, quantity })
      }
      this.isOpen = true
    },

    removeItem(productId: number | string) {
      this.items = this.items.filter(item => item.product.id !== productId)
    }
  }
})

