<template>
  <div class="shop-header">
    <div class="container-fluid px-4 px-lg-5 h-100">
      <div class="row align-items-center h-100">
        <div class="col-4 col-lg-3">
          <RouterLink to="/" class="brand-logo-link text-decoration-none d-inline-flex align-items-center">
            <img v-if="!logoLoadFailed" :src="logoAura" alt="DOMINUS PERFUME" class="brand-logo-img"
              @error="logoLoadFailed = true" />

            <div v-else class="brand-logo-text">
              <div class="brand-name">DOMINUS</div>
              <div class="brand-subtitle">PERFUME</div>
            </div>
          </RouterLink>
        </div>

        <div class="col-12 col-lg-6 order-3 order-lg-2 mt-3 mt-lg-0">

          <div
            class="search-wrapper position-relative mx-auto"
            ref="searchWrapperRef"
          >
            <input
              v-model="keyword"
              type="text"
              class="form-control rounded-pill search-input"
              placeholder="Tìm kiếm nước hoa..."
              @keyup.enter="handleSearch"
              @focus="handleSearchFocus"
              @keydown.esc="closeSuggest"
            />

            <button type="button" class="search-button" aria-label="Tìm kiếm" @click="handleSearch">
              <i class="bi bi-search"></i>
            </button>

            <!-- Dropdown gợi ý sản phẩm khi gõ -->
            <Transition name="suggest-fade">
              <div v-if="showSuggest" class="search-suggest">
                <div v-if="suggestLoading" class="suggest-state">
                  <span class="spinner-border spinner-border-sm me-2"></span>
                  Đang tìm...
                </div>

                <div v-else-if="suggestList.length === 0" class="suggest-state">
                  Không tìm thấy sản phẩm phù hợp.
                </div>

                <template v-else>
                  <a
                    v-for="item in suggestList"
                    :key="item.id"
                    href="#"
                    class="suggest-item"
                    @click.prevent="goToSuggest(item)"
                  >
                    <div class="suggest-img">
                      <img
                        v-if="item.image"
                        :src="item.image"
                        :alt="item.name"
                        @error="handleSuggestImageError"
                      />
                      <i v-else class="bi bi-image"></i>
                    </div>

                    <div class="suggest-info">
                      <p class="suggest-brand">{{ item.brand }}</p>
                      <p class="suggest-name text-truncate">{{ item.name }}</p>
                    </div>

                    <div class="suggest-price">
                      <span v-if="item.priceFrom" class="suggest-price-prefix">
                        từ
                      </span>
                      {{ formatSuggestPrice(item.price) }}
                    </div>
                  </a>

                  <button
                    type="button"
                    class="suggest-all"
                    @click="handleSearch"
                  >
                    Xem tất cả kết quả <i class="bi bi-arrow-right ms-1"></i>
                  </button>
                </template>
              </div>
            </Transition>
          </div>
        </div>

        <div class="col-8 col-lg-3 order-2 order-lg-3">
          <div class="d-flex align-items-center justify-content-end gap-3 gap-lg-4">
            <div class="account-dropdown-wrapper">
              <button type="button" class="header-action account-trigger d-flex align-items-center gap-2">
                <span class="header-account-avatar">
                  <img v-if="isAuthenticated && headerAvatarUrl" :src="headerAvatarUrl"
                    class="header-account-avatar-img" alt="avatar" @error="headerAvatarUrl = ''" />

                  <i v-else class="bi bi-person"></i>
                </span>

                <span class="d-none d-md-inline">
                  {{ isAuthenticated ? displayName : "Tài khoản" }}
                </span>

                <i class="bi bi-chevron-down account-chevron d-none d-md-inline"></i>
              </button>

              <div class="account-dropdown">
                <div v-if="!isAuthenticated" class="guest-dropdown">
                  <p class="dropdown-title mb-3">Tài khoản khách hàng</p>

                  <RouterLink to="/login" class="btn dropdown-login-btn w-100 mb-2">
                    Đăng nhập
                  </RouterLink>

                  <RouterLink to="/register" class="btn dropdown-register-btn w-100">
                    Đăng ký
                  </RouterLink>
                </div>

                <div v-else class="logged-dropdown">
                  <div class="user-block d-flex align-items-center gap-3">
                    <div class="user-avatar">
                      <img v-if="headerAvatarUrl" :src="headerAvatarUrl" class="user-avatar-img" alt="avatar"
                        @error="headerAvatarUrl = ''" />

                      <span v-else>
                        {{ userInitial }}
                      </span>
                    </div>

                    <div class="user-info">
                      <div class="user-name">{{ displayName }}</div>

                      <!-- ĐÃ SỬA: Ẩn Hạng và Điểm đi, thay bằng chữ Khách hàng -->
                      <div v-if="isUserRole" class="rank-badge mt-1">
                        <i class="bi bi-person-badge me-1"></i>
                        Khách hàng
                      </div>

                      <div v-else class="rank-badge mt-1">
                        <i class="bi bi-shield-lock me-1"></i>
                        {{ currentRole }}
                      </div>
                    </div>
                  </div>

                  <div class="dropdown-divider"></div>

                  <RouterLink v-if="isUserRole" :to="{
                    path: '/customer/profile',
                    query: { tab: 'profile' },
                  }" class="account-menu-item">
                    <i class="bi bi-person-circle"></i>
                    <span>Thông tin cá nhân</span>
                  </RouterLink>

                  <RouterLink v-if="isUserRole" :to="{
                    path: '/customer/profile',
                    query: { tab: 'password' },
                  }" class="account-menu-item">
                    <i class="bi bi-lock"></i>
                    <span>Đổi mật khẩu</span>
                  </RouterLink>

                  <RouterLink v-if="isUserRole" :to="{
                    path: '/customer/profile',
                    query: { tab: 'favorites' },
                  }" class="account-menu-item">
                    <i class="bi bi-heart"></i>
                    <span>Sản phẩm yêu thích</span>
                  </RouterLink>

                  <RouterLink v-if="isUserRole" :to="{
                    path: '/customer/profile',
                    query: { tab: 'orders' },
                  }" class="account-menu-item">
                    <i class="bi bi-receipt"></i>
                    <span>Lịch sử đơn hàng</span>
                  </RouterLink>

                  <RouterLink v-if="isOwnerRole" to="/admin/dashboard" class="account-menu-item">
                    <i class="bi bi-speedometer2"></i>
                    <span>Trang quản trị</span>
                  </RouterLink>

                  <RouterLink v-if="isStaffRole" to="/admin/pos" class="account-menu-item">
                    <i class="bi bi-shop"></i>
                    <span>Bán hàng POS</span>
                  </RouterLink>

                  <button type="button" class="account-menu-item logout-item" @click="handleLogout">
                    <i class="bi bi-box-arrow-right"></i>
                    <span>Đăng xuất</span>
                  </button>
                </div>
              </div>
            </div>

            <RouterLink v-if="!isStaffRole" to="/cart"
              class="header-action cart-action d-flex align-items-center gap-2 text-decoration-none position-relative">
              <span class="action-icon-circle">
                <i class="bi bi-bag"></i>
              </span>

              <span class="d-none d-md-inline">Giỏ hàng</span>

              <span class="cart-badge">
                {{ cartBadgeText }}
              </span>
            </RouterLink>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { RouterLink, useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import Swal from "sweetalert2";

import { useAuthStore } from "@/modules/auth/stores/authStore";
import { useCartStore } from "@/store/cartStore";
import api from "@/common/api";
import logoAura from "@/assets/logo.png";

const router = useRouter();
const authStore = useAuthStore();
const cartStore = useCartStore();

const { isAuthenticated, name } = storeToRefs(authStore);
const { cartCount } = storeToRefs(cartStore);

const logoLoadFailed = ref(false);
const keyword = ref("");

/* ===================== GỢI Ý TÌM KIẾM (THÊM MỚI) ===================== */

const SEARCH_BACKEND_URL = "http://localhost:8080";
const SUGGEST_DEBOUNCE_MS = 350;
const SUGGEST_SIZE = 6; // số sản phẩm hiển thị trong dropdown
const SUGGEST_FETCH_SIZE = 50; // số bản ghi tải về để lọc lại ở client

interface SuggestItem {
  id: number;
  name: string;
  brand: string;
  image: string;
  price: number;
  priceFrom: boolean;
}

const suggestList = ref<SuggestItem[]>([]);
const suggestLoading = ref(false);
const showSuggest = ref(false);
const searchWrapperRef = ref<HTMLElement | null>(null);

let suggestTimer: ReturnType<typeof window.setTimeout> | undefined;
let suggestSeq = 0;

const formatSuggestPrice = (value: number) => {
  return new Intl.NumberFormat("vi-VN").format(Number(value || 0)) + " đ";
};

const normalizeSuggestImage = (url: unknown) => {
  const rawUrl = String(url || "").trim();

  if (!rawUrl) {
    return "";
  }

  if (
    rawUrl.startsWith("http://") ||
    rawUrl.startsWith("https://") ||
    rawUrl.startsWith("data:image") ||
    rawUrl.startsWith("blob:")
  ) {
    return rawUrl;
  }

  if (rawUrl.startsWith("/")) {
    return `${SEARCH_BACKEND_URL}${rawUrl}`;
  }

  return `${SEARCH_BACKEND_URL}/${rawUrl}`;
};

const resolveSuggestImage = (raw: any): string => {
  const directImage =
    raw?.mainImage ??
    raw?.mainImageUrl ??
    raw?.MainImageUrl ??
    raw?.thumbnailUrl ??
    raw?.ThumbnailUrl ??
    raw?.imageUrl ??
    raw?.ImageUrl ??
    raw?.image ??
    raw?.Image;

  if (directImage) {
    return normalizeSuggestImage(directImage);
  }

  const imageList =
    raw?.images ||
    raw?.Images ||
    raw?.productImages ||
    raw?.ProductImages ||
    raw?.productImageList ||
    raw?.ProductImageList ||
    [];

  if (Array.isArray(imageList) && imageList.length > 0) {
    const firstImage = imageList[0];

    const url =
      typeof firstImage === "string"
        ? firstImage
        : firstImage?.imageUrl ??
          firstImage?.ImageUrl ??
          firstImage?.url ??
          firstImage?.path;

    if (url) {
      return normalizeSuggestImage(url);
    }
  }

  const variants =
    raw?.variants || raw?.productVariants || raw?.productVariantList || [];

  if (Array.isArray(variants) && variants.length > 0) {
    const variantImage =
      variants[0]?.mainImage ??
      variants[0]?.imageUrl ??
      variants[0]?.ImageUrl ??
      variants[0]?.image;

    if (variantImage) {
      return normalizeSuggestImage(variantImage);
    }
  }

  return "";
};

const resolveSuggestRows = (data: any): any[] => {
  if (Array.isArray(data)) {
    return data;
  }

  if (Array.isArray(data?.content)) {
    return data.content;
  }

  if (Array.isArray(data?.data?.content)) {
    return data.data.content;
  }

  if (Array.isArray(data?.data)) {
    return data.data;
  }

  return [];
};

const mapSuggestItem = (p: any): SuggestItem => {
  const rawVariants =
    p?.variants || p?.productVariants || p?.productVariantList || [];

  const variantList = Array.isArray(rawVariants) ? rawVariants : [];

  const variantPrices = variantList
    .map((v: any) => Number(v?.salePrice ?? v?.price ?? 0))
    .filter((n: number) => n > 0);

  const discountPercent = Number(p.discountPercent ?? p.discount ?? 0);

  let price = 0;
  let priceFrom = false;

  if (variantPrices.length > 0) {
    price = Math.min(...variantPrices);
    priceFrom = Math.max(...variantPrices) > price;
  } else {
    const basePrice =
      Number(p.minPrice ?? 0) ||
      Number(p.salePrice ?? 0) ||
      Number(p.price ?? 0) ||
      Number(p.originalPrice ?? 0) ||
      0;

    price =
      discountPercent > 0 && Number(p.salePrice ?? 0) <= 0
        ? basePrice - (basePrice * discountPercent) / 100
        : basePrice;
  }

  return {
    id: Number(p.productId ?? p.id ?? 0),
    name: p.name || p.productName || "Sản phẩm",
    brand: p?.brand?.name || p?.brandName || p?.brand || "Premium",
    image: resolveSuggestImage(p),
    price,
    priceFrom,
  };
};

const matchSuggestKeyword = (p: any, lowerText: string) => {
  const name = String(p?.name || p?.productName || "").toLowerCase();

  const brand = String(
    p?.brand?.name || p?.brandName || p?.brand || ""
  ).toLowerCase();

  return name.includes(lowerText) || brand.includes(lowerText);
};

const fetchSuggest = async (searchText: string) => {
  const currentSeq = ++suggestSeq;
  suggestLoading.value = true;

  try {
    const res = await api.get("/v1/products", {
      params: {
        keyword: searchText,
        page: 0,
        size: SUGGEST_FETCH_SIZE,
      },
    });

    if (currentSeq !== suggestSeq) {
      return; 
    }

    const rows = resolveSuggestRows(res.data);
    const lowerText = searchText.toLowerCase();

    const matched = rows.filter((p: any) => matchSuggestKeyword(p, lowerText));

    const finalRows = matched.length > 0 ? matched : [];

    suggestList.value = finalRows.slice(0, SUGGEST_SIZE).map(mapSuggestItem);
  } catch (error) {
    if (currentSeq !== suggestSeq) {
      return;
    }

    console.error("Lỗi gợi ý tìm kiếm:", error);
    suggestList.value = [];
  } finally {
    if (currentSeq === suggestSeq) {
      suggestLoading.value = false;
    }
  }
};

const handleSearchFocus = () => {
  if (keyword.value.trim()) {
    showSuggest.value = true;
  }
};

const closeSuggest = () => {
  showSuggest.value = false;
};

const goToSuggest = (item: SuggestItem) => {
  closeSuggest();

  if (item.id > 0) {
    router.push({
      name: "SingleProduct",
      params: {
        id: item.id,
      },
    });
  }
};

const handleSuggestImageError = (event: Event) => {
  const target = event.target as HTMLImageElement | null;

  if (target) {
    target.style.display = "none";
  }
};

const handleSuggestClickOutside = (event: MouseEvent) => {
  if (
    searchWrapperRef.value &&
    !searchWrapperRef.value.contains(event.target as Node)
  ) {
    showSuggest.value = false;
  }
};

watch(keyword, (value) => {
  const trimmedKeyword = value.trim();

  if (suggestTimer) {
    window.clearTimeout(suggestTimer);
  }

  if (!trimmedKeyword) {
    showSuggest.value = false;
    suggestList.value = [];
    suggestLoading.value = false;
    suggestSeq += 1;
    return;
  }

  showSuggest.value = true;

  suggestTimer = window.setTimeout(() => {
    fetchSuggest(trimmedKeyword);
  }, SUGGEST_DEBOUNCE_MS);
});

/* =================== HẾT PHẦN GỢI Ý TÌM KIẾM =================== */

const userRank = ref("Bronze");
const userPoints = ref(0);

const headerName = ref(localStorage.getItem("name") || "");
const headerAvatarUrl = ref(localStorage.getItem("customerAvatarUrl") || "");

const currentRole = computed(() => {
  return String(authStore.role || localStorage.getItem("role") || "")
    .toUpperCase()
    .trim();
});

const isUserRole = computed(() => currentRole.value === "USER");

const isOwnerRole = computed(() => currentRole.value === "OWNER");

const isStaffRole = computed(() =>
  ["OWNER", "MANAGER", "CASHIER"].includes(currentRole.value)
);

const visibleCartCount = computed(() => {
  if (!isAuthenticated.value || !isUserRole.value) {
    return 0;
  }

  return Number(cartCount.value || 0);
});

const cartBadgeText = computed(() => {
  return visibleCartCount.value > 99 ? "99+" : String(visibleCartCount.value);
});

const displayName = computed(() => {
  return (
    headerName.value || name.value || localStorage.getItem("name") || "Khách"
  );
});

const userInitial = computed(() => {
  const rawName = displayName.value.trim();

  if (!rawName) {
    return "U";
  }

  const nameParts = rawName.split(" ").filter(Boolean);
  const lastName = nameParts[nameParts.length - 1];

  return lastName?.charAt(0).toUpperCase() || "U";
});

const refreshCartCount = async () => {
  if (!isAuthenticated.value || !isUserRole.value) {
    cartStore.clearCartLocal();
    return;
  }

  await cartStore.loadCart();
};

const handleCartUpdated = () => {
  refreshCartCount();
};

const fetchCustomerProfile = async () => {
  if (!isAuthenticated.value || !isUserRole.value) {
    userRank.value = "Bronze";
    userPoints.value = 0;
    headerAvatarUrl.value = "";
    return;
  }

  try {
    const res = await api.get("/customer/profile");

    headerName.value =
      res.data?.name || localStorage.getItem("name") || "Khách";

    headerAvatarUrl.value = res.data?.avatarUrl || "";
    userRank.value = res.data?.customerRank || "Bronze";
    userPoints.value = Number(res.data?.loyaltyPoints || 0);

    localStorage.setItem("name", headerName.value);
    localStorage.setItem("customerAvatarUrl", headerAvatarUrl.value);
  } catch (error) {
    console.error("Lỗi lấy thông tin Rank/Điểm:", error);
  }
};

const handleProfileUpdated = (event: Event) => {
  const customEvent = event as CustomEvent<{
    name?: string;
    avatarUrl?: string | null;
    customerRank?: string;
    loyaltyPoints?: number;
  }>;

  const detail = customEvent.detail;

  if (detail?.name) {
    headerName.value = detail.name;
    localStorage.setItem("name", detail.name);
  }

  if (detail?.avatarUrl !== undefined) {
    headerAvatarUrl.value = detail.avatarUrl || "";
    localStorage.setItem("customerAvatarUrl", headerAvatarUrl.value);
  }

  if (detail?.customerRank) {
    userRank.value = detail.customerRank;
  }

  if (detail?.loyaltyPoints !== undefined) {
    userPoints.value = Number(detail.loyaltyPoints || 0);
  }
};

const handleSearch = () => {
  showSuggest.value = false; 

  const trimmedKeyword = keyword.value.trim();

  if (!trimmedKeyword) {
    router.push({ name: "ProductList" }); 
    return;
  }

  router.push({
    name: "ProductList",
    query: {
      keyword: trimmedKeyword,
    },
  });
};

const handleLogout = () => {
  Swal.fire({
    title: "Xác nhận đăng xuất?",
    text: "Bạn có chắc chắn muốn rời khỏi phiên làm việc này không?",
    icon: "warning",
    showCancelButton: true,
    confirmButtonColor: "#bd9a5f",
    cancelButtonColor: "#6c757d",
    confirmButtonText: "Đồng ý",
    cancelButtonText: "Hủy",
    background: "#ffffff",
    color: "#000000",
    iconColor: "#dc3545",
    customClass: {
      popup: "border-gold-sweetalert",
    },
  }).then((result) => {
    if (!result.isConfirmed) {
      return;
    }

    authStore.logout();
    cartStore.clearCartLocal();

    userRank.value = "Bronze";
    userPoints.value = 0;
    headerName.value = "";
    headerAvatarUrl.value = "";

    localStorage.removeItem("customerAvatarUrl");

    router.push("/");
  });
};

onMounted(() => {
  window.addEventListener("customer-profile-updated", handleProfileUpdated);
  window.addEventListener("cart-updated", handleCartUpdated);
  document.addEventListener("mousedown", handleSuggestClickOutside);

  fetchCustomerProfile();
  refreshCartCount();
});

onBeforeUnmount(() => {
  window.removeEventListener("customer-profile-updated", handleProfileUpdated);
  window.removeEventListener("cart-updated", handleCartUpdated);
  document.removeEventListener("mousedown", handleSuggestClickOutside);

  if (suggestTimer) {
    window.clearTimeout(suggestTimer);
  }
});

watch(
  () => [isAuthenticated.value, currentRole.value],
  () => {
    fetchCustomerProfile();
    refreshCartCount();
  }
);

const translateRank = (rankName: string) => {
  if (!rankName) return 'Thành viên mới';

  const ranks: Record<string, string> = {
    'BRONZE': 'Đồng',
    'SILVER': 'Bạc',
    'GOLD': 'Vàng',
    'PLATINUM': 'Bạch kim',
    'DIAMOND': 'Kim cương'
  };

  return ranks[rankName.toUpperCase()] || rankName;
};
</script>

<style scoped>
.shop-header {
  position: relative;
  z-index: 1050;
  min-height: 104px;
  overflow: visible;
  background: radial-gradient(circle at top left,
      rgba(189, 154, 95, 0.16),
      transparent 32%),
    linear-gradient(135deg, #030d1a 0%, #07172f 54%, #051024 100%);
  border-bottom: 1px solid rgba(189, 154, 95, 0.22);
}

.brand-logo-link {
  min-height: 104px;
  width: 260px;
  padding-left: 14px;
  overflow: visible;
}

.brand-logo-img {
  width: 210px;
  height: auto;
  max-height: 96px;
  object-fit: contain;
  object-position: left center;
  display: block;
  transform: scale(1.35);
  transform-origin: left center;
  filter: drop-shadow(0 10px 26px rgba(189, 154, 95, 0.26));
}

.brand-logo-text {
  text-align: center;
  line-height: 1;
}

.brand-name {
  font-family: Georgia, "Times New Roman", serif;
  font-size: 48px;
  letter-spacing: 5px;
  font-weight: 600;
  color: var(--aura-gold);
}

.brand-subtitle {
  margin-top: 7px;
  font-size: 12px;
  letter-spacing: 7px;
  color: var(--aura-gold);
  font-weight: 600;
}

.search-wrapper {
  max-width: 700px;
}

.search-input {
  height: 50px;
  padding-left: 26px;
  padding-right: 58px;
  border: 1px solid rgba(189, 154, 95, 0.42);
  background: rgba(255, 255, 255, 0.075);
  color: #ffffff;
  font-size: 15px;
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.72);
}

.search-input:focus {
  background: rgba(255, 255, 255, 0.11);
  color: #ffffff;
  border-color: var(--aura-gold);
  box-shadow: 0 0 0 0.22rem rgba(189, 154, 95, 0.16);
}

.search-button {
  position: absolute;
  top: 50%;
  right: 7px;
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 50%;
  transform: translateY(-50%);
  background: transparent;
  color: var(--aura-gold);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.22s ease;
}

.search-button:hover {
  background: rgba(189, 154, 95, 0.14);
}

.search-button i {
  font-size: 18px;
}

/* ============ DROPDOWN GỢI Ý TÌM KIẾM ============ */

.search-suggest {
  position: absolute;
  top: calc(100% + 10px);
  left: 0;
  right: 0;
  z-index: 3000;
  padding: 8px;
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid rgba(189, 154, 95, 0.2);
  box-shadow: 0 24px 60px rgba(5, 16, 36, 0.28);
  max-height: 420px;
  overflow-y: auto;
}

.suggest-state {
  padding: 22px 14px;
  text-align: center;
  color: #777777;
  font-size: 13px;
  font-weight: 600;
}

.suggest-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: 10px;
  text-decoration: none;
  transition: background 0.2s ease;
}

