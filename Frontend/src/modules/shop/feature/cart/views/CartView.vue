<template>
  <div class="page-wrapper">
    <ShopHeader />

    <main class="main-content full-width">
      <CartItemList
        :cartItems="cartItems"
        :isLoading="isLoading"
        :isUpdating="isUpdating"
        @update-qty="updateQty"
        @remove-item="removeItem"
        @update-variant="updateVariant"
      />

      <CartSummary
        v-if="cartItems.length > 0"
        :totalAmount="totalAmount"
        :discountAmount="discountAmount"
        :shippingFee="shippingFee"
        :finalTotal="finalTotal"
        :canCheckout="canCheckout"
        @checkout="goToCheckout"
        @apply-voucher="handleApplyVoucher"
      />
    </main>

    <ShopFooter />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRouter } from "vue-router";
import Swal from "sweetalert2";
import api from "@/common/api";

import ShopHeader from "@/modules/shop/layout/ShopHeader.vue";
import ShopFooter from "@/modules/shop/layout/ShopFooter.vue";
import CartItemList from "../components/CartItemList.vue";
import CartSummary from "../components/CartSummary.vue";

interface CartItem {
  cartItemId: number;
  productVariantId?: number;
  variantId?: number;
  id?: number;
  productId?: number;
  ProductId?: number;
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
  /** LEGACY compatibility. FE Cart không dùng để quyết định tồn. */
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
  
  // Flag nhận biết SP đã bị xóa hay chưa
  isDeleted?: boolean | null;
}

const router = useRouter();
const cartItems = ref<CartItem[]>([]);
const isLoading = ref(true);
const isUpdating = ref(false);
const discountAmount = ref(0);
const appliedVoucherCode = ref("");

const getItemPrice = (item: CartItem) => Number(item?.price || 0);
const getItemQuantity = (item: CartItem) => Number(item?.quantity || 0);

const getSellableQuantity = (item: CartItem | any) => {
  const value = Number(
    item?.sellableQuantity ??
      item?.productVariant?.sellableQuantity ??
      0
  );
  if (!Number.isFinite(value) || value <= 0) return 0;
  return Math.trunc(value);
};

const getUnavailableReason = (item: CartItem) => {
  if (!item) return "Sản phẩm không hợp lệ.";
  
  // ĐÃ THÊM: Check thêm SP bị xóa không?
  if (item.isDeleted === true || item.product?.isDeleted === true || item.productVariant?.isDeleted === true) {
      return "Sản phẩm đã bị xóa khỏi hệ thống.";
  }

  if (item.unavailableReason) return item.unavailableReason;
  if (item.available === false || item.sellable === false) return "Sản phẩm hiện không khả dụng.";
  if (item.variantStatus != null && Number(item.variantStatus) !== 1) return "Sản phẩm đang ngừng bán.";

  const quantity = getItemQuantity(item);
  const sellableQuantity = getSellableQuantity(item);

  if (quantity <= 0) return "Số lượng sản phẩm không hợp lệ.";
  if (sellableQuantity <= 0) return "Sản phẩm đã hết hàng.";
  if (quantity > sellableQuantity) {
    return `Số lượng trong giỏ vượt quá tồn kho. Sản phẩm chỉ còn ${sellableQuantity} sản phẩm có thể bán.`;
  }
  return "Sản phẩm hiện không khả dụng.";
};

const isItemAvailable = (item: CartItem) => {
  if (!item) return false;
  
  // ĐÃ THÊM: Check SP bị xóa không?
  if (item.isDeleted === true || item.product?.isDeleted === true || item.productVariant?.isDeleted === true) return false;

  if (item.available === false || item.sellable === false) return false;
  if (item.variantStatus != null && Number(item.variantStatus) !== 1) return false;

  const quantity = getItemQuantity(item);
  const sellableQuantity = getSellableQuantity(item);

  return quantity > 0 && sellableQuantity > 0 && quantity <= sellableQuantity;
};

const totalAmount = computed(() => cartItems.value.reduce((sum, item) => isItemAvailable(item) ? sum + getItemPrice(item) * getItemQuantity(item) : sum, 0));
const canCheckout = computed(() => cartItems.value.length > 0 && cartItems.value.every((item) => isItemAvailable(item)));

