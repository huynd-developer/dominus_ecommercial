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
  promotionStartDate?: string | null;
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
    item?.sellableQuantity ?? item?.productVariant?.sellableQuantity ?? 0
  );
  if (!Number.isFinite(value) || value <= 0) return 0;
  return Math.trunc(value);
};

const getUnavailableReason = (item: CartItem) => {
  if (!item) return "Sản phẩm không hợp lệ.";

  /*
   * Cart BE là nguồn nghiệp vụ cho trạng thái mua được.
   * Không dùng Product detail bổ sung để ghi đè kết luận này.
   */
  if (item.unavailableReason) return item.unavailableReason;
  if (item.available === false || item.sellable === false) {
    return "Sản phẩm hiện không khả dụng.";
  }
  if (item.variantStatus != null && Number(item.variantStatus) !== 1) {
    return "Sản phẩm đang ngừng bán.";
  }

  const quantity = Number(item.quantity || 0);
  const sellableQuantity = getSellableQuantity(item);

  if (quantity <= 0) return "Số lượng sản phẩm không hợp lệ.";
  if (sellableQuantity <= 0) return "Sản phẩm đã hết hàng.";
  if (quantity > sellableQuantity) {
    return `Số lượng trong giỏ vượt quá tồn kho. Chỉ còn ${sellableQuantity} cái có thể bán.`;
  }
  return "Sản phẩm hiện không khả dụng.";
};

const isItemAvailable = (item: CartItem) => {
  if (!item) return false;

  if (item.available === false || item.sellable === false) return false;
  if (item.variantStatus != null && Number(item.variantStatus) !== 1)
    return false;

  const quantity = Number(item.quantity || 0);
  const sellableQuantity = getSellableQuantity(item);

  return quantity > 0 && sellableQuantity > 0 && quantity <= sellableQuantity;
};

const totalAmount = computed(() =>
  cartItems.value.reduce(
    (sum, item) =>
      isItemAvailable(item)
        ? sum + getItemPrice(item) * getItemQuantity(item)
        : sum,
    0
  )
);
const canCheckout = computed(
  () =>
    cartItems.value.length > 0 &&
    cartItems.value.every((item) => isItemAvailable(item))
);

// Hàm Toast hiển thị chạy ngầm không có await để tránh chặn UI
const showToast = (
  icon: "success" | "error" | "warning" | "info",
  title: string
) => {
  Swal.fire({
    toast: true,
    position: "top-end",
    icon,
    title,
    showConfirmButton: false,
    timer: 1800,
    timerProgressBar: true,
  });
};
const showError = async (title: string, text: string) => {
  await Swal.fire({
    icon: "error",
    title,
    text,
    confirmButtonText: "Đóng",
    confirmButtonColor: "#bd9a5f",
  });
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
  if (hadVoucher)
    showToast(
      "info",
      "Vui lòng áp dụng lại mã giảm giá do giỏ hàng đã thay đổi!"
    );
};

const extractCartItems = (payload: any): CartItem[] => {
  const candidates = [
    payload,
    payload?.data,
    payload?.content,
    payload?.items,
    payload?.cartItems,
    payload?.data?.content,
    payload?.data?.items,
    payload?.data?.cartItems,
  ];
  for (const candidate of candidates) {
    if (Array.isArray(candidate)) return candidate;
  }
  return [];
};

const getItemProductId = (item: CartItem) => {
  const i = item as any;
  return Number(
    i?.productId ||
      i?.ProductId ||
      i?.product?.id ||
      i?.product?.productId ||
      i?.Product?.id ||
      i?.Product?.productId ||
      i?.productVariant?.productId ||
      i?.productVariant?.product?.id ||
      i?.ProductVariant?.ProductId ||
      i?.ProductVariant?.Product?.Id ||
      i?.variant?.productId ||
      i?.variant?.product?.id ||
      0
  );
};

const getItemVariantId = (item: CartItem) => {
  const i = item as any;
  return Number(
    i?.productVariantId ||
      i?.ProductVariantId ||
      i?.variantId ||
      i?.VariantId ||
      i?.productVariant?.id ||
      i?.ProductVariant?.Id ||
      i?.productVariant?.productVariantId ||
      i?.variant?.id ||
      i?.Variant?.Id ||
      0
  );
};

const extractImageValue = (
  value: any,
  visited = new WeakSet<object>()
): string => {
  if (!value) return "";
  if (typeof value === "string" || typeof value === "number")
    return String(value).trim();
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
      value?.image,
      value?.imageUrl,
      value?.mainImage,
      value?.thumbnailUrl,
      value?.productImage,
      value?.variantImage,
      value?.product,
      value?.productVariant,
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
    productData?.variants,
    productData?.Variants,
    productData?.productVariants,
    productData?.ProductVariants,
    productData?.productVariantList,
    productData?.ProductVariantList,
    productData?.productVariantResponses,
    productData?.productVariantDTOs,
  ];
  for (const candidate of candidates) {
    if (Array.isArray(candidate)) return candidate;
  }
  return [];
};

