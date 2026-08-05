<template>
  <div class="product-grid-wrapper">
    <div v-if="validProductList.length === 0" class="empty-product">
      Không tìm thấy sản phẩm phù hợp hoặc sản phẩm đã hết hạn.
    </div>

    <div v-else class="product-grid">
      <div
        v-for="item in validProductList"
        :key="item.id || item.productId"
        class="product-card luxury-card"
        @click="emit('open-detail', item)"
      >
        <div class="card-img-wrapper">
          <div v-if="getDiscountPercent(item) > 0" class="sale-badge">
            -{{ getDiscountPercent(item) }}%
          </div>
          <span v-if="isNearExpiry(item)" class="expiry-warning-badge">
            <i class="bi bi-exclamation-triangle-fill me-1"></i> Gần hết hạn
          </span>
          <img :src="getProductImage(item)" :alt="item?.name || 'Sản phẩm'" @error="handleImageError" />
          <button class="btn-heart-small" type="button" :class="{ active: isFavorited(item) }" :disabled="isFavoriteLoading(item)" @click.stop="toggleFavorite(item)" :title="isFavorited(item) ? 'Bỏ yêu thích' : 'Thêm vào yêu thích'">
            <span v-if="isFavoriteLoading(item)" class="spinner-border spinner-border-sm"></span>
            <svg v-else viewBox="0 0 24 24" :fill="isFavorited(item) ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="1.7">
              <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z" />
            </svg>
          </button>
          <button class="btn-compare-small" type="button" :class="{ active: isInCompare(item) }" @click.stop="toggleCompare(item)" :title="isInCompare(item) ? 'Bỏ so sánh' : 'Thêm vào so sánh'">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M16 3l4 4-4 4"/><path d="M20 7H4"/><path d="M8 21l-4-4 4-4"/><path d="M4 17h16"/>
            </svg>
          </button>
        </div>
        <div class="card-info">
          <div class="card-brand">{{ getBrandName(item) }}</div>
          <h3 class="card-name">{{ item?.name || "Tên sản phẩm" }}</h3>
          <div class="card-rating">
            <span class="stars">{{ getStarsDisplay(item) }}</span>
            <span class="score">{{ getRatingScore(item) }} | {{ getReviewCount(item) }} đánh giá</span>
          </div>
          <div class="card-price-box">
            <span class="card-price">{{ formatPrice(item) }}</span>
            <span v-if="getOldPrice(item) > getVariantPrice(item)" class="card-old-price">{{ formatCurrency(getOldPrice(item)) }}</span>
          </div>
          <div class="card-actions">
            <button type="button" class="btn-buy-now-small" :disabled="isBuyNowDisabled(item)" @click.stop="openVariantModal('BUY', item)">Mua ngay</button>
            <button type="button" class="btn-view-detail" @click.stop="emit('open-detail', item)">Chi tiết</button>
          </div>
          <div v-if="getVariantStock(item) <= 0" class="card-stock-warning">Tạm hết hàng</div>
        </div>
      </div>
    </div>

    <!-- THANH NỔI (FLOATING BAR) CHỌN SO SÁNH -->
    <div class="compare-bar" :class="{ show: compareList.length > 0 }">
      <div class="cb-container">
        <div class="cb-left">
          <div class="cb-title">So sánh ({{ compareList.length }}/3)</div>
          <div class="cb-slots">
            <div class="cb-slot filled" v-for="p in compareList" :key="p.id || p.productId">
              <img :src="getProductImage(p)" :alt="p.name" />
              <div class="cb-slot-info">
                <p>{{ p.name }}</p>
                <span>{{ formatCurrency(getComparePrice(p)) }}</span>
              </div>
              <button class="btn-remove-cb" @click="removeFromCompare(p)">✕</button>
            </div>
            <div class="cb-slot empty-slot cursor-pointer" v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty' + i" @click="openPickerModal" title="Chọn thêm sản phẩm so sánh">
              <i class="bi bi-plus-circle me-2"></i> Thêm sản phẩm
            </div>
          </div>
        </div>
        <div class="cb-right">
          <button class="cb-btn-clear" @click="compareList = []">Xóa hết</button>
          <button class="cb-btn-compare" :disabled="compareList.length < 2" @click="showCompareModal = true">So sánh ngay</button>
        </div>
      </div>
    </div>

    <!-- BẢNG POPUP SO SÁNH CHUYÊN NGHIỆP -->
    <Teleport to="body">
      <div class="compare-modal-overlay" v-if="showCompareModal" @click.self="showCompareModal = false">
        <div class="compare-modal-box">
          <div class="cm-header">
            <h3>So sánh thông số</h3>
            <button class="cm-close" @click="showCompareModal = false">✕</button>
          </div>
          <div class="cm-body">
            <table class="table-compare">
              <thead class="sticky-header">
                <tr>
                  <th class="spec-label-col">Sản phẩm</th>
                  <td v-for="p in compareList" :key="'img' + (p.id || p.productId)" class="spec-value-col">
                    <button class="btn-remove-from-table" @click="removeFromCompare(p)" title="Xóa khỏi so sánh"><i class="bi bi-x"></i></button>
                    <div class="cm-img-wrapper clickable-item" @click="goToDetailFromCompare(p)" title="Xem chi tiết">
                      <img :src="getProductImage(p)" class="cm-img" />
                    </div>
                    <h4 class="cm-name clickable-item" @click="goToDetailFromCompare(p)" title="Xem chi tiết">{{ p.name }}</h4>
                    <div class="cm-header-price">
                      {{ formatCurrency(getComparePrice(p)) }}
                      <span v-if="getCompareDiscount(p) > 0" class="flash-sale-badge ms-2">-{{ getCompareDiscount(p) }}%</span>
                    </div>
                  </td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-th-' + i" class="spec-value-col empty-col">
                    <div class="empty-product-slot" @click="openPickerModal" title="Chọn thêm sản phẩm so sánh">
                      <i class="bi bi-plus-circle-dotted"></i><span>Thêm sản phẩm</span>
                    </div>
                  </td>
                </tr>
              </thead>
              <tbody>
                <tr><td colspan="4" class="group-header">Thông tin cơ bản</td></tr>
                <tr>
                  <th class="spec-label-col">Thương hiệu</th>
                  <td v-for="p in compareList" :key="'brand' + (p.id || p.productId)" class="fw-bold text-dark">{{ getCompareValue(p, "brand") }}</td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-brand-' + i"></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Phân loại dung tích</th>
                  <td v-for="p in compareList" :key="'var' + (p.id || p.productId)">
                    <select v-if="p.variants && p.variants.length > 0" class="compare-select" v-model="compareVariantIds[p.id || p.productId]">
                      <option v-for="v in getSortedVariants(p)" :key="v.productVariantId || v.variantId || v.id" :value="v.productVariantId || v.variantId || v.id">
                        {{ formatVariantName(v) }}
                      </option>
                    </select>
                    <span v-else class="text-muted">Mặc định</span>
                  </td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-var-' + i"></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Loại chai</th>
                  <td v-for="p in compareList" :key="'bottle' + (p.id || p.productId)">{{ getCompareBottleType(p) }}</td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-bottle-' + i"></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Giá ưu đãi</th>
                  <td v-for="p in compareList" :key="'price' + (p.id || p.productId)">
                    <div class="cm-price-val">
                      {{ formatCurrency(getComparePrice(p)) }}
                      <span v-if="getCompareDiscount(p) > 0" class="flash-sale-badge ms-2">-{{ getCompareDiscount(p) }}%</span>
                    </div>
                    <div v-if="getCompareOriginalPrice(p) > getComparePrice(p)" class="text-decoration-line-through text-muted small mt-1">
                      {{ formatCurrency(getCompareOriginalPrice(p)) }}
                    </div>
                  </td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-price-' + i"></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Tình trạng kho</th>
                  <td v-for="p in compareList" :key="'stock' + (p.id || p.productId)">
                    <span :class="getCompareStock(p) > 0 ? 'text-success fw-bold' : 'text-danger fw-bold'">
                      {{ getCompareStock(p) > 0 ? "Còn hàng" : "Hết hàng" }}
                    </span>
                  </td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-stock-' + i"></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Đánh giá</th>
                  <td v-for="p in compareList" :key="'rating' + (p.id || p.productId)" class="cm-rating">
                    <div class="d-flex align-items-center gap-1" style="justify-content: center;">
                      <span class="score">{{ getRatingScore(p) }} ★</span>
                      <span class="count">({{ getReviewCount(p) }})</span>
                    </div>
                  </td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-rating-' + i"></td>
                </tr>
                <tr><td colspan="4" class="group-header">Đặc tính sản phẩm</td></tr>
                <tr>
                  <th class="spec-label-col">Nhóm hương chính</th>
                  <td v-for="p in compareList" :key="'scent' + (p.id || p.productId)">{{ getCompareValue(p, "scent") }}</td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-scent-' + i"></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Nồng độ lưu hương</th>
                  <td v-for="p in compareList" :key="'con' + (p.id || p.productId)">{{ getCompareValue(p, "concentration") }}</td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-con-' + i"></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Giới tính</th>
                  <td v-for="p in compareList" :key="'gen' + (p.id || p.productId)">{{ getCompareValue(p, "gender") }}</td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-gen-' + i"></td>
                </tr>
                <tr><td colspan="4" class="group-header bg-white border-bottom-0 pt-4"></td></tr>
                <tr>
                  <th class="spec-label-col border-bottom-0"></th>
                  <td v-for="p in compareList" :key="'act' + (p.id || p.productId)" class="border-bottom-0 pb-4">
                    <button class="cm-btn-buy" :disabled="isCompareBuyDisabled(p)" @click="buyFromCompare(p)">
                      <i class="bi bi-cart-plus me-1"></i> {{ isCompareBuyDisabled(p) ? "Tạm hết hàng" : "Thêm giỏ hàng" }}
                    </button>
                  </td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-act-' + i" class="border-bottom-0"></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- MODAL CHỌN NHANH SẢN PHẨM SO SÁNH -->
    <Teleport to="body">
      <div class="compare-modal-overlay" v-if="showPickerModal" @click.self="showPickerModal = false">
        <div class="product-picker-box">
          <div class="cm-header">
            <h3>Chọn sản phẩm so sánh</h3>
            <button class="cm-close" @click="showPickerModal = false">✕</button>
          </div>
          <div class="picker-search-bar">
            <input type="text" v-model="pickerSearchKeyword" placeholder="Tìm kiếm theo tên sản phẩm hoặc thương hiệu..." class="picker-input" />
          </div>
          <div class="picker-body">
            <div v-if="pickerLoading" class="text-center py-5">
              <span class="spinner-border text-warning"></span>
            </div>
            <div v-else-if="filteredPickerProducts.length === 0" class="text-center py-5 text-muted">
              Không tìm thấy sản phẩm phù hợp.
            </div>
            <div v-else class="picker-grid">
              <div v-for="item in filteredPickerProducts" :key="item.id || item.productId" class="picker-item" :class="{ selected: isInCompare(item) }" @click="toggleItemInPicker(item)">
                <img :src="getProductImage(item)" :alt="item.name" />
                <div class="picker-info">
                  <span class="brand">{{ getBrandName(item) }}</span>
                  <h5 class="name" :title="item.name">{{ item.name }}</h5>
                  <div class="picker-price">{{ formatCurrency(getComparePrice(item)) }}</div>
                </div>
                <div class="picker-check">
                  <i :class="isInCompare(item) ? 'bi bi-check-circle-fill text-success' : 'bi bi-circle text-muted'"></i>
                </div>
              </div>
            </div>
          </div>
          <div class="picker-footer">
            <button class="btn btn-secondary btn-sm px-4" @click="showPickerModal = false">Đóng</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- MODAL MUA NHANH CHỌN BIẾN THỂ -->
    <Teleport to="body">
      <div v-if="showVariantModal" class="custom-modal-overlay" @click.self="showVariantModal = false">
        <div class="variant-modal-box">
          <div class="vm-header">
            <h5>Chọn Phân Loại</h5>
            <button class="vm-close" @click="showVariantModal = false">
              <svg viewBox="0 0 24 24" width="24" height="24" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
            </button>
          </div>

          <div class="vm-product-info">
            <div class="vm-img-box">
              <img :src="modalImage" alt="Product Image" @error="handleImageError" />
            </div>
            <div class="vm-details">
              <h6>{{ modalProduct?.name }}</h6>
              <div class="d-flex align-items-end gap-2 flex-wrap">
                <p class="vm-price mb-0">
                  {{ formatCurrency(selectedVariant ? (selectedVariant.salePrice || selectedVariant.price) : getVariantPrice(modalProduct)) }}
                </p>
                <span v-if="selectedVariant && selectedVariant.salePrice && selectedVariant.salePrice < selectedVariant.originalPrice" class="text-decoration-line-through text-muted small">
                  {{ formatCurrency(selectedVariant.originalPrice) }}
                </span>
                <span v-if="calculatedDiscountPercent > 0" class="flash-sale-badge ms-2">-{{ calculatedDiscountPercent }}%</span>
              </div>
              
              <div class="date-info-box mt-2" v-if="getExpDate(selectedVariant || modalProduct)">
                <p v-if="getMfgDate(selectedVariant || modalProduct)" class="mb-1 text-muted" style="font-size: 13px;">
                  <i class="bi bi-calendar-check me-1"></i> NSX: <strong class="text-dark">{{ getMfgDate(selectedVariant || modalProduct) }}</strong>
                </p>
                <p class="mb-0 text-muted" style="font-size: 13px;">
                  <i class="bi bi-calendar-x me-1"></i> HSD: <strong class="text-dark">{{ getExpDate(selectedVariant || modalProduct) }}</strong>
                </p>
                <div v-if="isNearExpiry(selectedVariant || modalProduct)" class="mt-2 text-warning fw-bold" style="font-size: 13px;">
                  <i class="bi bi-exclamation-triangle-fill me-1"></i> Sản phẩm này cận date / gần hết hạn!
                </div>
              </div>
            </div>
          </div>

          <div class="vm-variants">
            <p class="vm-label">TÙY CHỌN PHÂN LOẠI:</p>
            <div v-if="isLoadingVariants" class="text-center py-4">
              <span class="spinner-border spinner-border-sm me-2" style="color: #b78d52"></span>
              <span style="color: #718096; font-size: 13px">Đang tải thông tin...</span>
            </div>
            <div v-else class="vm-grid">
              <button v-for="v in fullVariants" :key="v.productVariantId || v.id" class="vm-variant-btn" :class="{ selected: (selectedVariant?.productVariantId || selectedVariant?.id) === (v.productVariantId || v.id), disabled: Number(v.stockQuantity || v.stock || 0) <= 0 }" @click="selectedVariant = v; quantity = 1;">
                <span class="vm-v-name">{{ v.displayCapacity }}</span>
              </button>
            </div>
            <hr class="variant-divider" v-if="selectedVariant" />
            <div class="quantity-section" v-if="selectedVariant">
              <p class="vm-label mb-0">SỐ LƯỢNG:</p>
              <div class="quantity-control">
                <div class="qty-wrapper">
                  <button type="button" @click="quantity > 1 ? quantity-- : null" :disabled="quantity <= 1">−</button>
                  <input type="number" v-model="quantity" @input="validateQuantity" @blur="validateQuantity" @keyup.enter="validateQuantity" />
                  <button type="button" @click="increaseQuantity">+</button>
                </div>
                <span class="stock-info"> Kho: {{ maxQuantity }} </span>
              </div>
            </div>
          </div>
          
          <div class="vm-actions d-flex gap-2 mt-3">
            <button class="vm-btn-cart flex-grow-1" :disabled="!selectedVariant || actionLoading || isLoadingVariants" @click="confirmAction('CART')">
              <span v-if="actionLoading && actionType === 'CART'" class="spinner-border spinner-border-sm me-2"></span> THÊM VÀO GIỎ
            </button>
            <button class="vm-btn-buy flex-grow-1" :disabled="!selectedVariant || actionLoading || isLoadingVariants" @click="confirmAction('BUY')">
              <span v-if="actionLoading && actionType === 'BUY'" class="spinner-border spinner-border-sm me-2"></span> MUA NGAY
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch, computed } from "vue";
import { useRouter } from "vue-router";
import Swal from "sweetalert2";
import api from "@/common/api";
import { favoriteService } from "../services/favorite.service";

