<template>
  <div class="page-wrapper">
    <ShopHeader />

    <main class="main-content full-width">
      <CheckoutForm :form="orderForm" />

      <CheckoutSummary
        :cartItems="cartItems"
        :totalItems="totalItems"
        :totalAmount="totalAmount"
        :discountAmount="discountAmount"
        :finalTotal="finalTotal"
        :isSubmitting="isSubmitting"
        @submit-order="handlePlaceOrder"
        @back="goToCart"
      />
    </main>

    <ShopFooter />

    <Teleport to="body">
      <Transition name="fade-modal">
        <div v-if="showSuccessModal" class="premium-modal-overlay">
          <OrderResultCard
            mode="success"
            title="Đặt hàng thành công!"
            message="Cảm ơn bạn đã mua sắm tại Dominus. Đơn hàng của bạn đang chờ cửa hàng xác nhận."
            :details="successDetails"
            primary-text="Tiếp tục mua sắm"
            secondary-text="Xem lịch sử đơn hàng"
            primary-icon="bi bi-arrow-right ms-2"
            @primary="goToHome"
            @secondary="goToOrders"
          />
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import Swal from "sweetalert2";
import api from "@/common/api";

import ShopHeader from "@/modules/shop/layout/ShopHeader.vue";
import ShopFooter from "@/modules/shop/layout/ShopFooter.vue";
import CheckoutForm from "../components/CheckoutForm.vue";
import CheckoutSummary from "../components/CheckoutSummary.vue";
import OrderResultCard from "../components/OrderResultCard.vue";

const router = useRouter();

const cartItems = ref<any[]>([]);
const isSubmitting = ref(false);
const showSuccessModal = ref(false);

const successDetails = ref<
  {
    label: string;
    value: string | number;
  }[]
>([]);

const orderForm = ref({
  customerName: "",
  customerPhone: "",
  shippingAddress: "",
  note: "",
  paymentMethod: "COD",

  provinceName: "",
  wardName: "",
  specificAddress: "",

  requireVat: false,
  vatTaxCode: "",
  vatEmail: "",
  vatCompanyName: "",
  vatCompanyAddress: "",
});

const getItemPrice = (item: any) => {
  return Number(item?.price ?? item?.finalPrice ?? item?.originalPrice ?? 0);
};

const totalAmount = computed(() => {
  return cartItems.value.reduce((sum, item) => {
    return sum + getItemPrice(item) * Number(item.quantity || 0);
  }, 0);
});

const discountAmount = computed(() => {
  return 0;
});

const finalTotal = computed(() => {
  return totalAmount.value - discountAmount.value;
});

const totalItems = computed(() => {
  return cartItems.value.reduce((sum, item) => {
    return sum + Number(item.quantity || 0);
  }, 0);
});

const normalizeSpaces = (value: string) => {
  return String(value || "").trim().replace(/\s{2,}/g, " ");
};

