import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const usePosStore = defineStore('pos', () => {
  // --- State ---
  const categories = ['Tất cả', 'Nước hoa Nam', 'Nước hoa Nữ', 'Nước hoa Unisex', 'Giftset']
  const activeCategory = ref('Tất cả')
  const searchQuery = ref('')
  const discountCode = ref('')
  
  // Dữ liệu sản phẩm (Giả lập)
  const products = ref([
    { id: 1, name: 'DOMINUS Noir', subName: 'Absolute', price: 2500000, category: 'Nước hoa Nam', image: 'https://via.placeholder.com/300x400/1e293b/475569?text=Noir' },
    { id: 2, name: 'DOMINUS Gold', subName: 'Essence', price: 3200000, category: 'Nước hoa Nữ', image: 'https://via.placeholder.com/300x400/1e293b/475569?text=Gold' },
    { id: 3, name: 'DOMINUS Velvet', subName: 'Rose', price: 2800000, category: 'Nước hoa Nữ', image: 'https://via.placeholder.com/300x400/1e293b/475569?text=Velvet' },
  ])

  const cart = ref<any[]>([])

  // --- Computed ---
  // Lọc sản phẩm theo danh mục và tìm kiếm
  const filteredProducts = computed(() => {
    let result = products.value
    if (activeCategory.value !== 'Tất cả') {
      result = result.filter(p => p.category === activeCategory.value)
    }
    if (searchQuery.value) {
      const query = searchQuery.value.toLowerCase()
      result = result.filter(p => 
        p.name.toLowerCase().includes(query) || p.subName.toLowerCase().includes(query)
      )
    }
    return result
  })

  // Tính tổng tiền giỏ hàng
  const cartTotal = computed(() => {
    return cart.value.reduce((total, item) => total + (item.price * item.quantity), 0)
  })

  // --- Actions ---
  const addToCart = (product: any) => {
    const existingItem = cart.value.find(item => item.id === product.id)
    if (existingItem) {
      existingItem.quantity += 1
    } else {
      cart.value.push({ ...product, quantity: 1 })
    }
  }

  const clearCart = () => {
    cart.value = []
  }

  return { 
    categories, activeCategory, searchQuery, discountCode, 
    cart, filteredProducts, cartTotal, 
    addToCart, clearCart 
  }
})