const findMatchingVariant = (productData: any, variantId: number) => {
  if (!variantId) return null;
  return (
    getProductVariants(productData).find((variant: any) => {
      return (
        Number(variant?.productVariantId || variant?.id || variant?.Id || 0) ===
        variantId
      );
    }) || null
  );
};

const fetchProductDetail = async (productId: number) => {
  if (!productId) return null;
  try {
    const t = Date.now();
    let res = await api
      .get(`/v1/products/${productId}?t=${t}`)
      .catch(() => null);
    if (!res)
      res = await api
        .get(`/customer/products/${productId}?t=${t}`)
        .catch(() => null);
    if (!res || !res.data) return null;

    // Chống lỗi BE trả về 200 nhưng ruột data bị null
    if (res.data.hasOwnProperty("data") && res.data.data === null) return null;
    if (res.data.hasOwnProperty("result") && res.data.result === null)
      return null;

    const data = res.data.data ?? res.data.result ?? res.data;

    // Chặn object rỗng hoặc báo lỗi 404 fake
    if (Object.keys(data).length <= 2 || data.status === 404) return null;

    return data;
  } catch (error) {
    return null;
  }
};

const enrichCartItemForDisplay = async (item: CartItem): Promise<CartItem> => {
  if (!item) return item;

  /*
   * Cart BE là nguồn dữ liệu nghiệp vụ:
   * - price / Flash Sale
   * - available / sellable / unavailableReason
   * - sellableQuantity từ InventoryLot
   * - variantStatus
   *
   * Product detail chỉ bổ sung danh sách variant cho dropdown và ảnh fallback.
   * Nếu request Product detail lỗi hoặc không còn trả current variant thì
   * không được tự kết luận Product/SKU đã bị xóa.
   */
  const productId = getItemProductId(item);
  const variantId = getItemVariantId(item);

  if (!productId) return item;

  const productData = await fetchProductDetail(productId);
  if (!productData) return item;

  const matchedVariant = variantId
    ? findMatchingVariant(productData, variantId)
    : null;

  const fallbackImage =
    extractImageValue(matchedVariant) || extractImageValue(productData);

  return {
    ...item,
    // Ưu tiên ảnh hiện tại từ Cart BE; Product detail chỉ fallback.
    imageUrl: item.imageUrl || item.thumbnailUrl || fallbackImage || null,
    product: productData,
    productVariant: matchedVariant || item.productVariant || null,
  };
};

