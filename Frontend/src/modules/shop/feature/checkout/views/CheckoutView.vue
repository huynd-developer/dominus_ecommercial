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

        <CheckoutSummary :cartItems="cartItems" :totalItems="totalItems" :totalAmount="totalAmount"
          :discountAmount="discountAmount" :shippingFee="shippingFee" :isCalculatingShip="isCalculatingShip"
          :finalTotal="finalTotal" :isSubmitting="isSubmitting" :updatingItemKey="updatingItemKey"
          :selectedVoucherCode="appliedVoucherCode" :promotionRefreshing="isPromotionRefreshing"
          @update-quantity="handleUpdateQuantity" @submit-order="handlePlaceOrder" @back="goToCart"
          @apply-voucher="handleApplyVoucher" @cancel-voucher="handleCancelVoucher" />
      </template>
    </main>

    <ShopFooter />

    <Teleport to="body">
      <Transition name="fade-modal">
        <div v-if="showPaymentModal" class="premium-modal-overlay" @click.self="handleCancelPayment">
          <div
            class="payment-box bg-white p-4 rounded-4 shadow-lg text-center d-flex flex-column align-items-center mx-3 position-relative"
            style="max-width: 420px; animation: slideUp 0.3s ease-out">
            <button type="button" class="btn-close position-absolute top-0 end-0 m-3" aria-label="Close"
              @click="handleCancelPayment"></button>

            <div class="mb-2 mt-2">
              <i v-if="currentPaymentMethod === 'VIETQR'" class="bi bi-qr-code-scan text-primary"
                style="font-size: 2.5rem"></i>
              <i v-else-if="currentPaymentMethod === 'VNPAY'" class="bi bi-credit-card-2-front text-info"
                style="font-size: 2.5rem"></i>
            </div>

            <h4 class="mb-2 fw-bold" style="color: #06132b">
              Thanh toán đơn hàng
            </h4>


            <template v-if="currentPaymentMethod === 'VIETQR'">
              <p class="text-muted small mb-3">
                Vui lòng mở ứng dụng ngân hàng và quét mã QR bên dưới để hoàn
                tất.
              </p>
              <img :src="qrCodeUrl" alt="Mã VietQR" class="img-fluid rounded mb-3"
                style="border: 2px dashed #bd9a5f; padding: 8px" />
            </template>

            <template v-if="currentPaymentMethod === 'VNPAY'">
              <p class="text-muted small mb-3">
                Bạn đã chọn thanh toán an toàn qua cổng VNPay. Vui lòng bấm nút
                bên dưới để chuyển hướng.
              </p>
            </template>

            <div class="d-flex gap-2 w-100 mt-2 flex-wrap">
              <button @click="handleCancelPayment" class="btn btn-outline-danger flex-grow-1 py-3 fw-bold rounded-3"
                style="font-size: 0.9rem">
                Hủy thanh toán
              </button>

              <button v-if="currentPaymentMethod === 'VIETQR'" @click="confirmQrPayment"
                class="btn btn-success flex-grow-1 py-3 fw-bold rounded-3" style="
                  background-color: #10b981;
                  border: none;
                  font-size: 0.9rem;
                ">
                Đã chuyển khoản <i class="bi bi-check-circle ms-1"></i>
              </button>

              <button v-if="currentPaymentMethod === 'VNPAY'" @click="goToVnpayGateway"
                class="btn btn-primary flex-grow-1 py-3 fw-bold rounded-3" style="
                  background-color: #0284c7;
                  border: none;
                  font-size: 0.9rem;
                ">
                Thanh toán ngay <i class="bi bi-box-arrow-up-right ms-1"></i>
              </button>
            </div>
          </div>
        </div>
      </Transition>

      <Transition name="fade-modal">
        <div v-if="showSuccessModal" class="premium-modal-overlay">
          <OrderResultCard mode="success" title="Đặt hàng thành công!" :message="successMessage"
            :status-text="successStatusText" :details="successDetails" primary-text="Tiếp tục mua sắm"
            secondary-text="Xem lịch sử đơn hàng" primary-icon="bi bi-arrow-right ms-2" @primary="goToHome"
            @secondary="goToOrders" />
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";
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
const isPromotionRefreshing = ref(false);

let promotionExpiryTimer: ReturnType<typeof setTimeout> | null = null;
const MAX_PROMOTION_TIMER_DELAY = 2_147_000_000;

