<template>
  <div class="page-wrapper">
    <header class="lp-header">
      <div class="logo-section">
        <img src="@/assets/Logo.png" alt="Dominus" class="brand-logo" @click="$router.push('/')" />
      </div>
      <div class="header-features">
        <div class="feature-item"><svg class="icon-hdr" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg><p>Sản phẩm<br>chính hãng</p></div>
        <div class="feature-item"><svg class="icon-hdr" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="8" y="10" width="8" height="12" rx="2"/><path d="M10 10V6a2 2 0 014 0v4"/><path d="M9 2h6v2H9z"/></svg><p>Hương thơm<br>chuẩn Pháp</p></div>
        <div class="feature-item"><svg class="icon-hdr" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg><p>Đóng gói<br>sang trọng</p></div>
        <div class="feature-item"><svg class="icon-hdr" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 18v-6a9 9 0 0118 0v6"/><path d="M21 19a2 2 0 01-2 2h-1v-6h3v4zM3 19a2 2 0 002 2h1v-6H3v4z"/></svg><p>Tư vấn tận tâm<br>24/7</p></div>
        <div class="feature-item"><svg class="icon-hdr" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg><p>Thanh toán bảo mật<br>100% an toàn</p></div>
        
        <div class="header-actions">
          <div class="nav-cart" @click="$router.push('/cart')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 01-8 0"/></svg>
            <span class="cart-badge" v-if="cartItems.length > 0">{{ cartItems.length }}</span>
          </div>
        </div>
      </div>
    </header>

    <main class="main-content full-width">
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
              <p class="item-variant">100ml</p> 
              <div class="qty-wrapper">
                <button @click="updateQty(item, item.quantity - 1)" :disabled="item.quantity <= 1">−</button>
                <input type="text" :value="item.quantity" readonly />
                <button @click="updateQty(item, item.quantity + 1)" :disabled="item.quantity >= item.stockQuantity">+</button>
              </div>
            </div>
            
            <div class="item-action">
              <span class="price">{{ formatCurrency(item.price * item.quantity) }}</span>
              <button class="btn-delete" @click="removeItem(item.cartItemId)"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg></button>
            </div>
          </div>
        </div>
      </div>

      <div class="cart-right" v-if="cartItems.length > 0">
        <h3 class="summary-title">Tóm tắt đơn hàng</h3>
        <div class="voucher-row">
          <div class="voucher-input">
            <svg class="voucher-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="7" width="20" height="10" rx="2" ry="2"/><path d="M2 12a2 2 0 010-4m20 4a2 2 0 000-4M10 7v10m4-10v10"/></svg>
            <input type="text" placeholder="Nhập mã Voucher" />
          </div>
          <button class="btn-apply">Áp dụng</button>
        </div>

        <div class="summary-box">
          <div class="summary-line"><span>Tạm tính</span><span class="val">{{ formatCurrency(totalAmount) }}</span></div>
          <div class="summary-line"><span>Phí vận chuyển</span><span class="val">30.000₫</span></div>
          <div class="summary-line total-line"><span>Tổng thanh toán</span><span class="total-val">{{ formatCurrency(totalAmount + 30000) }}</span></div>
        </div>

        <button class="btn-checkout" @click="$router.push('/checkout')">
           ĐẶT HÀNG NGAY
        </button>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const router = useRouter();

interface CartItem { cartItemId: number; productVariantId: number; sku: string; quantity: number; price: number; stockQuantity: number; }
const cartItems = ref<CartItem[]>([]); 
const isLoading = ref<boolean>(true);

const formatCurrency = (val: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val).replace('VND', '₫');
const getHeaders = () => ({ Authorization: `Bearer ${localStorage.getItem('token')}` });
const totalAmount = computed(() => cartItems.value.reduce((sum, item) => sum + (item.price * item.quantity), 0));

const loadCart = async () => { try { const res = await axios.get('http://localhost:8080/api/v1/customer/cart/my-cart', { headers: getHeaders() }); cartItems.value = res.data; } catch (err) { console.error(err); } finally { isLoading.value = false; } };
const updateQty = async (item: CartItem, newQty: number) => { if (newQty < 1 || newQty > item.stockQuantity) return; try { await axios.put(`http://localhost:8080/api/v1/customer/cart/update/${item.cartItemId}`, { quantity: newQty }, { headers: getHeaders() }); item.quantity = newQty; } catch (err) { alert('Lỗi cập nhật số lượng!'); } };
const removeItem = async (cartItemId: number) => { if (!confirm('Xóa sản phẩm này?')) return; try { await axios.delete(`http://localhost:8080/api/v1/customer/cart/remove/${cartItemId}`, { headers: getHeaders() }); cartItems.value = cartItems.value.filter(i => i.cartItemId !== cartItemId); } catch (err) { alert('Không thể xóa sản phẩm'); } };

onMounted(loadCart);
</script>