.suggest-item:hover {
  background: #fffaf2;
}

.suggest-img {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid rgba(6, 19, 43, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.suggest-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.suggest-img i {
  color: #cbd5e0;
  font-size: 18px;
}

.suggest-info {
  flex: 1;
  min-width: 0;
}

.suggest-brand {
  margin: 0;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.6px;
  text-transform: uppercase;
  color: #8c8c8c;
}

.suggest-name {
  margin: 2px 0 0;
  font-size: 13px;
  font-weight: 700;
  color: var(--aura-black);
}

.suggest-price {
  font-size: 13px;
  font-weight: 800;
  color: var(--aura-black);
  white-space: nowrap;
  flex-shrink: 0;
}

.suggest-price-prefix {
  font-size: 11px;
  font-weight: 600;
  color: #8c8c8c;
  margin-right: 2px;
}

.suggest-all {
  width: 100%;
  margin-top: 4px;
  padding: 12px;
  border: none;
  border-top: 1px solid rgba(189, 154, 95, 0.14);
  background: transparent;
  color: var(--aura-gold);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.4px;
  cursor: pointer;
  transition: color 0.2s ease;
}

.suggest-all:hover {
  color: var(--aura-gold-hover);
}

.suggest-fade-enter-active,
.suggest-fade-leave-active {
  transition: all 0.18s ease;
}

.suggest-fade-enter-from,
.suggest-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* ============ HẾT PHẦN DROPDOWN GỢI Ý ============ */

.header-action {
  color: rgba(255, 255, 255, 0.92);
  font-weight: 600;
  font-size: 15px;
  transition: all 0.22s ease;
}

.header-action:hover {
  color: var(--aura-gold);
}

.account-trigger {
  border: none;
  outline: none;
  background: transparent;
  padding: 0;
}

.account-chevron {
  font-size: 12px;
  opacity: 0.8;
  transition: transform 0.22s ease;
}

.account-dropdown-wrapper:hover .account-chevron {
  transform: rotate(180deg);
}

.header-account-avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  border: 2px solid #bd9a5f;
  background: rgba(255, 255, 255, 0.045);
  color: var(--aura-gold);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
  transition: all 0.22s ease;
}

.header-account-avatar i {
  font-size: 19px;
  line-height: 1;
}

.header-account-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.account-trigger:hover .header-account-avatar {
  box-shadow: 0 0 0 4px rgba(189, 154, 95, 0.16);
}

.action-icon-circle {
  width: 40px;
  height: 40px;
  border: 1px solid rgba(189, 154, 95, 0.58);
  border-radius: 50%;
  color: var(--aura-gold);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.045);
  transition: all 0.22s ease;
}

