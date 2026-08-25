<template>

  <div class="cart-left">

    <div class="card-header">

      <div class="header-icon">

        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">

          <path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z" />

          <line x1="3" y1="6" x2="21" y2="6" />

          <path d="M16 10a4 4 0 01-8 0" />

        </svg>

      </div>

      <h2>Giỏ hàng của bạn</h2>

    </div>

    <div v-if="isLoading" class="loading">Đang tải dữ liệu...</div>

    <div v-else-if="cartItems.length === 0" class="empty">

      Giỏ hàng trống. <router-link to="/">Tiếp tục mua sắm ➔</router-link>

    </div>

    <div v-else class="item-list">

      <div v-for="item in cartItems" :key="item.cartItemId"
        :class="['cart-item', { 'item-unavailable': !isItemAvailable(item) }]">

        <img :src="getItemImage(item)" class="item-img" :alt="item.productName || 'Sản phẩm'"
          @error="handleImageError" />

        <div class="item-info">

          <div class="name-row">

            <h4 class="item-name">{{ item.productName || "Sản phẩm" }}</h4>

          </div>

          <div class="variant-grid">

            <template v-if="getVariantsList(item).length > 0">

              <select class="variant-select" :value="getVariantId(item)" @change="

                (e) =>

                  $emit(

                    'update-variant',

                    item,

                    Number((e.target as HTMLSelectElement).value),

                  )

              " :disabled="isUpdating">

                <option v-for="v in getVariantsList(item)" :key="v.id || v.productVariantId || v.variantId"
                  :value="v.id || v.productVariantId || v.variantId" :disabled="!isVariantSelectable(v)">

                  {{ formatVariantLabel(v) }}

                </option>

              </select>

            </template>

            <template v-else>

              <p class="item-variant">

                Dung tích: <strong>{{ item.capacity || "-" }}</strong>

              </p>

              <p class="item-variant">

                Loại chai: <strong>{{ item.bottleType || "-" }}</strong>

              </p>

            </template>

          </div>

          <!-- ĐÃ GỠ BỎ DATE-GRID HIỂN THỊ NSX/HSD THEO YÊU CẦU -->

          <p class="stock-line mt-2">

            Tồn kho: <strong>{{ getSellableQuantity(item) }}</strong>

          </p>

          <div class="status-row">

            <span :class="[

              'status-badge',

              isItemAvailable(item) ? 'status-ok' : 'status-error',

            ]">

              {{ isItemAvailable(item) ? "Có thể mua" : "Không khả dụng" }}

            </span>

          </div>

          <p v-if="!isItemAvailable(item)" class="unavailable-text">

            {{ getUnavailableReason(item) }}

          </p>

          <div class="qty-wrapper">

            <button type="button" @click="changeQuantity(item, -1)"
              :disabled="Number(item.quantity || 0) <= 1 || isUpdating">

              −

            </button>

            <input type="number" :value="Number(item.quantity || 0)" @input="handleManualQuantity(item, $event)"
              @blur="handleBlurQuantity(item, $event)" :disabled="isUpdating" />

            <button type="button" @click="changeQuantity(item, 1)" :disabled="!canIncreaseQuantity(item) || isUpdating">

              +

            </button>

          </div>

        </div>

        <div class="item-action">

          <template v-if="item.hasPromotion">

            <span class="unit-price text-muted" style="text-decoration: line-through">

              Giá gốc:

              {{ formatCurrency(item.originalPrice || item.price) }}

            </span>

            <div class="promotion-price-box">

              <span class="price">

                {{ formatCurrency(getLineTotal(item)) }}

              </span>

              <span class="discount-badge-new" v-if="(item.discountPercent || 0) > 0">

                -{{ formatDiscount(item.discountPercent || 0) }}%

              </span>

            </div>

          </template>

          <template v-else>

            <span class="unit-price">

              Đơn giá: {{ formatCurrency(item.price) }}

            </span>

            <span class="price">{{ formatCurrency(getLineTotal(item)) }}</span>

          </template>

          <button class="btn-delete" type="button" @click="$emit('remove-item', item.cartItemId)" title="Xóa sản phẩm"
            :disabled="isUpdating">

            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">

              <polyline points="3 6 5 6 21 6" />

              <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" />

              <line x1="10" y1="11" x2="10" y2="17" />

              <line x1="14" y1="11" x2="14" y2="17" />

            </svg>

          </button>

        </div>

      </div>

    </div>

  </div>

</template>

<script setup lang="ts">

import { ref } from "vue";

import Swal from "sweetalert2";

interface CartItem {

  cartItemId: number;

  productVariantId?: number;

