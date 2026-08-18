<template>
  <div class="page-wrapper">
    <ShopHeader />

    <main class="main-content full-width">
      <div v-if="isPageLoading" class="checkout-loading">
        <div class="spinner-border"></div>
        <p>Đang tải thông tin thanh toán...</p>
      </div>

      <template v-else>
        <CheckoutForm :key="formKey" :form="orderForm" />

        <CheckoutSummary
          :cartItems="cartItems"
          :totalItems="totalItems"
          :totalAmount="totalAmount"
          :discountAmount="discountAmount"
          :shippingFee="shippingFee"
          :isCalculatingShip="isCalculatingShip"
          :finalTotal="finalTotal"
          :isSubmitting="isSubmitting"
          :updatingItemKey="updatingItemKey"
          :selectedVoucherCode="appliedVoucherCode"
          @update-quantity="handleUpdateQuantity"
          @submit-order="handlePlaceOrder"
          @back="goToCart"
          @apply-voucher="handleApplyVoucher"
          @cancel-voucher="handleCancelVoucher"
        />
      </template>
    </main>

    <ShopFooter />

    <Teleport to="body">
      <Transition name="fade-modal">
        <div
          v-if="showPaymentModal"
          class="premium-modal-overlay"
          @click.self="handleCancelPayment"
        >
          <div
            class="payment-box bg-white p-4 rounded-4 shadow-lg text-center d-flex flex-column align-items-center mx-3 position-relative"
            style="max-width: 420px; animation: slideUp 0.3s ease-out"
          >
            <button
              type="button"
              class="btn-close position-absolute top-0 end-0 m-3"
              aria-label="Close"
              @click="handleCancelPayment"
            ></button>

            <div class="mb-2 mt-2">
              <i
                v-if="currentPaymentMethod === 'VIETQR'"
                class="bi bi-qr-code-scan text-primary"
                style="font-size: 2.5rem"
              ></i>
              <i
                v-else-if="currentPaymentMethod === 'VNPAY'"
                class="bi bi-credit-card-2-front text-info"
                style="font-size: 2.5rem"
              ></i>
            </div>

            <h4 class="mb-2 fw-bold" style="color: #06132b">
              Thanh toán đơn hàng
            </h4>

            <div
              class="timer-box d-flex align-items-center justify-content-center gap-2 mb-2 p-2 rounded-3"
              style="
                background-color: #fef2f2;
                color: #dc2626;
                border: 1px solid #fecaca;
                width: 100%;
              "
            >
              <i class="bi bi-clock-history fs-5"></i>
              <span class="fs-5 fw-bold">{{ formattedCountdown }}</span>
            </div>

            <template v-if="currentPaymentMethod === 'VIETQR'">
              <p class="text-muted small mb-3">
                Vui lòng mở ứng dụng ngân hàng và quét mã QR bên dưới để hoàn tất.
              </p>
              <img
                :src="qrCodeUrl"
                alt="Mã VietQR"
                class="img-fluid rounded mb-3"
                style="border: 2px dashed #bd9a5f; padding: 8px"
              />
            </template>

            <template v-if="currentPaymentMethod === 'VNPAY'">
              <p class="text-muted small mb-3">
                Bạn đã chọn thanh toán an toàn qua cổng VNPay. Vui lòng bấm nút bên dưới để chuyển hướng.
              </p>
            </template>

            <div class="d-flex gap-2 w-100 mt-2 flex-wrap">
              <button
                @click="handleCancelPayment"
                class="btn btn-outline-danger flex-grow-1 py-3 fw-bold rounded-3"
                style="font-size: 0.9rem"
              >
                Hủy thanh toán
              </button>

              <button
                v-if="currentPaymentMethod === 'VIETQR'"
                @click="confirmQrPayment"
                class="btn btn-success flex-grow-1 py-3 fw-bold rounded-3"
                style="background-color: #10b981; border: none; font-size: 0.9rem;"
              >
                Đã chuyển khoản <i class="bi bi-check-circle ms-1"></i>
              </button>

              <button
                v-if="currentPaymentMethod === 'VNPAY'"
                @click="goToVnpayGateway"
                class="btn btn-primary flex-grow-1 py-3 fw-bold rounded-3"
                style="background-color: #0284c7; border: none; font-size: 0.9rem;"
              >
                Thanh toán ngay <i class="bi bi-box-arrow-up-right ms-1"></i>
              </button>
            </div>
          </div>
        </div>
      </Transition>

      <Transition name="fade-modal">
        <div v-if="showSuccessModal" class="premium-modal-overlay">
          <OrderResultCard
            mode="success"
            title="Đặt hàng thành công!"
            :message="successMessage"
            :status-text="successStatusText"
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
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import Swal from "sweetalert2";
import api from "@/common/api";

