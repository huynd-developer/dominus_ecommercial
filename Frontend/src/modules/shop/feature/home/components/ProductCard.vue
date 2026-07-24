<template>
  <div class="product-card h-100 position-relative overflow-hidden" @click="goToDetail">
    <span v-if="product.discountPercent" class="discount-badge">
      -{{ product.discountPercent }}%
    </span>

    <button
      type="button"
      class="btn-favorite"
      :class="{ active: isFavorited }"
      :disabled="favoriteLoading"
      @click.stop="handleToggleFavorite"
      :title="isFavorited ? 'Bỏ yêu thích' : 'Thêm vào yêu thích'"
    >
      <i v-if="favoriteLoading" class="spinner-border spinner-border-sm"></i>
      <i v-else class="bi" :class="isFavorited ? 'bi-heart-fill' : 'bi-heart'"></i>
    </button>

    <div class="product-image-wrapper">
      <div class="product-bottle" :style="getBottleStyle(product.color)">
        <div class="product-bottle-cap"></div>
        <div class="product-bottle-neck"></div>

        <div class="product-bottle-body">
          <div class="product-bottle-label">
            <strong>{{ shortBrand }}</strong>
            <span>PERFUME</span>
          </div>
        </div>
      </div>
    </div>

    <div class="product-content">
      <p class="product-brand mb-1">
        {{ product.brand }}
      </p>

      <h3 class="product-name text-truncate mb-2">
        {{ product.name }}
      </h3>

      <div class="rating-row d-flex align-items-center gap-2 mb-2">
        <span class="stars">★★★★★</span>
        <span class="review-count">
          {{ product.rating }} | {{ product.reviewCount }} đánh giá
        </span>
      </div>

      <div class="price-row d-flex align-items-end gap-2 mb-3 flex-wrap">
        <span class="sale-price">
          {{ formatCurrency(product.salePrice) }}
        </span>

        <span
          v-if="product.discountPercent > 0"
          class="original-price text-decoration-line-through"
        >
          {{ formatCurrency(product.originalPrice) }}
        </span>
      </div>

      <!-- Khu vực 2 nút mua (Đã fix lỗi chặn click) -->
      <div class="product-actions" style="position: relative; z-index: 10;">
        <button
          type="button"
          class="btn buy-now-btn"
          @click.stop="openVariantModal('BUY')"
        >
          <i class="bi bi-lightning-charge me-2"></i> Mua ngay
        </button>

        <button
          type="button"
          class="btn add-cart-btn"
          @click.stop="openVariantModal('CART')"
        >
          <i class="bi bi-bag-plus me-2"></i> Thêm vào giỏ
        </button>
      </div>
    </div>

    <Teleport to="body">
      <Transition name="toast-slide">
        <div v-if="toast.show" class="custom-cart-toast" :class="toast.type">
          <div class="toast-icon">
            <i class="bi" :class="{ 'bi-check2': toast.type === 'success', 'bi-exclamation-triangle': toast.type === 'warning', 'bi-x-lg': toast.type === 'error' }"></i>
          </div>
          <div class="toast-info">
            <h4>{{ toast.title }}</h4>
            <p>{{ toast.message }}</p>
          </div>
          <RouterLink v-if="toast.showCartLink" to="/cart" class="toast-view-cart">
            XEM GIỎ HÀNG <i class="bi bi-arrow-right ms-1"></i>
          </RouterLink>
        </div>
      </Transition>
    </Teleport>

    <!-- MODAL CHỌN BIẾN THỂ -->
    <Teleport to="body">
      <div v-if="showVariantModal" class="custom-modal-overlay" @click.self="showVariantModal = false">
        <div class="variant-modal-box">
          <div class="vm-header">
            <h5>Chọn Phân Loại</h5>
            <button class="vm-close" @click="showVariantModal = false"><i class="bi bi-x-lg"></i></button>
          </div>

          <div class="vm-product-info">
            <div class="vm-img-box">
              <img :src="product.imageUrl" alt="Product Image" />
            </div>
            <div class="vm-details">
              <h6>{{ product.name }}</h6>
              <p class="vm-price">
                {{ formatCurrency(selectedVariant ? (selectedVariant.price || selectedVariant.salePrice || product.salePrice) : product.salePrice) }}
              </p>
            </div>
          </div>

          <div class="vm-variants">
            <p class="vm-label">Tùy chọn dung tích:</p>
            
            <div v-if="isLoadingVariants" class="text-center py-4">
              <span class="spinner-border spinner-border-sm me-2" style="color: #b78d52;"></span> 
              <span style="color: #718096; font-size: 13px;">Đang tải thông tin...</span>
            </div>

            <div v-else class="vm-grid">
              <button 
                v-for="v in fullVariants" 
                :key="v.productVariantId || v.id"
                class="vm-variant-btn"
                :class="{
                  'selected': (selectedVariant?.productVariantId || selectedVariant?.id) === (v.productVariantId || v.id),
                  'disabled': Number(v.stockQuantity || v.stock || 0) <= 0
                }"
                @click="selectedVariant = v"
              >
                <span class="vm-v-name">{{ v.displayCapacity || formatVariantName(v) }}</span>
                <span class="vm-v-stock">Kho: {{ v.stockQuantity || v.stock || 0 }}</span>
              </button>
            </div>
          </div>

          <button 
            class="vm-confirm-btn" 
            :disabled="!selectedVariant || addCartLoading || buyNowLoading || isLoadingVariants"
            @click="confirmAction"
          >
            <span v-if="addCartLoading || buyNowLoading" class="spinner-border spinner-border-sm me-2"></span>
            XÁC NHẬN {{ actionType === 'CART' ? 'THÊM VÀO GIỎ' : 'MUA NGAY' }}
          </button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import api from "@/common/api";