  variantId?: number;

  id?: number;

  sku?: string | null;

  productName?: string | null;

  capacity?: string | null;

  bottleType?: string | null;

  quantity?: number | null;

  price?: number | null;

  originalPrice?: number | null;

  salePrice?: number | null;

  discountPercent?: number | null;

  hasPromotion?: boolean | null;

  promotionId?: number | null;

  promotionName?: string | null;

  promotionEndDate?: string | null;

  /** LEGACY compatibility từ BE. */
  stockQuantity?: number | null;

  /** Tồn có thể bán thật từ InventoryLot. */
  sellableQuantity?: number | null;

  note?: string | null;

  image?: string | null;

  imageUrl?: string | null;

  thumbnailUrl?: string | null;

  mainImage?: string | null;

  productImage?: string | null;

  variantImage?: string | null;

  images?: any[] | null;

  product?: any;

  variant?: any;

  productVariant?: any;

  manufacturingDate?: string | null;

  expirationDate?: string | null;

  variantStatus?: number | null;

  expired?: boolean | null;

  available?: boolean | null;

  sellable?: boolean | null;

  unavailableReason?: string | null;
  isDeleted?: boolean | null;

}

const props = defineProps<{

  cartItems: CartItem[];

  isLoading: boolean;

  isUpdating?: boolean;

}>();

const emit = defineEmits<{

  (e: "update-qty", item: CartItem, quantity: number): void;

  (e: "remove-item", cartItemId: number): void;

  (e: "update-variant", item: CartItem, newVariantId: number): void;

}>();

let updateTimeout: ReturnType<typeof setTimeout> | null = null;

const getSellableQuantity = (item: CartItem | any) => {
  const value = Number(
    item?.sellableQuantity ??
    item?.productVariant?.sellableQuantity ??
    0
  );
  if (!Number.isFinite(value) || value <= 0) return 0;
  return Math.trunc(value);
};

const getVariantSellableQuantity = (variant: any) => {
  const value = Number(variant?.sellableQuantity ?? 0);
  if (!Number.isFinite(value) || value <= 0) return 0;
  return Math.trunc(value);
};

const isVariantSelectable = (variant: any) => {
  if (!variant) return false;
  return Number(variant?.status ?? 1) === 1 && getVariantSellableQuantity(variant) > 0;
};

const getMaxAllowedQuantity = (item: CartItem) =>
  Math.min(getSellableQuantity(item), 10);

const canIncreaseQuantity = (item: CartItem) => {
  if (!isItemAvailable(item)) return false;
  const maxAllowed = getMaxAllowedQuantity(item);
  const currentQuantity = Number(item?.quantity || 0);
  return maxAllowed > 0 && currentQuantity < maxAllowed;
};

const changeQuantity = (item: CartItem, delta: number) => {

  let currentQty = Number(item.quantity || 0);

  let newQty = currentQty + delta;

  if (newQty < 1) newQty = 1;

  const stock = getSellableQuantity(item);

  if (stock <= 0) {
    Swal.fire({
      toast: true,
      position: "top-end",
      icon: "warning",
      title: "Sản phẩm hiện đã hết tồn có thể bán!",
      showConfirmButton: false,
      timer: 2000,
    });
    return;
  }

  if (newQty > 10) {

    Swal.fire({

      toast: true,

      position: "top-end",

      icon: "warning",

      title: "Chỉ được mua tối đa 10 sản phẩm!",

      showConfirmButton: false,

      timer: 2000,

    });

    newQty = 10;

  } else if (newQty > stock) {

    Swal.fire({

      toast: true,

      position: "top-end",

      icon: "warning",

      title: `Chỉ còn ${stock} sản phẩm trong kho!`,

      showConfirmButton: false,

      timer: 2000,

    });

    newQty = stock;

  }

  if (currentQty === newQty) return;

  item.quantity = newQty;

  if (updateTimeout) clearTimeout(updateTimeout);

  updateTimeout = setTimeout(() => {

    emit("update-qty", item, newQty);

  }, 400);

};