const props = defineProps<{ productList: any[]; }>();
const emit = defineEmits<{ (e: "open-detail", item: any): void; }>();
const router = useRouter();
const favoritedMap = ref<Record<number, boolean>>({});
const favoriteLoadingMap = ref<Record<number, boolean>>({});
const BACKEND_URL = "http://localhost:8080";
const getToken = () => localStorage.getItem("token");
const getCurrentRole = () => String(localStorage.getItem("role") || localStorage.getItem("userRole") || "").replace("ROLE_", "").toUpperCase().trim();
const hasToken = () => Boolean(getToken());
const isCustomerLoggedIn = () => hasToken() && getCurrentRole() === "USER";

const showToast = (type: "success" | "warning" | "error", title: string, message: string) => {
  Swal.fire({ toast: true, position: 'top-end', icon: type, title: title, text: message, showConfirmButton: false, timer: 2000 });
};

const getStartOfDay = (time: number) => {
  const d = new Date(time);
  d.setHours(0, 0, 0, 0);
  return d.getTime();
};

const parseSafeDate = (dateString: any): number | null => {
  if (!dateString) return null;
  const str = String(dateString).trim();
  if (str.includes('-') && str.split('-')[0]?.length === 4) {
    const d = new Date(str);
    return isNaN(d.getTime()) ? null : d.getTime();
  }
  const parts = str.split(/[\/\-]/);
  if (parts.length >= 3 && parts[0] && parts[1] && parts[2]) {
    const day = parseInt(parts[0] as string, 10);
    const month = parseInt(parts[1] as string, 10) - 1;
    const year = parseInt(parts[2] as string, 10);
    const d = new Date(year, month, day);
    return isNaN(d.getTime()) ? null : d.getTime();
  }
  const fallback = new Date(str);
  return isNaN(fallback.getTime()) ? null : fallback.getTime();
};

