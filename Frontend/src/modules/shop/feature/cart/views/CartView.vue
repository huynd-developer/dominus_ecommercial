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
      />

      <CartSummary
        v-if="cartItems.length > 0"
        :totalAmount="totalAmount"
        :discountAmount="discountAmount"
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
  sku?: string | null;
  productName?: string | null;
  capacity?: string | null;
  bottleType?: string | null;
  quantity?: number | null;
  price?: number | null;
  stockQuantity?: number | null;
  note?: string | null;
  imageUrl?: string | null;
  thumbnailUrl?: string | null;
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

const toDateOnly = (value?: string | null) => {
  if (!value) return null;
  return String(value).substring(0, 10);
};

const isBeforeToday = (value?: string | null) => {
  const dateOnly = toDateOnly(value);

  if (!dateOnly) return false;

  const date = new Date(`${dateOnly}T00:00:00`);
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  return !Number.isNaN(date.getTime()) && date.getTime() < today.getTime();
};

const isAfterToday = (value?: string | null) => {
  const dateOnly = toDateOnly(value);

  if (!dateOnly) return false;

  const date = new Date(`${dateOnly}T00:00:00`);
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  return !Number.isNaN(date.getTime()) && date.getTime() > today.getTime();
};

const getItemPrice = (item: CartItem) => {
  return Number(item?.price || 0);
};

const getItemQuantity = (item: CartItem) => {
  return Number(item?.quantity || 0);
};

const isItemExpired = (item: CartItem) => {
  return Boolean(item?.expired) || isBeforeToday(item?.expirationDate);
};

const getUnavailableReason = (item: CartItem) => {
  if (!item) {
    return "Sản phẩm không hợp lệ.";
  }

  if (item.unavailableReason) {
    return item.unavailableReason;
  }

  if (item.available === false || item.sellable === false) {
    return "Sản phẩm hiện không khả dụng.";
  }

  if (item.variantStatus != null && Number(item.variantStatus) !== 1) {
    return "Sản phẩm đang ngừng bán.";
  }

  const quantity = getItemQuantity(item);
  const stockQuantity = Number(item?.stockQuantity || 0);

  if (quantity <= 0) {
    return "Số lượng sản phẩm không hợp lệ.";
  }

  if (stockQuantity <= 0) {
    return "Sản phẩm đã hết hàng.";
  }

  if (quantity > stockQuantity) {
    return `Số lượng trong giỏ vượt quá tồn kho. Sản phẩm chỉ còn ${stockQuantity}.`;
  }

  if (isAfterToday(item.manufacturingDate)) {
    return "Sản phẩm chưa tới ngày được bán.";
  }

  if (isItemExpired(item)) {
    return "Sản phẩm đã hết hạn sử dụng.";
  }

  return "Sản phẩm hiện không khả dụng.";
};

const isItemAvailable = (item: CartItem) => {
  if (!item) return false;

  if (item.available === false || item.sellable === false) {
    return false;
  }

  if (item.variantStatus != null && Number(item.variantStatus) !== 1) {
    return false;
  }

  const quantity = getItemQuantity(item);
  const stockQuantity = Number(item?.stockQuantity || 0);

  if (quantity <= 0 || stockQuantity <= 0 || quantity > stockQuantity) {
    return false;
  }

  if (isAfterToday(item.manufacturingDate)) {
    return false;
  }

  if (isItemExpired(item)) {
    return false;
  }

  return true;
};

const totalAmount = computed(() => {
  return cartItems.value.reduce((sum, item) => {
    if (!isItemAvailable(item)) return sum;
    return sum + getItemPrice(item) * getItemQuantity(item);
  }, 0);
});

const finalTotal = computed(() => {
  return Math.max(0, totalAmount.value - discountAmount.value);
});

const canCheckout = computed(() => {
  return (
    cartItems.value.length > 0 &&
    cartItems.value.every((item) => isItemAvailable(item))
  );
});