const handleManualQuantity = (item: CartItem, event: Event) => {

  const target = event.target as HTMLInputElement;

  if (target.value === "") return;

  let val = Number(target.value);

  const stock = getSellableQuantity(item);

  if (stock <= 0) {
    target.value = String(Number(item.quantity || 1));
    Swal.fire({
      toast: true,
      position: "top-end",
      icon: "warning",
      title: "Sản phẩm hiện đã hết tồn có thể bán!",
      showConfirmButton: false,
      timer: 2000,
    });
    return;
  }

  const maxAllow = Math.min(stock, 10);

  if (val > maxAllow) {

    Swal.fire({ toast: true, position: 'top-end', icon: 'warning', title: `Chỉ được mua tối đa ${maxAllow} sản phẩm!`, showConfirmButton: false, timer: 2000 });

    val = maxAllow;

  } else if (val < 1) {

    val = 1;

  } else {

    val = Math.floor(val);

  }

  target.value = String(val);

  if (Number(item.quantity) !== val) {

    item.quantity = val;

    if (updateTimeout) clearTimeout(updateTimeout);

    updateTimeout = setTimeout(() => {

      emit("update-qty", item, val);

    }, 400);

  }

};

const handleBlurQuantity = (item: CartItem, event: Event) => {

  const target = event.target as HTMLInputElement;

  if (target.value === "" || Number(target.value) < 1) {

    target.value = "1";

    handleManualQuantity(item, event);

  }

};

const getVariantId = (item: CartItem) => Number(item?.productVariantId || item?.variantId || item?.id || 0);

const getVariantsList = (item: CartItem) => {

  const i = item as any;

  const possibleProducts = [

    i?.product, i?.Product, i?.productVariant?.product, i?.ProductVariant?.Product,

    i?.variant?.product, i?.Variant?.Product, i,

  ];

  for (const p of possibleProducts) {

    if (!p) continue;

    const lists = [

      p.variants, p.Variants, p.productVariants, p.ProductVariants,

      p.productVariantList, p.ProductVariantList, p.productVariantResponses,

      p.ProductVariantResponses, p.productVariantDTOs, p.ProductVariantDTOs,

      p.lstProductVariant, p.LstProductVariant, p.items, p.Items,

    ];

    for (const list of lists) {

      if (Array.isArray(list) && list.length > 0) {
        // Không tự xét HSD ProductVariant; option dùng status + sellableQuantity.
        return list;
      }

    }

  }

  return [];

};

const BACKEND_URL = "http://localhost:8080";

const FALLBACK_IMAGE = "data:image/svg+xml;utf8," + encodeURIComponent(`<svg xmlns='http://www.w3.org/2000/svg' width='300' height='300'><rect width='100%' height='100%' fill='#f3f4f6'/><text x='50%' y='50%' dominant-baseline='middle' text-anchor='middle' fill='#9ca3af' font-family='Arial' font-size='20'>Không có ảnh</text></svg>`);

const extractImageValue = (value: any, visited = new WeakSet<object>()): string => {

  if (!value) return "";

  if (typeof value === "string" || typeof value === "number") return String(value).trim();

  if (Array.isArray(value)) {

    for (const item of value) {

      const image = extractImageValue(item, visited);

      if (image) return image;

    }

    return "";

  }

  if (typeof value === "object") {

    if (visited.has(value)) return "";

    visited.add(value);

    const candidates = [

      value?.image, value?.imageUrl, value?.mainImage, value?.thumbnailUrl,

      value?.productImage, value?.variantImage, value?.product, value?.productVariant,

    ];

    for (const candidate of candidates) {

      const image = extractImageValue(candidate, visited);

      if (image) return image;

    }

  }

  return "";

};

const getItemImage = (item: CartItem) => {

  const candidates = [

    item?.image, item?.imageUrl, item?.thumbnailUrl, item?.mainImage,

    item?.productImage, item?.variantImage, item?.product, item?.productVariant,

  ];

  for (const candidate of candidates) {

    const imageUrl = extractImageValue(candidate);

    if (imageUrl) {

      if (imageUrl.startsWith("http") || imageUrl.startsWith("data:")) return imageUrl;

      return `${BACKEND_URL}${imageUrl.startsWith("/") ? "" : "/"}${imageUrl}`;

    }

  }

  return FALLBACK_IMAGE;

};

const handleImageError = (event: Event) => {

  const target = event.target as HTMLImageElement | null;

  if (target) {

    target.onerror = null;

    target.src = FALLBACK_IMAGE;

  }

};

const getUnavailableReason = (item: CartItem) => {
  if (!item) return "Sản phẩm không hợp lệ";

  /*
   * Cart BE là nguồn quyết định trạng thái mua được.
   * Product detail enrich chỉ phục vụ hiển thị/dropdown, không được ghi đè.
   */
  if (item.unavailableReason) return item.unavailableReason;
  if (item.available === false || item.sellable === false) {
    return "Sản phẩm hiện không khả dụng";
  }
  if (item.variantStatus != null && Number(item.variantStatus) !== 1) {
    return "Sản phẩm đang ngừng bán";
  }

  const quantity = Number(item.quantity || 0);
  const sellableQuantity = getSellableQuantity(item);

  if (quantity <= 0) return "Số lượng sản phẩm không hợp lệ";
  if (sellableQuantity <= 0) return "Sản phẩm đã hết hàng";
  if (quantity > sellableQuantity) {
    return `Số lượng vượt quá tồn kho. Chỉ còn ${sellableQuantity} sản phẩm.`;
  }

  return "Sản phẩm hiện không khả dụng";
};