import { favoriteService } from "@/modules/shop/feature/product/services/favorite.service";

interface ProductVariant {
  id?: number;
  Id?: number;
  variantId?: number;
  productVariantId?: number;
  price?: number;
  stock?: number;
  stockQuantity?: number;
  availableQuantity?: number;
  quantity?: number;
  status?: number;
  capacity?: string;
  bottleType?: string;
}

interface Product {
  id: number;
  productId?: number;
  productVariantId?: number;
  variantId?: number;
  name: string;
  brand: string;
  color?: string;
  salePrice: number;
  originalPrice: number;
  discountPercent: number;
  rating: number;
  reviewCount: number;
  imageUrl?: string;
  stock?: number;
  stockQuantity?: number;
  availableQuantity?: number;
  status?: number;
  variants?: ProductVariant[];
}

const props = defineProps<{
  product: Product;
}>();

const router = useRouter();

const addCartLoading = ref(false);
const buyNowLoading = ref(false);
const favoriteLoading = ref(false);
const isFavorited = ref(false);

const showVariantModal = ref(false);
const isLoadingVariants = ref(false);
const actionType = ref<'CART' | 'BUY'>('CART');
const selectedVariant = ref<any>(null);
const fullVariants = ref<any[]>([]);

const toast = ref({
  show: false,
  type: "success" as "success" | "warning" | "error",
  title: "",
  message: "",
  showCartLink: false,
});

let toastTimer: ReturnType<typeof window.setTimeout> | undefined;

const brandMap: Record<string, string> = {
  Chanel: "CHANEL", Dior: "DIOR", "Yves Saint Laurent": "YSL", "Giorgio Armani": "ARMANI",
  Givenchy: "GIVENCHY", Creed: "CREED", Byredo: "BYREDO", "Tom Ford": "TOM FORD",
  "Maison Francis Kurkdjian": "MFK", "Le Labo": "LE LABO", "Paco Rabanne": "PACO",
};

const shortBrand = computed(() => brandMap[props.product.brand] || String(props.product.brand || "AURA").slice(0, 8).toUpperCase());
const getBottleStyle = (color?: string): Record<string, string> => ({ "--bottle-color": color || "#0a192f" });
const formatCurrency = (value: number) => new Intl.NumberFormat("vi-VN").format(Number(value || 0)) + " đ";

// Hàm đọc và format tên dung tích chuẩn xác từ API
const formatVariantName = (v: any) => {
  if (!v) return 'Loại';
  
  let cap = null;
  
  // Trích xuất dung tích từ nhiều trường hợp của Backend
  if (v.capacity && typeof v.capacity === "object") {
    cap = v.capacity.value ?? v.capacity.name;
  } else if (v.capacityValue != null) {
    cap = v.capacityValue;
  } else if (v.volume != null) {
    cap = v.volume;
  } else if (typeof v.capacity === "string" || typeof v.capacity === "number") {
    cap = v.capacity;
  }

  // Format thêm chữ "ml" cho đẹp
  if (cap != null && cap !== "") {
    const numeric = Number(cap);
    if (!Number.isNaN(numeric)) return `${numeric}ml`;
    const text = String(cap);
    return text.toLowerCase().includes("ml") ? text : `${text}ml`;
  }

  // Nếu không có dung tích thì lấy Loại chai (nếu có)
  if (v.bottleType) {
    return typeof v.bottleType === "object" ? v.bottleType.name : v.bottleType;
  }

  // Bước đường cùng mới trả về Loại + ID
  return 'Loại ' + (v.productVariantId || v.id);
};

