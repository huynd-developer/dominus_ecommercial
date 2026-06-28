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
            <span class="cart-badge">{{ cartCount || 0 }}</span>
          </div>
          <span>Giỏ hàng</span>
        </div>
      </div>
    </header>

    <div class="product-layout">
      <aside class="sidebar-filter">
        <div class="filter-header">
          <h3>BỘ LỌC TÌM KIẾM</h3>
          <svg class="icon-filter" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="4" y1="21" x2="4" y2="14"/><line x1="4" y1="10" x2="4" y2="3"/><line x1="12" y1="21" x2="12" y2="12"/><line x1="12" y1="8" x2="12" y2="3"/><line x1="20" y1="21" x2="20" y2="16"/><line x1="20" y1="12" x2="20" y2="3"/><line x1="1" y1="14" x2="7" y2="14"/><line x1="9" y1="8" x2="15" y2="8"/><line x1="17" y1="16" x2="23" y2="16"/></svg>
        </div>

        <div class="filter-group">
          <div class="group-title">Giới tính <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="18 15 12 9 6 15"/></svg></div>
          <div class="checkbox-list row">
            <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> Nam</label>
            <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> Nữ</label>
            <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> Unisex</label>
          </div>
        </div>

        <div class="filter-group">
          <div class="group-title">Dung tích <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="18 15 12 9 6 15"/></svg></div>
          <div class="checkbox-list col">
            <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> 10ml <span class="count">(18)</span></label>
            <label class="custom-checkbox"><input type="checkbox" checked><span class="checkmark"></span> 50ml <span class="count">(42)</span></label>
            <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> 100ml <span class="count">(27)</span></label>
          </div>
        </div>

        <div class="filter-group">
          <div class="group-title">Nồng độ <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="18 15 12 9 6 15"/></svg></div>
          <div class="checkbox-list col">
            <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> EDP <span class="count">(35)</span></label>
            <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> EDT <span class="count">(20)</span></label>
            <label class="custom-checkbox"><input type="checkbox" checked><span class="checkmark"></span> Parfum <span class="count">(32)</span></label>
          </div>
        </div>

        <div class="filter-group">
          <div class="group-title">Mùi hương <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="18 15 12 9 6 15"/></svg></div>
          <div class="pill-list">
            <button class="pill-btn">Gỗ</button>
            <button class="pill-btn">Biển</button>
            <button class="pill-btn">Citrus</button>
            <button class="pill-btn">Aromatic</button>
            <button class="pill-btn">Xạ hương</button>
            <button class="pill-btn">Hương thơm sạch</button>
          </div>
        </div>

        <div class="filter-group">
          <div class="group-title">Thương hiệu <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="18 15 12 9 6 15"/></svg></div>
          <div class="checkbox-list col">
            <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> Chanel <span class="count">(28)</span></label>
            <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> Dior <span class="count">(34)</span></label>
            <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> Tom Ford <span class="count">(22)</span></label>
            <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> Creed <span class="count">(18)</span></label>
            <label class="custom-checkbox"><input type="checkbox"><span class="checkmark"></span> Maison Francis Kurkdjian <span class="count">(14)</span></label>
          </div>
        </div>
      </aside>

      <main class="product-main" v-if="product">
        <nav class="breadcrumb">
          ⌂ / Nước hoa / Nam / {{ product.brand }} / <span class="active">{{ product.name }}</span>
        </nav>

        <div class="product-content">
          <div class="product-gallery">
            <div class="main-image-wrapper">
              <img :src="product.image" class="main-image" :alt="product.name" />
              <button class="btn-heart">♡</button>
            </div>
            <div class="thumbnail-list">
              <img :src="product.image" class="thumb active" />
              <img src="https://images.unsplash.com/photo-1526413232644-8a40f41ce931?auto=format&fit=crop&q=80&w=100" class="thumb" />
              <img src="https://images.unsplash.com/photo-1594035910387-fea47794261f?auto=format&fit=crop&q=80&w=100" class="thumb" />
              <img src="https://images.unsplash.com/photo-1588405748880-12d1d2a59f75?auto=format&fit=crop&q=80&w=100" class="thumb" />
            </div>
          </div>

          <div class="product-info">
            <div class="header-info">
              <div class="brand">{{ product.brand }}</div>
              <div class="share">
                <svg class="icon-sm" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/></svg> Chia sẻ
              </div>
            </div>
            <h1 class="title">{{ product.name }}</h1>
            <div class="rating">
              <span class="stars">★★★★★</span> <span class="score">4.8</span> | <span class="reviews">128 đánh giá</span>
            </div>
            
            <div class="price-box">
              <span class="current-price">{{ formatCurrency(selectedVariant?.price || 0) }}</span>
              <span class="old-price">{{ formatCurrency(product.oldPrice) }}</span>
            </div>
            <div class="save-badge">Tiết kiệm {{ formatCurrency(product.oldPrice - (selectedVariant?.price || 0)) }} (20%)</div>

            <p class="desc">{{ product.description }}</p>

            <div class="variant-selection">
              <h4>Chọn dung tích</h4>
              <div class="capacity-options">
                <button 
                  v-for="variant in product.variants" 
                  :key="variant.id"
                  :class="['cap-btn', { active: selectedVariant?.id === variant.id }]"
                  @click="selectVariant(variant)"
                >
                  {{ variant.capacity }}
                  <span v-if="selectedVariant?.id === variant.id" class="check-icon">✓</span>
                </button>
              </div>
            </div>

            <div class="quantity-section" v-if="selectedVariant">
              <h4>Số lượng</h4>
              <div class="qty-control">
                <button @click="decreaseQty" :disabled="quantity <= 1">-</button>
                <input type="number" v-model="quantity" readonly />
                <button @click="increaseQty" :disabled="quantity >= selectedVariant.stock">+</button>
              </div>
            </div>

            <div class="actions">
              <button class="btn-add-cart" @click="addToCart">
                <svg class="icon-bag" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 01-8 0"/></svg> THÊM VÀO GIỎ HÀNG
              </button>
              <button class="btn-buy-now" @click="buyNow">MUA NGAY ➔</button>
            </div>

            <div class="policy-footer">
              <div class="policy-item">
                <svg class="icon-policy" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                <div><strong>Cam kết chính hãng</strong><br>100% Authentic</div>
              </div>
              <div class="policy-item">
                <svg class="icon-policy" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 12a9 9 0 109-9 9.75 9.75 0 00-6.74 2.74L3 8"/><path d="M3 3v5h5"/></svg>
                <div><strong>Đổi trả dễ dàng</strong><br>Trong 7 ngày</div>
              </div>
              <div class="policy-item">
                <svg class="icon-policy" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>
                <div><strong>Giao hàng miễn phí</strong><br>Đơn từ 1.000.000đ</div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>

    <div class="luxury-toast" :class="{ 'show': showToast }">
      <div class="toast-content">
        <div class="icon-circle">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
        </div>
        <div class="toast-text">
          <strong>Thành công</strong>
          <span>Đã thêm {{ quantity }} sản phẩm vào giỏ.</span>
        </div>
      </div>
      <button class="toast-action" @click="$router.push('/cart')">Xem giỏ hàng ➔</button>
    </div>

    <div class="luxury-toast error" :class="{ 'show': showErrorToast }">
      <div class="toast-content">
        <div class="icon-circle error-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        </div>
        <div class="toast-text">
          <strong>Cần đăng nhập</strong>
          <span>Vui lòng đăng nhập để mua hàng.</span>
        </div>
      </div>
      <button class="toast-action" @click="$router.push('/login')">Đăng nhập ➔</button>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const router = useRouter();