const showToast = async (
  icon: "success" | "error" | "warning" | "info",
  title: string
) => {
  await Swal.fire({
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

const handleApplyVoucher = (discount: number, code: string) => {
  discountAmount.value = Number(discount || 0);
  appliedVoucherCode.value = code || "";

  if (code) {
    localStorage.setItem("applied_voucher", code);
  } else {
    localStorage.removeItem("applied_voucher");
  }
};

const resetVoucher = () => {
  const hadVoucher = discountAmount.value > 0 || appliedVoucherCode.value;

  discountAmount.value = 0;
  appliedVoucherCode.value = "";
  localStorage.removeItem("applied_voucher");

  if (hadVoucher) {
    showToast(
      "info",
      "Vui lòng áp dụng lại mã giảm giá do giỏ hàng đã thay đổi!"
    );
  }
};

const loadCart = async () => {
  try {
    isLoading.value = true;

    const res = await api.get("/v1/customer/cart/my-cart");

    cartItems.value = Array.isArray(res.data) ? res.data : [];

    if (!canCheckout.value) {
      resetVoucher();
    }
  } catch (err: any) {
    console.error("Lỗi tải giỏ hàng:", err);

    await showError(
      "Không tải được giỏ hàng",
      err?.response?.data?.message ||
        err?.response?.data ||
        "Vui lòng đăng nhập lại hoặc thử lại sau."
    );

    if (err?.response?.status === 401 || err?.response?.status === 403) {
      router.push("/login");
    }
  } finally {
    isLoading.value = false;
  }
};

const updateQty = async (item: CartItem, newQty: number) => {
  if (!item?.cartItemId) return;

  if (newQty < 1) {
    return;
  }

  const stockQuantity = Number(item?.stockQuantity || 0);

  if (stockQuantity <= 0) {
    await showToast("warning", "Sản phẩm đã hết hàng");
    return;
  }

  if (newQty > stockQuantity) {
    await showToast(
      "warning",
      `Sản phẩm chỉ còn ${stockQuantity} trong kho`
    );
    return;
  }

  if (
    item.available === false ||
    item.sellable === false ||
    item.variantStatus !== 1 ||
    isAfterToday(item.manufacturingDate) ||
    isItemExpired(item)
  ) {
    await Swal.fire({
      icon: "warning",
      title: "Không thể cập nhật",
      text: getUnavailableReason(item),
      confirmButtonColor: "#bd9a5f",
    });

    await loadCart();
    return;
  }

  try {
    isUpdating.value = true;

    await api.put(`/v1/customer/cart/update/${item.cartItemId}`, {
      quantity: newQty,
    });

    item.quantity = newQty;
    resetVoucher();

    await showToast("success", "Đã cập nhật số lượng");
  } catch (err: any) {
    console.error("Lỗi cập nhật số lượng:", err);

    await showError(
      "Không thể cập nhật giỏ hàng",
      err?.response?.data?.message ||
        err?.response?.data ||
        "Vui lòng thử lại sau."
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

    cartItems.value = cartItems.value.filter(
      (item) => item.cartItemId !== cartItemId
    );

    resetVoucher();

    await showToast("success", "Đã xóa sản phẩm khỏi giỏ");
  } catch (err: any) {
    console.error("Không thể xóa sản phẩm:", err);

    await showError(
      "Không thể xóa sản phẩm",
      err?.response?.data?.message ||
        err?.response?.data ||
        "Vui lòng thử lại sau."
    );

    await loadCart();
  } finally {
    isUpdating.value = false;
  }
};

const goToCheckout = async () => {
  if (cartItems.value.length === 0) {
    await showToast("warning", "Giỏ hàng đang trống");
    return;
  }

  const invalidItem = cartItems.value.find((item) => !isItemAvailable(item));

  if (invalidItem) {
    await Swal.fire({
      icon: "warning",
      title: "Giỏ hàng chưa hợp lệ",
      text: getUnavailableReason(invalidItem),
      confirmButtonText: "Đã hiểu",
      confirmButtonColor: "#bd9a5f",
    });

    await loadCart();
    return;
  }

  router.push("/checkout");
};

onMounted(() => {
  loadCart();
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