const formatSafeDateDisplay = (dateStr: any) => {
  const ts = parseSafeDate(dateStr);
  if (ts === null) return dateStr || "";
  const d = new Date(ts);
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
};

const getMfgDate = (item: any) => formatSafeDateDisplay(item?.manufacturingDate || item?.mfgDate || item?.productionDate);
const getExpDate = (item: any) => formatSafeDateDisplay(item?.expirationDate || item?.expDate);

const isExpiredDate = (dateStr: any): boolean => {
  const time = parseSafeDate(dateStr);
  if (time === null) return false;
  return getStartOfDay(time) < getStartOfDay(Date.now()); 
};

const isFullyExpired = (item: any): boolean => {
  if (!item) return false;
  if (!item.variants || item.variants.length === 0) {
    if (item.expirationDate && isExpiredDate(item.expirationDate)) return true;
    return false;
  }
  const hasValidVariant = item.variants.some((v: any) => {
    if (!v.expirationDate) return true; 
    return !isExpiredDate(v.expirationDate); 
  });
  return !hasValidVariant;
};

const isNearExpiry = (item: any): boolean => {
  if (!item) return false;
  
  if (item.expirationDate !== undefined && item.variants === undefined) {
      if (isExpiredDate(item.expirationDate)) return false;
      const t = parseSafeDate(item.expirationDate);
      if (t !== null) {
          const diffDays = (getStartOfDay(t) - getStartOfDay(Date.now())) / (1000 * 60 * 60 * 24);
          return diffDays >= 0 && diffDays <= 30;
      }
      return false;
  }

  let validTimes: number[] = [];
  if (item.expirationDate && !isExpiredDate(item.expirationDate)) {
    const t = parseSafeDate(item.expirationDate);
    if (t !== null) validTimes.push(getStartOfDay(t));
  }
  if (Array.isArray(item.variants)) {
    item.variants.forEach((v: any) => {
      if (v?.expirationDate && !isExpiredDate(v.expirationDate)) {
        const t = parseSafeDate(v.expirationDate);
        if (t !== null) validTimes.push(getStartOfDay(t));
      }
    });
  }
  if (validTimes.length === 0) return false;
  validTimes.sort((a, b) => a - b);
  const nearestExpTime = validTimes[0];
  if (nearestExpTime === undefined) return false;
  const diffDays = (nearestExpTime - getStartOfDay(Date.now())) / (1000 * 60 * 60 * 24);
  return diffDays >= 0 && diffDays <= 30;
};

const validProductList = computed(() => {
  if (!props.productList) return [];
  return props.productList.filter((item: any) => !isFullyExpired(item));
});

const getBrandName = (item: any) => {
  if (typeof item?.brand === "object") return item?.brand?.name || "Premium";
  return item?.brandName || item?.brand || "Premium";
};

const formatVariantName = (v: any) => {
  if (!v) return "Loại";
  const capCandidates = [ v.capacityName, v.capacityValue, v.volume, v.capacity?.value, v.capacity?.name, typeof v.capacity === "string" || typeof v.capacity === "number" ? v.capacity : null ];
  let cap = capCandidates.find((c) => c != null && c !== "");
  let capString = "";
  if (cap != null) {
    const text = String(cap).trim();
    const numeric = parseFloat(text.toLowerCase().replace("ml", ""));
    if (!Number.isNaN(numeric) && numeric > 0) capString = `${numeric}ml`;
    else capString = text.toLowerCase().includes("ml") ? text : `${text}ml`;
  }
  const bottleCandidates = [ v.bottleTypeName, v.bottleType?.name, typeof v.bottleType === "string" ? v.bottleType : null ];
  let bottleString = bottleCandidates.find((b) => b != null && b !== "");
  if (capString && bottleString) return `${capString} - ${bottleString}`;
  if (capString) return capString;
  if (bottleString) return bottleString;
  return "Loại " + (v.productVariantId || v.variantId || v.id || "");
};

const getSortedVariants = (p: any) => {
  if (!p || !p.variants || !Array.isArray(p.variants)) return [];
  return [...p.variants].sort((a: any, b: any) => {
    const getCap = (v: any) => {
      let cap = v.capacityName || v.capacityValue || v.volume || v.capacity?.value || v.capacity?.name || v.capacity;
      const text = String(cap || "").toLowerCase().replace(/\s+/g, "");
      const numeric = parseFloat(text.replace("ml", ""));
      return Number.isNaN(numeric) ? 0 : numeric;
    };
    return getCap(a) - getCap(b);
  });
};

const MAX_COMPARE = 3;
const compareList = ref<any[]>([]);
const showCompareModal = ref(false);
const showPickerModal = ref(false);
const pickerLoading = ref(false);
const pickerSearchKeyword = ref("");
const allProductsStore = ref<any[]>([]);
const compareVariantIds = ref<Record<number, number>>({});

const isInCompare = (item: any) => { const id = item.id || item.productId; return compareList.value.some((p: any) => (p.id || p.productId) === id); };

const toggleCompare = (item: any) => {
  const id = item.id || item.productId;
  if (isInCompare(item)) { compareList.value = compareList.value.filter((p: any) => (p.id || p.productId) !== id); delete compareVariantIds.value[id]; } 
  else {
    if (compareList.value.length >= MAX_COMPARE) { showToast("warning", "Giới hạn", `Chỉ được so sánh tối đa ${MAX_COMPARE} sản phẩm!`); return; }
    compareList.value.push(item);
  }
};
const removeFromCompare = (item: any) => { const id = item.id || item.productId; compareList.value = compareList.value.filter((p: any) => (p.id || p.productId) !== id); delete compareVariantIds.value[id]; };

const openPickerModal = async () => {
  showPickerModal.value = true;
  if (allProductsStore.value.length === 0) {
    if (props.productList && props.productList.length > 0) {
      allProductsStore.value = [...props.productList];
    }
    pickerLoading.value = true;
    try {
      const res = await api.get("/v1/products", { params: { size: 100, page: 0 } });
      let list = [];
      if (res.data?.data?.content) list = res.data.data.content;
      else if (Array.isArray(res.data?.data)) list = res.data.data;
      else if (res.data?.content) list = res.data.content;
      else if (Array.isArray(res.data)) list = res.data;
      if (list.length > 0) allProductsStore.value = list;
    } catch (e) { 
      console.error("Lỗi lấy danh sách sản phẩm:", e); 
    } finally { 
      pickerLoading.value = false; 
    }
  }
};

const filteredPickerProducts = computed(() => {
  if (!pickerSearchKeyword.value.trim()) return allProductsStore.value;
  const kw = pickerSearchKeyword.value.toLowerCase();
  return allProductsStore.value.filter((item: any) => {
    const name = String(item.name || "").toLowerCase();
    const brandObj = item.brand;
    const brandName = typeof brandObj === "object" ? brandObj?.name : brandObj;
    const brandStr = String(brandName || item.brandName || "").toLowerCase();
    return name.includes(kw) || brandStr.includes(kw);
  });
});

const toggleItemInPicker = (item: any) => {
  if (isInCompare(item)) removeFromCompare(item);
  else toggleCompare(item);
};