// Hàm Toast hiển thị chạy ngầm không có await để tránh chặn UI
const showToast = (icon: "success" | "error" | "warning" | "info", title: string) => {
  Swal.fire({ toast: true, position: "top-end", icon, title, showConfirmButton: false, timer: 1800, timerProgressBar: true });
};
const showError = async (title: string, text: string) => {
  await Swal.fire({ icon: "error", title, text, confirmButtonText: "Đóng", confirmButtonColor: "#bd9a5f" });
};

const handleApplyVoucher = (discount: number, voucherCode: string) => {
  discountAmount.value = Number(discount || 0);
  appliedVoucherCode.value = voucherCode || "";
  if (voucherCode) localStorage.setItem("applied_voucher", voucherCode);
  else localStorage.removeItem("applied_voucher");
};

const resetVoucher = () => {
  const hadVoucher = discountAmount.value > 0 || appliedVoucherCode.value;
  discountAmount.value = 0;
  appliedVoucherCode.value = "";
  localStorage.removeItem("applied_voucher");
  if (hadVoucher) showToast("info", "Vui lòng áp dụng lại mã giảm giá do giỏ hàng đã thay đổi!");
};

const extractCartItems = (payload: any): CartItem[] => {
  const candidates = [payload, payload?.data, payload?.content, payload?.items, payload?.cartItems, payload?.data?.content, payload?.data?.items, payload?.data?.cartItems];
  for (const candidate of candidates) {
    if (Array.isArray(candidate)) return candidate;
  }
  return [];
};

const getItemProductId = (item: CartItem) => {
  const i = item as any; 
  return Number(
    i?.productId || i?.ProductId ||
    i?.product?.id || i?.product?.productId || i?.Product?.id || i?.Product?.productId ||
    i?.productVariant?.productId || i?.productVariant?.product?.id ||
    i?.ProductVariant?.ProductId || i?.ProductVariant?.Product?.Id ||
    i?.variant?.productId || i?.variant?.product?.id ||
    0
  );
};

const getItemVariantId = (item: CartItem) => {
  const i = item as any;
  return Number(
    i?.productVariantId || i?.ProductVariantId ||
    i?.variantId || i?.VariantId ||
    i?.productVariant?.id || i?.ProductVariant?.Id ||
    i?.productVariant?.productVariantId ||
    i?.variant?.id || i?.Variant?.Id ||
    0
  );
};

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
      value?.productImage, value?.variantImage, value?.product, value?.productVariant
    ];
    for (const candidate of candidates) {
      const image = extractImageValue(candidate, visited);
      if (image) return image;
    }
  }
  return "";
};

const getProductVariants = (productData: any) => {
  const candidates = [
    productData?.variants, productData?.Variants, 
    productData?.productVariants, productData?.ProductVariants, 
    productData?.productVariantList, productData?.ProductVariantList,
    productData?.productVariantResponses, productData?.productVariantDTOs
  ];
  for (const candidate of candidates) {
    if (Array.isArray(candidate)) return candidate;
  }
  return [];
};

const findMatchingVariant = (productData: any, variantId: number) => {
  if (!variantId) return null;
  return getProductVariants(productData).find((variant: any) => {
    return Number(variant?.productVariantId || variant?.id || variant?.Id || 0) === variantId;
  }) || null;
};


const fetchProductDetail = async (productId: number) => {
  if (!productId) return null;
  
  try {
    const t = Date.now();
    let res = await api.get(`/v1/products/${productId}?t=${t}`).catch(() => null);
    if (!res) res = await api.get(`/customer/products/${productId}?t=${t}`).catch(() => null);
    if (!res) return null;

    return res.data?.data ?? res.data?.result ?? res.data;
  } catch (error) {
    return null;
  }
};

const enrichCartItemImage = async (item: CartItem): Promise<CartItem> => {
  if (!item) return item;
  
  const productId = getItemProductId(item);
  if (!productId) return item; 

  const productData = await fetchProductDetail(productId);
  // ĐÃ THÊM: Nếu BE trả null => Sản phẩm bị xóa/không tồn tại
  if (!productData) {
      return {
          ...item,
          isDeleted: true,
          variantStatus: 0,
          sellableQuantity: 0,
          stockQuantity: 0,
          sellable: false
      };
  }

  const matchedVariant = findMatchingVariant(productData, getItemVariantId(item));
  
  // ĐÃ THÊM: Check nếu Biến thể bị xóa
  if (!matchedVariant) {
       return {
          ...item,
          isDeleted: true,
          variantStatus: 0,
          sellableQuantity: 0,
          stockQuantity: 0,
          sellable: false
      };
  }

  const imageUrl = extractImageValue(matchedVariant) || extractImageValue(productData);

  return {
    ...item,
    imageUrl: extractImageValue(item) || imageUrl, 
    product: productData, 
    productVariant: matchedVariant || item.productVariant,
    isDeleted: productData?.isDeleted || matchedVariant?.isDeleted || false
  };
};