const enrichCartItemsForDisplay = async (items: CartItem[]) => {
  return await Promise.all(items.map((item) => enrichCartItemForDisplay(item)));
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

const MAX_PROMOTION_TIMER_DELAY = 2_147_000_000;

let promotionBoundaryTimer: ReturnType<typeof setTimeout> | null = null;

const clearPromotionBoundaryTimer = () => {
  if (promotionBoundaryTimer !== null) {
    clearTimeout(promotionBoundaryTimer);
    promotionBoundaryTimer = null;
  }
};

const getPromotionBoundaryTimes = (item: CartItem): number[] => {
  const times: number[] = [];

  const rawStartDate = item?.promotionStartDate;
  const rawEndDate = item?.promotionEndDate;

  if (rawStartDate) {
    const startTime = new Date(rawStartDate).getTime();

    if (Number.isFinite(startTime)) {
      times.push(startTime);
    }
  }

  if (rawEndDate) {
    const endTime = new Date(rawEndDate).getTime();

    if (Number.isFinite(endTime)) {
      times.push(endTime);
    }
  }

  return times;
};

const schedulePromotionBoundaryRefresh = () => {
  clearPromotionBoundaryTimer();

  const now = Date.now();

  const futureTimes = cartItems.value
    .flatMap(getPromotionBoundaryTimes)
    .filter((time) => time > now);

  if (futureTimes.length === 0) {
    return;
  }

  const nextBoundary = Math.min(...futureTimes);

  const delay = Math.min(
    Math.max(0, nextBoundary - now + 200),
    MAX_PROMOTION_TIMER_DELAY
  );

  promotionBoundaryTimer = setTimeout(() => {
    promotionBoundaryTimer = null;

    /*
     * Không reload trang.
     * Chỉ gọi lại API hiện tại để BE tính giá theo thời gian mới.
     */
    void refreshCurrentCart();
  }, delay);
};

const loadCart = async (
  options: { preserveOrder?: boolean } = {}
): Promise<boolean> => {
  try {
    isLoading.value = true;
    const res = await api.get(`/v1/customer/cart/my-cart?t=${Date.now()}`);
    let items = extractCartItems(res.data);

    // Cart BE quyết định nghiệp vụ; enrich chỉ bổ sung dữ liệu hiển thị/dropdown.
    const enrichedItems = await enrichCartItemsForDisplay(items);

    cartItems.value = options.preserveOrder
      ? preserveCartOrder(enrichedItems)
      : enrichedItems;

    /*
     * Sau mỗi lần lấy Cart mới, đặt timer tới StartDate/EndDate gần nhất.
     */
    schedulePromotionBoundaryRefresh();

    if (!canCheckout.value) resetVoucher();
    return true;
  } catch (err: any) {
    showError("Lỗi", "Không tải được giỏ hàng");
    if (err?.response?.status === 401 || err?.response?.status === 403)
      router.push("/login");
    return false;
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
    showToast(
      "warning",
      `Sản phẩm chỉ còn ${sellableQuantity} sản phẩm có thể bán`
    );
    return;
  }

  try {
    isUpdating.value = true;
    await api.put("/v1/customer/cart/update", {
      cartItemId: item.cartItemId,
      productVariantId: getItemVariantId(item),
      quantity: newQty,
    });

    resetVoucher();

    /*
     * Mutation thành công vẫn refetch Cart để nhận giá/Flash Sale/tồn/status
     * mới nhất nếu module khác vừa thay đổi đồng thời.
     */
    await loadCart({ preserveOrder: true });
  } catch (err: any) {
    const message =
      err?.response?.data?.message ||
      err?.response?.data?.error ||
      "Không thể cập nhật giỏ hàng";

    await showError("Giỏ hàng đã thay đổi", message);
    await loadCart({ preserveOrder: true });
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
      quantity: Number(item.quantity || 1),
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
      err?.response?.data?.message ||
        "Không thể đổi loại sản phẩm. Vui lòng thử lại!"
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
    resetVoucher();
    await loadCart({ preserveOrder: true });
  } catch (err: any) {
    await showError(
      "Lỗi",
      err?.response?.data?.message || "Không thể xóa sản phẩm"
    );
    await loadCart({ preserveOrder: true });
  } finally {
    isUpdating.value = false;
  }
};

const goToCheckout = async () => {
  if (cartItems.value.length === 0) {
    showToast("warning", "Giỏ hàng đang trống");
    return;
  }

  /*
   * Re-read Cart ngay trước khi rời trang.
   * Checkout vẫn revalidate lần cuối ở BE; bước này chỉ bảo đảm Cart không
   * đưa người dùng sang Checkout bằng UI đã cũ.
   */
  const refreshed = await loadCart({ preserveOrder: true });
  if (!refreshed) return;

  await loadSavedVoucher();

  const invalidItem = cartItems.value.find((item) => !isItemAvailable(item));
  if (invalidItem) {
    await Swal.fire({
      icon: "warning",
      title: "Giỏ hàng đã thay đổi",
      text: getUnavailableReason(invalidItem),
    });
    return;
  }

  router.push("/checkout");
};

const shippingFee = ref(30000);

const finalTotal = computed(() => {
  if (cartItems.value.length === 0) return 0;
  return (
    Math.max(0, totalAmount.value - discountAmount.value) + shippingFee.value
  );
});

const loadSavedVoucher = async () => {
  const savedCode = localStorage.getItem("applied_voucher");
  if (!savedCode || totalAmount.value <= 0) {
    discountAmount.value = 0;
    appliedVoucherCode.value = "";
    return;
  }

  try {
    const res = await api.get("/v1/customer/vouchers/apply", {
      params: { code: savedCode, orderTotal: totalAmount.value },
    });
    const discount = Number(
      res.data?.discountAmount ?? res.data?.discount ?? res.data?.amount ?? 0
    );
    discountAmount.value = Math.min(
      Math.max(discount, 0),
      Number(totalAmount.value || 0)
    );
    appliedVoucherCode.value = savedCode;
  } catch (error) {
    discountAmount.value = 0;
    appliedVoucherCode.value = "";
    localStorage.removeItem("applied_voucher");
    showToast("info", "Mã giảm giá đã hết hạn hoặc bị vô hiệu hóa!");
  }
};

let externalRefreshInProgress = false;

const refreshCurrentCart = async () => {
  if (externalRefreshInProgress || isUpdating.value) return;

  externalRefreshInProgress = true;
  try {
    const loaded = await loadCart({ preserveOrder: true });
    if (loaded) {
      await loadSavedVoucher();
    }
  } finally {
    externalRefreshInProgress = false;
  }
};

const handleFocus = () => {
  void refreshCurrentCart();
};

const handleVisibilityChange = () => {
  if (document.visibilityState === "visible") {
    void refreshCurrentCart();
  }
};

onMounted(async () => {
  await loadCart();
  await loadSavedVoucher();

  window.addEventListener("focus", handleFocus);
  document.addEventListener("visibilitychange", handleVisibilityChange);
});

onUnmounted(() => {
  clearPromotionBoundaryTimer();

  window.removeEventListener("focus", handleFocus);
  document.removeEventListener("visibilitychange", handleVisibilityChange);
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
  .main-content.full-width {
    flex-direction: column;
  }
}
</style>
