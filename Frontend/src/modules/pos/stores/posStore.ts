import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const usePosStore = defineStore('pos', () => {
  const categories = ['Tất cả', 'Nước hoa Nam', 'Nước hoa Nữ', 'Nước hoa Unisex', 'Giftset']
  const activeCategory = ref('Tất cả')
  const searchQuery = ref('')
  const discountCode = ref('')
  
  const products = ref([
    { id: 1, name: 'DOMINUS Noir', subName: 'Absolute EDP', price: 2500000, category: 'Nước hoa Nam', image: 'https://images.unsplash.com/photo-1541643600914-78b084683601?auto=format&fit=crop&w=400&q=80' },
    { id: 2, name: 'DOMINUS Gold', subName: 'Essence Luxury', price: 3200000, category: 'Nước hoa Nữ', image: 'https://images.unsplash.com/photo-1523293182086-7651a899d37f?auto=format&fit=crop&w=400&q=80' },
    { id: 3, name: 'DOMINUS Velvet', subName: 'Rose Oud Intense', price: 2800000, category: 'Nước hoa Nữ', image: 'https://images.unsplash.com/photo-1594035910387-fea47794261f?auto=format&fit=crop&w=400&q=80' },
    { id: 4, name: 'DOMINUS Oud', subName: 'Wood Rare', price: 4500000, category: 'Nước hoa Unisex', image: 'https://images.unsplash.com/photo-1547887537-6158d64c35b3?auto=format&fit=crop&w=400&q=80' },
  ])

  const cart = ref<any[]>([])

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

  const cartTotal = computed(() => {
    return cart.value.reduce((total, item) => total + (item.price * item.quantity), 0)
  })

  // --- NGHIỆP VỤ GIỎ HÀNG ĐẦY ĐỦ ---
  const addToCart = (product: any) => {
    const existingItem = cart.value.find(item => item.id === product.id)
    if (existingItem) {
      existingItem.quantity += 1
    } else {
      cart.value.push({ ...product, quantity: 1 })
    }
  }

  const decreaseQuantity = (productId: number) => {
    const existingItem = cart.value.find(item => item.id === productId)
    if (existingItem) {
      if (existingItem.quantity > 1) {
        existingItem.quantity -= 1
      } else {
        removeItem(productId)
      }
    }
  }

  const removeItem = (productId: number) => {
    cart.value = cart.value.filter(item => item.id !== productId)
  }

  return { 
    categories, activeCategory, searchQuery, discountCode, 
    cart, filteredProducts, cartTotal, 
    addToCart, decreaseQuantity, removeItem 
  }
})