import ShopHeader from "@/modules/shop/layout/ShopHeader.vue";
import ShopFooter from "@/modules/shop/layout/ShopFooter.vue";
import CheckoutForm from "../components/CheckoutForm.vue";
import CheckoutSummary from "../components/CheckoutSummary.vue";
import OrderResultCard from "../components/OrderResultCard.vue";

interface ResultDetail {
  label: string;
  value: string | number;
  money?: boolean;
}

const router = useRouter();

const cartItems = ref<any[]>([]);
const cartSnapshot = ref<any[]>([]);
const isSubmitting = ref(false);
const isPageLoading = ref(true);
const updatingItemKey = ref<string | number | null>(null);

const formKey = ref(0);

const showPaymentModal = ref(false);
const showSuccessModal = ref(false);
const currentPaymentMethod = ref("");
const qrCodeUrl = ref("");
const vnpayUrl = ref("");
const createdOrderId = ref<number | null>(null);

const shippingFee = ref(30000);
const isCalculatingShip = ref(false);

const paymentCountdown = ref(900);
let paymentTimer: ReturnType<typeof setInterval> | null = null;

const formattedCountdown = computed(() => {
  const m = Math.floor(paymentCountdown.value / 60).toString().padStart(2, "0");
  const s = (paymentCountdown.value % 60).toString().padStart(2, "0");
  return `${m}:${s}`;
});

const startPaymentTimer = () => {
  paymentCountdown.value = 900;
  if (paymentTimer) clearInterval(paymentTimer);
  paymentTimer = setInterval(() => {
    paymentCountdown.value--;
    if (paymentCountdown.value <= 0) {
      if (paymentTimer) clearInterval(paymentTimer);
      handleTimeoutPayment();
    }
  }, 1000);
};

const stopPaymentTimer = () => {
  if (paymentTimer) clearInterval(paymentTimer);
};

const successStatusText = ref("");
const successMessage = ref("Cảm ơn bạn đã mua sắm tại Dominus. Đơn hàng của bạn đang chờ cửa hàng xác nhận.");
const successDetails = ref<ResultDetail[]>([]);
const discountAmount = ref(0);
const appliedVoucherCode = ref("");

const orderForm = ref({
  customerId: null as number | null,
  customerName: "",
  customerPhone: "",
  shippingAddress: "",
  note: "",
  paymentMethod: "COD",
  provinceName: "",
  wardName: "",
  specificAddress: "",
  profileLoaded: false,
});

watch(
  () => orderForm.value,
  (newVal) => {
    const { 
      profileLoaded, 
      shippingAddress, 
      provinceName, 
      wardName, 
      specificAddress, 
      ...draftData 
    } = newVal;
    sessionStorage.setItem("dominus_checkout_draft", JSON.stringify(draftData));
  },
  { deep: true }
);

const getCartItemKey = (item: any) => item?.cartItemId || item?.id || item?.productVariantId || item?.variantId || item?.sku;
const getProductVariantId = (item: any) => Number(item?.productVariantId || item?.variantId || item?.productVariant?.id || item?.id || 0);
const getCartItemId = (item: any) => Number(item?.cartItemId || item?.id || 0);
const getItemPrice = (item: any) => Number(item?.price ?? item?.salePrice ?? item?.finalPrice ?? item?.originalPrice ?? 0);