const enrichCartItemsWithImages = async (items: CartItem[]) => {
  return await Promise.all(items.map((item) => enrichCartItemImage(item)));
};

const preserveCartOrder = (items: CartItem[]) => {
  if (!cartItems.value.length) return items;

  const orderMap = new Map<number, number>();
  cartItems.value.forEach((item, index) => {
    if (item?.cartItemId) orderMap.set(Number(item.cartItemId), index);
  });

  return [...items].sort((a, b) => {
    const aOrder = orderMap.get(Number(a?.cartItemId || 0));
    const bOrder = orderMap.get(Number(b?.cartItemId || 0));

    if (aOrder == null && bOrder == null) return 0;
    if (aOrder == null) return 1;
    if (bOrder == null) return -1;
    return aOrder - bOrder;
  });
};

const loadCart = async (options: { preserveOrder?: boolean } = {}) => {
  try {
    isLoading.value = true;
    
    // Lấy giỏ hàng mới nhất
    const res = await api.get(`/v1/customer/cart/my-cart?t=${Date.now()}`);
    let items = extractCartItems(res.data);
    
    // Lấy thông tin mới nhất từ Admin để cập nhật hiển thị
    if (items.length > 0) {
      items = await Promise.all(items.map(async (item: any) => {
        try {
          const productId = getItemProductId(item);
          const variantId = getItemVariantId(item);
          if (!productId) return item;
          
          const productData = await fetchProductDetail(productId);

          // ĐÃ THÊM: Nếu BE trả null -> Sản phẩm bị xóa/không tồn tại
          if (!productData || productData?.isDeleted === true) {
            return {
              ...item,
              isDeleted: true,
              variantStatus: 0,
              sellableQuantity: 0,
              stockQuantity: 0,
              sellable: false,
            };
          }

          const matchedVariant = findMatchingVariant(productData, variantId);

          if (matchedVariant && !matchedVariant?.isDeleted) {
            // Giữ nguyên price/discount từ Cart BE, chỉ enrich metadata + tồn bán được.
            const sellableQuantity = Math.max(
              Number(
                matchedVariant?.sellableQuantity ??
                  item?.sellableQuantity ??
                  0
              ) || 0,
              0
            );

            return {
              ...item,
              sellableQuantity,
              stockQuantity: sellableQuantity,
              variantStatus: Number(
                matchedVariant?.status ??
                  item?.variantStatus ??
                  0
              ),
              product: productData,
              productVariant: matchedVariant,
            };
          }

          // Trưởng hợp có product nhưng KHÔNG có biến thể (Biến thể bị xóa)
          return {
            ...item,
            isDeleted: true,
            variantStatus: 0,
            sellableQuantity: 0,
            stockQuantity: 0,
            sellable: false,
          };
        } catch (e) {
          return item;
        }
      }));
    }
    
    const enrichedItems = await enrichCartItemsWithImages(items);
    cartItems.value = options.preserveOrder ? preserveCartOrder(enrichedItems) : enrichedItems;
    if (!canCheckout.value) resetVoucher();
  } catch (err: any) {
    showError("Lỗi", "Không tải được giỏ hàng");
    if (err?.response?.status === 401 || err?.response?.status === 403) router.push("/login");
  } finally {
    isLoading.value = false;
  }
};

const updateQty = async (item: CartItem, newQty: number) => {
  if (!item?.cartItemId || newQty < 1) return;

  const sellableQuantity = getSellableQuantity(item);

  if (sellableQuantity <= 0) {
    showToast("warning", "Sản phẩm đã hết tồn có thể bán");
    return;
  }

  if (newQty > sellableQuantity) {
    showToast("warning", `Sản phẩm chỉ còn ${sellableQuantity} sản phẩm có thể bán`);
    return;
  }

  try {
    isUpdating.value = true;
    await api.put("/v1/customer/cart/update", {
      cartItemId: item.cartItemId,
      productVariantId: getItemVariantId(item),
      quantity: newQty,
    });
    item.quantity = newQty;
    resetVoucher();
  } catch (err: any) {
    showError("Lỗi", "Không thể cập nhật giỏ hàng");
    loadCart();
  } finally {
    isUpdating.value = false;
  }
};

