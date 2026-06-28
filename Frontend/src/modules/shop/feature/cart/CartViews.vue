<template>
  <div class="page-wrapper">
    <header class="dark-navbar">
      <div class="logo-section">
        <img src="@/assets/Logo.png" alt="Dominus" class="brand-logo" @click="$router.push('/')" />
      </div>

      <div class="nav-right">
        <div class="nav-item account-menu">
          <div class="nav-trigger">
            <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            <span>Tài khoản</span>
            <svg class="chevron-down" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
          </div>

          <div class="dropdown-content">
            <div class="dropdown-header">
              <div class="avatar">H</div>
              <div class="user-info">
                <span class="user-name">Hoàng Khách Hàng</span>
                <span class="rank-badge">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 3h12l4 6-10 13L2 9z"/></svg>
                  BRONZE RANK - 0 PTS
                </span>
              </div>
            </div>
            <ul class="dropdown-list">
              <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg> Thông tin cá nhân</li>
              <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg> Sản phẩm yêu thích</li>
              <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg> Lịch sử đơn hàng</li>
              <li class="logout" @click="handleLogout"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg> Đăng xuất</li>
            </ul>
          </div>
        </div>

        <div class="nav-item cart-btn" @click="$router.push('/cart')">
          <div class="cart-icon-wrapper">
            <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 01-8 0"/></svg>
            <span class="cart-badge">{{ cartItems.length || 0 }}</span>
          </div>
          <span>Giỏ hàng</span>
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

const handleLogout = () => {
  if (confirm("Bạn có chắc chắn muốn đăng xuất?")) {
    localStorage.removeItem("token");
    router.push("/login");
  }
};

const loadCart = async () => { try { const res = await axios.get('http://localhost:8080/api/v1/customer/cart/my-cart', { headers: getHeaders() }); cartItems.value = res.data; } catch (err) { console.error(err); } finally { isLoading.value = false; } };
const updateQty = async (item: CartItem, newQty: number) => { if (newQty < 1 || newQty > item.stockQuantity) return; try { await axios.put(`http://localhost:8080/api/v1/customer/cart/update/${item.cartItemId}`, { quantity: newQty }, { headers: getHeaders() }); item.quantity = newQty; } catch (err) { alert('Lỗi cập nhật số lượng!'); } };
const removeItem = async (cartItemId: number) => { if (!confirm('Xóa sản phẩm này?')) return; try { await axios.delete(`http://localhost:8080/api/v1/customer/cart/remove/${cartItemId}`, { headers: getHeaders() }); cartItems.value = cartItems.value.filter(i => i.cartItemId !== cartItemId); } catch (err) { alert('Không thể xóa sản phẩm'); } };

onMounted(loadCart);
</script>

<style scoped>
* { box-sizing: border-box; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }
.page-wrapper { background-color: #fafbfc; min-height: 100vh; padding-bottom: 50px; color: #06132b; }

/* =========================================
   DARK NAVBAR LUXURY CSS
========================================== */
.dark-navbar { 
  display: flex; justify-content: space-between; align-items: center; 
  padding: 0 40px; height: 70px; 
  background-color: #08101f; 
  border-bottom: 1px solid rgba(255, 255, 255, 0.05); 
}
.logo-section { display: flex; align-items: center; }
.brand-logo { height: 35px; object-fit: contain; cursor: pointer; transition: 0.2s; filter: brightness(0) invert(1) sepia(1) saturate(5) hue-rotate(3deg); }
.brand-logo:hover { opacity: 0.8; }

.nav-right { display: flex; align-items: center; gap: 30px; height: 100%; }
.nav-item { display: flex; align-items: center; gap: 8px; color: #fdfdfd; font-size: 14px; cursor: pointer; height: 100%; transition: 0.2s; position: relative; }
.nav-item:hover, .nav-item:hover .nav-icon { color: #b78d52; }
.nav-icon { width: 22px; height: 22px; color: #fdfdfd; transition: 0.2s; }

.cart-icon-wrapper { position: relative; display: flex; align-items: center; }
.cart-badge { position: absolute; top: -8px; right: -8px; background: #b78d52; color: white; font-size: 11px; font-weight: bold; width: 18px; height: 18px; border-radius: 50%; display: flex; align-items: center; justify-content: center; }

.account-menu { position: relative; }
.nav-trigger { display: flex; align-items: center; gap: 8px; height: 100%; }
.chevron-down { width: 16px; height: 16px; margin-left: -2px; }

.dropdown-content { position: absolute; top: 100%; right: -20px; width: 260px; background: white; border-radius: 8px; box-shadow: 0 10px 30px rgba(0,0,0,0.1); opacity: 0; visibility: hidden; transition: all 0.2s ease-in-out; transform: translateY(10px); z-index: 100; border: 1px solid #eaeaea;}
.account-menu:hover .dropdown-content { opacity: 1; visibility: visible; transform: translateY(0); }
.dropdown-content::before { content: ''; position: absolute; top: -6px; right: 50px; border-left: 6px solid transparent; border-right: 6px solid transparent; border-bottom: 6px solid white; }

.dropdown-header { display: flex; align-items: center; gap: 15px; padding: 20px; border-bottom: 1px solid #f0f0f0; }
.avatar { width: 45px; height: 45px; border-radius: 50%; background: #c19b6c; color: white; font-size: 20px; font-weight: bold; display: flex; align-items: center; justify-content: center; }
.user-info { display: flex; flex-direction: column; gap: 5px; }
.user-name { color: #111; font-weight: bold; font-size: 15px; }
.rank-badge { display: inline-flex; align-items: center; gap: 4px; border: 1px solid #b78d52; color: #b78d52; background: #faf7f2; padding: 3px 8px; border-radius: 20px; font-size: 10px; font-weight: bold; text-transform: uppercase; letter-spacing: 0.5px;}
.rank-badge svg { width: 12px; height: 12px; }

.dropdown-list { list-style: none; margin: 0; padding: 10px 0; }
.dropdown-list li { display: flex; align-items: center; gap: 12px; padding: 12px 20px; color: #333; font-size: 14px; font-weight: 500; transition: 0.2s; }
.dropdown-list li svg { width: 18px; height: 18px; color: #a0aec0; }
.dropdown-list li:hover { background: #f8fafc; color: #b78d52; }
.dropdown-list li:hover svg { color: #b78d52; }
.dropdown-list li.logout { color: #dc3545; border-top: 1px solid #f0f0f0; margin-top: 5px; padding-top: 15px;}
.dropdown-list li.logout svg { color: #dc3545; }
.dropdown-list li.logout:hover { background: #fff5f5; }

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