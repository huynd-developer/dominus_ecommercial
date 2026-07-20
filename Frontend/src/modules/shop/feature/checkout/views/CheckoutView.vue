<template>
  <div class="page-wrapper">
    <ShopHeader />

    <main class="main-content full-width">
      <div v-if="isPageLoading" class="checkout-loading">
        <div class="spinner-border"></div>
        <p>Đang tải thông tin thanh toán...</p>
      </div>

      <template v-else>
        <CheckoutForm :form="orderForm" />

        <CheckoutSummary
          :cartItems="cartItems"
          :totalItems="totalItems"
          :totalAmount="totalAmount"
          :discountAmount="discountAmount"
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
      <!-- BƯỚC 1: POPUP MÃ QR (Chỉ hiện khi chọn VietQR) -->
      <Transition name="fade-modal">
        <div v-if="showQrModal" class="premium-modal-overlay">
          <div class="qr-payment-box bg-white p-4 rounded-4 shadow-lg text-center d-flex flex-column align-items-center mx-3" style="max-width: 400px; animation: slideUp 0.3s ease-out;">
            <div class="mb-2 text-primary">
              <i class="bi bi-qr-code-scan" style="font-size: 2.5rem; color: #10b981;"></i>
            </div>
            <h4 class="mb-2 fw-bold" style="color: #06132b;">Thanh toán đơn hàng</h4>
            <p class="text-muted small mb-3">Vui lòng mở ứng dụng ngân hàng và quét mã QR bên dưới để hoàn tất.</p>
            
            <img :src="qrCodeUrl" alt="Mã VietQR" class="img-fluid rounded mb-3" style="border: 2px dashed #bd9a5f; padding: 8px;" />
            
            <div class="alert alert-warning py-2 px-3 mb-3 w-100 text-start" style="font-size: 0.85rem;">
              <i class="bi bi-info-circle me-1"></i> Vui lòng không đóng cửa sổ này cho đến khi thanh toán xong!
            </div>

            <button @click="confirmQrPayment" class="btn btn-success w-100 py-3 fw-bold rounded-3" style="background-color: #10b981; border: none; font-size: 1.1rem;">
              Xác nhận thanh toán! <i class="bi bi-check-circle ms-1"></i>
            </button>
          </div>
        </div>
      </Transition>

      <!-- BƯỚC 2: BẢNG HÓA ĐƠN GỐC (Hiện ra sau khi COD hoặc sau khi xác nhận QR) -->
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
import { computed, onMounted, ref } from "vue";
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

interface CheckoutSubmitData {
  customerName: string;
  customerPhone: string;
  shippingAddress: string;
  note: string | null;
  paymentMethod: string;
  voucherCode: string | null;
}

const router = useRouter();

const cartItems = ref<any[]>([]);
const isSubmitting = ref(false);
const isPageLoading = ref(true);
const updatingItemKey = ref<string | number | null>(null);

// Tách riêng 2 trạng thái quản lý Modal
const showQrModal = ref(false);
const showSuccessModal = ref(false);
const qrCodeUrl = ref("");

const successStatusText = ref("");
const successMessage = ref(
  "Cảm ơn bạn đã mua sắm tại Dominus. Đơn hàng của bạn đang chờ cửa hàng xác nhận."
);

const successDetails = ref<ResultDetail[]>([]);

const discountAmount = ref(0);
const appliedVoucherCode = ref("");

const orderForm = ref({
  customerName: "",
  customerPhone: "",
  shippingAddress: "",
  note: "",
  paymentMethod: "COD",

  provinceName: "",
  wardName: "",
  specificAddress: "",

  profileLoaded: false,
  profileAddress: "",

  requireVat: false,
  vatTaxCode: "",
  vatEmail: "",
  vatCompanyName: "",
  vatCompanyAddress: "",
});

const getCartItemKey = (item: any) => {
  return (
    item?.cartItemId ||
    item?.id ||
    item?.productVariantId ||
    item?.variantId ||
    item?.sku
  );
};

const getProductVariantId = (item: any) => {
  return Number(
    item?.productVariantId ||
      item?.variantId ||
      item?.productVariant?.id ||
      item?.id ||
      0
  );
};

const getCartItemId = (item: any) => {
  return Number(item?.cartItemId || item?.id || 0);
};

const getStock = (item: any) => {
  return Number(
    item?.stockQuantity ??
      item?.stock ??
      item?.availableQuantity ??
      item?.maxQuantity ??
      0
  );
};