const goToDetailFromCompare = (p: any) => {
  showCompareModal.value = false;
  emit("open-detail", p);
};

watch(showCompareModal, (val) => {
  if (val) {
    compareList.value.forEach(p => { 
      const id = p.id || p.productId; 
      if (!compareVariantIds.value[id]) {
        const primaryV = getPrimaryVariantObject(p);
        compareVariantIds.value[id] = Number(primaryV?.productVariantId ?? primaryV?.variantId ?? primaryV?.id ?? 0);
      }
    });
  }
});

const getSafeNumber = (val: any) => {
  if (val === null || val === undefined || val === '') return 0;
  if (typeof val === 'number') return isNaN(val) ? 0 : val;
  if (typeof val === 'string') {
    const cleanStr = val.replace(/[^\d]/g, '');
    return parseInt(cleanStr, 10) || 0;
  }
  return 0;
};

const getPrimaryVariantObject = (item: any) => {
  if (!item) return null;
  if (Array.isArray(item?.variants) && item.variants.length > 0) {
    return item.variants.find((variant: any) => {
        const stock = getSafeNumber(variant?.stockQuantity ?? variant?.stock ?? variant?.availableQuantity ?? variant?.quantity);
        const price = getSafeNumber(variant?.salePrice ?? variant?.promotionPrice ?? variant?.price ?? variant?.Price);
        const status = Number(variant?.status ?? 1);
        return status === 1 && stock > 0 && price > 0 && !isExpiredDate(variant?.expirationDate);
      }) || item.variants[0];
  }
  return item; 
};

const getCompareVariant = (p: any) => {
  const pId = p.id || p.productId;
  const vId = compareVariantIds.value[pId];
  if (vId && p.variants && Array.isArray(p.variants)) {
    const found = p.variants.find((v: any) => (v.productVariantId || v.variantId || v.id) === vId);
    if (found) return found;
  }
  return getPrimaryVariantObject(p);
};

const getCompareBottleType = (p: any) => {
  const v = getCompareVariant(p);
  if (v) {
    const bottleCandidates = [ v.bottleTypeName, v.bottleType?.name, typeof v.bottleType === "string" ? v.bottleType : null ];
    let bottleString = bottleCandidates.find((b) => b != null && b !== "");
    return bottleString || "Đang cập nhật";
  }
  return "Đang cập nhật";
};

const getComparePrice = (p: any) => {
  const v = getCompareVariant(p);
  let vSale = getSafeNumber(v?.salePrice ?? v?.promotionPrice ?? v?.flashSalePrice ?? v?.currentPrice ?? v?.price ?? v?.Price);
  if (vSale > 0) return vSale;
  return getSafeNumber(p?.salePrice ?? p?.promotionPrice ?? p?.flashSalePrice ?? p?.currentPrice ?? p?.price ?? p?.Price ?? p?.minPrice ?? p?.maxPrice ?? p?.basePrice);
};

const getCompareOriginalPrice = (p: any) => {
  const v = getCompareVariant(p);
  const sale = getComparePrice(p);
  let vOrig = getSafeNumber(v?.originalPrice ?? v?.oldPrice ?? v?.listPrice ?? v?.price ?? v?.Price);
  if (vOrig > sale) return vOrig;
  let pOrig = getSafeNumber(p?.originalPrice ?? p?.oldPrice ?? p?.listPrice ?? p?.price ?? p?.Price ?? p?.maxPrice ?? p?.minPrice);
  return pOrig > sale ? pOrig : sale;
};

const getCompareStock = (p: any) => getSafeNumber(getCompareVariant(p)?.stockQuantity ?? getCompareVariant(p)?.stock ?? getCompareVariant(p)?.availableQuantity ?? getCompareVariant(p)?.quantity);

const getCompareDiscount = (p: any) => {
  const sale = getComparePrice(p); const orig = getCompareOriginalPrice(p);
  if (orig > sale && sale > 0) return Math.round(((orig - sale) / orig) * 100); return 0;
};
const isCompareBuyDisabled = (p: any) => getCompareStock(p) <= 0 || getComparePrice(p) <= 0;
const buyFromCompare = (p: any) => {
  const pId = p.id || p.productId; const vId = compareVariantIds.value[pId];
  openVariantModal('BUY', p, vId); showCompareModal.value = false;
};

const getGenderText = (item: any) => {
  const g = item?.gender;
  if (g === 1 || String(g) === '1' || String(g).toLowerCase() === 'nam') return "Nam";
  if (g === 2 || String(g) === '2' || String(g).toLowerCase() === 'nữ') return "Nữ";
  if (g === 0 || String(g) === '0' || String(g).toLowerCase() === 'unisex') return "Unisex";
  return typeof g === 'object' ? (g?.name || "Đang cập nhật") : (g || "Đang cập nhật");
};
const getAttributeText = (item: any, field: string) => {
  const obj = item[field]; const nameField = item[`${field}Name`];
  if (typeof obj === 'object' && obj !== null) return obj.name || obj.value || "Đang cập nhật";
  return nameField || obj || "Đang cập nhật";
};
const getFragranceFamily = (item: any) => {
  if (Array.isArray(item?.scents) && item.scents.length > 0) return item.scents.join(", ");
  return getAttributeText(item, 'fragranceFamily');
};
const getCompareValue = (p: any, type: string) => {
  if (type === 'brand') return getBrandName(p); if (type === 'scent') return getFragranceFamily(p);
  if (type === 'concentration') return getAttributeText(p, 'concentration'); if (type === 'gender') return getGenderText(p);
  if (type === 'rating') return getRatingScore(p); if (type === 'price') return getComparePrice(p);
  if (type === 'stock') return getCompareStock(p) > 0 ? 'Còn hàng' : 'Hết hàng'; return '';
};

const DEFAULT_RATING = 5;
const syncedRatingsMap = ref<Record<number, number>>({});
const syncedReviewsMap = ref<Record<number, number>>({});

const getReviewCount = (item: any) => {
  const id = Number(item?.id || item?.productId || 0);
  if (id > 0 && syncedReviewsMap.value[id] !== undefined) return syncedReviewsMap.value[id];
  return Number(item?.reviewCount || item?.reviews || item?.totalReviews || 0);
};

const getRatingValue = (item: any) => {
  const id = Number(item?.id || item?.productId || 0);
  let raw = id > 0 && syncedRatingsMap.value[id] !== undefined ? syncedRatingsMap.value[id] : Number(item?.averageRating || item?.avgRating || item?.rating || 0);
  const reviews = getReviewCount(item);
  if (reviews > 0 || raw > 0) return Math.min(5, Math.max(0, raw));
  return DEFAULT_RATING; 
};

const getRatingScore = (item: any) => getRatingValue(item).toFixed(1);
const getStarsDisplay = (item: any) => {
  const rounded = Math.round(getRatingValue(item)); const filled = Math.max(0, Math.min(5, rounded));
  return "★".repeat(filled) + "☆".repeat(5 - filled);
};

const syncGridRatings = () => {
  if (!props.productList || !Array.isArray(props.productList)) return;
  props.productList.forEach(async (item) => {
    const id = Number(item?.id || item?.productId || 0);
    const currentCount = Number(item?.reviewCount || item?.reviews || item?.totalReviews || 0);
    if (id > 0 && currentCount === 0 && syncedReviewsMap.value[id] === undefined) {
      try {
        const res = await api.get(`/v1/products/${id}`);
        const data = res.data?.data || res.data;
        if (data) { syncedRatingsMap.value[id] = Number(data.averageRating || data.avgRating || data.rating || 0); syncedReviewsMap.value[id] = Number(data.reviewCount || data.reviews || data.totalReviews || 0); }
      } catch (e) { /* Bỏ qua */ }
    }
  });
};