const showToast = (type: "success" | "warning" | "error", title: string, message: string, showCartLink = false) => {
  toast.value = { show: true, type, title, message, showCartLink };
  if (toastTimer) window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => { toast.value.show = false; }, 2800);
};

// Hàm check Token tối giản, dùng Toast xịn xò thay cho alert()
// Hàm check Phân quyền: Cả Token và Role phải hợp lệ
const checkLoginBeforeAction = () => {
  const token = localStorage.getItem("token");
  
  // Lấy role, cắt bỏ chữ ROLE_ (nếu có) và in hoa để so sánh cho chuẩn
  const rawRole = localStorage.getItem("role") || localStorage.getItem("userRole") || "";
  const role = rawRole.replace("ROLE_", "").toUpperCase().trim();

  // 1. Chưa đăng nhập
  if (!token) {
    showToast(
      "warning", 
      "Yêu cầu đăng nhập", 
      "Vui lòng đăng nhập để tiếp tục trải nghiệm mua sắm tại Dominus."
    );
    setTimeout(() => {
      router.push({ 
        name: "Login", 
        query: { redirect: router.currentRoute.value.fullPath } 
      }).catch(()=>{});
    }, 1500);
    return false;
  }

  // 2. Đã đăng nhập nhưng không phải USER (VD: Admin, Manager...)
  if (role !== "USER" && role !== "CUSTOMER") { 
    showToast(
      "error", 
      "Từ chối thao tác", 
      "Chức năng này chỉ dành cho tài khoản Khách hàng."
    );
    return false;
  }

  // 3. Đã đăng nhập và đúng Role
  return true;
};

const openVariantModal = async (type: 'CART' | 'BUY') => {
  if (!checkLoginBeforeAction()) return;

  actionType.value = type;
  selectedVariant.value = null;
  fullVariants.value = [];
  
  showVariantModal.value = true;
  isLoadingVariants.value = true;

  try {
    const res = await api.get(`/v1/products/${getProductId()}`);
    const data = res.data?.data || res.data;

    let rawVariants = data?.variants || data?.productVariants || data?.productVariantList;

    if (!rawVariants || rawVariants.length === 0) {
      rawVariants = props.product.variants || [props.product];
    }

    const processedVariants = rawVariants.map((v: any) => {
      let cap = null;

      // Đã update thêm `capacityName` theo đúng cấu trúc ảnh m gửi
      if (v.capacityName != null) {
        cap = v.capacityName;
      } else if (v.capacity && typeof v.capacity === "object") {
        cap = v.capacity.value ?? v.capacity.name;
      } else if (v.capacityValue != null) {
        cap = v.capacityValue;
      } else if (v.volume != null) {
        cap = v.volume;
      } else if (typeof v.capacity === "string" || typeof v.capacity === "number") {
        cap = v.capacity;
      }

      let displayCap = "";
      let numericCap = 0;

      // Xử lý mất đuôi .0 (VD: "50.0" -> 50) và ghép đuôi "ml"
      if (cap != null && cap !== "") {
        numericCap = parseFloat(String(cap).replace("ml", "")) || 0;
        displayCap = numericCap > 0 ? `${numericCap}ml` : String(cap);
      }

      // Đã update thêm `bottleTypeName`
      if (!displayCap || numericCap === 0) {
         const bottle = v.bottleTypeName || v.bottleType;
         displayCap = typeof bottle === "object" ? bottle?.name : bottle;
      }

      if (!displayCap) {
         displayCap = 'Loại ' + (v.productVariantId || v.id);
      }

      return {
        ...v,
        displayCapacity: displayCap,
        numericCapacity: numericCap
      };
    });

    processedVariants.sort((a: any, b: any) => a.numericCapacity - b.numericCapacity);
    fullVariants.value = processedVariants;

  } catch (error) {
    console.error("Lỗi lấy danh sách biến thể:", error);
    const fallbackVariants = props.product.variants || [props.product];
    fullVariants.value = fallbackVariants.map((v: any) => ({
        ...v,
        displayCapacity: 'Loại ' + (v.productVariantId || v.id),
        numericCapacity: 0
    }));
  } finally {
    isLoadingVariants.value = false;
  }
};