const formKey = ref(0);

const showPaymentModal = ref(false);
const showSuccessModal = ref(false);
const currentPaymentMethod = ref("");
const qrCodeUrl = ref("");
const vnpayUrl = ref("");
const createdOrderId = ref<number | null>(null);
const isConfirmingQrPayment = ref(false);

const shippingFee = ref(30000);
const isCalculatingShip = ref(false);

const paymentCountdown = ref(900);
let paymentTimer: ReturnType<typeof setInterval> | null = null;



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
const successMessage = ref(
  "Cảm ơn bạn đã mua sắm tại Dominus. Đơn hàng của bạn đang chờ cửa hàng xác nhận."
);
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

const getCartItemKey = (item: any) =>
  item?.cartItemId ||
  item?.id ||
  item?.productVariantId ||
  item?.variantId ||
  item?.sku;
const getProductVariantId = (item: any) =>
  Number(
    item?.productVariantId ||
    item?.variantId ||
    item?.productVariant?.id ||
    item?.id ||
    0
  );
const getCartItemId = (item: any) => Number(item?.cartItemId || item?.id || 0);
const getItemPrice = (item: any) =>
  Number(
    item?.price ??
    item?.salePrice ??
    item?.finalPrice ??
    item?.originalPrice ??
    0
  );

const getItemSellableQuantity = (item: any) => {
  const quantity = Number(
    item?.sellableQuantity ?? item?.productVariant?.sellableQuantity ?? 0
  );

  if (!Number.isFinite(quantity) || quantity <= 0) {
    return 0;
  }

  return Math.trunc(quantity);
};

const getItemDisplayName = (item: any) =>
  String(item?.productName || item?.sku || "Sản phẩm");

const totalAmount = computed(() =>
  cartItems.value.reduce(
    (sum, item) => sum + getItemPrice(item) * Number(item.quantity || 0),
    0
  )
);

const finalTotal = computed(
  () =>
    Math.max(
      0,
      Number(totalAmount.value || 0) - Number(discountAmount.value || 0)
    ) + Number(shippingFee.value || 0)
);

const totalItems = computed(() =>
  cartItems.value.reduce((sum, item) => sum + Number(item.quantity || 0), 0)
);

const clearPromotionExpiryTimer = () => {
  if (promotionExpiryTimer) {
    clearTimeout(promotionExpiryTimer);
    promotionExpiryTimer = null;
  }
};