const getPlaceholderImage = () => "data:image/svg+xml;utf8," + encodeURIComponent(`<svg xmlns="http://www.w3.org/2000/svg" width="300" height="300"><rect width="100%" height="100%" fill="#f3f4f6"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="#9ca3af" font-family="Arial" font-size="20">No Image</text></svg>`);
const normalizeImageUrl = (url: unknown) => {
  const rawUrl = String(url || "").trim();
  if (!rawUrl) return "";
  if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://") || rawUrl.startsWith("data:image") || rawUrl.startsWith("blob:")) return rawUrl;
  if (rawUrl.startsWith("/")) return `${BACKEND_URL}${rawUrl}`;
  return `${BACKEND_URL}/${rawUrl}`;
};
const getImageUrlFromObject = (value: any) => {
  if (!value) return "";
  if (typeof value === "string") return normalizeImageUrl(value);
  return normalizeImageUrl(value?.imageUrl ?? value?.ImageUrl ?? value?.url ?? value?.Url ?? value?.mediaUrl ?? value?.MediaUrl ?? value?.path ?? value?.Path ?? value?.fileUrl ?? value?.FileUrl ?? "");
};
const appendImage = (images: string[], value: any) => { const imageUrl = getImageUrlFromObject(value); if (imageUrl && !images.includes(imageUrl)) images.push(imageUrl); };
const appendImageList = (images: string[], value: any) => {
  if (!value) return; if (Array.isArray(value)) { value.forEach((item) => appendImage(images, item)); return; }
  appendImage(images, value);
};

const getProductImages = (item: any) => {
  const images: string[] = [];
  appendImage(images, item?.mainImage); appendImage(images, item?.mainImageUrl); appendImage(images, item?.thumbnailUrl); appendImage(images, item?.imageUrl); appendImage(images, item?.ImageUrl); appendImage(images, item?.image);
  appendImageList(images, item?.images); appendImageList(images, item?.Images); appendImageList(images, item?.galleryImages); appendImageList(images, item?.imageList); appendImageList(images, item?.ImageList); appendImageList(images, item?.productImages); appendImageList(images, item?.ProductImages); appendImageList(images, item?.productImageList); appendImageList(images, item?.ProductImageList);
  if (Array.isArray(item?.variants)) {
    item.variants.forEach((variant: any) => {
      appendImage(images, variant?.mainImage); appendImage(images, variant?.mainImageUrl); appendImage(images, variant?.thumbnailUrl); appendImage(images, variant?.imageUrl); appendImage(images, variant?.ImageUrl); appendImage(images, variant?.image);
      appendImageList(images, variant?.images); appendImageList(images, variant?.Images); appendImageList(images, variant?.productImages); appendImageList(images, variant?.ProductImages);
    });
  }
  return images;
};
const getProductImage = (item: any) => getProductImages(item)[0] || getPlaceholderImage();
const handleImageError = (event: Event) => { const target = event.target as HTMLImageElement | null; if (!target) return; target.onerror = null; target.src = getPlaceholderImage(); };

const getPrimaryVariantId = (item: any) => {
  const variant = getPrimaryVariantObject(item);
  if (!variant) return 0;
  return Number(variant?.productVariantId ?? variant?.variantId ?? variant?.id ?? variant?.Id ?? item?.productVariantId ?? item?.variantId ?? 0);
};

const getVariantStock = (item: any) => getSafeNumber(getPrimaryVariantObject(item)?.stockQuantity ?? getPrimaryVariantObject(item)?.stock ?? getPrimaryVariantObject(item)?.availableQuantity ?? getPrimaryVariantObject(item)?.quantity);
const getVariantPrice = (item: any) => {
  const variant = getPrimaryVariantObject(item);
  if (variant?.salePrice != null) return getSafeNumber(variant.salePrice);
  if (variant?.price != null || variant?.Price != null) return getSafeNumber(variant?.price ?? variant?.Price);
  return Number(item?.salePrice ?? item?.flashSalePrice ?? item?.price ?? item?.Price ?? 0);
};

const getOldPrice = (item: any) => {
  const variant = getPrimaryVariantObject(item);
  if (variant?.originalPrice != null || variant?.oldPrice != null) return Number(variant.originalPrice || variant.oldPrice);
  return Number(item?.oldPrice ?? item?.originalPrice ?? 0);
};

const getDiscountPercent = (item: any) => {
  let percent = Number(item?.discountPercent ?? item?.discount ?? 0);
  if (percent > 0) return Math.round(percent);
  const sale = getVariantPrice(item);
  const orig = getOldPrice(item);
  if (orig > sale && sale > 0) return Math.round(((orig - sale) / orig) * 100);
  return 0;
};

const isBuyNowDisabled = (item: any) => { const variantId = getPrimaryVariantId(item); const price = getVariantPrice(item); const stock = getVariantStock(item); return !variantId || price <= 0 || stock <= 0; };
const isFavorited = (item: any) => { const variantId = getPrimaryVariantId(item); if (!variantId) return false; return Boolean(favoritedMap.value[variantId]); };
const isFavoriteLoading = (item: any) => { const variantId = getPrimaryVariantId(item); if (!variantId) return false; return Boolean(favoriteLoadingMap.value[variantId]); };
const setFavoriteLoading = (variantId: number, value: boolean) => favoriteLoadingMap.value = { ...favoriteLoadingMap.value, [variantId]: value };
const setFavorited = (variantId: number, value: boolean) => favoritedMap.value = { ...favoritedMap.value, [variantId]: value };

const checkLoginBeforeAction = () => {
  const token = localStorage.getItem("token");
  const rawRole = localStorage.getItem("role") || localStorage.getItem("userRole") || "";
  const role = rawRole.replace("ROLE_", "").toUpperCase().trim();
  if (!token) {
    Swal.fire({ icon: "info", title: "Bạn chưa đăng nhập", text: "Vui lòng đăng nhập để tiếp tục trải nghiệm mua sắm tại Dominus.", showCancelButton: true, confirmButtonText: "Đăng nhập ngay", cancelButtonText: "Ở lại xem tiếp", confirmButtonColor: "#bd9a5f", cancelButtonColor: "#6b7280" })
      .then((result) => { if (result.isConfirmed) router.push({ name: "Login", query: { redirect: router.currentRoute.value.fullPath } }); });
    return false;
  }
  if (role !== "USER" && role !== "CUSTOMER") {
    Swal.fire({ icon: "error", title: "Từ chối thao tác", text: "Chức năng này chỉ dành cho tài khoản Khách hàng.", confirmButtonColor: "#bd9a5f" });
    return false;
  }
  return true;
};

const loadMyFavorites = async () => {
  if (!isCustomerLoggedIn()) { favoritedMap.value = {}; return; }
  try {
    const res = await favoriteService.getFavorites();
    const list = Array.isArray(res.data) ? res.data : [];
    const nextMap: Record<number, boolean> = {};
    list.forEach((item: any) => { const variantId = Number(item?.productVariantId || 0); if (variantId > 0) nextMap[variantId] = true; });
    favoritedMap.value = nextMap;
  } catch (error) { favoritedMap.value = {}; }
};

const toggleFavorite = async (item: any) => {
  const variantId = getPrimaryVariantId(item);
  if (!variantId || Number.isNaN(variantId)) { showToast("warning", "Không xác định", "Sản phẩm này chưa có biến thể hợp lệ để thêm yêu thích."); return; }
  if (!checkLoginBeforeAction()) return;
  try {
    setFavoriteLoading(variantId, true);
    const res = await favoriteService.toggleFavorite(variantId);
    const favorited = Boolean(res.data?.favorited);
    setFavorited(variantId, favorited);
    window.dispatchEvent(new CustomEvent("favorite-updated", { detail: { productVariantId: variantId, favorited } }));
    showToast(favorited ? "success" : "warning", favorited ? "Đã thêm yêu thích" : "Đã bỏ yêu thích", res.data?.message || "");
  } catch (error: any) { showToast("error", "Lỗi", "Không thể xử lý yêu thích"); } 
  finally { setFavoriteLoading(variantId, false); }
};

const showVariantModal = ref(false);
const modalProduct = ref<any>(null);
const isLoadingVariants = ref(false);
const selectedVariant = ref<any>(null);
const fullVariants = ref<any[]>([]);
const quantity = ref(1);
const actionLoading = ref(false);
const actionType = ref('');

const modalImage = computed(() => {
  if (selectedVariant.value) { const variantImages = getProductImages(selectedVariant.value); if (variantImages[0]) return variantImages[0]; }
  return modalProduct.value ? getProductImage(modalProduct.value) : getPlaceholderImage();
});

const maxQuantity = computed(() => selectedVariant.value ? Number(selectedVariant.value.stockQuantity || selectedVariant.value.stock || 0) : 0);

const calculatedDiscountPercent = computed(() => {
  if (selectedVariant.value) {
    const original = selectedVariant.value.originalPrice || selectedVariant.value.oldPrice || selectedVariant.value.price;
    const sale = selectedVariant.value.salePrice || selectedVariant.value.price;
    if (original && sale && original > sale) return Math.round(((original - sale) / original) * 100); return 0;
  }
  return modalProduct.value ? getDiscountPercent(modalProduct.value) : 0;
});

