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
import { computed, onMounted, ref } from "vue";
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
  stockQuantity?: number | null;
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
}

const router = useRouter();
const cartItems = ref<CartItem[]>([]);
const isLoading = ref(true);
const isUpdating = ref(false);
const discountAmount = ref(0);
const appliedVoucherCode = ref("");

const toDateOnly = (value?: string | null) => value ? String(value).substring(0, 10) : null;
const isBeforeToday = (value?: string | null) => {
  const dateOnly = toDateOnly(value);
  if (!dateOnly) return false;
  const date = new Date(`${dateOnly}T00:00:00`);
  const today = new Date(); today.setHours(0, 0, 0, 0);
  return !Number.isNaN(date.getTime()) && date.getTime() < today.getTime();
};
const isAfterToday = (value?: string | null) => {
  const dateOnly = toDateOnly(value);
  if (!dateOnly) return false;
  const date = new Date(`${dateOnly}T00:00:00`);
  const today = new Date(); today.setHours(0, 0, 0, 0);
  return !Number.isNaN(date.getTime()) && date.getTime() > today.getTime();
};

const getItemPrice = (item: CartItem) => Number(item?.price || 0);
const getItemQuantity = (item: CartItem) => Number(item?.quantity || 0);
const isItemExpired = (item: CartItem) => Boolean(item?.expired) || isBeforeToday(item?.expirationDate);

const getUnavailableReason = (item: CartItem) => {
  if (!item) return "Sản phẩm không hợp lệ.";
  if (item.unavailableReason) return item.unavailableReason;
  if (item.available === false || item.sellable === false) return "Sản phẩm hiện không khả dụng.";
  if (item.variantStatus != null && Number(item.variantStatus) !== 1) return "Sản phẩm đang ngừng bán.";
  const quantity = getItemQuantity(item);
  const stockQuantity = Number(item?.stockQuantity || 0);
  if (quantity <= 0) return "Số lượng sản phẩm không hợp lệ.";
  if (stockQuantity <= 0) return "Sản phẩm đã hết hàng.";
  if (quantity > stockQuantity) return `Số lượng trong giỏ vượt quá tồn kho. Sản phẩm chỉ còn ${stockQuantity}.`;
  if (isAfterToday(item.manufacturingDate)) return "Sản phẩm chưa tới ngày được bán.";
  if (isItemExpired(item)) return "Sản phẩm đã hết hạn sử dụng.";
  return "Sản phẩm hiện không khả dụng.";
};

const isItemAvailable = (item: CartItem) => {
  if (!item) return false;
  if (item.available === false || item.sellable === false) return false;
  if (item.variantStatus != null && Number(item.variantStatus) !== 1) return false;
  const quantity = getItemQuantity(item);
  const stockQuantity = Number(item?.stockQuantity || 0);
  if (quantity <= 0 || stockQuantity <= 0 || quantity > stockQuantity) return false;
  if (isAfterToday(item.manufacturingDate)) return false;
  if (isItemExpired(item)) return false;
  return true;
};

const totalAmount = computed(() => cartItems.value.reduce((sum, item) => isItemAvailable(item) ? sum + getItemPrice(item) * getItemQuantity(item) : sum, 0));
const canCheckout = computed(() => cartItems.value.length > 0 && cartItems.value.every((item) => isItemAvailable(item)));