const showWarning = async (title: string, text: string) => {
  await Swal.fire({
    icon: "warning",
    title,
    text,
    confirmButtonText: "Đã hiểu",
    confirmButtonColor: "#bd9a5f",
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

const validateCheckoutForm = async () => {
  const customerName = normalizeSpaces(orderForm.value.customerName);
  const customerPhone = String(orderForm.value.customerPhone || "").replace(/[^\d]/g, "");
  const shippingAddress = normalizeSpaces(orderForm.value.shippingAddress);
  const note = normalizeSpaces(orderForm.value.note);
  const paymentMethod = String(orderForm.value.paymentMethod || "").toUpperCase();

  const provinceName = normalizeSpaces(orderForm.value.provinceName || "");
  const wardName = normalizeSpaces(orderForm.value.wardName || "");
  const specificAddress = normalizeSpaces(orderForm.value.specificAddress || "");

  if (cartItems.value.length === 0) {
    await showWarning(
      "Giỏ hàng trống",
      "Vui lòng thêm sản phẩm vào giỏ hàng trước khi đặt hàng."
    );
    router.push("/products");
    return null;
  }

  if (customerName.length < 2 || customerName.length > 100) {
    await showWarning(
      "Tên người nhận không hợp lệ",
      "Tên người nhận phải từ 2 đến 100 ký tự."
    );
    return null;
  }

  if (!/^[\p{L}\s]+$/u.test(customerName)) {
    await showWarning(
      "Tên người nhận không hợp lệ",
      "Tên người nhận chỉ được nhập chữ và khoảng trắng."
    );
    return null;
  }

  if (!/^0\d{9}$/.test(customerPhone)) {
    await showWarning(
      "Số điện thoại không hợp lệ",
      "Số điện thoại phải gồm 10 số và bắt đầu bằng 0."
    );
    return null;
  }

  if (!provinceName) {
    await showWarning("Thiếu tỉnh/thành phố", "Vui lòng chọn tỉnh/thành phố nhận hàng.");
    return null;
  }

  if (!wardName) {
    await showWarning("Thiếu phường/xã", "Vui lòng chọn phường/xã/đặc khu nhận hàng.");
    return null;
  }

  if (specificAddress.length < 3 || specificAddress.length > 255) {
    await showWarning(
      "Địa chỉ cụ thể không hợp lệ",
      "Vui lòng nhập số nhà, ngõ, đường hoặc tòa nhà từ 3 đến 255 ký tự."
    );
    return null;
  }

  if (shippingAddress.length < 5 || shippingAddress.length > 500) {
    await showWarning(
      "Địa chỉ không hợp lệ",
      "Địa chỉ giao hàng phải từ 5 đến 500 ký tự."
    );
    return null;
  }

  if (!/^[\p{L}\d\s,./#()\-]+$/u.test(shippingAddress)) {
    await showWarning(
      "Địa chỉ không hợp lệ",
      "Địa chỉ chỉ nên chứa chữ, số, khoảng trắng và các ký tự , . / # ( ) -"
    );
    return null;
  }

  if (note && note.length > 255) {
    await showWarning("Ghi chú quá dài", "Ghi chú tối đa 255 ký tự.");
    return null;
  }

  if (!["COD", "VNPAY"].includes(paymentMethod)) {
    await showWarning(
      "Phương thức thanh toán không hợp lệ",
      "Chỉ hỗ trợ COD hoặc VNPAY."
    );
    return null;
  }

  orderForm.value.customerName = customerName;
  orderForm.value.customerPhone = customerPhone;
  orderForm.value.shippingAddress = shippingAddress;
  orderForm.value.note = note;
  orderForm.value.paymentMethod = paymentMethod;
  orderForm.value.provinceName = provinceName;
  orderForm.value.wardName = wardName;
  orderForm.value.specificAddress = specificAddress;

  return {
    customerName,
    customerPhone,
    shippingAddress,
    note: note || null,
    paymentMethod,
  };
};

const loadCartSummary = async () => {
  try {
    const res = await api.get("/v1/customer/cart/my-cart");

    cartItems.value = Array.isArray(res.data) ? res.data : [];

    if (cartItems.value.length === 0) {
      await Swal.fire({
        icon: "info",
        title: "Giỏ hàng trống",
        text: "Bạn chưa có sản phẩm nào trong giỏ hàng.",
        confirmButtonText: "Tiếp tục mua sắm",
        confirmButtonColor: "#bd9a5f",
      });

      router.replace("/products");
    }
  } catch (error: any) {
    console.error("Lỗi tải giỏ hàng:", error);

    await showError(
      "Không tải được giỏ hàng",
      error?.response?.data?.message ||
        error?.response?.data ||
        "Vui lòng đăng nhập lại hoặc thử lại sau."
    );

    router.replace("/cart");
  }
};

const handlePlaceOrder = async () => {
  const submitData = await validateCheckoutForm();

  if (!submitData) return;

  const confirmResult = await Swal.fire({
    icon: "question",
    title: "Xác nhận đặt hàng?",
    html: `
      <div style="text-align:left">
        <p><b>Người nhận:</b> ${submitData.customerName}</p>
        <p><b>Số điện thoại:</b> ${submitData.customerPhone}</p>
        <p><b>Thanh toán:</b> ${submitData.paymentMethod}</p>
        <p><b>Tổng thanh toán:</b> ${formatCurrency(finalTotal.value)}</p>
      </div>
    `,
    showCancelButton: true,
    confirmButtonText: "Đặt hàng",
    cancelButtonText: "Kiểm tra lại",
    confirmButtonColor: "#bd9a5f",
    cancelButtonColor: "#6b7280",
  });

  if (!confirmResult.isConfirmed) return;

  isSubmitting.value = true;

  try {
    const res = await api.post("/v1/orders/checkout", submitData);

    if (submitData.paymentMethod === "VNPAY" && res.data?.paymentUrl) {
      window.location.href = res.data.paymentUrl;
      return;
    }

    successDetails.value = [
      {
        label: "Mã đơn hàng",
        value: res.data?.orderId ? `#${res.data.orderId}` : "-",
      },
      {
        label: "Phương thức",
        value: "COD",
      },
      {
        label: "Tổng thanh toán",
        value: formatCurrency(Number(res.data?.finalAmount ?? finalTotal.value)),
      },
    ];

    showSuccessModal.value = true;
  } catch (error: any) {
    console.error("Lỗi đặt hàng:", error);

    await showError(
      "Không thể đặt hàng",
      error?.response?.data?.message ||
        error?.response?.data ||
        "Có lỗi xảy ra khi đặt hàng. Vui lòng thử lại."
    );
  } finally {
    isSubmitting.value = false;
  }
};

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(Number(value || 0));
};

const goToCart = () => {
  router.push("/cart");
};

const goToHome = () => {
  showSuccessModal.value = false;
  router.push("/");
};

const goToOrders = () => {
  showSuccessModal.value = false;
  router.push({
    path: "/customer/profile",
    query: {
      tab: "orders",
    },
  });
};

onMounted(() => {
  loadCartSummary();
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

.premium-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 24px;
}

.fade-modal-enter-active,
.fade-modal-leave-active {
  transition: all 0.3s ease;
}

.fade-modal-enter-from,
.fade-modal-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

@media (max-width: 992px) {
  .main-content.full-width {
    flex-direction: column;
  }
}
</style>