const validateQuantity = () => {
  let val = Number(quantity.value);
  if (Number.isNaN(val) || val < 1) quantity.value = 1;
  else if (val > 10) { quantity.value = 10; showToast("warning", "Giới hạn mua", "Bạn chỉ được mua tối đa 10 sản phẩm!"); }
  else if (val > maxQuantity.value) { quantity.value = maxQuantity.value; showToast("warning", "Hết hàng", `Chỉ còn ${maxQuantity.value} trong kho!`); }
  else quantity.value = Math.floor(val);
};

const increaseQuantity = () => {
  if (quantity.value >= 10) { showToast("warning", "Giới hạn mua", "Chỉ được mua tối đa 10 sản phẩm!"); return; }
  if (quantity.value >= maxQuantity.value) { showToast("warning", "Hết hàng", `Chỉ còn ${maxQuantity.value} trong kho!`); return; }
  quantity.value++;
};

const openVariantModal = async (type: "CART" | "BUY", item: any, preselectedVariantId?: number) => {
  if (!checkLoginBeforeAction()) return;

  actionType.value = type;
  modalProduct.value = item; selectedVariant.value = null; fullVariants.value = []; quantity.value = 1; showVariantModal.value = true; isLoadingVariants.value = true;

  try {
    const flashSalePriceMap = new Map<number, number>(); const originalPriceMap = new Map<number, number>();
    if (item?.variants && Array.isArray(item.variants)) {
      item.variants.forEach((pv: any) => {
        const vId = Number(pv.productVariantId || pv.variantId || pv.id);
        if (vId) { 
          if (pv.salePrice != null || pv.flashSalePrice != null) flashSalePriceMap.set(vId, Number(pv.salePrice || pv.flashSalePrice)); 
          if (pv.originalPrice != null || pv.oldPrice != null || pv.price != null) originalPriceMap.set(vId, Number(pv.originalPrice || pv.oldPrice || pv.price)); 
        }
      });
    }

    const res = await api.get(`/v1/products/${item.id || item.productId}`);
    const data = res.data?.data || res.data;
    let rawVariants = data?.variants || data?.productVariants || data?.productVariantList || item.variants || [item];

    const processedVariants = rawVariants.map((v: any) => {
      const vId = Number(v.productVariantId || v.variantId || v.id);
      let cap = v.capacityName || v.capacityValue || v.volume || v.capacity;
      if (typeof cap === "object") cap = cap?.value ?? cap?.name;
      let numericCap = parseFloat(String(cap).replace("ml", "")) || 0;
      let displayCap = numericCap > 0 ? `${numericCap}ml` : String(cap || "");
      const bottle = v.bottleTypeName || v.bottleType;
      const bottleName = typeof bottle === "object" ? bottle?.name : bottle;
      if (displayCap && bottleName) displayCap = `${displayCap} - ${bottleName}`;
      else if (bottleName) displayCap = bottleName;
      else if (!displayCap) displayCap = "Loại " + (vId || "");
      const mappedSalePrice = flashSalePriceMap.get(vId) ?? v.salePrice ?? v.flashSalePrice ?? v.promotionPrice ?? v.price;
      const mappedOriginalPrice = originalPriceMap.get(vId) ?? v.originalPrice ?? v.oldPrice ?? v.price;

      return { ...v, productVariantId: vId, id: vId, originalPrice: mappedOriginalPrice, salePrice: mappedSalePrice, displayCapacity: displayCap, numericCapacity: numericCap, manufacturingDate: v.manufacturingDate || v.mfgDate || item.manufacturingDate, expirationDate: v.expirationDate || v.expDate || item.expirationDate };
    }).filter((v: any) => !isExpiredDate(v.expirationDate));

    processedVariants.sort((a: any, b: any) => a.numericCapacity - b.numericCapacity); fullVariants.value = processedVariants;

    if (fullVariants.value.length > 0) {
      const targetId = preselectedVariantId || getPrimaryVariantId(item);
      selectedVariant.value = fullVariants.value.find((v: any) => v.productVariantId === targetId) || fullVariants.value[0];
    }
  } catch (error) {
    fullVariants.value = (item.variants || [item]).map((v: any) => ({ ...v, displayCapacity: "Phân loại", numericCapacity: 0 }));
    selectedVariant.value = fullVariants.value[0];
  } finally { isLoadingVariants.value = false; }
};

const confirmAction = async (type: 'CART' | 'BUY') => {
  if (!selectedVariant.value || !modalProduct.value) return;
  const variantId = Number(selectedVariant.value.productVariantId || selectedVariant.value.variantId || selectedVariant.value.id || modalProduct.value.id);

  try {
    actionType.value = type; actionLoading.value = true;
    await api.post("/v1/customer/cart/add", { productVariantId: variantId, quantity: quantity.value });
    window.dispatchEvent(new Event("cart-updated")); showVariantModal.value = false;
    if (type === 'BUY') router.push({ name: "Checkout" });
    else showToast('success', 'Thành công', 'Đã thêm sản phẩm vào giỏ hàng.');
  } catch (error: any) { showToast('error', 'Lỗi', error?.response?.data?.message || 'Không thể thực hiện.'); } 
  finally { actionLoading.value = false; actionType.value = ''; }
};

const handleFavoriteUpdated = (event: Event) => {
  const customEvent = event as CustomEvent<{ productVariantId?: number; favorited?: boolean; }>;
  const variantId = Number(customEvent.detail?.productVariantId || 0);
  if (!variantId) return; setFavorited(variantId, Boolean(customEvent.detail?.favorited));
};

const formatCurrency = (value: number) => {
  if (value == null || Number.isNaN(Number(value)) || Number(value) <= 0) return "Liên hệ";
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(Number(value));
};
const formatPrice = (item: any) => formatCurrency(getVariantPrice(item));

onMounted(() => { 
  window.addEventListener("favorite-updated", handleFavoriteUpdated); 
  loadMyFavorites(); 
  syncGridRatings(); 
});
onBeforeUnmount(() => { 
  window.removeEventListener("favorite-updated", handleFavoriteUpdated); 
});
watch(() => props.productList, () => { loadMyFavorites(); syncGridRatings(); }, { deep: true });
</script>