.action-icon-circle i {
  font-size: 18px;
  line-height: 1;
}

.header-action:hover .action-icon-circle {
  background: var(--aura-gold);
  color: var(--aura-navy);
}

.cart-badge {
  position: absolute;
  top: -6px;
  right: -8px;
  width: 17px;
  height: 17px;
  border-radius: 50%;
  background: var(--aura-gold);
  color: var(--aura-navy);
  border: 2px solid var(--aura-navy);
  font-size: 10px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

.account-dropdown-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.account-dropdown {
  position: absolute;
  top: calc(100% + 18px);
  right: 0;
  z-index: 3000;
  width: 320px;
  padding: 18px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid rgba(189, 154, 95, 0.18);
  box-shadow: 0 24px 60px rgba(5, 16, 36, 0.22);
  opacity: 0;
  visibility: hidden;
  transform: translateY(10px);
  transition: all 0.22s ease;
  pointer-events: none;
}

.account-dropdown::before {
  content: "";
  position: absolute;
  top: -8px;
  right: 46px;
  width: 16px;
  height: 16px;
  background: #ffffff;
  border-left: 1px solid rgba(189, 154, 95, 0.18);
  border-top: 1px solid rgba(189, 154, 95, 0.18);
  transform: rotate(45deg);
}

.account-dropdown::after {
  content: "";
  position: absolute;
  top: -24px;
  left: 0;
  width: 100%;
  height: 24px;
  background: transparent;
}

.account-dropdown-wrapper:hover .account-dropdown {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
  pointer-events: auto;
}

.dropdown-title {
  color: var(--aura-black);
  font-weight: 800;
  font-size: 15px;
}

.dropdown-login-btn {
  background: var(--aura-gold);
  color: #ffffff;
  font-weight: 800;
  border-radius: 8px;
  border: 1px solid var(--aura-gold);
}

.dropdown-login-btn:hover {
  background: var(--aura-gold-hover);
  color: #ffffff;
}

.dropdown-register-btn {
  background: #ffffff;
  color: var(--aura-gold);
  font-weight: 800;
  border-radius: 8px;
  border: 1px solid var(--aura-gold);
}

.dropdown-register-btn:hover {
  background: rgba(189, 154, 95, 0.08);
  color: var(--aura-gold-hover);
}

.user-block {
  padding-bottom: 4px;
}

.user-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #d1b06e, var(--aura-gold));
  color: #ffffff;
  font-weight: 900;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 24px rgba(189, 154, 95, 0.24);
  overflow: hidden;
  border: 2px solid #bd9a5f;
}

