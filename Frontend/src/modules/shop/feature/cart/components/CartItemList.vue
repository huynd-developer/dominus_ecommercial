<template>
  <div class="cart-left">
    <div class="card-header">
      <div class="header-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 01-8 0"/></svg></div>
      <h2>Giỏ hàng của bạn</h2>
    </div>

    <div v-if="isLoading" class="loading">Đang tải dữ liệu...</div>
    <div v-else-if="cartItems.length === 0" class="empty">Giỏ hàng trống. <router-link to="/product">Tiếp tục mua sắm</router-link></div>

    <div v-else class="item-list">
      <div class="cart-item" v-for="item in cartItems" :key="item.cartItemId">
        <img src="https://images.unsplash.com/photo-1526413232644-8a40f41ce931?auto=format&fit=crop&q=80&w=150" class="item-img" />
        <div class="item-info">
          <h4 class="item-name">{{ item.sku }}</h4>
          <p class="item-variant">{{ item.capacity }}</p>
          <div class="qty-wrapper">
            <button @click="$emit('update-qty', item, item.quantity - 1)" :disabled="item.quantity <= 1">−</button>
            <input type="text" :value="item.quantity" readonly />
            <button @click="$emit('update-qty', item, item.quantity + 1)" :disabled="item.quantity >= item.stockQuantity">+</button>
          </div>
        </div>
        <div class="item-action">
          <span class="price">{{ formatCurrency(item.price * item.quantity) }}</span>
          <button class="btn-delete" @click="$emit('remove-item', item.cartItemId)"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg></button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ cartItems: any[], isLoading: boolean }>();
defineEmits(['update-qty', 'remove-item']);
const formatCurrency = (val: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val).replace('VND', '₫');
</script>

<style scoped>
.cart-left { flex: 2; background: white; border: 1px solid #eaeaea; border-radius: 8px; padding: 35px; box-shadow: 0 4px 15px rgba(0,0,0,0.03); }
.card-header { display: flex; align-items: center; gap: 10px; margin-bottom: 25px; }
.header-icon { background: #06132b; color: white; width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.header-icon svg { width: 16px; height: 16px; }
.card-header h2 { font-size: 20px; color: #06132b; margin: 0; position: relative; }
.card-header h2::after { content: ''; position: absolute; bottom: -8px; left: 0; width: 40px; height: 2px; background: #b78d52; }
.cart-item { display: flex; align-items: center; gap: 20px; padding: 20px 0; border-bottom: 1px solid #f0f0f0; }
.cart-item:last-child { border-bottom: none; padding-bottom: 0; }
.item-img { width: 90px; height: 90px; object-fit: cover; border-radius: 8px; border: 1px solid #f5f5f5; }
.item-info { flex: 1; }
.item-name { font-size: 16px; font-weight: 600; margin: 0 0 5px 0; color: #333;}
.item-variant { font-size: 13px; color: #666; margin: 0 0 12px 0; }
.qty-wrapper { display: inline-flex; border: 1px solid #ddd; border-radius: 4px; overflow: hidden; }
.qty-wrapper button { width: 32px; height: 32px; background: white; border: none; cursor: pointer; color: #333; font-size: 16px; }
.qty-wrapper input { width: 40px; text-align: center; border: none; font-size: 14px; outline: none; border-left: 1px solid #ddd; border-right: 1px solid #ddd; color: #333;}
.item-action { text-align: right; display: flex; flex-direction: column; align-items: flex-end; gap: 15px; }
.price { font-weight: 700; font-size: 18px; color: #333;}
.btn-delete { background: none; border: none; cursor: pointer; color: #a0aec0; display: flex; }
.btn-delete svg { width: 22px; height: 22px; }
.btn-delete:hover { color: #e53e3e; }
.empty { margin-top: 20px; color: #666; }
</style>