<template>
  <div class="list-view-container">
    <section class="home-section">
      <div class="section-header">
        <h2 class="section-title text-flash">Flash Sale Đang Diễn Ra</h2>
        <div class="countdown-timer">Kết thúc sau: <span>02:15:45</span></div>
      </div>
      <div class="product-grid">
        <div class="product-card luxury-card" v-for="item in productList" :key="'fs-'+item.id" @click="$emit('open-detail', item)">
          <div class="card-img-wrapper">
            <div class="sale-badge">-20%</div>
            <img :src="item?.imageUrl || item?.image || 'https://via.placeholder.com/300x300?text=No+Image'" :alt="item?.name || 'Sản phẩm'" />
            <button class="btn-heart-small" @click.stop><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg></button>
          </div>
          <div class="card-info">
            <div class="card-brand">{{ item?.brand?.name || item?.brand || 'Premium' }}</div>
            <h3 class="card-name">{{ item?.name || 'Tên sản phẩm' }}</h3>
            <div class="card-rating"><span class="stars">★★★★★</span> <span class="score">{{ item?.rating || '5.0' }}</span></div>
            <div class="card-price-box">
              <span class="card-price">{{ formatPrice(item) }}</span>
              <span class="card-old-price" v-if="item?.oldPrice">{{ formatOldPrice(item?.oldPrice) }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="pagination-container">
        <button class="page-btn" disabled>&lt;</button>
        <button class="page-btn active">1</button>
        <button class="page-btn">2</button>
        <button class="page-btn">&gt;</button>
      </div>
    </section>

    <section class="home-section">
      <div class="section-header">
        <h2 class="section-title">Sản Phẩm Nổi Bật</h2>
      </div>
      <div class="product-grid">
        <div class="product-card luxury-card" v-for="item in productList" :key="'feat-'+item.id" @click="$emit('open-detail', item)">
          <div class="card-img-wrapper">
            <img :src="item?.imageUrl || item?.image || 'https://via.placeholder.com/300x300?text=No+Image'" :alt="item?.name || 'Sản phẩm'" />
            <button class="btn-heart-small" @click.stop><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg></button>
          </div>
          <div class="card-info">
            <div class="card-brand">{{ item?.brand?.name || item?.brand || 'Premium' }}</div>
            <h3 class="card-name">{{ item?.name || 'Tên sản phẩm' }}</h3>
            <div class="card-rating"><span class="stars">★★★★★</span> <span class="score">{{ item?.rating || '5.0' }}</span></div>
            <div class="card-price-box">
              <span class="card-price">{{ formatPrice(item) }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="pagination-container">
        <button class="page-btn" disabled>&lt;</button>
        <button class="page-btn active">1</button>
        <button class="page-btn">2</button>
        <button class="page-btn">3</button>
        <button class="page-btn">&gt;</button>
      </div>
    </section>

    <section class="home-section">
      <div class="section-header">
        <h2 class="section-title">Nước Hoa Mới Nhất</h2>
        <div class="sort-box">
          <span>Sắp xếp:</span>
          <select><option>Mới nhất</option><option>Giá: Thấp đến Cao</option><option>Giá: Cao đến Thấp</option></select>
        </div>
      </div>
      <div class="product-grid">
        <div class="product-card luxury-card" v-for="item in productList" :key="'new-'+item.id" @click="$emit('open-detail', item)">
          <div class="card-img-wrapper">
            <div class="new-badge">NEW</div>
            <img :src="item?.imageUrl || item?.image || 'https://via.placeholder.com/300x300?text=No+Image'" :alt="item?.name || 'Sản phẩm'" />
            <button class="btn-heart-small" @click.stop><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg></button>
          </div>
          <div class="card-info">
            <div class="card-brand">{{ item?.brand?.name || item?.brand || 'Premium' }}</div>
            <h3 class="card-name">{{ item?.name || 'Tên sản phẩm' }}</h3>
            <div class="card-rating"><span class="stars">★★★★★</span> <span class="score">{{ item?.rating || '5.0' }}</span></div>
            <div class="card-price-box">
              <span class="card-price">{{ formatPrice(item) }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="pagination-container">
        <button class="page-btn">&lt;</button>
        <button class="page-btn">1</button>
        <button class="page-btn active">2</button>
        <button class="page-btn">3</button>
        <button class="page-btn">4</button>
        <button class="page-btn">&gt;</button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
defineProps<{ productList: any[] }>();
defineEmits(['open-detail']);

// Hàm format giá tiền thông minh, bao sân mọi trường hợp
const formatPrice = (item: any) => {
  let price = null;
  // 1. Nếu giá nằm trực tiếp trong Product
  if (item?.price != null) {
    price = item.price;
  } 
  // 2. Nếu giá nằm trong biến thể Variant (dữ liệu thật từ Database)
  else if (item?.variants && Array.isArray(item.variants) && item.variants.length > 0) {
    price = item.variants[0]?.price;
  }
  
  // Nếu vẫn không có giá hoặc giá bị lỗi (NaN)
  if (price == null || isNaN(price)) return 'Liên hệ';
  
  // Format ra tiền Việt
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
};

const formatOldPrice = (val: number) => {
  if (val == null || isNaN(val)) return '';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
};
</script>

<style scoped>
/* GIỮ NGUYÊN TOÀN BỘ CSS CỦA M, KHÔNG ĐỤNG CHẠM TỚI 1 CHỮ */
.home-section { margin-bottom: 70px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; padding-bottom: 15px; border-bottom: 2px solid #eaeaea; }
.section-title { font-size: 20px; margin: 0; color: #0a142f; font-weight: 700; text-transform: uppercase; }
.sort-box { display: flex; align-items: center; gap: 10px; font-size: 14px; color: #718096; }
.sort-box select { padding: 8px 15px; border: 1px solid #e2e8f0; border-radius: 6px; outline: none; }
.product-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 30px; }
.product-card.luxury-card { background: #fff; border-radius: 12px; overflow: hidden; cursor: pointer; transition: all 0.3s ease; display: flex; flex-direction: column; position: relative; box-shadow: 0 4px 20px rgba(0,0,0,0.03);}
.product-card.luxury-card:hover { box-shadow: 0 15px 35px rgba(0,0,0,0.08); transform: translateY(-4px); }
.card-img-wrapper { position: relative; background: #f8f9fa; padding: 40px 20px; height: 280px; display: flex; align-items: center; justify-content: center; }
.card-img-wrapper img { max-width: 100%; max-height: 100%; object-fit: contain; mix-blend-mode: multiply; transition: 0.4s ease; }
.product-card.luxury-card:hover .card-img-wrapper img { transform: scale(1.06); }
.btn-heart-small { position: absolute; top: 15px; right: 15px; background: white; border: 1px solid #eaeaea; width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #a0aec0; transition: 0.2s; box-shadow: 0 2px 8px rgba(0,0,0,0.05);}
.btn-heart-small:hover { color: #e53e3e; border-color: #e53e3e; }
.sale-badge { position: absolute; top: 15px; left: 15px; background: #e53e3e; color: white; font-size: 11px; font-weight: bold; padding: 4px 8px; border-radius: 4px; }
.new-badge { position: absolute; top: 15px; left: 15px; background: #0a142f; color: white; font-size: 11px; font-weight: bold; padding: 4px 8px; border-radius: 4px; letter-spacing: 1px;}
.card-info { padding: 20px; display: flex; flex-direction: column; align-items: center; text-align: center; gap: 6px; }
.card-brand { font-size: 11px; font-weight: 700; color: #c69c6d; text-transform: uppercase; letter-spacing: 1.5px; }
.card-name { font-size: 16px; font-weight: 600; color: #0a142f; margin: 0; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;}
.card-rating { font-size: 12px; display: flex; align-items: center; gap: 6px; }
.card-rating .stars { color: #ecc94b; letter-spacing: 1px; }
.card-rating .score { color: #a0aec0; }
.card-price-box { display: flex; align-items: baseline; gap: 12px; margin-top: 5px; }
.card-price { font-size: 16px; font-weight: 700; color: #0a142f; }
.card-old-price { font-size: 13px; color: #a0aec0; text-decoration: line-through; }
.pagination-container { display: flex; justify-content: center; align-items: center; gap: 8px; margin-top: 40px; }
.page-btn { width: 36px; height: 36px; border: 1px solid #eaeaea; background: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 14px; cursor: pointer; color: #4a5568; transition: 0.2s; font-family: inherit;}
.page-btn:hover:not(:disabled) { border-color: #c69c6d; color: #c69c6d; }
.page-btn.active { background: #0a142f; color: white; border-color: #0a142f; }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
</style>