const getItemPrice = (item: any) => {
  return Number(
    item?.price ??
      item?.salePrice ??
      item?.finalPrice ??
      item?.originalPrice ??
      0
  );
};

const totalAmount = computed(() => {
  return cartItems.value.reduce((sum, item) => {
    return sum + getItemPrice(item) * Number(item.quantity || 0);
  }, 0);
});

const finalTotal = computed(() => {
  return Math.max(
    0,
    Number(totalAmount.value || 0) - Number(discountAmount.value || 0)
  );
});

const totalItems = computed(() => {
  return cartItems.value.reduce((sum, item) => {
    return sum + Number(item.quantity || 0);
  }, 0);
});

const updateCartQuantityApi = async (item: any, quantity: number) => {
  await api.put("/v1/customer/cart/update", {
    cartItemId: getCartItemId(item),
    productVariantId: getProductVariantId(item),
    quantity,
  });
};

const collapseSpacesForProfile = (value: string) => {
  return String(value || "")
    .trim()
    .replace(/\s{2,}/g, " ");
};

const extractObjectData = (data: any) => {
  return data?.data || data?.result || data || {};
};

const getErrorMessage = (error: any, fallback: string) => {
  const data = error?.response?.data;

  if (typeof data === "string") {
    return data;
  }

  if (data?.message) {
    return data.message;
  }

  if (data?.errors && typeof data.errors === "object") {
    const firstError = Object.values(data.errors)[0];

    if (firstError) {
      return String(firstError);
    }
  }

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

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(Number(value || 0));
};

const formatPaymentMethod = (value: string | null | undefined) => {
  const normalized = String(value || "").toUpperCase();

  if (normalized === "COD") return "Thanh toán khi nhận hàng";
  if (normalized === "VNPAY") return "VNPay";
  if (normalized === "VIETQR") return "Chuyển khoản VietQR";
  if (normalized === "CASH") return "Tiền mặt";
  if (normalized === "BANK_TRANSFER") return "Chuyển khoản";
  if (normalized === "TRANSFER") return "Chuyển khoản";
  if (normalized === "MIXED") return "Tiền mặt + chuyển khoản";

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
    case 5:
      return "Giao hàng thất bại";
    case 6:
      return "Yêu cầu hoàn hàng / đổi trả";
    case 7:
      return "Hoàn hàng / đổi trả hoàn tất";
    default:
      return "Không xác định";
  }
};

const handleApplyVoucher = (discount: number, code: string) => {
  const safeDiscount = Math.min(
    Math.max(Number(discount || 0), 0),
    Number(totalAmount.value || 0)
  );

  discountAmount.value = safeDiscount;
  appliedVoucherCode.value = code || "";

  if (code) {
    localStorage.setItem("applied_voucher", code);
  } else {
    localStorage.removeItem("applied_voucher");
  }
};

const handleCancelVoucher = () => {
  discountAmount.value = 0;
  appliedVoucherCode.value = "";
  localStorage.removeItem("applied_voucher");
};

const loadCustomerProfile = async () => {
  try {
    const res = await api.get("/customer/profile");
    const profile = extractObjectData(res.data);

    const name = collapseSpacesForProfile(
      profile.name || profile.fullName || profile.customerName || ""
    );

    const phone = String(profile.phone || profile.customerPhone || "").trim();

    const address = collapseSpacesForProfile(
      profile.address || profile.shippingAddress || ""
    );

    orderForm.value.profileLoaded = true;
    orderForm.value.customerName = name;
    orderForm.value.customerPhone = phone;
    orderForm.value.profileAddress = address;

    if (address) {
      orderForm.value.shippingAddress = address;
      orderForm.value.provinceName = "";
      orderForm.value.wardName = "";
      orderForm.value.specificAddress = "";
    }
  } catch (error: any) {
    console.error("Lỗi tải thông tin tài khoản:", error);

    await showError(
      "Vui lòng đăng nhập",
      getErrorMessage(
        error,
        "Bạn cần đăng nhập tài khoản khách hàng để thanh toán."
      )
    );

    router.replace({
      name: "Login",
      query: {
        redirect: "/checkout",
      },
    });

    return false;
  }

  return true;
};

