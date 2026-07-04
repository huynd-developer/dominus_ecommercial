<template>
  <div class="cart-left">
    <div class="card-header">
      <div class="header-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 01-8 0"/></svg></div>
      <h2>Giỏ hàng của bạn</h2>
    </div>

    <div v-if="isLoading" class="loading">Đang tải dữ liệu...</div>
    <div v-else-if="cartItems.length === 0" class="empty">Giỏ hàng trống. <router-link to="/products">Tiếp tục mua sắm ➔</router-link></div>

    <div v-else class="item-list">
      <div class="cart-item" v-for="item in cartItems" :key="item.cartItemId">
        <img :src="item.image || 'https://images.unsplash.com/photo-1526413232644-8a40f41ce931?auto=format&fit=crop&q=80&w=300'" class="item-img" />
        
        <div class="item-info">
          <h4 class="item-name">{{ item.sku || 'Nước hoa cao cấp' }}</h4>

          <h4 class="item-name">{{ item.productName || item.sku }}</h4>
          
          <p class="item-variant">Dung tích: <strong>{{ item.capacity }}</strong></p>
          
          <div class="qty-wrapper">
            <button @click="$emit('update-qty', item, item.quantity - 1)" :disabled="item.quantity <= 1">−</button>
            <input type="text" :value="item.quantity" readonly />
            <button @click="$emit('update-qty', item, item.quantity + 1)" :disabled="item.quantity >= item.stockQuantity">+</button>
          </div>
        </div>
        
        <div class="item-action">
          <span class="price">{{ formatCurrency(item.price * item.quantity) }}</span>
          <button class="btn-delete" @click="$emit('remove-item', item.cartItemId)" title="Xóa sản phẩm">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
          </button>
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
.cart-left { flex: 2; background: white; border: 1px solid #eaeaea; border-radius: 12px; padding: 40px; box-shadow: 0 4px 15px rgba(0,0,0,0.02); }
.card-header { display: flex; align-items: center; gap: 12px; margin-bottom: 30px; }
.header-icon { background: #06132b; color: white; width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.header-icon svg { width: 18px; height: 18px; }
.card-header h2 { font-size: 22px; color: #06132b; margin: 0; position: relative; font-weight: 700; }
.card-header h2::after { content: ''; position: absolute; bottom: -10px; left: 0; width: 50px; height: 3px; background: #b78d52; border-radius: 2px;}

/* Nới rộng khoảng cách từng món hàng, đệm thoải mái */
.cart-item { display: flex; align-items: center; gap: 30px; padding: 30px 0; border-bottom: 1px solid #f0f0f0; }
.cart-item:last-child { border-bottom: none; padding-bottom: 0; }

/* ẢNH SẢN PHẨM PHÓNG TO KHỔNG LỒ */
.item-img { 
  width: 140px; 
  height: 140px; 
  object-fit: cover; 
  border-radius: 12px; 
  border: 1px solid #eaeaea; 
  box-shadow: 0 4px 10px rgba(0,0,0,0.04); 
}

/* THÔNG TIN SẢN PHẨM */
.item-info { flex: 1; display: flex; flex-direction: column; gap: 8px; }
.item-name { font-size: 19px; font-weight: 700; margin: 0; color: #0a142f;}

/* Dòng dung tích nổi bật */
.item-variant { font-size: 15px; color: #718096; margin: 0 0 8px 0; }
.item-variant strong { color: #b78d52; font-weight: 700; font-size: 16px; background: rgba(183, 141, 82, 0.1); padding: 4px 10px; border-radius: 6px;}

/* Nút bấm tăng giảm to và dễ chạm hơn */
.qty-wrapper { display: inline-flex; border: 1px solid #cbd5e0; border-radius: 8px; overflow: hidden; width: fit-content; margin-top: 5px; }
.qty-wrapper button { width: 45px; height: 45px; background: #f8f9fa; border: none; cursor: pointer; color: #0a142f; font-size: 20px; font-weight: bold; transition: 0.2s;}
.qty-wrapper button:hover { background: #e2e8f0; }
.qty-wrapper button:disabled { color: #cbd5e0; cursor: not-allowed; background: #f8f9fa; }
.qty-wrapper input { width: 55px; text-align: center; border: none; font-size: 16px; font-weight: 600; outline: none; border-left: 1px solid #cbd5e0; border-right: 1px solid #cbd5e0; color: #0a142f;}

/* Khung Giá + Xóa (Đẩy đều lên xuống cho cân) */
.item-action { text-align: right; display: flex; flex-direction: column; align-items: flex-end; justify-content: space-between; height: 130px; }
.price { font-weight: 800; font-size: 22px; color: #e53e3e;}

/* Style xịn xò cho nút thùng rác */
.btn-delete { background: #fff5f5; border: 1px solid #fed7d7; cursor: pointer; color: #e53e3e; display: flex; padding: 12px; border-radius: 8px; transition: 0.2s; }
.btn-delete svg { width: 22px; height: 22px; }
.btn-delete:hover { background: #e53e3e; color: white; border-color: #e53e3e; box-shadow: 0 4px 10px rgba(229, 62, 62, 0.2);}

.empty { margin-top: 20px; color: #718096; font-size: 16px; }
.empty a { color: #b78d52; font-weight: bold; text-decoration: none; margin-left: 5px;}
.empty a:hover { text-decoration: underline; color: #8e6c3a;}
</style>