/**
 * Tồn có thể bán thật mà Checkout FE sử dụng.
 *
 * Chỉ lấy sellableQuantity đã được BE Product map từ InventoryLot.
 * Không dùng ProductVariant.stockQuantity / NSX / HSD legacy để quyết định bán.
 */
const getItemSellableQuantity = (item: any) => {
  const quantity = Number(
    item?.sellableQuantity ??
      item?.productVariant?.sellableQuantity ??
      0
  );

  if (!Number.isFinite(quantity) || quantity <= 0) {
    return 0;
  }

  return Math.trunc(quantity);
};

const getItemDisplayName = (item: any) =>
  String(item?.productName || item?.sku || "Sản phẩm");

const totalAmount = computed(() =>
  cartItems.value.reduce((sum, item) => sum + getItemPrice(item) * Number(item.quantity || 0), 0)
);

// ĐÃ SỬA: Đảm bảo số tiền tối thiểu bằng tiền ship
const finalTotal = computed(() =>
  Math.max(0, Number(totalAmount.value || 0) - Number(discountAmount.value || 0)) + Number(shippingFee.value || 0)
);

const totalItems = computed(() =>
  cartItems.value.reduce((sum, item) => sum + Number(item.quantity || 0), 0)
);

const updateCartQuantityApi = async (item: any, quantity: number) => {
  await api.put("/v1/customer/cart/update", {
    cartItemId: getCartItemId(item),
    productVariantId: getProductVariantId(item),
    quantity,
  });
};

const collapseSpacesForProfile = (value: string) => String(value || "").trim().replace(/\s{2,}/g, " ");
const extractObjectData = (data: any) => data?.data || data?.result || data || {};

const getErrorMessage = (error: any, fallback: string) => {
  const data = error?.response?.data;
  if (typeof data === "string") return data;
  if (data?.message) return data.message;
  return error?.message || fallback;
};

const showWarning = async (title: string, text: string) => {
  await Swal.fire({ icon: "warning", title, text, confirmButtonText: "Đã hiểu", confirmButtonColor: "#bd9a5f" });
};

const showError = async (title: string, text: string) => {
  await Swal.fire({ icon: "error", title, text, confirmButtonText: "Đóng", confirmButtonColor: "#bd9a5f" });
};