<style scoped>
.product-grid-wrapper { width: 100%; }
.empty-product { padding: 60px 20px; text-align: center; color: #718096; background: #f9fafb; border-radius: 14px; font-weight: 500;}
.product-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 28px; }
.product-card.luxury-card { background: #fff; border-radius: 16px; overflow: hidden; cursor: pointer; transition: all 0.3s ease; display: flex; flex-direction: column; position: relative; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03); border: 1px solid #eaeaea; }
.product-card.luxury-card:hover { box-shadow: 0 15px 35px rgba(0, 0, 0, 0.08); transform: translateY(-4px); }

.card-img-wrapper { position: relative; background: #f8f9fa; border-top-left-radius: 16px; border-top-right-radius: 16px; overflow: hidden; aspect-ratio: 1 / 1; width: 100%; display: flex; align-items: center; justify-content: center; }
.card-img-wrapper img { width: 100%; height: 100%; object-fit: cover; transition: 0.4s ease; }
.product-card.luxury-card:hover .card-img-wrapper img { transform: scale(1.06); }

.sale-badge { position: absolute; top: 15px; left: 15px; background: #e53e3e; color: white; font-size: 11px; font-weight: bold; padding: 4px 8px; border-radius: 4px; z-index: 2; }
.expiry-warning-badge { position: absolute; top: 15px; left: 15px; background: #d97706; color: white; font-size: 11px; font-weight: 800; padding: 4px 8px; border-radius: 999px; z-index: 2; box-shadow: 0 4px 10px rgba(217, 119, 6, 0.3); display: inline-flex; align-items: center; }
.sale-badge ~ .expiry-warning-badge { top: 48px; }

.btn-heart-small { position: absolute; top: 15px; right: 15px; background: white; border: 1px solid #eaeaea; width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #a0aec0; transition: 0.2s; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05); z-index: 2; }
.btn-heart-small:hover:not(:disabled) { color: #e53e3e; border-color: #e53e3e; }
.btn-heart-small.active { color: #e53e3e; border-color: #e53e3e; background: #fff5f5; }
.btn-heart-small:disabled { opacity: 0.7; cursor: not-allowed; }
.btn-heart-small svg { width: 18px; height: 18px; }

.btn-compare-small { position: absolute; top: 58px; right: 15px; background: white; border: 1px solid #eaeaea; width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #a0aec0; transition: 0.2s; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05); z-index: 2; }
.btn-compare-small:hover { color: #bd9a5f; border-color: #bd9a5f; }
.btn-compare-small.active { background: #fdfaf6; color: #bd9a5f; border-color: #bd9a5f; }
.btn-compare-small svg { width: 17px; height: 17px; }

.card-info { padding: 18px; display: flex; flex-direction: column; flex: 1; }
.card-brand { font-size: 12px; font-weight: 700; color: #bd9a5f; text-transform: uppercase; margin-bottom: 6px; }
.card-name { font-size: 17px; color: #0a142f; font-weight: 700; min-height: 44px; line-height: 1.3; margin: 0 0 10px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-rating { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.stars { color: #bd9a5f; font-size: 12px; }
.score { color: #718096; font-size: 13px; }
.card-price-box { display: flex; align-items: baseline; gap: 10px; margin-bottom: 16px; }
.card-price { color: #0a142f; font-weight: 800; font-size: 17px; }
.card-old-price { color: #a0aec0; font-size: 13px; text-decoration: line-through; }
.card-actions { margin-top: auto; display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.btn-buy-now-small, .btn-view-detail { border-radius: 8px; padding: 9px 10px; font-size: 13px; font-weight: 700; transition: 0.2s; }
.btn-buy-now-small { border: none; background: #bd9a5f; color: #ffffff; }
.btn-buy-now-small:hover:not(:disabled) { background: #a3824d; }
.btn-buy-now-small:disabled { opacity: 0.55; cursor: not-allowed; }
.btn-view-detail { border: 1px solid #0a142f; background: #ffffff; color: #0a142f; }
.btn-view-detail:hover { background: #0a142f; color: #ffffff; }
.card-stock-warning { margin-top: 10px; color: #dc2626; font-size: 13px; font-weight: 700; }
@media (max-width: 1199px) { .product-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 575px) { .product-grid { grid-template-columns: 1fr; } }

.compare-bar { position: fixed; bottom: 0; left: 0; width: 100%; background: #ffffff; box-shadow: 0 -4px 20px rgba(0,0,0,0.1); transform: translateY(100%); transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1); z-index: 9998; padding: 15px 0; border-top: 2px solid #bd9a5f; }
.compare-bar.show { transform: translateY(0); }
.cb-container { max-width: 1200px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center; padding: 0 20px; }
.cb-left { display: flex; align-items: center; gap: 24px; flex-wrap: wrap; }
.cb-title { font-weight: 800; font-size: 15px; color: #0a142f; text-transform: uppercase; letter-spacing: 0.5px; }
.cb-slots { display: flex; gap: 15px; }
.cb-slot { width: 230px; height: 60px; border: 1px dashed #cbd5e0; border-radius: 8px; display: flex; align-items: center; padding: 5px; gap: 10px; background: #f8fafc; position: relative; }
.cb-slot.filled { border-style: solid; border-color: #eaeaea; background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.cb-slot img { width: 48px; height: 48px; object-fit: cover; border-radius: 6px; }
.cb-slot-info { flex: 1; overflow: hidden; }
.cb-slot-info p { margin: 0; font-size: 13px; font-weight: 700; color: #0a142f; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.cb-slot-info span { font-size: 12px; color: #bd9a5f; font-weight: 600; }
.btn-remove-cb { position: absolute; top: -8px; right: -8px; background: #e53e3e; color: white; border: none; border-radius: 50%; width: 22px; height: 22px; font-size: 12px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: 0.2s; box-shadow: 0 2px 5px rgba(229,62,62,0.3); }
.btn-remove-cb:hover { transform: scale(1.1); }
.cb-slot.empty-slot { justify-content: center; color: #a0aec0; font-size: 13px; cursor: pointer; transition: 0.2s; }
.cb-slot.empty-slot:hover { color: #bd9a5f; border-color: #bd9a5f; background: #fdfaf6; }
.cb-right { display: flex; gap: 15px; align-items: center; }
.cb-btn-clear { background: transparent; border: none; color: #718096; font-weight: 600; cursor: pointer; font-size: 14px; transition: 0.2s; }
.cb-btn-clear:hover { color: #e53e3e; text-decoration: underline; }
.cb-btn-compare { background: #0a142f; color: white; border: none; padding: 12px 28px; border-radius: 8px; font-weight: 700; text-transform: uppercase; cursor: pointer; transition: 0.2s; letter-spacing: 0.5px; }
.cb-btn-compare:disabled { opacity: 0.5; cursor: not-allowed; }
.cb-btn-compare:hover:not(:disabled) { background: #bd9a5f; box-shadow: 0 4px 12px rgba(189,154,95,0.3); }

.compare-modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); z-index: 999999; display: flex; align-items: center; justify-content: center; backdrop-filter: blur(6px); }
.compare-modal-box { background: white; width: 95%; max-width: 1100px; max-height: 90vh; border-radius: 16px; display: flex; flex-direction: column; overflow: hidden; animation: modalFadeIn 0.3s ease; box-shadow: 0 20px 60px rgba(0,0,0,0.2); }
.cm-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid #eaeaea; background: #fdfaf6; }
.cm-header h3 { margin: 0; font-family: "Playfair Display", serif; font-size: 22px; font-weight: 800; color: #0a142f; }
.cm-close { background: none; border: none; font-size: 24px; cursor: pointer; color: #a0aec0; transition: 0.2s; padding: 0; line-height: 1; }
.cm-close:hover { color: #e53e3e; transform: rotate(90deg); }
.cm-body { padding: 0; overflow-y: auto; }
.table-compare { width: 100%; border-collapse: collapse; text-align: left; }
.sticky-header th, .sticky-header td { position: sticky; top: 0; background: white; z-index: 10; border-bottom: 2px solid #eaeaea; padding-top: 25px; padding-bottom: 20px; }
.table-compare th, .table-compare td { padding: 18px 24px; border-bottom: 1px solid #eaeaea; vertical-align: middle; }

.spec-label-col { width: 18%; background: #f8fafc; font-weight: 700; color: #4a5568; font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px; border-right: 1px solid #eaeaea; }
.spec-value-col { width: 27.33%; font-size: 15px; color: #0a142f; text-align: center; position: relative; }
.spec-value-col:not(:last-child) { border-right: 1px solid #eaeaea; }

.cm-img-wrapper { text-align: center; margin-bottom: 15px; cursor: pointer; display: flex; justify-content: center; }
.cm-img-wrapper img { width: 140px; height: 140px; object-fit: cover; border-radius: 12px; border: 1px solid #eaeaea; box-shadow: 0 4px 15px rgba(0,0,0,0.04); transition: transform 0.2s; }
.cm-img-wrapper:hover img { transform: scale(1.05); }

.cm-name { font-family: "Playfair Display", serif; font-size: 18px; font-weight: 800; color: #0a142f; margin: 0 0 5px; text-align: center; line-height: 1.3; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; cursor: pointer; transition: color 0.2s; }
.cm-name:hover { color: #bd9a5f; }

.btn-remove-from-table { position: absolute; top: 10px; right: 10px; background: #e53e3e; color: white; border: none; width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; cursor: pointer; opacity: 0; transition: 0.2s; z-index: 15; }
.spec-value-col:hover .btn-remove-from-table { opacity: 1; }

.cm-header-price { text-align: center; font-size: 16px; font-weight: 800; color: #e53e3e; }
.group-header { background: #f8fafc; font-weight: 800; color: #0a142f; font-size: 16px; padding: 12px 24px; border-top: 2px solid #eaeaea; border-bottom: 1px solid #eaeaea; }

.empty-product-slot { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; min-height: 180px; border: 2px dashed #cbd5e0; border-radius: 12px; color: #a0aec0; cursor: pointer; transition: 0.2s; background: #f8fafc; }
.empty-product-slot i { font-size: 32px; margin-bottom: 10px; }
.empty-product-slot span { font-weight: 600; font-size: 14px; }
.empty-product-slot:hover { border-color: #bd9a5f; color: #bd9a5f; background: #fffcf7; }

.compare-select { width: 80%; padding: 8px 10px; border: 1px solid #cbd5e0; border-radius: 6px; font-size: 13px; font-weight: 600; color: #0a142f; outline: none; cursor: pointer; margin: 0 auto; display: block; }
.compare-select:focus { border-color: #bd9a5f; }

.cm-price-val { font-size: 16px; font-weight: 800; color: #e53e3e; display: flex; align-items: center; justify-content: flex-start; }
.cm-rating { color: #bd9a5f; font-weight: 700; font-size: 15px; text-align: left; }
.cm-rating .d-flex { justify-content: flex-start !important; }
.cm-btn-buy { width: 80%; margin: 0 auto; display: flex; justify-content: center; align-items: center; padding: 12px; background: #0a142f; color: white; border: none; border-radius: 8px; font-weight: 700; cursor: pointer; transition: 0.2s; text-transform: uppercase; letter-spacing: 0.5px; font-size: 13px; }
.cm-btn-buy:hover:not(:disabled) { background: #bd9a5f; box-shadow: 0 4px 15px rgba(189,154,95,0.3); }
.cm-btn-buy:disabled { opacity: 0.6; cursor: not-allowed; background: #718096; }

.product-picker-box { background: white; width: 95%; max-width: 700px; max-height: 80vh; border-radius: 16px; display: flex; flex-direction: column; overflow: hidden; animation: modalFadeIn 0.3s ease; }
.picker-search-bar { padding: 15px 24px; border-bottom: 1px solid #eaeaea; background: #f8fafc; }
.picker-input { width: 100%; padding: 12px 15px; border: 1px solid #cbd5e0; border-radius: 8px; font-size: 14px; outline: none; transition: 0.2s; }
.picker-input:focus { border-color: #bd9a5f; box-shadow: 0 0 0 3px rgba(189,154,95,0.1); }
.picker-body { flex: 1; overflow-y: auto; padding: 20px 24px; }
.picker-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 15px; }

.picker-item { display: flex; flex-direction: column; align-items: center; text-align: center; padding: 15px; border: 1px solid #eaeaea; border-radius: 12px; cursor: pointer; transition: 0.2s; position: relative; justify-content: space-between; }
.picker-item:hover { border-color: #bd9a5f; transform: translateY(-3px); box-shadow: 0 6px 15px rgba(0,0,0,0.05); }
.picker-item.selected { border-color: #bd9a5f; background: #fdfaf6; }
.picker-item img { width: 80px; height: 80px; object-fit: cover; border-radius: 8px; margin-bottom: 12px; flex-shrink: 0; }
.picker-info { display: flex; flex-direction: column; align-items: center; width: 100%; flex: 1; }
.picker-info .brand { font-size: 11px; color: #bd9a5f; font-weight: 700; text-transform: uppercase; margin-bottom: 4px; }
.picker-info .name { font-size: 13px; font-weight: 700; color: #0a142f; margin: 0 0 8px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; line-height: 1.3; }
.picker-price { font-size: 15px; font-weight: 800; color: #e53e3e; margin-top: auto; }
.picker-check { position: absolute; top: 10px; right: 10px; font-size: 18px; }
.picker-footer { padding: 15px 24px; border-top: 1px solid #eaeaea; display: flex; justify-content: flex-end; background: #f8fafc; }

.custom-modal-overlay { backdrop-filter: blur(5px); position: fixed; inset: 0; background: rgba(0, 0, 0, 0.6); z-index: 999999; display: flex; align-items: center; justify-content: center; }
.variant-modal-box { background: #ffffff; width: 100%; max-width: 440px; border-radius: 20px; padding: 28px; box-shadow: 0 24px 54px rgba(6, 19, 43, 0.25); animation: modalFadeIn 0.3s ease-out forwards; }
@keyframes modalFadeIn { from { opacity: 0; transform: translateY(20px) scale(0.98); } to { opacity: 1; transform: translateY(0) scale(1); } }
.vm-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; border-bottom: 1px solid rgba(189, 154, 95, 0.15); padding-bottom: 16px; }
.vm-header h5 { margin: 0; font-family: "Playfair Display", serif; font-weight: 800; color: #06132b; font-size: 20px; letter-spacing: -0.5px; }
.vm-close { background: transparent; border: none; padding: 4px; color: #a0aec0; cursor: pointer; transition: 0.2s; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.vm-close:hover { color: #e53e3e; background: #fff5f5; transform: rotate(90deg); }
.vm-product-info { display: flex; gap: 18px; margin-bottom: 28px; align-items: center; }
.vm-img-box { width: 82px; height: 82px; border-radius: 14px; background: #f8fafc; display: flex; align-items: center; justify-content: center; overflow: hidden; border: 1px solid rgba(6, 19, 43, 0.08); flex-shrink: 0; padding: 6px; }
.vm-img-box img { width: 100%; height: 100%; object-fit: contain; }
.vm-details h6 { margin: 0 0 6px 0; font-weight: 800; font-size: 16px; color: #06132b; font-family: "Playfair Display", serif; line-height: 1.3; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.vm-price { margin: 0; font-weight: 800; color: #b78d52; font-size: 18px; }
.vm-label { font-weight: 700; font-size: 13px; color: #4a5568; margin-bottom: 12px; text-transform: uppercase; letter-spacing: 0.5px; }
.vm-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin-bottom: 28px; }
.vm-variant-btn { background: #ffffff; border: 1px solid #cbd5e0; border-radius: 12px; padding: 12px 6px; text-align: center; cursor: pointer; transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; }
.vm-variant-btn:hover:not(.disabled) { border-color: #b78d52; background: #fffcf7; transform: translateY(-2px); }
.vm-variant-btn.selected { border-color: #b78d52; background: #fffcf7; border-width: 2px; box-shadow: 0 6px 14px rgba(183, 141, 82, 0.15); padding: 11px 5px; }
.vm-v-name { font-size: 14px; font-weight: 800; color: #06132b; }
.vm-variant-btn.disabled { opacity: 0.45; cursor: not-allowed; background: #f1f5f9; border-color: #e2e8f0; }
.vm-variant-btn.disabled .vm-v-name { text-decoration: line-through; color: #a0aec0; }
.variant-divider { border: 0; border-top: 1px dashed #cbd5e0; margin: 20px 0 16px 0; }
.quantity-section { display: flex; align-items: center; gap: 24px; margin-bottom: 24px; }
.quantity-control { display: flex; align-items: center; gap: 12px; }
.qty-wrapper { display: inline-flex; border: 1px solid #cbd5e0; border-radius: 6px; overflow: hidden; }
.qty-wrapper button { width: 32px; height: 32px; background: #ffffff; border: none; cursor: pointer; color: #06132b; font-size: 18px; display: flex; align-items: center; justify-content: center; transition: 0.2s; }
.qty-wrapper button:hover:not(:disabled) { background: #f1f5f9; }
.qty-wrapper button:disabled { color: #cbd5e0; cursor: not-allowed; }
.qty-wrapper input { width: 44px; text-align: center; border: none; font-size: 15px; font-weight: 700; outline: none; border-left: 1px solid #cbd5e0; border-right: 1px solid #cbd5e0; color: #06132b; }
.stock-info { font-size: 13px; color: #718096; }
.vm-actions { display: flex; gap: 10px; margin-top: 15px; }
.vm-btn-cart, .vm-btn-buy { border-radius: 12px; padding: 14px; font-size: 13px; font-weight: 800; letter-spacing: 0.5px; transition: all 0.25s ease; text-transform: uppercase; display: flex; justify-content: center; align-items: center; border: none; flex-grow: 1; }
.vm-btn-cart { background: #0a142f; color: #ffffff; }
.vm-btn-cart:hover:not(:disabled) { background: #13275a; transform: translateY(-2px); box-shadow: 0 6px 14px rgba(10, 20, 47, 0.2); }
.vm-btn-buy { background: #b78d52; color: #ffffff; }
.vm-btn-buy:hover:not(:disabled) { background: #9b7541; transform: translateY(-2px); box-shadow: 0 6px 14px rgba(183, 141, 82, 0.25); }
.vm-btn-cart:disabled, .vm-btn-buy:disabled { opacity: 0.6; cursor: not-allowed; transform: none; box-shadow: none; }
.qty-wrapper input[type="number"]::-webkit-inner-spin-button, .qty-wrapper input[type="number"]::-webkit-outer-spin-button { -webkit-appearance: none; margin: 0; }
.qty-wrapper input[type="number"] { appearance: textfield; -moz-appearance: textfield; }
.flash-sale-badge { background: #b31320; color: #ffffff; border-radius: 999px; padding: 3px 10px; font-size: 12px; font-weight: 800; margin-bottom: 2px; }
.date-info-box { background: #fffcf7; border-left: 3px solid #d97706; padding: 8px 12px; border-radius: 4px; margin-top: 8px; }
</style>