.user-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-name {
  color: var(--aura-black);
  font-weight: 800;
  font-size: 15px;
}

.rank-badge {
  width: fit-content;
  padding: 5px 9px;
  border-radius: 999px;
  background: rgba(189, 154, 95, 0.14);
  color: var(--aura-gold);
  border: 1px solid rgba(189, 154, 95, 0.26);
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

.dropdown-divider {
  margin: 16px 0 10px;
  border-top: 1px solid rgba(26, 26, 26, 0.08);
}

.account-menu-item {
  width: 100%;
  min-height: 42px;
  padding: 10px 8px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--aura-black);
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  font-weight: 700;
  transition: all 0.2s ease;
}

.account-menu-item i {
  width: 20px;
  color: var(--aura-gold);
  font-size: 17px;
}

.account-menu-item:hover {
  background: rgba(189, 154, 95, 0.09);
  color: var(--aura-gold);
}

.logout-item {
  cursor: pointer;
  color: #b31320;
}

.logout-item i {
  color: #b31320;
}

.logout-item:hover {
  background: rgba(179, 19, 32, 0.08);
  color: #b31320;
}

@media (max-width: 991.98px) {
  .shop-header {
    min-height: auto;
    padding: 16px 0;
  }

  .brand-logo-link {
    min-height: auto;
    width: 190px;
    padding-left: 4px;
  }

  .brand-logo-img {
    width: 150px;
    max-height: 72px;
    transform: scale(1.18);
  }

  .brand-name {
    font-size: 32px;
  }

  .brand-subtitle {
    font-size: 10px;
    letter-spacing: 5px;
  }

  .account-dropdown {
    right: -70px;
  }

  .account-dropdown::before {
    right: 116px;
  }
}

@media (max-width: 575.98px) {
  .brand-logo-link {
    width: 140px;
  }

  .brand-logo-img {
    width: 118px;
    max-height: 58px;
    transform: scale(1.12);
  }

  .action-icon-circle {
    width: 35px;
    height: 35px;
  }

  .header-action {
    font-size: 14px;
  }

  .account-dropdown {
    width: 290px;
    right: -88px;
  }

  .account-dropdown::before {
    right: 132px;
  }

  .header-account-avatar {
    width: 38px;
    height: 38px;
  }
}
</style>