const confirmAction = async () => {
  if (!selectedVariant.value) return;
  const variantId = Number(selectedVariant.value.productVariantId || selectedVariant.value.variantId || selectedVariant.value.id || props.product.id);

  if (actionType.value === 'CART') {
    try {
      addCartLoading.value = true;
      await api.post("/v1/customer/cart/add", { productVariantId: variantId, quantity: 1 });
      window.dispatchEvent(new Event("cart-updated"));
      showVariantModal.value = false;
      showToast("success", "Thêm thành công", "Đã thêm 1 sản phẩm vào giỏ.", true);
    } catch (error: any) {
      showToast("error", "Lỗi", error?.response?.data?.message || "Không thể thêm vào giỏ.");
    } finally {
      addCartLoading.value = false;
    }
  } else {
    try {
      buyNowLoading.value = true;
      await api.post("/v1/customer/cart/add", { productVariantId: variantId, quantity: 1 });
      window.dispatchEvent(new Event("cart-updated"));
      showVariantModal.value = false;
      router.push({ name: "Checkout" });
    } catch (error: any) {
      showToast("error", "Lỗi", error?.response?.data?.message || "Không thể mua ngay lúc này.");
    } finally {
      buyNowLoading.value = false;
    }
  }
};

const loadFavoriteStatus = async () => {
  const variantId = Number(props.product.productVariantId || props.product.id);
  if (!variantId || !localStorage.getItem("token")) { isFavorited.value = false; return; }
  try {
    const res = await favoriteService.checkFavorite(variantId);
    isFavorited.value = Boolean(res.data?.favorited);
  } catch (error) { isFavorited.value = false; }
};

const handleToggleFavorite = async () => {
  const variantId = Number(props.product.productVariantId || props.product.id);
  if (!variantId || Number.isNaN(variantId)) return;
  if (!checkLoginBeforeAction()) return;
  
  try {
    favoriteLoading.value = true;
    const res = await favoriteService.toggleFavorite(variantId);
    isFavorited.value = Boolean(res.data?.favorited);
    window.dispatchEvent(new CustomEvent("favorite-updated", { detail: { productVariantId: variantId, favorited: isFavorited.value } }));
    showToast(isFavorited.value ? "success" : "warning", isFavorited.value ? "Đã thêm yêu thích" : "Đã bỏ yêu thích", res.data?.message || "");
  } catch (error: any) {
    showToast("error", "Lỗi", "Không thể xử lý yêu thích");
  } finally {
    favoriteLoading.value = false;
  }
};

const handleFavoriteUpdated = (event: Event) => {
  const customEvent = event as CustomEvent<{ productVariantId?: number; favorited?: boolean; }>;
  const variantId = Number(customEvent.detail?.productVariantId || 0);
  if (!variantId || variantId !== Number(props.product.productVariantId || props.product.id)) return;
  isFavorited.value = Boolean(customEvent.detail?.favorited);
};

const getProductId = () => Number(props.product.productId || props.product.id || 0);
const goToDetail = () => {
  const productId = getProductId();
  if (productId > 0) {
    router.push({ name: "SingleProduct", params: { id: productId } });
  } else {
    router.push("/product");
  }
};

onMounted(() => {
  window.addEventListener("favorite-updated", handleFavoriteUpdated);
  loadFavoriteStatus();
});

onBeforeUnmount(() => {
  window.removeEventListener("favorite-updated", handleFavoriteUpdated);
  if (toastTimer) window.clearTimeout(toastTimer);
});
</script>