const isItemAvailable = (item: CartItem) => {
  if (!item) return false;

  /*
   * available/sellable/variantStatus/sellableQuantity đều lấy từ Cart BE,
   * mà BE đã kiểm Product/SKU hiện tại + InventoryLot hiện tại.
   */
  if (item.available === false || item.sellable === false) return false;
  if (item.variantStatus != null && Number(item.variantStatus) !== 1) return false;

  const quantity = Number(item.quantity || 0);
  const sellableQuantity = getSellableQuantity(item);

  return quantity > 0 && sellableQuantity > 0 && quantity <= sellableQuantity;
};


const formatVariantLabel = (v: any) => {

  if (!v) return "";

  let cap = v.capacityName || v.capacity?.name || v.capacity?.value || v.capacity || v.volume || "";

  let bot = v.bottleTypeName || v.bottleType?.name || v.bottleType?.value || v.bottleType || "";

  cap = String(cap).replace(/ml/i, "").trim();

  let label = "";

  if (cap) label += `${cap}ml`;

  if (cap && bot) label += " - ";

  if (bot) label += bot;

  return label || "Biến thể";

};

const getLineTotal = (item: CartItem) => Number(item?.price || 0) * Number(item?.quantity || 0);

const formatCurrency = (val?: number | null) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(Number(val || 0));

const formatDiscount = (value?: number | null) => {

  const num = Number(value || 0);

  return Number.isInteger(num) ? String(num) : num.toFixed(2).replace(/\.?0+$/, "");

};

</script>

<style scoped>
.cart-left {

  flex: 2;

  background: white;

  border: 1px solid #eaeaea;

  border-radius: 12px;

  padding: 40px;

  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.02);

}

.card-header {

  display: flex;

  align-items: center;

  gap: 12px;

  margin-bottom: 30px;

}

.header-icon {

  background: #06132b;

  color: white;

  width: 36px;

  height: 36px;

  border-radius: 50%;

  display: flex;

  align-items: center;

  justify-content: center;

}

.header-icon svg {

  width: 18px;

  height: 18px;

}

.card-header h2 {

  font-size: 22px;

  color: #06132b;

  margin: 0;

  position: relative;

  font-weight: 700;

}

.card-header h2::after {

  content: "";

  position: absolute;

  bottom: -10px;

  left: 0;

  width: 50px;

  height: 3px;

  background: #b78d52;

  border-radius: 2px;

}

.loading {

  padding: 30px 0;

  color: #718096;

}

.empty {

  margin-top: 20px;

  color: #718096;

  font-size: 16px;

}

.empty a {

  color: #b78d52;

  font-weight: bold;

  text-decoration: none;

  margin-left: 5px;

}

.empty a:hover {

  text-decoration: underline;

  color: #8e6c3a;

}

.cart-item {

  display: flex;

  align-items: center;

  gap: 30px;

  padding: 30px 0;

  border-bottom: 1px solid #f0f0f0;

}

.cart-item:last-child {

  border-bottom: none;

  padding-bottom: 0;

}

.item-unavailable {

  opacity: 0.72;

}

.item-img {

  width: 140px;

  height: 140px;

  object-fit: cover;

  border-radius: 12px;

  border: 1px solid #eaeaea;

  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.04);

  background: #f8fafc;

}

.item-info {

  flex: 1;

  display: flex;

  flex-direction: column;

  gap: 8px;

}

.name-row {

  display: flex;

  flex-direction: column;

  gap: 6px;

}

.item-name {

  font-size: 19px;

  font-weight: 700;

  margin: 0;

  color: #0a142f;

  line-height: 1.35;

}

.variant-grid {

  display: flex;

  flex-wrap: wrap;

  gap: 8px;

}

.item-variant {

  font-size: 15px;

  color: #718096;

  margin: 0;

}

.item-variant strong {

  color: #b78d52;

  font-weight: 700;

  font-size: 15px;

  background: rgba(183, 141, 82, 0.1);

  padding: 4px 10px;

  border-radius: 6px;

}