const getPromotionBoundaryTimes = (item: any): number[] => {
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

const schedulePromotionExpiryRefresh = () => {
  clearPromotionExpiryTimer();

  const now = Date.now();

  const futurePromotionTimes = cartItems.value
    .flatMap(getPromotionBoundaryTimes)
    .filter((time: number) => time > now);

  if (futurePromotionTimes.length === 0) return;

  const nextPromotionTime = Math.min(...futurePromotionTimes);
  const delay = Math.min(
    Math.max(0, nextPromotionTime - now + 200),
    MAX_PROMOTION_TIMER_DELAY
  );

  promotionExpiryTimer = setTimeout(() => {
    void refreshCheckoutAtPromotionExpiry();
  }, delay);
};

const refreshCheckoutAtPromotionExpiry = async () => {
  if (
    isSubmitting.value ||
    showPaymentModal.value ||
    showSuccessModal.value ||
    isPromotionRefreshing.value
  ) {
    return;
  }

  isPromotionRefreshing.value = true;

  await nextTick();

  try {
    await loadCartSummary();

    if (localStorage.getItem("applied_voucher") || appliedVoucherCode.value) {
      await loadSavedVoucher();
    }
  } finally {
    isPromotionRefreshing.value = false;
    schedulePromotionExpiryRefresh();
  }
};

const updateCartQuantityApi = async (item: any, quantity: number) => {
  await api.put("/v1/customer/cart/update", {
    cartItemId: getCartItemId(item),
    productVariantId: getProductVariantId(item),
    quantity,
  });
};

const collapseSpacesForProfile = (value: string) =>
  String(value || "")
    .trim()
    .replace(/\s{2,}/g, " ");
const extractObjectData = (data: any) =>
  data?.data || data?.result || data || {};

const getErrorMessage = (error: any, fallback: string) => {
  const data = error?.response?.data;
  if (typeof data === "string") return data;
  if (data?.message) return data.message;
  return error?.message || fallback;
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

const formatCurrency = (value: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(
    Number(value || 0)
  );

const formatPaymentMethod = (value: string | null | undefined) => {
  const normalized = String(value || "").toUpperCase();
  if (normalized === "COD") return "Thanh toán khi nhận hàng";
  if (normalized === "VNPAY") return "VNPay";
  if (normalized === "VIETQR") return "Chuyển khoản VietQR";
  return value || "-";
};

const getStatusText = (status: number) => {
  switch (Number(status)) {
    case 0:
      return "Chờ xác nhận";
    case 1:
      return "Đã xác nhận";
    case 2:
      return "Đang giao hàng";
    case 3:
      return "Hoàn thành";
    case 4:
      return "Đã hủy";
    default:
      return "Không xác định";
  }
};

const handleApplyVoucher = (discount: number, code: string) => {
  discountAmount.value = Math.min(
    Math.max(Number(discount || 0), 0),
    Number(totalAmount.value || 0)
  );
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
    const name = collapseSpacesForProfile(
      profile.name || profile.fullName || profile.customerName || ""
    );
    const phone = String(profile.phone || profile.customerPhone || "").trim();

    orderForm.value.profileLoaded = true;
    orderForm.value.profileLoaded = true;
// Luôn lấy thông tin chính chủ từ Profile đè lên dữ liệu nháp
orderForm.value.customerName = name;
orderForm.value.customerPhone = phone;
  } catch (error: any) {
    await showError(
      "Vui lòng đăng nhập",
      getErrorMessage(
        error,
        "Bạn cần đăng nhập tài khoản khách hàng để thanh toán."
      )
    );
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
  const paymentMethod = String(
    orderForm.value.paymentMethod || ""
  ).toUpperCase();

  if (cartItems.value.length === 0) {
    await showWarning(
      "Giỏ hàng trống",
      "Vui lòng thêm sản phẩm vào giỏ hàng trước khi đặt hàng."
    );
    router.push("/products");
    return null;
  }

  for (const item of cartItems.value) {
    const requestedQuantity = Number(item?.quantity || 0);
    const sellableQuantity = getItemSellableQuantity(item);
    const variantStatus = Number(
      item?.variantStatus ?? item?.productVariant?.status ?? 1
    );

    if (variantStatus !== 1 || item?.sellable === false) {
      await showWarning(
        "Sản phẩm không thể đặt hàng",
        String(
          item?.unavailableReason ||
          `${getItemDisplayName(
            item
          )} hiện đang ngừng bán hoặc không còn khả dụng.`
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
        `${getItemDisplayName(
          item
        )} chỉ còn ${sellableQuantity} sản phẩm có thể bán. Vui lòng cập nhật lại số lượng.`
      );
      return null;
    }
  }

  if (customerName.length < 2) {
    await showWarning(
      "Tên người nhận không hợp lệ",
      "Tên người nhận phải từ 2 ký tự trở lên."
    );
    return null;
  }

  if (!/^0\d{9}$/.test(customerPhone)) {
    await showWarning(
      "Số điện thoại không hợp lệ",
      "Số điện thoại phải gồm đúng 10 chữ số, bắt đầu bằng 0."
    );
    return null;
  }

  if (shippingAddress.length < 5) {
    await showWarning(
      "Địa chỉ không hợp lệ",
      "Vui lòng chọn hoặc nhập địa chỉ giao hàng hợp lệ."
    );
    return null;
  }

  return {
    customerName,
    customerPhone,
    shippingAddress,
    note: note || null,
    paymentMethod,
    voucherCode: appliedVoucherCode.value || null,
  };
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
      items = await Promise.all(
        items.map(async (item: any) => {
          try {
            const productId = Number(
              item?.productId ||
              item?.ProductId ||
              item?.product?.id ||
              item?.product?.productId ||
              item?.Product?.id ||
              item?.Product?.productId ||
              item?.productVariant?.productId ||
              item?.productVariant?.product?.id ||
              item?.ProductVariant?.ProductId ||
              item?.ProductVariant?.Product?.Id ||
              item?.variant?.productId ||
              item?.variant?.product?.id ||
              0
            );
            const variantId = Number(
              item?.productVariantId ||
              item?.ProductVariantId ||
              item?.variantId ||
              item?.VariantId ||
              item?.productVariant?.id ||
              item?.ProductVariant?.Id ||
              item?.productVariant?.productVariantId ||
              item?.variant?.id ||
              item?.Variant?.Id ||
              0
            );

            if (!productId) {
              const sellableQuantity = Math.max(
                Number(item?.sellableQuantity ?? 0) || 0,
                0
              );

              return {
                ...item,
                sellableQuantity,
                stockQuantity: sellableQuantity,
                sellable:
                  item?.sellable ??
                  (Number(item?.variantStatus ?? 1) === 1 &&
                    sellableQuantity > 0),
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
                unavailableReason:
                  "Không thể xác minh tồn kho hiện tại của sản phẩm.",
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
                matchedVariant?.status ?? item?.variantStatus ?? 0
              );

              const sellable =
                matchedVariant?.sellable ??
                (variantStatus === 1 && sellableQuantity > 0);

              return {
                ...item,
                sellableQuantity,
                stockQuantity: sellableQuantity,
                variantStatus,
                sellable: Boolean(sellable),
                unavailableReason:
                  matchedVariant?.unavailableReason ??
                  item?.unavailableReason ??
                  (variantStatus !== 1
                    ? "Sản phẩm đang ngừng bán."
                    : sellableQuantity <= 0
                      ? "Sản phẩm hiện không còn tồn có thể bán."
                      : null),

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
              unavailableReason:
                "Không thể xác minh tồn kho hiện tại của sản phẩm.",
            };
          }
        })
      );
    }

    cartItems.value = items;
    schedulePromotionExpiryRefresh();
  } catch (error: any) {
    console.error(error);
  }
};

const isVoucherCurrentlyActive = (voucher: any) => {
  if (!voucher || voucher?.isDeleted === true || voucher?.deleted === true) {
    return false;
  }

  const status = voucher?.status;
  if (status != null) {
    const normalizedStatus =
      typeof status === "number" ? status : String(status).toUpperCase().trim();

    const active =
      normalizedStatus === 1 ||
      ["1", "ACTIVE", "ENABLE", "ENABLED", "VALID", "AVAILABLE"].includes(
        String(normalizedStatus)
      );

    if (!active) return false;
  }

  const now = Date.now();
  const startTime = voucher?.startDate
    ? new Date(voucher.startDate).getTime()
    : null;
  const endTime = voucher?.endDate ? new Date(voucher.endDate).getTime() : null;

  if (startTime !== null && Number.isFinite(startTime) && now < startTime) {
    return false;
  }

  if (endTime !== null && Number.isFinite(endTime) && now >= endTime) {
    return false;
  }

  return true;
};

const loadSavedVoucher = async () => {
  const savedCode = localStorage.getItem("applied_voucher");
  if (!savedCode || totalAmount.value <= 0) return;

  try {
    const resVouchers = await api.get(`/v1/customer/vouchers?t=${Date.now()}`);
    let vouchers = [];
    if (Array.isArray(resVouchers.data)) vouchers = resVouchers.data;
    else if (Array.isArray(resVouchers.data?.content))
      vouchers = resVouchers.data.content;
    else if (Array.isArray(resVouchers.data?.data))
      vouchers = resVouchers.data.data;

    const matchedVoucher = vouchers.find(
      (v: any) =>
        String(v?.code || v?.voucherCode || v?.name || "")
          .trim()
          .toUpperCase() === savedCode.toUpperCase()
    );

    if (matchedVoucher && !isVoucherCurrentlyActive(matchedVoucher)) {
      handleCancelVoucher();
      return;
    }

    if (matchedVoucher) {
      const minOrder = Number(
        matchedVoucher.minOrderValue ||
        matchedVoucher.minimumOrderValue ||
        matchedVoucher.minOrderAmount ||
        0
      );
      if (totalAmount.value >= minOrder) {
        const type = String(
          matchedVoucher.discountType || matchedVoucher.type || ""
        ).toUpperCase();
        const value = Number(
          matchedVoucher.discountValue ||
          matchedVoucher.value ||
          matchedVoucher.discount ||
          0
        );
        const maxDiscount = Number(
          matchedVoucher.maxDiscount ||
          matchedVoucher.maxDiscountAmount ||
          matchedVoucher.maximumDiscountAmount ||
          matchedVoucher.maxAmount ||
          0
        );

        let calc = 0;
        if (type === "PERCENT" || type === "PERCENTAGE") {
          calc = (totalAmount.value * value) / 100;
          if (maxDiscount > 0) calc = Math.min(calc, maxDiscount);
        } else {
          calc = value;
        }
        discountAmount.value = Math.min(calc, totalAmount.value);
        appliedVoucherCode.value = savedCode;
        return;
      }
    }

    const resApply = await api.get("/v1/customer/vouchers/apply", {
      params: { code: savedCode, orderTotal: totalAmount.value },
    });
    const respData =
      resApply.data?.data ?? resApply.data?.result ?? resApply.data;
    const discount = Number(
      respData?.discountAmount ||
      respData?.discountValue ||
      respData?.discount ||
      respData?.amount ||
      0
    );
    discountAmount.value = Math.min(
      Math.max(discount, 0),
      Number(totalAmount.value || 0)
    );
    appliedVoucherCode.value = savedCode;
  } catch (error) {
    handleCancelVoucher();
  }
};

const handleUpdateQuantity = async (item: any, quantity: number) => {
  if (isSubmitting.value || updatingItemKey.value || quantity < 1) return;

  if (quantity > 10) {
    Swal.fire({
      toast: true,
      position: "top-end",
      icon: "warning",
      title: "Chỉ được mua tối đa 10 sản phẩm!",
      showConfirmButton: false,
      timer: 3000,
    });
    return;
  }

  const sellableQuantity = getItemSellableQuantity(item);

  if (quantity > sellableQuantity && quantity > Number(item.quantity || 0)) {
    await showWarning(
      "Không đủ tồn kho",
      `${getItemDisplayName(
        item
      )} chỉ còn ${sellableQuantity} sản phẩm có thể bán.`
    );
    return;
  }

  const finalQuantity = Math.min(quantity, sellableQuantity);

  try {
    updatingItemKey.value = getCartItemKey(item);
    await updateCartQuantityApi(item, finalQuantity);
    window.dispatchEvent(new Event("cart-updated"));
    await loadCartSummary();
    if (appliedVoucherCode.value) await loadSavedVoucher();
  } finally {
    updatingItemKey.value = null;
  }
};

const refreshCheckoutAfterConflict = async () => {
  await loadCartSummary();

  if (localStorage.getItem("applied_voucher") || appliedVoucherCode.value) {
    await loadSavedVoucher();
  }

  window.dispatchEvent(new Event("cart-updated"));
};

const handlePlaceOrder = async () => {
  const submitData = await validateCheckoutForm();
  if (!submitData || isSubmitting.value) return;

  isSubmitting.value = true;

  try {
    const feSubtotal = Number(totalAmount.value || 0);
    const feDiscount = Number(discountAmount.value || 0);
    const feShipping = Number(shippingFee.value || 0);
    const feVoucherCode = appliedVoucherCode.value || null;
    const feFinalTotal = Math.max(0, feSubtotal - feDiscount) + feShipping;

    cartSnapshot.value = JSON.parse(JSON.stringify(cartItems.value));

    const orderPayload = {
      ...submitData,
      voucherCode: feVoucherCode,

      expectedTotalAmount: feSubtotal,
      expectedDiscountAmount: feDiscount,
      expectedShippingFee: feShipping,
      expectedFinalAmount: feFinalTotal,
    };

    const res = await api.post("/v1/orders/checkout", orderPayload);
    const respData = res.data?.data ?? res.data?.result ?? res.data;

    createdOrderId.value = respData?.orderId ?? res.data?.orderId ?? null;

    successStatusText.value = getStatusText(
      Number(respData?.status ?? res.data?.status ?? 0)
    );

    successMessage.value =
      respData?.message ??
      res.data?.message ??
      "Cảm ơn bạn đã mua sắm tại Dominus. Đơn hàng của bạn đang chờ cửa hàng xác nhận.";

    const confirmedSubtotal = Number(
      respData?.totalAmount ?? res.data?.totalAmount ?? feSubtotal
    );

    const confirmedDiscount = Number(
      respData?.discountAmount ?? res.data?.discountAmount ?? feDiscount
    );

    const confirmedShipping = Number(
      respData?.shippingFee ?? res.data?.shippingFee ?? feShipping
    );

    const confirmedFinalTotal = Number(
      respData?.finalAmount ?? res.data?.finalAmount ?? feFinalTotal
    );

    successDetails.value = [
      {
        label: "Mã đơn hàng",
        value: createdOrderId.value ? `#${createdOrderId.value}` : "-",
      },
      {
        label: "Trạng thái",
        value: successStatusText.value,
      },
      {
        label: "Phương thức",
        value: formatPaymentMethod(
          respData?.paymentMethod ??
          res.data?.paymentMethod ??
          orderPayload.paymentMethod
        ),
      },
      ...(confirmedDiscount > 0
        ? [
          {
            label: "Mã giảm giá",
            value: feVoucherCode || "Đã áp dụng",
          },
        ]
        : []),
      {
        label: "Tạm tính",
        value: formatCurrency(confirmedSubtotal),
        money: true,
      },
      ...(confirmedDiscount > 0
        ? [
          {
            label: "Giảm giá",
            value: `-${formatCurrency(confirmedDiscount)}`,
            money: true,
          },
        ]
        : []),
      {
        label: "Phí vận chuyển",
        value: formatCurrency(confirmedShipping),
        money: true,
      },
      {
        label: "Tổng thanh toán",
        value: formatCurrency(confirmedFinalTotal),
        money: true,
      },
    ];

    if (
      orderPayload.paymentMethod === "VIETQR" ||
      orderPayload.paymentMethod === "VNPAY"
    ) {
      currentPaymentMethod.value = orderPayload.paymentMethod;

      if (orderPayload.paymentMethod === "VIETQR") {
        const orderId =
          createdOrderId.value || Math.floor(Math.random() * 100000);

        const amount = confirmedFinalTotal;

        qrCodeUrl.value =
          respData?.qrUrl ??
          res.data?.qrUrl ??
          `https://img.vietqr.io/image/TCB-3714082007-compact2.png?amount=${amount}&addInfo=DH${orderId}&accountName=NGUYEN%20DINH%20HUY`;
      } else {
        const url =
          respData?.paymentUrl ??
          respData?.vnpUrl ??
          respData?.url ??
          res.data?.paymentUrl ??
          res.data?.vnpUrl ??
          res.data?.url;

        if (!url) {
          throw new Error("Không lấy được link thanh toán VNPay");
        }

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
    if (Number(error?.response?.status) === 409) {
      await refreshCheckoutAfterConflict();

      await showWarning(
        "Thông tin thanh toán đã thay đổi",
        getErrorMessage(
          error,
          "Giá, khuyến mãi hoặc tổng thanh toán đã thay đổi. Hệ thống đã cập nhật dữ liệu mới nhất, vui lòng kiểm tra và xác nhận lại."
        )
      );

      return;
    }

    await showError(
      "Lỗi đặt hàng",
      getErrorMessage(error, "Có lỗi xảy ra khi đặt hàng.")
    );
  } finally {
    isSubmitting.value = false;
  }
};

const cancelAndRestoreCart = async () => {
  stopPaymentTimer();
  try {
    isPageLoading.value = true;
    
    const orderIdToCancel = createdOrderId.value || localStorage.getItem('pendingVnpayOrderId');
    createdOrderId.value = null;

    if (orderIdToCancel) {
      await api.patch(`/customer/orders/${orderIdToCancel}/cancel`, {
        cancelReason: "Khác"
      });
      localStorage.removeItem('pendingVnpayOrderId');
    }

    const snapshotToRestore = [...cartSnapshot.value];
    cartSnapshot.value = [];

    if (snapshotToRestore && snapshotToRestore.length > 0) {
      const addPromises = snapshotToRestore.map((item: any) => {
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

    Swal.fire({
      toast: true,
      position: "top-end",
      icon: "info",
      title: "Chưa thanh toán",
      text: "Giỏ hàng đã được khôi phục để bạn tiếp tục mua sắm.",
      showConfirmButton: false,
      timer: 3500,
    });
  } catch (error: any) {
    console.error(error);
    await showError(
      "Không thể hủy thanh toán",
      getErrorMessage(
        error,
        "Hệ thống chưa hủy được phiên thanh toán. Giỏ hàng chưa được khôi phục để tránh tạo đơn trùng. Vui lòng thử lại."
      )
    );
  } finally {
    isPageLoading.value = false;
  }
};

const handleCancelPayment = async () => {
  if (!showPaymentModal.value || isConfirmingQrPayment.value) return;
  showPaymentModal.value = false;
  window.removeEventListener("popstate", handleBrowserBackDuringPayment);
  await cancelAndRestoreCart();
};

const handleTimeoutPayment = async () => {
  if (!showPaymentModal.value || isConfirmingQrPayment.value) return;
  showPaymentModal.value = false;
  window.removeEventListener("popstate", handleBrowserBackDuringPayment);
  await cancelAndRestoreCart();
};

const handleBrowserBackDuringPayment = async () => {
  if (showPaymentModal.value && !isConfirmingQrPayment.value) {
    showPaymentModal.value = false;
    window.removeEventListener("popstate", handleBrowserBackDuringPayment);
    await cancelAndRestoreCart();
  }
};

const goToVnpayGateway = () => {
  stopPaymentTimer();
  sessionStorage.removeItem("pending_vnpay_outcome");
  sessionStorage.setItem(
    "pending_vnpay_cart",
    JSON.stringify(cartSnapshot.value)
  );
  sessionStorage.setItem("pending_vnpay_order", String(createdOrderId.value));
  sessionStorage.setItem("pending_vnpay_form", JSON.stringify(orderForm.value));
  
  if (createdOrderId.value) {
    localStorage.setItem('pendingVnpayOrderId', String(createdOrderId.value));
  }

  if (appliedVoucherCode.value)
    sessionStorage.setItem("pending_vnpay_voucher", appliedVoucherCode.value);
  window.location.href = vnpayUrl.value;
};

const confirmQrPayment = async () => {
  if (!createdOrderId.value || isConfirmingQrPayment.value) {
    if (!createdOrderId.value) {
      await showError(
        "Không thể xác nhận thanh toán",
        "Không xác định được đơn hàng VietQR cần xác nhận."
      );
    }
    return;
  }

  isConfirmingQrPayment.value = true;

  try {
    await api.post(`/v1/orders/${createdOrderId.value}/report-payment`);

    stopPaymentTimer();
    showPaymentModal.value = false;
    window.removeEventListener("popstate", handleBrowserBackDuringPayment);

    discountAmount.value = 0;
    appliedVoucherCode.value = "";
    localStorage.removeItem("applied_voucher");
    sessionStorage.removeItem("dominus_checkout_draft");

    try {
      const saved = localStorage.getItem("dominus_paid_orders");
      const map = saved ? JSON.parse(saved) : {};
      map[String(createdOrderId.value)] = true;
      localStorage.setItem("dominus_paid_orders", JSON.stringify(map));
    } catch (e) { }

    setTimeout(() => {
      showSuccessModal.value = true;
    }, 200);
  } catch (error: any) {
    if (paymentCountdown.value <= 0) {
      isConfirmingQrPayment.value = false;
      showPaymentModal.value = false;
      window.removeEventListener("popstate", handleBrowserBackDuringPayment);
      await cancelAndRestoreCart();
      return;
    }

    await showError(
      "Không thể xác nhận thanh toán",
      getErrorMessage(
        error,
        "Hệ thống chưa ghi nhận được thanh toán VietQR. Đơn hàng chưa được đánh dấu đã thanh toán, vui lòng thử lại."
      )
    );
  } finally {
    isConfirmingQrPayment.value = false;
  }
};

const goToCart = () => router.push("/cart");
const goToHome = () => {
  showSuccessModal.value = false;
  router.push("/");
};
const goToOrders = () => {
  showSuccessModal.value = false;
  router.push({ path: "/customer/profile", query: { tab: "orders" } });
};

interface PendingVnpayOutcome {
  orderId?: string | number | null;
  status?: number | null;
  success?: boolean;
  message?: string;
  processedAt?: string;
}

const PENDING_VNPAY_OUTCOME_KEY = "pending_vnpay_outcome";

const readPendingVnpayOutcome = (): PendingVnpayOutcome | null => {
  const raw = sessionStorage.getItem(PENDING_VNPAY_OUTCOME_KEY);

  if (!raw) {
    return null;
  }

  try {
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === "object"
      ? (parsed as PendingVnpayOutcome)
      : null;
  } catch {
    sessionStorage.removeItem(PENDING_VNPAY_OUTCOME_KEY);
    return null;
  }
};

const clearPendingVnpayBackup = () => {
  sessionStorage.removeItem("pending_vnpay_order");
  sessionStorage.removeItem("pending_vnpay_cart");
  sessionStorage.removeItem("pending_vnpay_form");
  sessionStorage.removeItem("pending_vnpay_voucher");
  localStorage.removeItem("pendingVnpayOrderId");
  sessionStorage.removeItem(PENDING_VNPAY_OUTCOME_KEY);
};

const restorePendingVnpayFormAndVoucher = () => {
  const pendingForm = sessionStorage.getItem("pending_vnpay_form");

  if (pendingForm) {
    try {
      Object.assign(orderForm.value, JSON.parse(pendingForm));
      formKey.value++;
    } catch { }
  }

  const pendingVoucher = sessionStorage.getItem("pending_vnpay_voucher");

  if (pendingVoucher) {
    localStorage.setItem("applied_voucher", pendingVoucher);
    appliedVoucherCode.value = pendingVoucher;
  }
};

const restorePendingVnpayCart = async (items: any[]) => {
  if (!Array.isArray(items) || items.length === 0) {
    return;
  }

  for (const item of items) {
    const variantId = Number(
      item?.productVariantId ||
      item?.variantId ||
      item?.productVariant?.id ||
      item?.id ||
      0
    );

    const quantity = Number(item?.quantity || 1);

    if (!variantId || !Number.isFinite(quantity) || quantity <= 0) {
      continue;
    }

    await api.post("/v1/customer/cart/add", {
      productVariantId: variantId,
      quantity: Math.trunc(quantity),
    });
  }
};

// Khóa đồng bộ chống gọi trùng lặp (race condition) do pageshow và onMounted kích hoạt song song
let isRestoringBackup = false;

const checkAndRestoreVnpayBackup = async () => {
  if (isRestoringBackup) {
    return false;
  }

  const pendingOrder = sessionStorage.getItem("pending_vnpay_order");
  const pendingCart = sessionStorage.getItem("pending_vnpay_cart");

  if (!pendingOrder || !pendingCart) {
    return false;
  }

  // Khóa ngay lập tức trước khi thực hiện bất kỳ async await nào
  isRestoringBackup = true;
  clearPendingVnpayBackup();

  isPageLoading.value = true;
  showPaymentModal.value = false;

  try {
    const outcome = readPendingVnpayOutcome();

    const sameOrder =
      !outcome?.orderId || String(outcome.orderId) === String(pendingOrder);

    const outcomeStatus =
      outcome?.status === null || outcome?.status === undefined
        ? null
        : Number(outcome.status);

    const alreadyPaidOrCompleted =
      sameOrder &&
      (outcome?.success === true || outcomeStatus === 1 || outcomeStatus === 3);

    if (alreadyPaidOrCompleted) {
      localStorage.removeItem("applied_voucher");
      isRestoringBackup = false;
      return false;
    }

    try {
      await api.patch(`/customer/orders/${pendingOrder}/cancel`, {
        cancelReason: "Khác"
      });
    } catch (e) {
      console.error("Lỗi khi hủy đơn tự động:", e);
    }

    const items = JSON.parse(pendingCart);
    const safeItems = Array.isArray(items) ? items : [];

    restorePendingVnpayFormAndVoucher();

    await restorePendingVnpayCart(safeItems);

    cartSnapshot.value = safeItems;
    createdOrderId.value = null;
    currentPaymentMethod.value = "";
    vnpayUrl.value = "";
    qrCodeUrl.value = "";

    window.dispatchEvent(new Event("cart-updated"));
    await loadCartSummary();

    if (appliedVoucherCode.value) {
      await loadSavedVoucher();
    }

    Swal.fire({
      toast: true,
      position: "top-end",
      icon: "info",
      title: "Chưa thanh toán",
      text: "Giỏ hàng đã được khôi phục để bạn tiếp tục mua sắm.",
      showConfirmButton: false,
      timer: 3500,
    });

    return true;
  } catch (error: any) {
    await showError(
      "Lỗi khôi phục giỏ hàng",
      "Có lỗi xảy ra khi khôi phục giỏ hàng của bạn. Vui lòng tải lại trang."
    );

    return false;
  } finally {
    isPageLoading.value = false;
    // Mở khóa sau khi hoàn tất xử lý
    isRestoringBackup = false;
  }
};

window.addEventListener("pageshow", async (event) => {
  if (event.persisted || sessionStorage.getItem("pending_vnpay_order")) {
    await checkAndRestoreVnpayBackup();
  }
});

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
      } catch (e) { }
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
  clearPromotionExpiryTimer();
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

.checkout-loading {
  width: 100%;
  min-height: 380px;
  background: #ffffff;
  border: 1px solid #eaeaea;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  flex-direction: column;
  gap: 12px;
}

.premium-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 24px;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
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