const formatCurrency = (value: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(Number(value || 0));

const formatPaymentMethod = (value: string | null | undefined) => {
  const normalized = String(value || "").toUpperCase();
  if (normalized === "COD") return "Thanh toán khi nhận hàng";
  if (normalized === "VNPAY") return "VNPay";
  if (normalized === "VIETQR") return "Chuyển khoản VietQR";
  return value || "-";
};

const getStatusText = (status: number) => {
  switch (Number(status)) {
    case 0: return "Chờ xác nhận";
    case 1: return "Đã xác nhận";
    case 2: return "Đang giao hàng";
    case 3: return "Hoàn thành";
    case 4: return "Đã hủy";
    default: return "Không xác định";
  }
};

const handleApplyVoucher = (discount: number, code: string) => {
  discountAmount.value = Math.min(Math.max(Number(discount || 0), 0), Number(totalAmount.value || 0));
  appliedVoucherCode.value = code || "";
  if (code) localStorage.setItem("applied_voucher", code);
  else localStorage.removeItem("applied_voucher");
};

const handleCancelVoucher = () => {
  discountAmount.value = 0;
  appliedVoucherCode.value = "";
  localStorage.removeItem("applied_voucher");
};

const loadCustomerProfile = async () => {
  try {
    const res = await api.get(`/customer/profile?t=${Date.now()}`);
    const profile = extractObjectData(res.data);
    
    orderForm.value.customerId = profile.userId || profile.id || null;
    const name = collapseSpacesForProfile(profile.name || profile.fullName || profile.customerName || "");
    const phone = String(profile.phone || profile.customerPhone || "").trim();

    orderForm.value.profileLoaded = true;
    if (!orderForm.value.customerName) orderForm.value.customerName = name;
    if (!orderForm.value.customerPhone) orderForm.value.customerPhone = phone;
  } catch (error: any) {
    await showError("Vui lòng đăng nhập", getErrorMessage(error, "Bạn cần đăng nhập tài khoản khách hàng để thanh toán."));
    router.replace({ name: "Login", query: { redirect: "/checkout" } });
    return false;
  }
  return true;
};

const validateCheckoutForm = async (): Promise<any | null> => {
  const customerName = String(orderForm.value.customerName || "").trim();
  const customerPhone = String(orderForm.value.customerPhone || "").trim();
  const shippingAddress = String(orderForm.value.shippingAddress || "").trim();
  const note = String(orderForm.value.note || "").trim();
  const paymentMethod = String(orderForm.value.paymentMethod || "").toUpperCase();

  if (cartItems.value.length === 0) {
    await showWarning("Giỏ hàng trống", "Vui lòng thêm sản phẩm vào giỏ hàng trước khi đặt hàng.");
    router.push("/products");
    return null;
  }

  /*
   * Validate UX bằng tồn sellable thật đã load từ InventoryLot.
   * BE /v1/orders/checkout vẫn re-check lần cuối nên FE không phải nguồn sự thật.
   */
  for (const item of cartItems.value) {
    const requestedQuantity = Number(item?.quantity || 0);
    const sellableQuantity = getItemSellableQuantity(item);
    const variantStatus = Number(
      item?.variantStatus ??
        item?.productVariant?.status ??
        1
    );

    if (variantStatus !== 1 || item?.sellable === false) {
      await showWarning(
        "Sản phẩm không thể đặt hàng",
        String(
          item?.unavailableReason ||
            `${getItemDisplayName(item)} hiện đang ngừng bán hoặc không còn khả dụng.`
        )
      );
      return null;
    }

    if (
      !Number.isFinite(requestedQuantity) ||
      requestedQuantity <= 0 ||
      requestedQuantity > sellableQuantity
    ) {
      await showWarning(
        "Số lượng không còn đủ",
        `${getItemDisplayName(item)} chỉ còn ${sellableQuantity} sản phẩm có thể bán. Vui lòng cập nhật lại số lượng.`
      );
      return null;
    }
  }

  if (customerName.length < 2) {
    await showWarning("Tên người nhận không hợp lệ", "Tên người nhận phải từ 2 ký tự trở lên.");
    return null;
  }

  if (!/^0\d{9}$/.test(customerPhone)) {
    await showWarning("Số điện thoại không hợp lệ", "Số điện thoại phải gồm đúng 10 chữ số, bắt đầu bằng 0.");
    return null;
  }

  if (shippingAddress.length < 5) {
    await showWarning("Địa chỉ không hợp lệ", "Vui lòng chọn hoặc nhập địa chỉ giao hàng hợp lệ.");
    return null;
  }

  return {
    customerName,
    customerPhone,
    shippingAddress,
    note: note || null,
    paymentMethod,
    shippingFee: shippingFee.value,
    voucherCode: appliedVoucherCode.value || null,
  };
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

const loadCartSummary = async () => {
  try {
    const res = await api.get(`/v1/customer/cart/my-cart?t=${Date.now()}`);
    let items = Array.isArray(res.data) ? res.data : [];

    if (items.length > 0) {
      items = await Promise.all(items.map(async (item: any) => {
        try {
          const productId = Number(item?.productId || item?.ProductId || item?.product?.id || item?.product?.productId || item?.Product?.id || item?.Product?.productId || item?.productVariant?.productId || item?.productVariant?.product?.id || item?.ProductVariant?.ProductId || item?.ProductVariant?.Product?.Id || item?.variant?.productId || item?.variant?.product?.id || 0);
          const variantId = Number(item?.productVariantId || item?.ProductVariantId || item?.variantId || item?.VariantId || item?.productVariant?.id || item?.ProductVariant?.Id || item?.productVariant?.productVariantId || item?.variant?.id || item?.Variant?.Id || 0);
          
          /*
           * Checkout cần Product detail để lấy sellableQuantity mới nhất.
           * Nếu không xác định được Product thì không được fallback sang stock legacy.
           */
          if (!productId) {
            const sellableQuantity = Math.max(
              Number(item?.sellableQuantity ?? 0) || 0,
              0
            );

            return {
              ...item,
              sellableQuantity,
              // compatibility cho component/code cũ trong lúc migrate
              stockQuantity: sellableQuantity,
              sellable:
                item?.sellable ??
                (
                  Number(item?.variantStatus ?? 1) === 1 &&
                  sellableQuantity > 0
                ),
            };
          }

          const productData = await fetchProductDetail(productId);

          if (!productData) {
            return {
              ...item,
              variantStatus: 0,
              sellableQuantity: 0,
              stockQuantity: 0,
              sellable: false,
              unavailableReason: "Không thể xác minh tồn kho hiện tại của sản phẩm.",
            };
          }

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

          let variants = [];

          for (const candidate of candidates) {
            if (Array.isArray(candidate)) {
              variants = candidate;
              break;
            }
          }

          const matchedVariant = variants.find(
            (v: any) =>
              Number(v?.productVariantId || v?.id || v?.Id || 0) === variantId
          );

          if (matchedVariant) {
            const sellableQuantity = Math.max(
              Number(
                matchedVariant?.sellableQuantity ??
                  item?.sellableQuantity ??
                  0
              ) || 0,
              0
            );

            const variantStatus = Number(
              matchedVariant?.status ??
                item?.variantStatus ??
                0
            );

            const sellable =
              matchedVariant?.sellable ??
              (
                variantStatus === 1 &&
                sellableQuantity > 0
              );

            return {
              ...item,

              /*
               * sellableQuantity là field nghiệp vụ thật.
               * stockQuantity chỉ map cùng giá trị để không làm vỡ code compatibility.
               */
              sellableQuantity,
              stockQuantity: sellableQuantity,

              variantStatus,
              sellable: Boolean(sellable),
              unavailableReason:
                matchedVariant?.unavailableReason ??
                item?.unavailableReason ??
                (
                  variantStatus !== 1
                    ? "Sản phẩm đang ngừng bán."
                    : sellableQuantity <= 0
                      ? "Sản phẩm hiện không còn tồn có thể bán."
                      : null
                ),

              product: productData,
              productVariant: matchedVariant,
            };
          }

          return {
            ...item,
            variantStatus: 0,
            sellableQuantity: 0,
            stockQuantity: 0,
            sellable: false,
            unavailableReason: "Không tìm thấy biến thể sản phẩm hiện tại.",
          };
        } catch (e) {
          return {
            ...item,
            sellableQuantity: 0,
            stockQuantity: 0,
            sellable: false,
            unavailableReason: "Không thể xác minh tồn kho hiện tại của sản phẩm.",
          };
        }
      }));
    }

    cartItems.value = items;
  } catch (error: any) {
    console.error(error);
  }
};

const loadSavedVoucher = async () => {
  const savedCode = localStorage.getItem("applied_voucher");
  if (!savedCode || totalAmount.value <= 0) return;

  try {
    const res = await api.get("/v1/customer/vouchers/apply", {
      params: { code: savedCode, orderTotal: totalAmount.value },
    });
    const discount = Number(res.data?.discountAmount ?? res.data?.discount ?? res.data?.amount ?? 0);
    discountAmount.value = Math.min(Math.max(discount, 0), Number(totalAmount.value || 0));
    appliedVoucherCode.value = savedCode;
  } catch (error) {
    handleCancelVoucher();
  }
};

const handleUpdateQuantity = async (item: any, quantity: number) => {
  if (isSubmitting.value || updatingItemKey.value || quantity < 1) return;

  const sellableQuantity = getItemSellableQuantity(item);

  if (quantity > sellableQuantity) {
    await showWarning(
      "Không đủ tồn kho",
      `${getItemDisplayName(item)} chỉ còn ${sellableQuantity} sản phẩm có thể bán.`
    );
    return;
  }

  try {
    updatingItemKey.value = getCartItemKey(item);
    await updateCartQuantityApi(item, quantity);
    window.dispatchEvent(new Event("cart-updated"));
    await loadCartSummary();
    if (appliedVoucherCode.value) await loadSavedVoucher();
  } finally {
    updatingItemKey.value = null;
  }
};

const handlePlaceOrder = async () => {
  const submitData = await validateCheckoutForm();
  if (!submitData || isSubmitting.value) return;
  isSubmitting.value = true;

  try {
    cartSnapshot.value = JSON.parse(JSON.stringify(cartItems.value));

    const orderPayload = { 
      ...submitData,
      shippingFee: shippingFee.value,
      voucherCode: appliedVoucherCode.value || null
    };

    const res = await api.post("/v1/orders/checkout", orderPayload);
    createdOrderId.value = res.data?.orderId || null;

    const finalDiscount = Number(res.data?.discountAmount ?? discountAmount.value);
    successStatusText.value = getStatusText(Number(res.data?.status ?? 0));
    successMessage.value = res.data?.message || "Cảm ơn bạn đã mua sắm tại Dominus.";

    // ĐÃ SỬA: Đảm bảo số tiền tối thiểu bằng tiền ship
    const calculatedFinalTotal = Math.max(0, Number(res.data?.totalAmount ?? totalAmount.value) - finalDiscount) + Number(shippingFee.value);
    
    successDetails.value = [
      { label: "Mã đơn hàng", value: res.data?.orderId ? `#${res.data.orderId}` : "-" },
      { label: "Trạng thái", value: successStatusText.value },
      { label: "Phương thức", value: formatPaymentMethod(res.data?.paymentMethod || orderPayload.paymentMethod) },
      ...(finalDiscount > 0 ? [{ label: "Mã giảm giá", value: res.data?.voucherCode || orderPayload.voucherCode || "Không có" }] : []),
      { label: "Tạm tính", value: formatCurrency(Number(res.data?.totalAmount ?? totalAmount.value)), money: true },
      ...(finalDiscount > 0 ? [{ label: "Giảm giá", value: `-${formatCurrency(finalDiscount)}`, money: true }] : []),
      { label: "Phí vận chuyển", value: formatCurrency(Number(shippingFee.value)), money: true },
      { label: "Tổng thanh toán", value: formatCurrency(calculatedFinalTotal), money: true },
    ];

    if (orderPayload.paymentMethod === "VIETQR" || orderPayload.paymentMethod === "VNPAY") {
      currentPaymentMethod.value = orderPayload.paymentMethod;

      if (orderPayload.paymentMethod === "VIETQR") {
        const orderId = res.data?.orderId || Math.floor(Math.random() * 100000);
        const amount = finalTotal.value;
        qrCodeUrl.value = res.data?.qrUrl || `https://img.vietqr.io/image/TCB-3714082007-compact2.png?amount=${amount}&addInfo=DH${orderId}&accountName=NGUYEN%20DINH%20HUY`;
      } else {
        const url = res.data?.paymentUrl || res.data?.vnpUrl || res.data?.url;
        if (!url) throw new Error("Không lấy được link thanh toán VNPay");
        vnpayUrl.value = url;
      }

      showPaymentModal.value = true;
      startPaymentTimer();

      window.history.pushState({ paymentOpen: true }, "", window.location.href);
      window.addEventListener("popstate", handleBrowserBackDuringPayment);
    } else {
      discountAmount.value = 0;
      appliedVoucherCode.value = "";
      localStorage.removeItem("applied_voucher");
      sessionStorage.removeItem("dominus_checkout_draft");

      showSuccessModal.value = true;
      window.dispatchEvent(new Event("cart-updated"));
    }
  } catch (error: any) {
    await showError("Lỗi đặt hàng", getErrorMessage(error, "Có lỗi xảy ra khi đặt hàng."));
  } finally {
    isSubmitting.value = false;
  }
};

const cancelAndRestoreCart = async () => {
  stopPaymentTimer();
  try {
    isPageLoading.value = true;
    if (createdOrderId.value) {
      await api.patch(`/customer/orders/${createdOrderId.value}/cancel`, { cancelReason: "Khác" }).catch(() => {});
    }

    if (cartSnapshot.value && cartSnapshot.value.length > 0) {
      const addPromises = cartSnapshot.value.map((item: any) => {
        const variantId = item.productVariantId || item.variantId || item.id;
        return api.post("/v1/customer/cart/add", {
          productVariantId: Number(variantId),
          quantity: Number(item.quantity || 1),
        });
      });
      await Promise.all(addPromises);
    }

    await loadCustomerProfile();
    await loadCartSummary();
    window.dispatchEvent(new Event("cart-updated"));
    await loadSavedVoucher();
    formKey.value++;

    Swal.fire({ toast: true, position: "top-end", icon: "info", title: "Chưa thanh toán", text: "Bạn vừa rời khỏi quá trình thanh toán.", showConfirmButton: false, timer: 3500 });
  } catch (error) {
    console.error(error);
  } finally {
    isPageLoading.value = false;
  }
};

const handleCancelPayment = async () => {
  if (!showPaymentModal.value) return;
  showPaymentModal.value = false;
  window.removeEventListener("popstate", handleBrowserBackDuringPayment);
  await cancelAndRestoreCart();
};

const handleTimeoutPayment = async () => {
  if (!showPaymentModal.value) return;
  showPaymentModal.value = false;
  window.removeEventListener("popstate", handleBrowserBackDuringPayment);
  await cancelAndRestoreCart();
};

const handleBrowserBackDuringPayment = async () => {
  if (showPaymentModal.value) {
    showPaymentModal.value = false;
    window.removeEventListener("popstate", handleBrowserBackDuringPayment);
    await cancelAndRestoreCart();
  }
};

const goToVnpayGateway = () => {
  stopPaymentTimer();
  sessionStorage.setItem("pending_vnpay_cart", JSON.stringify(cartSnapshot.value));
  sessionStorage.setItem("pending_vnpay_order", String(createdOrderId.value));
  sessionStorage.setItem("pending_vnpay_form", JSON.stringify(orderForm.value));
  if (appliedVoucherCode.value) sessionStorage.setItem("pending_vnpay_voucher", appliedVoucherCode.value);
  window.location.href = vnpayUrl.value;
};

const confirmQrPayment = async () => {
  stopPaymentTimer();
  showPaymentModal.value = false;
  window.removeEventListener("popstate", handleBrowserBackDuringPayment);

  discountAmount.value = 0;
  appliedVoucherCode.value = "";
  localStorage.removeItem("applied_voucher");
  sessionStorage.removeItem("dominus_checkout_draft");

  if (createdOrderId.value) {
    try {
      const saved = localStorage.getItem("dominus_paid_orders");
      const map = saved ? JSON.parse(saved) : {};
      map[String(createdOrderId.value)] = true;
      localStorage.setItem("dominus_paid_orders", JSON.stringify(map));
      api.post(`/v1/orders/${createdOrderId.value}/report-payment`).catch(() => {});
    } catch (e) {}
  }

  setTimeout(() => { showSuccessModal.value = true; }, 200);
};

const goToCart = () => router.push("/cart");
const goToHome = () => { showSuccessModal.value = false; router.push("/"); };
const goToOrders = () => { showSuccessModal.value = false; router.push({ path: "/customer/profile", query: { tab: "orders" } }); };

const checkAndRestoreVnpayBackup = async () => {
  const pendingOrder = sessionStorage.getItem("pending_vnpay_order");
  const pendingCart = sessionStorage.getItem("pending_vnpay_cart");

  if (pendingOrder && pendingCart) {
    isPageLoading.value = true;
    showPaymentModal.value = false;
    sessionStorage.removeItem("pending_vnpay_order");
    sessionStorage.removeItem("pending_vnpay_cart");

    const pendingForm = sessionStorage.getItem("pending_vnpay_form");
    if (pendingForm) {
      Object.assign(orderForm.value, JSON.parse(pendingForm));
      sessionStorage.removeItem("pending_vnpay_form");
      formKey.value++;
    }

    const pendingVoucher = sessionStorage.getItem("pending_vnpay_voucher");
    if (pendingVoucher) {
      localStorage.setItem("applied_voucher", pendingVoucher);
      appliedVoucherCode.value = pendingVoucher;
      sessionStorage.removeItem("pending_vnpay_voucher");
    }

    try {
      const items = JSON.parse(pendingCart);
      cartItems.value = items;
      await api.patch(`/customer/orders/${createdOrderId.value || pendingOrder}/cancel`, { cancelReason: "Khác" }).catch(() => {});

      if (items && items.length > 0) {
        const addPromises = items.map((item: any) => {
          const variantId = item.productVariantId || item.variantId || item.id;
          return api.post("/v1/customer/cart/add", { productVariantId: Number(variantId), quantity: Number(item.quantity || 1) });
        });
        await Promise.all(addPromises);
      }

      window.dispatchEvent(new Event("cart-updated"));
      await loadCartSummary();
      if (appliedVoucherCode.value) await loadSavedVoucher();
    } catch (e) {
    } finally {
      isPageLoading.value = false;
    }
  }
};

window.addEventListener("pageshow", async (event) => {
  if (event.persisted || sessionStorage.getItem("pending_vnpay_order")) {
    await checkAndRestoreVnpayBackup();
  }
});

// BẮT SỰ KIỆN CLICK CHUỘT VÀO CỬA SỔ ĐỂ TỰ ĐỘNG LOAD LẠI FLASH SALE & VOUCHER
const handleFocus = async () => {
  if (!showPaymentModal.value && !showSuccessModal.value) {
    await loadCartSummary();
    if (localStorage.getItem("applied_voucher") || appliedVoucherCode.value) {
      await loadSavedVoucher();
    }
  }
};

onMounted(async () => {
  try {
    isPageLoading.value = true;
    const draft = sessionStorage.getItem("dominus_checkout_draft");
    if (draft) {
      try {
        const parsedDraft = JSON.parse(draft);
        delete parsedDraft.shippingAddress;
        delete parsedDraft.provinceName;
        delete parsedDraft.wardName;
        delete parsedDraft.specificAddress;
        Object.assign(orderForm.value, parsedDraft);
      } catch (e) {}
    }

    await checkAndRestoreVnpayBackup();
    if (await loadCustomerProfile()) {
      await loadCartSummary();
      await loadSavedVoucher();
      formKey.value++;
    }
  } finally {
    isPageLoading.value = false;
  }

  window.addEventListener("focus", handleFocus);
});

onUnmounted(() => {
  window.removeEventListener("focus", handleFocus);
});
</script>

<style scoped>
.page-wrapper { background-color: #fafbfc; min-height: 100vh; padding-bottom: 50px; color: #06132b; }
.main-content.full-width { max-width: 1400px; width: 100%; margin: 40px auto; padding: 0 20px; display: flex; gap: 30px; align-items: flex-start; }
.checkout-loading { width: 100%; min-height: 380px; background: #ffffff; border: 1px solid #eaeaea; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: #64748b; flex-direction: column; gap: 12px; }
.premium-modal-overlay { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.75); backdrop-filter: blur(5px); display: flex; align-items: center; justify-content: center; z-index: 9999; padding: 24px; }
@keyframes slideUp { from { opacity: 0; transform: translateY(30px); } to { opacity: 1; transform: translateY(0); } }
.fade-modal-enter-active, .fade-modal-leave-active { transition: all 0.3s ease; }
.fade-modal-enter-from, .fade-modal-leave-to { opacity: 0; transform: scale(0.9); }
@media (max-width: 992px) { .main-content.full-width { flex-direction: column; } }
</style>