.variant-select {

  width: fit-content;

  min-width: 180px;

  padding: 8px 32px 8px 12px;

  font-size: 14px;

  font-weight: 700;

  color: #b78d52;

  background-color: #fdfaf6;

  border: 1px solid #e8d5b5;

  border-radius: 8px;

  cursor: pointer;

  outline: none;

  transition: all 0.2s ease;

  appearance: none;

  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23b78d52' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");

  background-repeat: no-repeat;

  background-position: right 10px center;

  background-size: 16px;

  margin-top: 5px;

}

.variant-select:hover:not(:disabled) {

  border-color: #b78d52;

  background-color: #fcf4e8;

}

.variant-select:disabled {

  opacity: 0.6;

  cursor: not-allowed;

  background-color: #f8fafc;

  border-color: #e2e8f0;

  color: #94a3b8;

  background-image: none;

}

.stock-line {

  margin: 0;

  color: #64748b;

  font-size: 13px;

}

.stock-line strong {

  color: #06132b;

}

.status-row {

  display: flex;

  align-items: center;

  gap: 8px;

}

.status-badge {

  width: fit-content;

  border-radius: 999px;

  padding: 4px 10px;

  font-size: 12px;

  font-weight: 800;

}

.status-ok {

  background: #dcfce7;

  color: #166534;

  border: 1px solid #bbf7d0;

}

.status-error {

  background: #fee2e2;

  color: #991b1b;

  border: 1px solid #fecaca;

}

.unavailable-text {

  margin: 0;

  color: #dc2626;

  background: #fee2e2;

  border: 1px solid #fecaca;

  padding: 8px 10px;

  border-radius: 8px;

  width: fit-content;

  font-size: 13px;

}

.qty-wrapper {

  display: inline-flex;

  border: 1px solid #cbd5e0;

  border-radius: 8px;

  overflow: hidden;

  width: fit-content;

  margin-top: 5px;

}

.qty-wrapper button {

  width: 45px;

  height: 45px;

  background: #f8f9fa;

  border: none;

  cursor: pointer;

  color: #0a142f;

  font-size: 20px;

  font-weight: bold;

  transition: 0.2s;

}

.qty-wrapper button:hover:not(:disabled) {

  background: #e2e8f0;

}

.qty-wrapper button:disabled {

  color: #cbd5e0;

  cursor: not-allowed;

  background: #f8f9fa;

}

.qty-wrapper input {

  width: 55px;

  text-align: center;

  border: none;

  font-size: 16px;

  font-weight: 600;

  outline: none;

  border-left: 1px solid #cbd5e0;

  border-right: 1px solid #cbd5e0;

  color: #0a142f;

}

.item-action {

  text-align: right;

  display: flex;

  flex-direction: column;

  align-items: flex-end;

  justify-content: center;

  gap: 10px;

  min-height: 130px;

}

.unit-price {

  color: #64748b;

  font-size: 13px;

  white-space: nowrap;

}

.promotion-price-box {

  display: flex;

  align-items: center;

  justify-content: flex-end;

  gap: 8px;

  margin-top: 4px;

}

.discount-badge-new {

  background: #fee2e2;

  color: #dc2626;

  border: 1px solid #fecaca;

  border-radius: 999px;

  padding: 2px 8px;

  font-size: 12px;

  font-weight: 800;

}

.price {

  font-weight: 800;

  font-size: 20px;

  color: #e53e3e;

  white-space: nowrap;

  margin-bottom: 0;

}

.btn-delete {

  background: #fff5f5;

  border: 1px solid #fed7d7;

  cursor: pointer;

  color: #e53e3e;

  display: flex;

  padding: 12px;

  border-radius: 8px;

  transition: 0.2s;

  margin-top: 8px;

}

.btn-delete svg {

  width: 22px;

  height: 22px;

}

.btn-delete:hover:not(:disabled) {

  background: #e53e3e;

  color: white;

  border-color: #e53e3e;

  box-shadow: 0 4px 10px rgba(229, 62, 62, 0.2);

}

.btn-delete:disabled {

  opacity: 0.5;

  cursor: not-allowed;

}

@media (max-width: 768px) {

  .cart-left {

    padding: 24px;

  }

  .cart-item {

    align-items: flex-start;

    gap: 16px;

  }

  .item-img {

    width: 90px;

    height: 90px;

  }

  .item-action {

    min-height: auto;

  }

  .price {

    font-size: 17px;

  }

  .qty-wrapper input[type="number"]::-webkit-inner-spin-button,

  .qty-wrapper input[type="number"]::-webkit-outer-spin-button {

    -webkit-appearance: none;

    margin: 0;

  }

  .qty-wrapper input[type="number"] {

    appearance: textfield;

    -moz-appearance: textfield;

  }

}
</style>