interface Variant { id: number; capacity: string; price: number; stock: number; }
interface Product { brand: string; name: string; oldPrice: number; image: string; description: string; variants: Variant[]; }

const product = ref<Product>({
  brand: 'Chanel',
  name: 'Bleu de Chanel Parfum',
  oldPrice: 5300000,
  image: 'https://images.unsplash.com/photo-1526413232644-8a40f41ce931?auto=format&fit=crop&q=80&w=400',
  description: 'Bleu de Chanel Parfum là sự thể hiện đậm đà và sâu lắng nhất của sự tự do. Hương thơm nam tính, thanh lịch với chiều sâu của gỗ tuyết tùng\nNew Caledonia kết hợp cùng sự ấm áp của hoắc hương và labdanum. Một mùi hương tạo dấu ấn mạnh mẽ và bền bỉ.',
  variants: [
    { id: 1, capacity: '10ml', price: 850000, stock: 100 },
    { id: 2, capacity: '50ml', price: 4250000, stock: 50 },
    { id: 3, capacity: '100ml', price: 6500000, stock: 20 }
  ]
});

const selectedVariant = ref<Variant>(product.value.variants[1] as Variant);
const quantity = ref<number>(1);
const showToast = ref(false);
const showErrorToast = ref(false);
const cartCount = ref(0); 