const validateCheckoutForm = async (): Promise<CheckoutSubmitData | null> => {
  const rawCustomerName = String(orderForm.value.customerName || "");
  const rawCustomerPhone = String(orderForm.value.customerPhone || "");
  const rawShippingAddress = String(orderForm.value.shippingAddress || "");
  const rawNote = String(orderForm.value.note || "");
  const rawSpecificAddress = String(orderForm.value.specificAddress || "");

  const customerName = rawCustomerName.trim();
  const customerPhone = rawCustomerPhone.trim();
  const shippingAddress = rawShippingAddress.trim();
  const note = rawNote.trim();
  const specificAddress = rawSpecificAddress.trim();

  const paymentMethod = String(
    orderForm.value.paymentMethod || ""
  ).toUpperCase();

  const provinceName = String(orderForm.value.provinceName || "").trim();
  const wardName = String(orderForm.value.wardName || "").trim();

  const profileAddressStr = String(orderForm.value.profileAddress || "");
  let isSavedAddress = false;
  
  if (shippingAddress) {
    try {
      if (profileAddressStr.startsWith('[')) {
        const arr = JSON.parse(profileAddressStr);
        isSavedAddress = arr.some((a: any) => a.fullAddress === shippingAddress || a.fullAddress === rawShippingAddress);
      } else {
        isSavedAddress = profileAddressStr === shippingAddress || profileAddressStr === rawShippingAddress;
      }
    } catch (e) {
      isSavedAddress = profileAddressStr.includes(shippingAddress);
    }
  }

  const isEditingStructuredAddress =
    !isSavedAddress && (Boolean(provinceName) || Boolean(wardName) || Boolean(specificAddress));

  if (cartItems.value.length === 0) {
    await showWarning(
      "Giỏ hàng trống",
      "Vui lòng thêm sản phẩm vào giỏ hàng trước khi đặt hàng."
    );

    router.push("/products");
    return null;
  }

  if (rawCustomerName !== customerName) {
    await showWarning(
      "Tên người nhận không hợp lệ",
      "Tên người nhận không được có khoảng trắng ở đầu hoặc cuối."
    );
    return null;
  }

  if (customerName.length < 2 || customerName.length > 100) {
    await showWarning(
      "Tên người nhận không hợp lệ",
      "Tên người nhận phải từ 2 đến 100 ký tự."
    );
    return null;
  }

  if (!/^(?!.*\s{2,})[\p{L}]+(?:\s[\p{L}]+)*$/u.test(customerName)) {
    await showWarning(
      "Tên người nhận không hợp lệ",
      "Tên người nhận chỉ được chứa chữ, không có số/ký tự đặc biệt và không có khoảng trắng thừa."
    );
    return null;
  }

  if (rawCustomerPhone !== customerPhone) {
    await showWarning(
      "Số điện thoại không hợp lệ",
      "Số điện thoại không được có khoảng trắng ở đầu hoặc cuối."
    );
    return null;
  }

  if (!/^0\d{9}$/.test(customerPhone)) {
    await showWarning(
      "Số điện thoại không hợp lệ",
      "Số điện thoại phải gồm đúng 10 chữ số, bắt đầu bằng 0 và không chứa chữ/ký tự đặc biệt."
    );
    return null;
  }

  if (isEditingStructuredAddress) {
    if (!provinceName) {
      await showWarning(
        "Thiếu tỉnh/thành phố",
        "Vui lòng chọn tỉnh/thành phố nhận hàng."
      );
      return null;
    }

    if (!wardName) {
      await showWarning(
        "Thiếu phường/xã",
        "Vui lòng chọn phường/xã/đặc khu nhận hàng."
      );
      return null;
    }

    if (rawSpecificAddress !== specificAddress) {
      await showWarning(
        "Địa chỉ cụ thể không hợp lệ",
        "Địa chỉ cụ thể không được có khoảng trắng ở đầu hoặc cuối."
      );
      return null;
    }

    if (specificAddress.length < 3 || specificAddress.length > 255) {
      await showWarning(
        "Địa chỉ cụ thể không hợp lệ",
        "Vui lòng nhập số nhà, ngõ, đường hoặc tòa nhà từ 3 đến 255 ký tự."
      );
      return null;
    }

    if (!/^(?!.*\s{2,})[\p{L}\d][\p{L}\d\s,./#()\-]*[\p{L}\d)]$/u.test(specificAddress)) {
      await showWarning(
        "Địa chỉ cụ thể không hợp lệ",
        "Địa chỉ cụ thể chỉ được chứa chữ, số, khoảng trắng và các ký tự , . / # ( ) -, không có ký tự đặc biệt."
      );
      return null;
    }
  }

  if (rawShippingAddress !== shippingAddress) {
    await showWarning(
      "Địa chỉ không hợp lệ",
      "Địa chỉ không được có khoảng trắng ở đầu hoặc cuối."
    );
    return null;
  }

  if (shippingAddress.length < 5 || shippingAddress.length > 500) {
    await showWarning(
      "Địa chỉ không hợp lệ",
      "Vui lòng chọn hoặc nhập địa chỉ giao hàng từ 5 đến 500 ký tự."
    );
    return null;
  }

  if (!/^(?!.*\s{2,})[\p{L}\d][\p{L}\d\s,./#()\-]*[\p{L}\d)]$/u.test(shippingAddress)) {
    await showWarning(
      "Địa chỉ không hợp lệ",
      "Địa chỉ chỉ được chứa chữ, số, khoảng trắng và các ký tự , . / # ( ) -, không có khoảng trắng thừa hoặc ký tự đặc biệt."
    );
    return null;
  }

  if (rawNote && rawNote !== note) {
    await showWarning(
      "Ghi chú không hợp lệ",
      "Ghi chú không được có khoảng trắng ở đầu hoặc cuối."
    );
    return null;
  }

  if (note && note.length > 255) {
    await showWarning("Ghi chú quá dài", "Ghi chú tối đa 255 ký tự.");
    return null;
  }

  if (note && !/^[\p{L}\d\s,./#()\-:;!?]*$/u.test(note)) {
    await showWarning(
      "Ghi chú không hợp lệ",
      "Ghi chú chỉ được chứa chữ, số, khoảng trắng và các ký tự , . / # ( ) - : ; ! ?"
    );
    return null;
  }

  if (!["COD", "VNPAY", "VIETQR"].includes(paymentMethod)) {
    await showWarning(
      "Phương thức thanh toán không hợp lệ",
      "Chỉ hỗ trợ thanh toán được cấu hình sẵn."
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
    voucherCode: appliedVoucherCode.value || null,
  };
};

const loadCartSummary = async () => {
  try {
    const res = await api.get("/v1/customer/cart/my-cart");

    cartItems.value = Array.isArray(res.data) ? res.data : [];

    if (cartItems.value.length === 0) {
      handleCancelVoucher();

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
      getErrorMessage(error, "Vui lòng đăng nhập lại hoặc thử lại sau.")
    );

    router.replace("/cart");
  }
};

const loadSavedVoucher = async () => {
  const savedCode = localStorage.getItem("applied_voucher");

  if (!savedCode || totalAmount.value <= 0) {
    discountAmount.value = 0;
    appliedVoucherCode.value = "";
    return;
  }

  try {
    const res = await api.get("/v1/customer/vouchers/apply", {
      params: {
        code: savedCode,
        orderTotal: totalAmount.value,
      },
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
    console.warn("Voucher không còn hợp lệ cho đơn hàng này:", error);
    handleCancelVoucher();
  }
};

const handleUpdateQuantity = async (item: any, quantity: number) => {
  if (isSubmitting.value || updatingItemKey.value) {
    return;
  }

  if (quantity < 1) {
    await showWarning(
      "Số lượng không hợp lệ",
      "Số lượng sản phẩm phải lớn hơn hoặc bằng 1."
    );
    return;
  }

  const stock = getStock(item);

  if (stock > 0 && quantity > stock) {
    await showWarning(
      "Vượt quá tồn kho",
      `Sản phẩm này chỉ còn ${stock} sản phẩm.`
    );
    return;
  }

  try {
    updatingItemKey.value = getCartItemKey(item);

    await updateCartQuantityApi(item, quantity);

    window.dispatchEvent(new Event("cart-updated"));

    await loadCartSummary();

    if (appliedVoucherCode.value || localStorage.getItem("applied_voucher")) {
      await loadSavedVoucher();
    }
  } catch (error: any) {
    console.error("Lỗi cập nhật số lượng:", error);

    await showError(
      "Không thể cập nhật số lượng",
      getErrorMessage(error, "Vui lòng thử lại sau.")
    );
  } finally {
    updatingItemKey.value = null;
  }
};

const handlePlaceOrder = async () => {
  const submitData = await validateCheckoutForm();

  if (!submitData) return;
  if (isSubmitting.value) return;

  isSubmitting.value = true;

  try {
    const res = await api.post("/v1/orders/checkout", submitData);

    localStorage.removeItem("applied_voucher");

    // LUỒNG 1: NẾU LÀ VNPAY -> CHUYỂN HƯỚNG SANG TRANG THANH TOÁN VNPay
    if (submitData.paymentMethod === "VNPAY") {
      // Backend của m trả về key gì thì m nhớ đổi lại cho đúng (thường là paymentUrl)
      const vnpayUrl = res.data?.paymentUrl || res.data?.vnpUrl || res.data?.url; 
      
      if (vnpayUrl) {
        window.location.href = vnpayUrl; // Bắn khách qua VNPay
        return; 
      } else {
        await showError("Lỗi VNPay", "Không lấy được đường dẫn thanh toán VNPay từ hệ thống.");
        isSubmitting.value = false;
        return;
      }
    }

    // LUỒNG CHUNG CHO VIETQR & COD: Chuẩn bị Data cho bảng Thành công
    const responseStatus = Number(res.data?.status ?? 0);
    const responseStatusText = getStatusText(responseStatus);

    successStatusText.value = responseStatusText;
    successMessage.value = res.data?.message || "Cảm ơn bạn đã mua sắm tại Dominus. Đơn hàng của bạn đang chờ cửa hàng xác nhận.";
    successDetails.value = [
      { label: "Mã đơn hàng", value: res.data?.orderId ? `#${res.data.orderId}` : "-" },
      { label: "Trạng thái", value: responseStatusText },
      { label: "Phương thức", value: formatPaymentMethod(res.data?.paymentMethod || submitData.paymentMethod) },
      { label: "Mã giảm giá", value: res.data?.voucherCode || submitData.voucherCode || "Không có" },
      { label: "Tạm tính", value: formatCurrency(Number(res.data?.totalAmount ?? totalAmount.value)), money: true },
      { label: "Giảm giá", value: `-${formatCurrency(Number(res.data?.discountAmount ?? discountAmount.value))}`, money: true },
      { label: "Tổng thanh toán", value: formatCurrency(Number(res.data?.finalAmount ?? finalTotal.value)), money: true },
    ];

    discountAmount.value = 0;
    appliedVoucherCode.value = "";
    window.dispatchEvent(new Event("cart-updated"));

    // LUỒNG 2: NẾU LÀ VIETQR -> HIỆN POPUP QR
    if (submitData.paymentMethod === "VIETQR") {
      const orderId = res.data?.orderId || Math.floor(Math.random() * 100000);
      const amount = res.data?.finalAmount ?? finalTotal.value;
      
      qrCodeUrl.value = res.data?.qrUrl || `https://img.vietqr.io/image/970422-0123456789-compact2.png?amount=${amount}&addInfo=Thanh toan don ${orderId}&accountName=SHOP DOMINUS`;
      
      showQrModal.value = true;
    } 
    // LUỒNG 3: NẾU LÀ COD -> HIỆN BILL THÀNH CÔNG
    else {
      showSuccessModal.value = true;
    }

  } catch (error: any) {
    console.error("Lỗi đặt hàng:", error);
    await showError("Không thể đặt hàng", getErrorMessage(error, "Có lỗi xảy ra khi đặt hàng. Vui lòng thử lại."));
  } finally {
    isSubmitting.value = false;
  }
};

// Hàm xử lý khi khách bấm "Tôi đã thanh toán"
const confirmQrPayment = () => {
  showQrModal.value = false; // Tắt Modal QR
  
  // Tùy chọn: Thêm 1 chút thời gian delay tạo cảm giác "hệ thống đang xử lý"
  setTimeout(() => {
    showSuccessModal.value = true; // Bật Modal Thành công
  }, 200);
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

const loadInitialData = async () => {
  try {
    isPageLoading.value = true;
    const profileOk = await loadCustomerProfile();
    if (!profileOk) return;

    await loadCartSummary();
    await loadSavedVoucher();
  } finally {
    isPageLoading.value = false;
  }
};

onMounted(() => {
  loadInitialData();
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
  background: rgba(0, 0, 0, 0.75); /* Tăng độ tối nhẹ để tập trung vào popup */
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 24px;
}

/* Hiệu ứng trượt lên nhẹ nhàng cho Popup QR */
@keyframes slideUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
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