const updateVariant = async (item: CartItem, newVariantId: number) => {
  if (!item?.cartItemId || !newVariantId) return;
  if (getItemVariantId(item) === newVariantId) return;

  try {
    isUpdating.value = true;

    // Thêm phân loại mới vào giỏ
    await api.post("/v1/customer/cart/add", {
      productVariantId: newVariantId,
      quantity: Number(item.quantity || 1)
    });

    // Xóa phân loại cũ khỏi giỏ
    await api.delete(`/v1/customer/cart/remove/${item.cartItemId}`);

    resetVoucher();
    showToast("success", "Đã đổi phân loại sản phẩm");
    
    // Tải lại dữ liệu
    await loadCart();
  } catch (err: any) {
    console.error("Lỗi đổi biến thể:", err);
    showError(
      "Lỗi",
      err?.response?.data?.message || "Không thể đổi loại sản phẩm. Vui lòng thử lại!"
    );
    await loadCart();
  } finally {
    isUpdating.value = false;
  }
};

const removeItem = async (cartItemId: number) => {
  if (!cartItemId) return;
  try {
    isUpdating.value = true;
    await api.delete(`/v1/customer/cart/remove/${cartItemId}`);
    cartItems.value = cartItems.value.filter((item) => item.cartItemId !== cartItemId);
    resetVoucher();
  } catch (err: any) {
    showError("Lỗi", "Không thể xóa sản phẩm");
    loadCart();
  } finally {
    isUpdating.value = false;
  }
};

const goToCheckout = async () => {
  if (cartItems.value.length === 0) {
    showToast("warning", "Giỏ hàng đang trống");
    return;
  }
  const invalidItem = cartItems.value.find((item) => !isItemAvailable(item));
  if (invalidItem) {
    await Swal.fire({ icon: "warning", title: "Cảnh báo", text: getUnavailableReason(invalidItem) });
    return loadCart();
  }
  router.push("/checkout");
};

const shippingFee = ref(30000);

const finalTotal = computed(() => {
  if (cartItems.value.length === 0) return 0;
  return Math.max(0, totalAmount.value - discountAmount.value) + shippingFee.value;
});

const loadSavedVoucher = async () => {
  const savedCode = localStorage.getItem("applied_voucher");
  if (!savedCode || totalAmount.value <= 0) {
    if (discountAmount.value > 0) {
      discountAmount.value = 0;
      appliedVoucherCode.value = "";
    }
    return;
  }

  try {
    const res = await api.get("/v1/customer/vouchers/apply", {
      params: { code: savedCode, orderTotal: totalAmount.value },
    });
    const discount = Number(res.data?.discountAmount ?? res.data?.discount ?? res.data?.amount ?? 0);
    discountAmount.value = Math.min(Math.max(discount, 0), Number(totalAmount.value || 0));
    appliedVoucherCode.value = savedCode;
  } catch (error) {
    discountAmount.value = 0;
    appliedVoucherCode.value = "";
    localStorage.removeItem("applied_voucher");
    showToast("info", "Mã giảm giá đã hết hạn hoặc bị vô hiệu hóa!");
  }
};

const handleFocus = async () => {
  await loadCart({ preserveOrder: true });
  await loadSavedVoucher();
};

onMounted(async () => {
  await loadCart();
  await loadSavedVoucher();
  window.addEventListener("focus", handleFocus);
});

onUnmounted(() => {
  window.removeEventListener("focus", handleFocus);
});
</script>

<style scoped>
.page-wrapper {
  background-color: #fafbfc;
  min-height: 100vh;
  padding-bottom: 50px;
  color: #06132b;
}
.main-content.full-width {
  max-width: 1400px;
  width: 100%;
  margin: 40px auto;
  padding: 0 20px;
  display: flex;
  gap: 30px;
  align-items: flex-start;
}
@media (max-width: 992px) {
  .main-content.full-width { flex-direction: column; }
}
</style>