const formatCurrency = (val: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);

const selectVariant = (variant: Variant) => { selectedVariant.value = variant; quantity.value = 1; };
const decreaseQty = () => { if (quantity.value > 1) quantity.value--; };
const increaseQty = () => { if (quantity.value < selectedVariant.value.stock) quantity.value++; };

const handleLogout = () => {
  if (confirm("Bạn có chắc chắn muốn đăng xuất?")) {
    localStorage.removeItem("token");
    router.push("/login");
  }
};

const fetchCartCount = async () => {
  const token = localStorage.getItem('token');
  if (token) {
    try {
      const res = await axios.get('http://localhost:8080/api/v1/customer/cart/my-cart', { headers: { Authorization: `Bearer ${token}` } });
      cartCount.value = res.data.length; 
    } catch (error) { console.error(error); }
  }
};

const addToCart = async () => {
  const token = localStorage.getItem('token');

  if (!token) {
    showErrorToast.value = true;
    setTimeout(() => { showErrorToast.value = false; }, 4000);
    return;
  }

  try {
    await axios.post('http://localhost:8080/api/v1/customer/cart/add', 
      { productVariantId: selectedVariant.value.id, quantity: quantity.value }, 
      { headers: { Authorization: `Bearer ${token}` } }
    );
    
    showToast.value = true;
    fetchCartCount(); 
    setTimeout(() => { showToast.value = false; }, 4000);
    
  } catch (error: any) {
    alert(error.response?.data?.message || 'Có lỗi xảy ra, vui lòng thử lại!');
  }
};

const buyNow = async () => {
  const token = localStorage.getItem('token');

  if (!token) {
    showErrorToast.value = true;
    setTimeout(() => { showErrorToast.value = false; }, 4000);
    return;
  }

  try {
    await axios.post('http://localhost:8080/api/v1/customer/cart/add', 
      { productVariantId: selectedVariant.value.id, quantity: quantity.value }, 
      { headers: { Authorization: `Bearer ${token}` } }
    );
    
    router.push('/checkout');
    
  } catch (error: any) {
    alert(error.response?.data?.message || 'Có lỗi xảy ra, vui lòng thử lại!');
  }
};

onMounted(() => {
  fetchCartCount();
});
</script>