const showToast = async (icon: "success" | "error" | "warning" | "info", title: string) => {
  await Swal.fire({ toast: true, position: "top-end", icon, title, showConfirmButton: false, timer: 1800, timerProgressBar: true });
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

// ==========================================
// T FIX LẠI HÀM LẤY ID: QUÉT CẠN KIỆT MỌI NGÓC NGÁCH
// Dùng TOÁN TỬ || để nếu cái trước trả về 0 thì nó vẫn tìm tiếp
// ==========================================
// ==========================================
// FIX TYPESCRIPT LỖI GẠCH ĐỎ (DÙNG AS ANY ĐỂ BỎ QUA CHECK TYPE)
// ==========================================
const getItemProductId = (item: CartItem) => {
  const i = item as any; // Ép kiểu về any để TS không bắt bẻ các biến viết hoa
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

const productDetailCache = new Map<number, any>();

const fetchProductDetail = async (productId: number) => {
  if (!productId) return null;
  if (productDetailCache.has(productId)) return productDetailCache.get(productId);
  try {
    let res = await api.get(`/v1/products/${productId}`).catch(() => null);
    if (!res) {
      // Đề phòng endpoint là /customer/products
      res = await api.get(`/customer/products/${productId}`).catch(() => null);
    }
    if (!res) return null;

    const data = res.data?.data ?? res.data?.result ?? res.data;
    productDetailCache.set(productId, data);
    return data;
  } catch (error) {
    productDetailCache.set(productId, null);
    return null;
  }
};

const enrichCartItemImage = async (item: CartItem): Promise<CartItem> => {
  if (!item) return item;
  
  const productId = getItemProductId(item);
  if (!productId) return item; 

  const productData = await fetchProductDetail(productId);
  if (!productData) return item;

  const matchedVariant = findMatchingVariant(productData, getItemVariantId(item));
  const imageUrl = extractImageValue(matchedVariant) || extractImageValue(productData);

  return {
    ...item,
    imageUrl: extractImageValue(item) || imageUrl, 
    product: productData, 
    productVariant: matchedVariant || item.productVariant,
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
    const res = await api.get("/v1/customer/cart/my-cart");
    
    // --- THÊM 2 DÒNG NÀY ĐỂ BẮT SỐNG DATA ---
    console.log("📦 DỮ LIỆU GIỎ HÀNG GỐC:", res.data);
    const items = extractCartItems(res.data);
    console.log("🛒 TỪNG ITEM TRONG GIỎ:", items);
    // ----------------------------------------

    const enrichedItems = await enrichCartItemsWithImages(items);
    cartItems.value = options.preserveOrder
      ? preserveCartOrder(enrichedItems)
      : enrichedItems;
    if (!canCheckout.value) resetVoucher();
  } catch (err: any) {
    await showError("Lỗi", "Không tải được giỏ hàng");
    if (err?.response?.status === 401 || err?.response?.status === 403) router.push("/login");
  } finally {
    isLoading.value = false;
  }
};

const updateQty = async (item: CartItem, newQty: number) => {
  if (!item?.cartItemId || newQty < 1) return;
  const stockQuantity = Number(item?.stockQuantity || 0);

  if (stockQuantity <= 0) return showToast("warning", "Sản phẩm đã hết hàng");
  if (newQty > stockQuantity) return showToast("warning", `Sản phẩm chỉ còn ${stockQuantity} trong kho`);

  try {
    isUpdating.value = true;
    await api.put("/v1/customer/cart/update", {
      cartItemId: item.cartItemId,
      productVariantId: getItemVariantId(item),
      quantity: newQty,
    });
    item.quantity = newQty;
    resetVoucher();
    await showToast("success", "Đã cập nhật số lượng");
  } catch (err: any) {
    await showError("Lỗi", "Không thể cập nhật giỏ hàng");
    await loadCart();
  } finally {
    isUpdating.value = false;
  }
};

// ==========================================
// ĐỔI BIẾN THỂ: UPDATE TRỰC TIẾP CART ITEM HIỆN TẠI
// Không dùng add mới -> delete cũ vì cách đó tạo cartItemId mới,
// backend trả lại giỏ theo id/thời gian sẽ làm sản phẩm nhảy vị trí.
// ==========================================
const updateVariant = async (item: CartItem, newVariantId: number) => {
  if (!item?.cartItemId || !newVariantId) return;
  if (getItemVariantId(item) === newVariantId) return;

  try {
    isUpdating.value = true;

    await api.put("/v1/customer/cart/update", {
      cartItemId: item.cartItemId,
      productVariantId: newVariantId,
      quantity: Number(item.quantity || 1),
      note: item.note || "",
    });

    resetVoucher();
    await showToast("success", "Đã đổi phân loại sản phẩm");

    // Reload lại để lấy đúng giá, tồn kho, NSX/HSD, ảnh... từ backend,
    // nhưng giữ nguyên thứ tự các dòng đang hiển thị trên màn hình.
    await loadCart({ preserveOrder: true });
  } catch (err: any) {
    console.error("Lỗi đổi biến thể:", err);
    await showError(
      "Lỗi",
      err?.response?.data?.message ||
        "Không thể đổi loại sản phẩm. Vui lòng thử lại!",
    );
    await loadCart({ preserveOrder: true });
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
    await showToast("success", "Đã xóa sản phẩm khỏi giỏ");
  } catch (err: any) {
    await showError("Lỗi", "Không thể xóa sản phẩm");
    await loadCart();
  } finally {
    isUpdating.value = false;
  }
};


const goToCheckout = async () => {
  if (cartItems.value.length === 0) return showToast("warning", "Giỏ hàng đang trống");
  const invalidItem = cartItems.value.find((item) => !isItemAvailable(item));
  if (invalidItem) {
    await Swal.fire({ icon: "warning", title: "Cảnh báo", text: getUnavailableReason(invalidItem) });
    return loadCart();
  }
  router.push("/checkout");
};

// Thêm biến phí ship (fix cứng 30k theo đúng DB của m)
const shippingFee = ref(30000);

// Tìm biến finalTotal cũ và chép đè đoạn này vào để nó cộng thêm phí ship
const finalTotal = computed(() => {
  if (cartItems.value.length === 0) return 0;
  // Tổng tiền = (Tạm tính - Giảm giá) + Phí ship
  return Math.max(0, totalAmount.value - discountAmount.value) + shippingFee.value;
});

onMounted(() => loadCart());
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