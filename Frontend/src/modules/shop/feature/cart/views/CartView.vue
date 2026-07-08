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

const router = useRouter();

const cartItems = ref<any[]>([]);
const isLoading = ref(true);
const isUpdating = ref(false);

const getItemPrice = (item: any) => {
  return Number(item?.price || 0);
};

const getItemQuantity = (item: any) => {
  return Number(item?.quantity || 0);
};

const isItemAvailable = (item: any) => {
  if (item?.available === false) return false;

  const quantity = getItemQuantity(item);
  const stockQuantity = Number(item?.stockQuantity || 0);

  return quantity > 0 && stockQuantity > 0 && quantity <= stockQuantity;
};

const totalAmount = computed(() => {
  return cartItems.value.reduce((sum, item) => {
    if (!isItemAvailable(item)) return sum;
    return sum + getItemPrice(item) * getItemQuantity(item);
  }, 0);
});

const discountAmount = computed(() => {
  return 0;
});

const finalTotal = computed(() => {
  return totalAmount.value - discountAmount.value;
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

const loadCart = async () => {
  try {
    isLoading.value = true;

    const res = await api.get("/v1/customer/cart/my-cart");

    cartItems.value = Array.isArray(res.data) ? res.data : [];
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

const updateQty = async (item: any, newQty: number) => {
  if (!item?.cartItemId) return;

  if (newQty < 1) {
    return;
  }

  const stockQuantity = Number(item?.stockQuantity || 0);

  if (newQty > stockQuantity) {
    await showToast(
      "warning",
      `Sản phẩm chỉ còn ${stockQuantity} trong kho`
    );
    return;
  }

  try {
    isUpdating.value = true;

    await api.put(`/v1/customer/cart/update/${item.cartItemId}`, {
      quantity: newQty,
    });

    item.quantity = newQty;

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

  const confirmResult = await Swal.fire({
    icon: "question",
    title: "Xóa sản phẩm?",
    text: "Sản phẩm này sẽ được xóa khỏi giỏ hàng của bạn.",
    showCancelButton: true,
    confirmButtonText: "Xóa",
    cancelButtonText: "Hủy",
    confirmButtonColor: "#dc2626",
    cancelButtonColor: "#6b7280",
  });

  if (!confirmResult.isConfirmed) return;

  try {
    isUpdating.value = true;

    await api.delete(`/v1/customer/cart/remove/${cartItemId}`);

    cartItems.value = cartItems.value.filter(
      (item) => item.cartItemId !== cartItemId
    );

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
      text:
        invalidItem.unavailableReason ||
        "Có sản phẩm không khả dụng hoặc vượt tồn kho. Vui lòng kiểm tra lại.",
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