<style scoped>
* { box-sizing: border-box; }
.page-wrapper { background-color: #fafbfc; min-height: 100vh; padding-bottom: 50px; color: #06132b; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }

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

/* Product Layout */
.product-layout { display: flex; gap: 40px; max-width: 1400px; margin: 40px auto; padding: 0 20px; }

/* Filter Sidebar */
.sidebar-filter { width: 250px; flex-shrink: 0; border-right: 1px solid #edf2f7; padding-right: 20px; }
.filter-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #edf2f7; padding-bottom: 15px; margin-bottom: 15px; }
.filter-header h3 { font-size: 16px; margin: 0; color: #06132b; font-weight: 700; }
.icon-filter { width: 22px; height: 22px; color: #b78d52; }

.filter-group { border-bottom: 1px solid #edf2f7; padding: 15px 0; }
.group-title { display: flex; justify-content: space-between; align-items: center; font-size: 15px; font-weight: 600; margin-bottom: 15px; cursor: pointer; color: #06132b; }
.chevron { width: 16px; height: 16px; color: #06132b; }

.checkbox-list { display: flex; gap: 10px; }
.checkbox-list.col { flex-direction: column; }
.checkbox-list.row { justify-content: space-between; }
.custom-checkbox { display: flex; align-items: center; position: relative; padding-left: 25px; cursor: pointer; font-size: 14px; user-select: none; color: #4a5568; }
.custom-checkbox input { position: absolute; opacity: 0; cursor: pointer; height: 0; width: 0; }
.checkmark { position: absolute; top: 0; left: 0; height: 16px; width: 16px; background-color: white; border: 1px solid #cbd5e0; border-radius: 3px; }
.custom-checkbox:hover input ~ .checkmark { background-color: #f7fafc; }
.custom-checkbox input:checked ~ .checkmark { background-color: #06132b; border-color: #06132b; }
.checkmark:after { content: ""; position: absolute; display: none; }
.custom-checkbox input:checked ~ .checkmark:after { display: block; left: 5px; top: 2px; width: 4px; height: 8px; border: solid white; border-width: 0 2px 2px 0; transform: rotate(45deg); }
.count { color: #a0aec0; font-size: 13px; margin-left: 4px;}

.pill-list { display: flex; flex-wrap: wrap; gap: 10px; }
.pill-btn { background: white; border: 1px solid #e2e8f0; border-radius: 20px; padding: 6px 15px; font-size: 13px; color: #4a5568; cursor: pointer; transition: 0.2s; }
.pill-btn:hover { border-color: #06132b; color: #06132b; }

/* Main Content */
.product-main { flex: 1; }
.breadcrumb { font-size: 13px; color: #718096; margin-bottom: 20px; }
.breadcrumb .active { color: #b78d52; font-weight: 500; }
.product-content { display: flex; gap: 50px; }
.product-gallery { flex: 1; max-width: 450px; }
.main-image-wrapper { position: relative; background: #f8fafc; border-radius: 12px; padding: 20px; margin-bottom: 15px; }
.main-image { width: 100%; display: block; mix-blend-mode: multiply; }
.btn-heart { position: absolute; top: 15px; right: 15px; background: white; border: none; width: 40px; height: 40px; border-radius: 50%; box-shadow: 0 2px 5px rgba(0,0,0,0.1); font-size: 20px; cursor: pointer; color: #718096; }
.thumbnail-list { display: flex; gap: 10px; }
.thumb { width: calc(25% - 7.5px); border-radius: 8px; border: 1px solid #e2e8f0; cursor: pointer; padding: 5px; background: #f8fafc; }
.thumb.active { border: 2px solid #06132b; }

/* Product Info */
.product-info { flex: 1.2; }
.header-info { display: flex; justify-content: space-between; align-items: center; margin-bottom: 5px; }
.brand { color: #b78d52; font-size: 18px; text-transform: uppercase; letter-spacing: 1px; }
.share { font-size: 13px; color: #718096; cursor: pointer; display: flex; align-items: center; gap: 5px; }
.icon-sm { width: 16px; height: 16px; }
.title { font-size: 32px; font-weight: 700; color: #06132b; margin: 0 0 10px 0; }
.rating { font-size: 14px; color: #718096; margin-bottom: 25px; }
.stars { color: #ecc94b; letter-spacing: 2px; }
.score { font-weight: bold; color: #06132b; }

.price-box { display: flex; align-items: center; gap: 15px; }
.current-price { font-size: 28px; font-weight: bold; color: #06132b; }
.old-price { font-size: 16px; text-decoration: line-through; color: #a0aec0; border-left: 1px solid #cbd5e0; padding-left: 15px;}
.save-badge { font-size: 13px; color: #b78d52; margin-top: 5px; margin-bottom: 25px; }

.desc { font-size: 14px; color: #4a5568; line-height: 1.6; margin-bottom: 30px; white-space: pre-line; }

.variant-selection h4, .quantity-section h4 { font-size: 14px; color: #06132b; margin: 0 0 10px 0; }
.capacity-options { display: flex; gap: 15px; margin-bottom: 25px; }
.cap-btn { position: relative; padding: 12px 30px; border: 1px solid #cbd5e0; background: white; border-radius: 6px; cursor: pointer; font-size: 14px; color: #4a5568; transition: 0.2s; }
.cap-btn.active { background: #06132b; color: white; border-color: #06132b; }
.check-icon { position: absolute; top: -8px; right: -8px; background: #b78d52; color: white; width: 20px; height: 20px; border-radius: 50%; font-size: 12px; display: flex; align-items: center; justify-content: center; }

.qty-control { display: inline-flex; border: 1px solid #cbd5e0; border-radius: 6px; overflow: hidden; margin-bottom: 30px; }
.qty-control button { width: 45px; height: 45px; border: none; background: white; cursor: pointer; font-size: 20px; color: #4a5568; }
.qty-control input { width: 60px; text-align: center; border: none; font-size: 16px; outline: none; font-weight: 500; }

.actions { display: flex; gap: 15px; margin-bottom: 30px; }
.btn-add-cart { flex: 1; padding: 16px; background: #06132b; color: white; border: none; border-radius: 6px; font-weight: bold; font-size: 14px; cursor: pointer; display: flex; justify-content: center; align-items: center; gap: 10px; transition: 0.2s; }
.icon-bag { width: 20px; height: 20px; }
.btn-add-cart:hover { background: #0a1f44; }
.btn-buy-now { flex: 1; padding: 16px; background: #b78d52; color: white; border: none; border-radius: 6px; font-weight: bold; font-size: 14px; cursor: pointer; transition: 0.2s; }
.btn-buy-now:hover { background: #a37c46; }

.policy-footer { display: flex; justify-content: space-between; border-top: 1px solid #edf2f7; padding-top: 20px; }
.policy-item { display: flex; align-items: flex-start; gap: 10px; font-size: 12px; color: #718096; }
.icon-policy { width: 24px; height: 24px; color: #b78d52; }
.policy-item strong { color: #4a5568; font-size: 13px; }

/* TOAST CSS */
.luxury-toast {
  position: fixed;
  bottom: 40px;
  right: 40px;
  background: #06132b;
  color: white;
  padding: 16px 24px;
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.15);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 30px;
  min-width: 380px;
  transform: translateY(100px);
  opacity: 0;
  visibility: hidden;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  z-index: 1000;
  border: 1px solid rgba(183, 141, 82, 0.3);
}

.luxury-toast.show {
  transform: translateY(0);
  opacity: 1;
  visibility: visible;
}

.toast-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.icon-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(183, 141, 82, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b78d52;
}

.icon-circle svg { width: 20px; height: 20px; }

.toast-text { display: flex; flex-direction: column; }
.toast-text strong { font-size: 15px; margin-bottom: 2px; letter-spacing: 0.5px; }
.toast-text span { font-size: 13px; color: #a0aec0; }

.toast-action {
  background: transparent;
  border: none;
  color: #b78d52;
  font-weight: bold;
  font-size: 13px;
  cursor: pointer;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  padding: 0;
  transition: 0.2s;
}
.toast-action:hover { color: #d4a373; text-decoration: underline; }

/* Error Toast Style */
.luxury-toast.error { background: #fff; color: #06132b; border: 1px solid #fee2e2; border-left: 4px solid #ef4444; }
.icon-circle.error-icon { background: #fee2e2; color: #ef4444; }
.luxury-toast.error .toast-text span { color: #64748b; }
.luxury-toast.error .toast-action { color: #ef4444; }
.luxury-toast.error .toast-action:hover { color: #b91c1c; }
</style>