<style scoped>
/* CSS GỐC */
.product-card { border-radius: 16px; background: #ffffff; border: 1px solid rgba(26, 26, 26, 0.055); box-shadow: 0 8px 28px rgba(5, 16, 36, 0.045); transition: all 0.28s ease; cursor: pointer; }
.product-card:hover { transform: translateY(-5px); box-shadow: 0 22px 48px rgba(5, 16, 36, 0.105); }
.discount-badge { position: absolute; top: 14px; left: 14px; z-index: 3; background: #b31320; color: #ffffff; border-radius: 999px; padding: 5px 10px; font-size: 12px; font-weight: 800; }
.btn-favorite { position: absolute; top: 14px; right: 14px; z-index: 5; width: 38px; height: 38px; border-radius: 999px; border: 1px solid rgba(189, 154, 95, 0.35); background: rgba(255, 255, 255, 0.94); color: #8c8c8c; display: inline-flex; align-items: center; justify-content: center; cursor: pointer; transition: all 0.22s ease; box-shadow: 0 8px 18px rgba(5, 16, 36, 0.08); }
.btn-favorite:hover:not(:disabled) { color: #dc2626; border-color: #dc2626; transform: scale(1.05); }
.btn-favorite.active { color: #dc2626; border-color: #dc2626; background: #fff5f5; }
.btn-favorite:disabled { opacity: 0.65; cursor: not-allowed; }
.product-image-wrapper { height: 235px; padding: 26px 24px 10px; display: flex; align-items: center; justify-content: center; background: radial-gradient(circle at center, rgba(189, 154, 95, 0.1), transparent 48%), #ffffff; }
.product-bottle { width: 120px; height: 190px; display: flex; flex-direction: column; align-items: center; filter: drop-shadow(0 18px 24px rgba(5, 16, 36, 0.18)); transition: transform 0.28s ease; }
.product-card:hover .product-bottle { transform: scale(1.05); }
.product-bottle-cap { width: 58px; height: 34px; border-radius: 15px 15px 6px 6px; background: linear-gradient(135deg, #f1d08a, #9b6f2e); }
.product-bottle-neck { width: 32px; height: 22px; background: linear-gradient(135deg, #d2ad68, #8b642c); }
.product-bottle-body { width: 120px; height: 134px; border-radius: 16px 16px 24px 24px; background: linear-gradient(135deg, rgba(255, 255, 255, 0.32), transparent 32%), linear-gradient(145deg, var(--bottle-color), #080808 86%); border: 2px solid rgba(255, 255, 255, 0.24); display: flex; align-items: center; justify-content: center; }
.product-bottle-label { width: 82px; height: 70px; border: 1px solid var(--aura-gold); background: rgba(5, 16, 36, 0.88); color: var(--aura-gold); display: flex; flex-direction: column; align-items: center; justify-content: center; }
.product-bottle-label strong { font-family: var(--aura-serif); font-size: 14px; letter-spacing: 1.5px; line-height: 1; }
.product-bottle-label span { margin-top: 5px; font-size: 7px; letter-spacing: 2px; font-family: var(--aura-sans); }
.product-content { padding: 18px 20px 20px; }
.product-brand { color: #8c8c8c; font-family: var(--aura-sans); font-size: 11px; letter-spacing: 1px; text-transform: uppercase; font-weight: 700; }
.product-name { font-family: var(--aura-serif); font-size: 17px; font-weight: 700; color: var(--aura-black); line-height: 1.28; letter-spacing: 0; }
.stars { color: var(--aura-gold); font-size: 13px; letter-spacing: 1px; }
.review-count { color: #777777; font-size: 12px; font-weight: 500; }
.sale-price { color: #111111; font-size: 18px; font-weight: 800; font-family: var(--aura-sans); }
.original-price { color: #a8a8a8; font-size: 13px; font-weight: 500; }
.product-actions { display: grid; grid-template-columns: 0.9fr 1.1fr; gap: 10px; width: 100%; }
.buy-now-btn, .add-cart-btn { font-size: 13px; font-weight: 800; border-radius: 7px; padding: 10px 10px; transition: all 0.22s ease; min-height: 42px; }
.buy-now-btn { border: none; background: var(--aura-gold); color: #ffffff; }
.buy-now-btn:hover:not(:disabled) { background: #a3824d; color: #ffffff; }
.add-cart-btn { border: 1px solid var(--aura-gold); color: var(--aura-gold); background: #ffffff; }
.add-cart-btn:hover:not(:disabled) { background: var(--aura-gold); color: #ffffff; }
.buy-now-btn:disabled, .add-cart-btn:disabled { opacity: 0.55; cursor: not-allowed; }
</style>

<style>
/* STYLE TOAST VÀ CSS MỚI CHO MODAL BIẾN THỂ */
.custom-cart-toast { position: fixed; bottom: 24px; right: 24px; background-color: #0b1120; border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 8px; padding: 16px 24px; display: flex; align-items: center; gap: 16px; z-index: 99999; box-shadow: 0 10px 40px rgba(0, 0, 0, 0.4); max-width: 520px; }
.custom-cart-toast.success .toast-icon { background-color: rgba(34, 197, 94, 0.16); color: #86efac; }
.custom-cart-toast.warning .toast-icon { background-color: rgba(245, 158, 11, 0.16); color: #fbbf24; }
.custom-cart-toast.error .toast-icon { background-color: rgba(239, 68, 68, 0.16); color: #fca5a5; }
.toast-icon { width: 34px; height: 34px; background-color: rgba(255, 255, 255, 0.1); border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-size: 18px; flex-shrink: 0; }
.toast-info h4 { margin: 0; font-size: 15px; font-weight: 700; color: #ffffff; }
.toast-info p { margin: 4px 0 0 0; font-size: 13px; color: #94a3b8; }
.toast-view-cart { margin-left: 12px; color: #d2ad68; font-size: 12px; font-weight: 700; text-decoration: none; display: flex; align-items: center; transition: color 0.25s ease; white-space: nowrap; }
.toast-view-cart:hover { color: #f1d08a; }
.toast-slide-enter-active, .toast-slide-leave-active { transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1); }
.toast-slide-enter-from, .toast-slide-leave-to { transform: translateX(120%); opacity: 0; }

/* CSS MODAL BIẾN THỂ SANG TRỌNG */
.custom-modal-overlay { backdrop-filter: blur(5px); position: fixed; inset: 0; background: rgba(0,0,0,0.6); z-index: 99999; display: flex; align-items: center; justify-content: center; }
.variant-modal-box { background: #ffffff; width: 100%; max-width: 440px; border-radius: 20px; padding: 28px; box-shadow: 0 24px 54px rgba(6, 19, 43, 0.25); animation: modalFadeIn 0.3s ease-out forwards; }
@keyframes modalFadeIn { from { opacity: 0; transform: translateY(20px) scale(0.98); } to { opacity: 1; transform: translateY(0) scale(1); } }
.vm-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; border-bottom: 1px solid rgba(189, 154, 95, 0.15); padding-bottom: 16px; }
.vm-header h5 { margin: 0; font-family: 'Playfair Display', serif; font-weight: 800; color: #06132b; font-size: 20px; letter-spacing: -0.5px; }
.vm-close { background: transparent; border: none; font-size: 20px; color: #a0aec0; cursor: pointer; transition: all 0.2s ease; padding: 4px; border-radius: 50%; }
.vm-close:hover { color: #e53e3e; background: #fff5f5; transform: rotate(90deg); }
.vm-product-info { display: flex; gap: 18px; margin-bottom: 28px; align-items: center; }
.vm-img-box { width: 82px; height: 82px; border-radius: 14px; background: #f8fafc; display: flex; align-items: center; justify-content: center; overflow: hidden; border: 1px solid rgba(6, 19, 43, 0.08); flex-shrink: 0; padding: 6px; }
.vm-img-box img { width: 100%; height: 100%; object-fit: contain; }
.vm-details h6 { margin: 0 0 6px 0; font-weight: 800; font-size: 16px; color: #06132b; font-family: 'Playfair Display', serif; line-height: 1.3; }
.vm-price { margin: 0; font-weight: 800; color: #b78d52; font-size: 18px; }
.vm-label { font-weight: 700; font-size: 14px; color: #4a5568; margin-bottom: 12px; text-transform: uppercase; letter-spacing: 0.5px; }
.vm-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 28px; }
.vm-variant-btn { background: #ffffff; border: 1px solid #cbd5e0; border-radius: 12px; padding: 12px 6px; text-align: center; cursor: pointer; transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; }
.vm-variant-btn:hover:not(.disabled) { border-color: #b78d52; background: #fffcf7; transform: translateY(-2px); }
.vm-variant-btn.selected { border-color: #b78d52; background: #fffcf7; border-width: 2px; box-shadow: 0 6px 14px rgba(183, 141, 82, 0.15); padding: 11px 5px; }
.vm-v-name { font-size: 14px; font-weight: 800; color: #06132b; }
.vm-v-stock { font-size: 11px; color: #718096; font-weight: 500; }
.vm-variant-btn.disabled { opacity: 0.45; cursor: not-allowed; background: #f1f5f9; border-color: #e2e8f0; }
.vm-variant-btn.disabled .vm-v-name { text-decoration: line-through; color: #a0aec0; }
.vm-confirm-btn { width: 100%; background: #b78d52; color: #ffffff; border: none; border-radius: 12px; padding: 16px; font-size: 14px; font-weight: 800; letter-spacing: 1px; transition: all 0.25s ease; text-transform: uppercase; }
.vm-confirm-btn:hover:not(:disabled) { background: #9b7541; transform: translateY(-2px); box-shadow: 0 10px 20px rgba(183, 141, 82, 0.35); }
.vm-confirm-btn:disabled { opacity: 0.6; cursor: not-allowed; }
</style>