<style scoped>
* { box-sizing: border-box; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }
.page-wrapper { background-color: #fafbfc; min-height: 100vh; padding-bottom: 50px; color: #06132b; }

/* Navbar */
.lp-header { display: flex; justify-content: space-between; align-items: center; padding: 15px 40px; background: white; border-bottom: 1px solid #eee; box-shadow: 0 2px 4px rgba(0,0,0,0.02); }
.logo-section { display: flex; align-items: center; }
.brand-logo { height: 45px; object-fit: contain; cursor: pointer; transition: 0.2s; }
.brand-logo:hover { opacity: 0.8; }
.header-features { display: flex; align-items: center; gap: 30px; }
.feature-item { display: flex; align-items: center; gap: 10px; font-size: 12px; color: #06132b; font-weight: 500;}
.icon-hdr { width: 22px; height: 22px; color: #b78d52; }
.feature-item p { margin: 0; line-height: 1.4; }

/* Nút Giỏ hàng trên Navbar */
.header-actions { padding-left: 30px; border-left: 1px solid #eaeaea; display: flex; align-items: center; }
.nav-cart { position: relative; cursor: pointer; color: #06132b; transition: 0.2s; }
.nav-cart:hover { color: #b78d52; }
.nav-cart svg { width: 28px; height: 28px; }
.cart-badge { position: absolute; top: -6px; right: -8px; background: #b78d52; color: white; font-size: 11px; font-weight: bold; width: 18px; height: 18px; border-radius: 50%; display: flex; align-items: center; justify-content: center; }

/* Full Width Split Layout */
.main-content.full-width { max-width: 1400px; width: 100%; margin: 40px auto; padding: 0 20px; display: flex; gap: 30px; align-items: flex-start; }
.cart-left { flex: 2; background: white; border: 1px solid #eaeaea; border-radius: 8px; padding: 35px; box-shadow: 0 4px 15px rgba(0,0,0,0.03); }
.cart-right { flex: 1; background: white; border: 1px solid #eaeaea; border-radius: 8px; padding: 30px; box-shadow: 0 4px 15px rgba(0,0,0,0.03); position: sticky; top: 20px; }

.card-header { display: flex; align-items: center; gap: 10px; margin-bottom: 25px; }
.header-icon { background: #06132b; color: white; width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.header-icon svg { width: 16px; height: 16px; }
.card-header h2 { font-size: 20px; color: #06132b; margin: 0; position: relative; }
.card-header h2::after { content: ''; position: absolute; bottom: -8px; left: 0; width: 40px; height: 2px; background: #b78d52; }

/* Item List */
.cart-item { display: flex; align-items: center; gap: 20px; padding: 20px 0; border-bottom: 1px solid #f0f0f0; }
.cart-item:last-child { border-bottom: none; padding-bottom: 0; }
.item-img { width: 90px; height: 90px; object-fit: cover; border-radius: 8px; border: 1px solid #f5f5f5; }
.item-info { flex: 1; }
.item-name { font-size: 16px; font-weight: 600; margin: 0 0 5px 0; }
.item-variant { font-size: 13px; color: #666; margin: 0 0 12px 0; }
.qty-wrapper { display: inline-flex; border: 1px solid #ddd; border-radius: 4px; overflow: hidden; }
.qty-wrapper button { width: 32px; height: 32px; background: white; border: none; cursor: pointer; color: #333; font-size: 16px; }
.qty-wrapper input { width: 40px; text-align: center; border: none; font-size: 14px; outline: none; border-left: 1px solid #ddd; border-right: 1px solid #ddd; }
.item-action { text-align: right; display: flex; flex-direction: column; align-items: flex-end; gap: 15px; }
.price { font-weight: 700; font-size: 18px; }
.btn-delete { background: none; border: none; cursor: pointer; color: #a0aec0; display: flex; }
.btn-delete svg { width: 22px; height: 22px; }
.btn-delete:hover { color: #e53e3e; }

/* Right Column Summary */
.summary-title { font-size: 18px; margin: 0 0 20px 0; color: #06132b; }
.voucher-row { display: flex; gap: 10px; margin-bottom: 25px; }
.voucher-input { flex: 1; display: flex; align-items: center; border: 1px solid #ddd; border-radius: 6px; padding: 0 15px; }
.voucher-icon { width: 20px; height: 20px; color: #b78d52; margin-right: 10px; }
.voucher-input input { flex: 1; border: none; outline: none; padding: 12px 0; font-size: 14px; }
.btn-apply { background: #06132b; color: white; border: none; padding: 0 20px; border-radius: 6px; font-weight: 500; cursor: pointer; }

.summary-box { background: #fafbfc; border: 1px solid #f0f0f0; border-radius: 8px; padding: 20px; margin-bottom: 20px; }
.summary-line { display: flex; justify-content: space-between; margin-bottom: 12px; font-size: 15px; color: #333; }
.summary-line .val { font-weight: 500; }
.total-line { border-top: 1px solid #eaeaea; padding-top: 15px; margin-top: 5px; font-weight: 700; font-size: 16px; }
.total-val { font-size: 22px; color: #b78d52; }

.btn-checkout { width: 100%; padding: 16px; background: #06132b; color: white; border: none; border-radius: 6px; font-weight: bold; font-size: 15px; cursor: pointer; transition: 0.2s;}
.btn-checkout:hover { background: #0a1f44; }
</style>