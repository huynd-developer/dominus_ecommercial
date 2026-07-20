import { defineStore } from "pinia";
import api from "@/common/api";

export interface PosProduct {
  id: number;
  sku: string;
  name: string;
  subName?: string;
  price: number;
  stockQuantity: number;
  image: string;
  category: string;
  manufacturingDate?: string | null;
  expirationDate?: string | null;
  status?: number | null;
  expired?: boolean;
  sellable?: boolean;
  unavailableReason?: string | null;
}

export interface CartItem {
  product: PosProduct;
  quantity: number;
}

export interface PosCustomer {
  customerId?: number;
  name: string;
  phone: string;
  email: string;
  customerRank?: string;
  loyaltyPoints: number;
}

export interface PosHeldOrder {
  orderId: number;
  status: string;
  paymentMethod: string;
  totalAmount: number;
  discountAmount: number;
  finalAmount: number;
  createdAt: string;
  cashierId?: number;
  cashierName?: string;
  customerId?: number;
  customerName?: string;
  customerPhone?: string;
  customerEmail?: string;
  ownOrder?: boolean;
  isOwnOrder?: boolean;
  canOpen?: boolean;
  canCheckout?: boolean;
  canTransfer?: boolean;
  canCancel?: boolean;
}

export interface PosVoucher {
  id?: number;
  code: string;
  name: string;
  description?: string;
  discountType: string;
  discountValue: number;
  discountPercent?: number;
  maxDiscountAmount?: number;
  minOrderAmount?: number;
  discountAmount: number;
  startDate?: string | null;
  endDate?: string | null;
  remainingQuantity?: number | null;
  eligible: boolean;
  ineligibleReason?: string | null;
}

export interface PosTransferTarget {
  employeeId: number;
  employeeCode?: string;
  name?: string;
  email?: string;
  phone?: string;
  roleName?: string;
  jobTitle?: string;
}

export interface PosFilterOption {
  id?: number;
  name: string;
  label?: string;
  value?: string | number;
}

export type PosPaymentMethod = "CASH" | "VNPAY" | "VIETQR";
export type PosCheckoutPaymentMethod = "CASH" | "VNPAY" | "VIETQR" | "MIXED";
export type PosTransferProvider = "" | "VNPAY" | "VIETQR";

export interface PosStoreState {
  allProducts: PosProduct[];
  categories: string[];
  brandOptions: PosFilterOption[];
  capacityOptions: PosFilterOption[];
  bottleTypeOptions: PosFilterOption[];
  selectedCategory: string;
  searchQuery: string;
  availableVouchers: PosVoucher[];
  voucherLoading: boolean;
  cart: CartItem[];
  customer: PosCustomer | null;
  customerSavedKey: string;

  voucherCode: string;
  discountAmount: number;

  paymentMethod: PosPaymentMethod;
  transferProvider: PosTransferProvider;
  cashPaid: number;

  vnpayUrl: string;
  vietQrImageUrl: string;
  vietQrContent: string;
  pendingVietQrOrderId: number | string | null;
  pendingVietQrAmount: number;
  activePendingPaymentOrderId: number | string | null;
  activePendingPaymentTransferProvider: PosTransferProvider;
  isLoading: boolean;
  errorMsg: string;
  lastOrderId: string | number | null;

  heldOrders: PosHeldOrder[];
  transferTargets: PosTransferTarget[];

  activeHeldOrderId: number | null;
  activeHeldOrderCashierName: string;
  activeHeldOrderOwnOrder: boolean;
  activeHeldOrderCanOpen: boolean;
  activeHeldOrderCanCheckout: boolean;
  activeHeldOrderCanTransfer: boolean;
  activeHeldOrderCanCancel: boolean;
  showHeldOrdersPanel: boolean;
}

const normalizePhone = (phone?: string | null): string => {
  return (phone || "").replace(/[\s.-]/g, "").trim();
};

const isValidVietnamPhone = (phone: string): boolean => {
  return /^(03|05|07|08|09)\d{8}$/.test(phone);
};

const normalizeText = (value?: string | null): string => {
  return (value || "").trim().replace(/\s+/g, " ");
};

const normalizeEmail = (value?: string | null): string => {
  return (value || "").trim().toLowerCase();
};

const buildCustomerSavedKey = (customer?: PosCustomer | null): string => {
  if (!customer) return "";

  return [
    normalizePhone(customer.phone),
    normalizeText(customer.name),
    normalizeEmail(customer.email),
  ].join("|");
};

const isValidEmail = (email: string): boolean => {
  const value = normalizeEmail(email);

  if (!value) return false;
  if (value.length > 255) return false;

  const parts = value.split("@");

  if (parts.length !== 2) return false;

  const [localPart, domain] = parts;

  if (!localPart || !domain) return false;

  if (localPart.startsWith(".") || localPart.endsWith(".")) return false;
  if (domain.startsWith(".") || domain.endsWith(".")) return false;
  if (value.includes("..")) return false;

  if (!/^[A-Za-z0-9._%+-]+$/.test(localPart)) return false;

  const domainParts = domain.split(".");

  if (domainParts.length < 2) return false;

  for (const label of domainParts) {
    if (!label) return false;
    if (!/^[A-Za-z0-9-]+$/.test(label)) return false;
    if (label.startsWith("-") || label.endsWith("-")) return false;
  }

  const tld = domainParts[domainParts.length - 1];

  if (!tld) return false;

  if (!/^[A-Za-z]{2,}$/.test(tld)) return false;

  return true;
};
const isValidCustomerName = (name: string): boolean => {
  const value = normalizeText(name).normalize("NFC");

  if (!value) return false;
  if (value.length < 2 || value.length > 100) return false;

  return /^\p{L}+(?:\s+\p{L}+)*$/u.test(value);
};
const formatMoney = (value: number): string => {
  return new Intl.NumberFormat("vi-VN").format(Number(value || 0));
};

const getBackendMessage = (error: any, fallback: string): string => {
  const data = error?.response?.data;

  if (!data) return fallback;
  if (typeof data === "string") return data;
  if (data.message) return String(data.message);
  if (data.detail) return String(data.detail);
  if (data.error) return String(data.error);

  if (data.errors && typeof data.errors === "object") {
    const firstError = Object.values(data.errors)[0];

    if (Array.isArray(firstError) && firstError.length > 0) {
      return String(firstError[0]);
    }

    if (firstError) {
      return String(firstError);
    }
  }

  return fallback;
};

const toDateOnly = (value?: string | null): string | null => {
  if (!value) return null;
  return String(value).substring(0, 10);
};

const isDateBeforeToday = (value?: string | null): boolean => {
  const dateOnly = toDateOnly(value);
  if (!dateOnly) return false;

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const date = new Date(`${dateOnly}T00:00:00`);
  return !Number.isNaN(date.getTime()) && date.getTime() < today.getTime();
};

const isDateAfterToday = (value?: string | null): boolean => {
  const dateOnly = toDateOnly(value);
  if (!dateOnly) return false;

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const date = new Date(`${dateOnly}T00:00:00`);
  return !Number.isNaN(date.getTime()) && date.getTime() > today.getTime();
};

const buildVariantText = (variant: any): string => {
  const parts = [
    variant.capacityLabel,
    variant.capacityValue ? `${variant.capacityValue} ml` : "",
    variant.bottleTypeName,
  ].filter(Boolean);

  return parts.length > 0 ? parts.join(" - ") : "Biến thể mặc định";
};

const getProductUnavailableReason = (
  product?: PosProduct | null
): string | null => {
  if (!product) return "Sản phẩm không hợp lệ.";
  if (product.unavailableReason) return product.unavailableReason;
  if (product.sellable === false) return "Sản phẩm hiện không được bán.";

  if (product.status != null && Number(product.status) !== 1) {
    return "Sản phẩm đang ngừng bán.";
  }

  if (Number(product.stockQuantity || 0) <= 0) {
    return `Sản phẩm ${product.name} đã hết hàng trong kho!`;
  }

  if (isDateAfterToday(product.manufacturingDate)) {
    return `Sản phẩm ${product.name} chưa tới ngày được bán.`;
  }

  if (product.expired || isDateBeforeToday(product.expirationDate)) {
    return `Sản phẩm ${product.name} đã hết hạn sử dụng.`;
  }

  return null;
};

const mapPosProductFromBackend = (
  raw: any,
  parent: any = {},
  fallbackImage = ""
): PosProduct => {
  const productName = String(
    raw.productName ||
      raw.name ||
      parent.productName ||
      parent.name ||
      "Sản phẩm"
  );

  const image =
    raw.imageUrl ||
    parent.imageUrl ||
    fallbackImage ||
    `https://ui-avatars.com/api/?name=${encodeURIComponent(
      productName || "Product"
    )}&background=random&color=fff&size=200`;

  const manufacturingDate = toDateOnly(raw.manufacturingDate);
  const expirationDate = toDateOnly(raw.expirationDate);
  const status = raw.status ?? raw.variantStatus ?? null;
  const stockQuantity = Number(raw.stockQuantity ?? raw.stock ?? 0);
  const expired = Boolean(raw.expired ?? isDateBeforeToday(expirationDate));

  let sellable = raw.sellable;

  if (sellable == null) {
    sellable =
      Number(status ?? 1) === 1 &&
      stockQuantity > 0 &&
      !expired &&
      !isDateAfterToday(manufacturingDate);
  }

  return {
    id: Number(
      raw.variantId || raw.productVariantId || raw.id || parent.id || 0
    ),
    sku: String(raw.sku || ""),
    name: productName,
    subName: buildVariantText(raw),
    price: Number(raw.price || raw.unitPrice || 0),
    stockQuantity,
    image,
    category:
      raw.brandName ||
      raw.categoryName ||
      parent.brandName ||
      parent.categoryName ||
      parent.category ||
      "Tất cả",
    manufacturingDate,
    expirationDate,
    status,
    expired,
    sellable: Boolean(sellable),
    unavailableReason: raw.unavailableReason || null,
  };
};

const numberValue = (...values: any[]): number => {
  for (const value of values) {
    if (value !== undefined && value !== null && value !== "") {
      const number = Number(value);

      if (!Number.isNaN(number)) {
        return number;
      }
    }
  }

  return 0;
};

const getVoucherCode = (raw: any): string => {
  return String(raw?.code || raw?.voucherCode || raw?.voucher_code || "")
    .trim()
    .toUpperCase();
};

const calculateVoucherDiscountAmount = (
  raw: any,
  orderTotal: number
): number => {
  const total = Math.max(Number(orderTotal || 0), 0);
  const responseDiscountAmount = numberValue(raw?.discountAmount);

  if (responseDiscountAmount > 0) {
    return Math.min(responseDiscountAmount, total);
  }

  const discountType = String(raw?.discountType || raw?.type || "")
    .trim()
    .toUpperCase();

  const discountValue = numberValue(
    raw?.discountValue,
    raw?.value,
    raw?.discount,
    raw?.discountPercent
  );

  const maxDiscountAmount = numberValue(
    raw?.maxDiscountAmount,
    raw?.maxDiscount,
    raw?.maximumDiscountAmount
  );

  let discountAmount = 0;

  if (discountType === "PERCENT" || discountType === "PERCENTAGE") {
    discountAmount = Math.round((total * discountValue) / 100);
  } else {
    discountAmount = discountValue;
  }

  if (maxDiscountAmount > 0) {
    discountAmount = Math.min(discountAmount, maxDiscountAmount);
  }

  return Math.max(Math.min(discountAmount, total), 0);
};

const mapPosVoucherFromBackend = (raw: any, orderTotal: number): PosVoucher => {
  const code = getVoucherCode(raw);
  const discountType = String(raw?.discountType || raw?.type || "")
    .trim()
    .toUpperCase();

  const discountValue = numberValue(
    raw?.discountValue,
    raw?.value,
    raw?.discount
  );
  const discountPercent = numberValue(raw?.discountPercent);

  const maxDiscountAmount = numberValue(
    raw?.maxDiscountAmount,
    raw?.maxDiscount,
    raw?.maximumDiscountAmount
  );

  const minOrderAmount = numberValue(
    raw?.minOrderAmount,
    raw?.minOrderValue,
    raw?.minimumOrderValue
  );

  const discountAmount = calculateVoucherDiscountAmount(raw, orderTotal);
  const remainingQuantityRaw =
    raw?.remainingQuantity ?? raw?.quantityLeft ?? raw?.remaining;

  const eligible =
    raw?.eligible === undefined || raw?.eligible === null
      ? Number(orderTotal || 0) >= minOrderAmount && discountAmount > 0
      : Boolean(raw.eligible);

  return {
    id: raw?.id ?? raw?.voucherId,
    code,
    name: String(raw?.name || raw?.voucherName || code || "Voucher"),
    description: raw?.description || raw?.note || undefined,
    discountType,
    discountValue,
    discountPercent:
      discountPercent > 0
        ? discountPercent
        : discountType === "PERCENT" || discountType === "PERCENTAGE"
        ? discountValue
        : 0,
    maxDiscountAmount,
    minOrderAmount,
    discountAmount: eligible ? discountAmount : 0,
    startDate: raw?.startDate || raw?.startAt || null,
    endDate: raw?.endDate || raw?.endAt || null,
    remainingQuantity:
      remainingQuantityRaw === undefined || remainingQuantityRaw === null
        ? null
        : Number(remainingQuantityRaw),
    eligible,
    ineligibleReason:
      raw?.ineligibleReason ||
      raw?.reason ||
      (!eligible && minOrderAmount > Number(orderTotal || 0)
        ? `Đơn tối thiểu ${formatMoney(minOrderAmount)} ₫ để dùng voucher này.`
        : null),
  };
};

const getOptionName = (raw: any): string => {
  return String(
    raw?.name ||
      raw?.label ||
      raw?.capacityLabel ||
      raw?.bottleTypeName ||
      raw?.brandName ||
      raw?.value ||
      ""
  ).trim();
};

const mapFilterOption = (raw: any): PosFilterOption | null => {
  const name = getOptionName(raw);

  if (!name) {
    return null;
  }

  return {
    id: raw?.id ?? raw?.brandId ?? raw?.capacityId ?? raw?.bottleTypeId,
    name,
    label: raw?.label || name,
    value: raw?.value ?? name,
  };
};

const extractArrayFromResponse = (payload: any): any[] => {
  const candidates = [
    payload,
    payload?.data,
    payload?.content,
    payload?.items,
    payload?.data?.content,
    payload?.data?.items,
    payload?.result,
    payload?.result?.content,
    payload?.result?.items,
  ];

  for (const candidate of candidates) {
    if (Array.isArray(candidate)) {
      return candidate;
    }
  }

  return [];
};

const mapFilterOptions = (payload: any): PosFilterOption[] => {
  return extractArrayFromResponse(payload)
    .map(mapFilterOption)
    .filter((item: PosFilterOption | null): item is PosFilterOption => !!item);
};

const fetchFirstAvailable = async (urls: string[]) => {
  let lastError: any = null;

  for (const url of urls) {
    try {
      return await api.get(url);
    } catch (error: any) {
      lastError = error;
    }
  }

  throw lastError;
};

const sortVouchersByBest = (vouchers: PosVoucher[]): PosVoucher[] => {
  return [...vouchers].sort((a, b) => {
    if (a.eligible !== b.eligible) {
      return a.eligible ? -1 : 1;
    }

    const discountDiff =
      Number(b.discountAmount || 0) - Number(a.discountAmount || 0);

    if (discountDiff !== 0) {
      return discountDiff;
    }

    const minOrderDiff =
      Number(a.minOrderAmount || 0) - Number(b.minOrderAmount || 0);

    if (minOrderDiff !== 0) {
      return minOrderDiff;
    }

    return String(a.code || "").localeCompare(String(b.code || ""));
  });
};

export const usePosStore = defineStore("posStore", {
  state: (): PosStoreState => ({
    allProducts: [],
    categories: ["Tất cả"],
    brandOptions: [],
    capacityOptions: [],
    bottleTypeOptions: [],
    selectedCategory: "Tất cả",
    searchQuery: "",
    availableVouchers: [],
    voucherLoading: false,
    cart: [],
    customer: null,
    customerSavedKey: "",

    voucherCode: "",
    discountAmount: 0,

    paymentMethod: "CASH",
    transferProvider: "",
    cashPaid: 0,

    vnpayUrl: "",
    vietQrImageUrl: "",
    vietQrContent: "",
    pendingVietQrOrderId: null,
    pendingVietQrAmount: 0,
    activePendingPaymentOrderId: null,
    activePendingPaymentTransferProvider: "",
    isLoading: false,
    errorMsg: "",
    lastOrderId: null,

    heldOrders: [],
    transferTargets: [],

    activeHeldOrderId: null,
    activeHeldOrderCashierName: "",
    activeHeldOrderOwnOrder: true,
    activeHeldOrderCanOpen: true,
    activeHeldOrderCanCheckout: true,
    activeHeldOrderCanTransfer: true,
    activeHeldOrderCanCancel: true,
    showHeldOrdersPanel: false,
  }),

  getters: {
    filteredProducts(state): PosProduct[] {
      const keyword = state.searchQuery.trim().toLowerCase();

      return state.allProducts.filter((product) => {
        const matchesCategory =
          state.selectedCategory === "Tất cả" ||
          product.category === state.selectedCategory;

        const matchesSearch =
          !keyword ||
          product.name.toLowerCase().includes(keyword) ||
          product.sku.toLowerCase().includes(keyword) ||
          (product.subName || "").toLowerCase().includes(keyword) ||
          (product.category || "").toLowerCase().includes(keyword);

        return matchesCategory && matchesSearch;
      });
    },

    totalAmount(state): number {
      return state.cart.reduce((total, item) => {
        return total + item.product.price * item.quantity;
      }, 0);
    },

    finalAmount(): number {
      const final = this.totalAmount - this.discountAmount;
      return final > 0 ? final : 0;
    },

    remainingAmount(): number {
      const remaining = this.finalAmount - this.cashPaid;
      return remaining > 0 ? remaining : 0;
    },

    hasPartialCashPayment(state): boolean {
      return state.cashPaid > 0;
    },

    isHeldOrderLoaded(state): boolean {
      return state.activeHeldOrderId !== null;
    },

    isOrderLocked(state): boolean {
      return state.cashPaid > 0 || state.activePendingPaymentOrderId !== null;
    },

    isCustomerLocked(state): boolean {
      return (
        state.cashPaid > 0 ||
        state.activeHeldOrderId !== null ||
        state.activePendingPaymentOrderId !== null
      );
    },

    isCustomerSavedForCurrentInfo(state): boolean {
      if (!state.customer) return false;

      return (
        !!state.customerSavedKey &&
        state.customerSavedKey === buildCustomerSavedKey(state.customer)
      );
    },
  },

  actions: {
    clearVoucherLocal(silent = false) {
      const hadVoucher = Boolean(this.voucherCode || this.discountAmount > 0);

      this.voucherCode = "";
      this.discountAmount = 0;

      if (!silent && hadVoucher) {
        this.errorMsg = "Đã xóa mã giảm giá.";
      }
    },

    clearVoucher() {
      if (this.cashPaid > 0 || this.activePendingPaymentOrderId) {
        this.errorMsg =
          "Đơn đã nhận tiền/tạo thanh toán online, không được đổi mã giảm giá.";
        return;
      }

      this.clearVoucherLocal(false);
    },

    handleVoucherTyping() {
      if (this.discountAmount > 0) {
        this.discountAmount = 0;
      }
    },

    async fetchAvailableVouchers() {
      const total = Number(this.totalAmount || 0);

      if (
        this.cart.length === 0 ||
        total <= 0 ||
        this.cashPaid > 0 ||
        this.activePendingPaymentOrderId
      ) {
        this.availableVouchers = [];
        return [];
      }

      this.voucherLoading = true;

      try {
        const { data } = await api.get("/admin/vouchers/available", {
          params: {
            orderTotal: total,
          },
        });

        const rawVouchers = Array.isArray(data)
          ? data
          : data?.data || data?.content || data?.vouchers || data?.items || [];

        const mappedVouchers = rawVouchers
          .map((item: any) => mapPosVoucherFromBackend(item, total))
          .filter((item: PosVoucher) => !!item.code);

        this.availableVouchers = sortVouchersByBest(mappedVouchers);

        return this.availableVouchers;
      } catch (error: any) {
        this.availableVouchers = [];
        this.errorMsg = getBackendMessage(
          error,
          "Không thể tải danh sách voucher khả dụng."
        );
        return false;
      } finally {
        this.voucherLoading = false;
      }
    },

    async applyVoucher() {
      this.errorMsg = "";

      if (this.cashPaid > 0 || this.activePendingPaymentOrderId) {
        this.errorMsg =
          "Đơn đã nhận tiền/tạo thanh toán online, không được áp mã giảm giá.";
        return false;
      }

      if (this.cart.length === 0) {
        this.errorMsg = "Giỏ hàng chưa có sản phẩm để áp mã giảm giá.";
        return false;
      }

      const cleanCode = normalizeText(this.voucherCode).toUpperCase();

      if (!cleanCode) {
        this.errorMsg = "Vui lòng nhập mã giảm giá.";
        return false;
      }

      this.isLoading = true;
      this.errorMsg = "";

      try {
        const { data } = await api.get("/admin/vouchers/apply", {
          params: {
            code: cleanCode,
            orderTotal: this.totalAmount,
          },
        });

        this.voucherCode = String(data?.voucherCode || data?.code || cleanCode)
          .trim()
          .toUpperCase();

        this.discountAmount = Number(data?.discountAmount || 0);
        this.errorMsg = "Áp voucher thành công.";

        return {
          success: true,
          data,
        };
      } catch (error: any) {
        this.discountAmount = 0;
        this.errorMsg = getBackendMessage(
          error,
          "Mã giảm giá không hợp lệ hoặc không đủ điều kiện áp dụng."
        );
        return false;
      } finally {
        this.isLoading = false;
      }
    },
    savePendingCheckoutDraft() {
      const draft = {
        cart: this.cart,
        customer: this.customer,
        customerSavedKey: this.customerSavedKey,
        voucherCode: this.voucherCode,
        discountAmount: this.discountAmount,
        availableVouchers: this.availableVouchers,
        paymentMethod: this.paymentMethod,
        transferProvider: this.transferProvider,
        cashPaid: this.cashPaid,
        pendingVietQrOrderId: this.pendingVietQrOrderId,
        pendingVietQrAmount: this.pendingVietQrAmount,
        activePendingPaymentOrderId: this.activePendingPaymentOrderId,
        activePendingPaymentTransferProvider:
          this.activePendingPaymentTransferProvider,
        lastOrderId: this.lastOrderId,
        activeHeldOrderId: this.activeHeldOrderId,
        activeHeldOrderCashierName: this.activeHeldOrderCashierName,
        savedAt: new Date().toISOString(),
      };

      sessionStorage.setItem(
        "pos_pending_checkout_draft",
        JSON.stringify(draft)
      );
    },

    restorePendingCheckoutDraft() {
      /*
       * Draft dùng cho đơn đang chờ thanh toán online.
       * Không để đơn pending payment bị hiểu nhầm là phiếu treo HOLD.
       */
      if (this.activeHeldOrderId) {
        return false;
      }

      const raw = sessionStorage.getItem("pos_pending_checkout_draft");

      if (!raw) {
        return false;
      }

      try {
        const draft = JSON.parse(raw);

        this.cart = Array.isArray(draft.cart) ? draft.cart : [];
        this.customer = draft.customer || null;
        this.customerSavedKey = draft.customerSavedKey || "";

        this.voucherCode = draft.voucherCode || "";
        this.discountAmount = Number(draft.discountAmount || 0);
        this.availableVouchers = Array.isArray(draft.availableVouchers)
          ? draft.availableVouchers
          : [];

        this.paymentMethod = draft.paymentMethod || "CASH";
        this.transferProvider = draft.transferProvider || "";
        this.cashPaid = Number(draft.cashPaid || 0);
        this.pendingVietQrOrderId = draft.pendingVietQrOrderId || null;
        this.pendingVietQrAmount = Number(draft.pendingVietQrAmount || 0);

        this.lastOrderId = draft.lastOrderId || null;
        this.activePendingPaymentOrderId =
          draft.activePendingPaymentOrderId ||
          draft.pendingVietQrOrderId ||
          draft.lastOrderId ||
          null;
        this.activePendingPaymentTransferProvider =
          draft.activePendingPaymentTransferProvider ||
          draft.transferProvider ||
          (draft.paymentMethod === "VNPAY" ? "VNPAY" : "");

        this.activeHeldOrderId = this.activePendingPaymentOrderId
          ? null
          : draft.activeHeldOrderId || null;
        this.activeHeldOrderCashierName = this.activePendingPaymentOrderId
          ? ""
          : draft.activeHeldOrderCashierName || "";

        this.errorMsg =
          "Đã khôi phục hóa đơn đang chờ thanh toán. Nếu khách chưa thanh toán, có thể chọn lại phương thức hoặc thanh toán lại.";

        return true;
      } catch (error) {
        sessionStorage.removeItem("pos_pending_checkout_draft");
        return false;
      }
    },

    clearPendingCheckoutDraft() {
      sessionStorage.removeItem("pos_pending_checkout_draft");
    },
    resetLocalOrderOnly() {
      this.cart = [];
      this.customer = null;
      this.customerSavedKey = "";
      this.voucherCode = "";
      this.discountAmount = 0;
      this.availableVouchers = [];
      this.voucherLoading = false;
      this.paymentMethod = "CASH";
      this.transferProvider = "";
      this.cashPaid = 0;
      this.vnpayUrl = "";
      this.vietQrImageUrl = "";
      this.vietQrContent = "";
      this.pendingVietQrOrderId = null;
      this.pendingVietQrAmount = 0;
      this.activePendingPaymentOrderId = null;
      this.activePendingPaymentTransferProvider = "";
      this.errorMsg = "";
      this.lastOrderId = null;
      this.activeHeldOrderId = null;
      this.activeHeldOrderCashierName = "";
      this.activeHeldOrderOwnOrder = true;
      this.activeHeldOrderCanOpen = true;
      this.activeHeldOrderCanCheckout = true;
      this.activeHeldOrderCanTransfer = true;
      this.activeHeldOrderCanCancel = true;

      this.clearPendingCheckoutDraft();
    },

    async fetchPosFilters() {
      /*
       * Bộ lọc lấy từ API danh mục thật.
       * Nếu API trả format khác nhau thì vẫn parse được.
       * Nếu lỗi thì im lặng fallback sang dữ liệu lấy từ /admin/pos/products.
       */
      const [brandRes, capacityRes, bottleTypeRes] = await Promise.allSettled([
        fetchFirstAvailable(["/brands", "/admin/brands"]),
        fetchFirstAvailable(["/capacities", "/admin/capacities"]),
        fetchFirstAvailable(["/bottle-types", "/admin/bottle-types"]),
      ]);

      if (brandRes.status === "fulfilled") {
        this.brandOptions = mapFilterOptions(brandRes.value.data);

        if (this.brandOptions.length > 0) {
          this.categories = [
            "Tất cả",
            ...this.brandOptions.map((item) => item.name),
          ];
        }
      }

      if (capacityRes.status === "fulfilled") {
        this.capacityOptions = mapFilterOptions(capacityRes.value.data);
      }

      if (bottleTypeRes.status === "fulfilled") {
        this.bottleTypeOptions = mapFilterOptions(bottleTypeRes.value.data);
      }

      /*
       * Không set errorMsg ở đây.
       * Vì nếu filter API fail hoặc parse ra rỗng, ProductGrid vẫn fallback từ sản phẩm.
       */
    },

    async fetchProducts() {
      this.isLoading = true;
      this.errorMsg = "";

      try {
        const { data } = await api.get("/admin/pos/products");

        const rawProducts = Array.isArray(data)
          ? data
          : data?.data || data?.content || [];

        const flatProducts: PosProduct[] = rawProducts.map((item: any) =>
          mapPosProductFromBackend(item, item, item.imageUrl)
        );

        this.allProducts = flatProducts;

        /*
         * Nếu đã tải brand từ API thật thì giữ nguyên danh sách brand API.
         * Nếu API filter lỗi/chưa có thì fallback như logic cũ: build brand từ sản phẩm POS.
         */
        if (this.brandOptions.length === 0) {
          this.categories = [
            "Tất cả",
            ...new Set(
              flatProducts
                .map((p) => p.category)
                .filter((category) => !!category)
            ),
          ];
        }
      } catch (error: any) {
        this.allProducts = [];
        this.categories = ["Tất cả"];
        this.errorMsg = getBackendMessage(
          error,
          "Không thể tải danh sách sản phẩm POS từ máy chủ!"
        );
      } finally {
        this.isLoading = false;
      }
    },

    async fetchHeldOrders() {
      try {
        const { data } = await api.get("/admin/pos/held-orders");
        this.heldOrders = Array.isArray(data) ? data : [];
      } catch (error: any) {
        this.errorMsg = getBackendMessage(
          error,
          "Không thể tải danh sách phiếu treo."
        );
      }
    },

    async fetchTransferTargets() {
      this.isLoading = true;
      this.errorMsg = "";

      try {
        const { data } = await api.get("/admin/pos/transfer-targets");
        this.transferTargets = Array.isArray(data) ? data : [];
      } catch (error: any) {
        this.transferTargets = [];
        this.errorMsg = getBackendMessage(
          error,
          "Không thể tải danh sách nhân viên nhận phiếu."
        );
      } finally {
        this.isLoading = false;
      }
    },

    getCartItemBySku(sku?: string | null): CartItem | null {
      const cleanSku = normalizeText(sku);

      if (!cleanSku) {
        return null;
      }

      return (
        this.cart.find(
          (item) =>
            String(item.product?.sku || "").toLowerCase() ===
            cleanSku.toLowerCase()
        ) || null
      );
    },

    isProductInCart(sku?: string | null): boolean {
      return this.getCartItemBySku(sku) !== null;
    },

    toggleProductSelection(product: PosProduct) {
      this.errorMsg = "";

      if (this.cashPaid > 0 || this.activePendingPaymentOrderId) {
        this.errorMsg =
          "Đơn đã nhận tiền/tạo thanh toán online, không được thêm/xóa sản phẩm.";
        return;
      }

      if (!product || !product.sku) {
        this.errorMsg = "Sản phẩm không hợp lệ.";
        return;
      }

      const existingItem = this.getCartItemBySku(product.sku);

      if (existingItem) {
        this.removeFromCart(product.sku);
        return;
      }

      this.addToCart(product);
    },

    addToCart(product: PosProduct) {
      this.errorMsg = "";

      if (this.cashPaid > 0 || this.activePendingPaymentOrderId) {
        this.errorMsg =
          "Đơn đã nhận tiền/tạo thanh toán online, không được thêm sản phẩm.";
        return;
      }

      if (!product || !product.sku) {
        this.errorMsg = "Sản phẩm không hợp lệ.";
        return;
      }

      const unavailableReason = getProductUnavailableReason(product);

      if (unavailableReason) {
        this.errorMsg = unavailableReason;
        return;
      }

      const existingItem = this.cart.find(
        (item) => item.product.sku === product.sku
      );

      if (existingItem) {
        if (existingItem.quantity >= product.stockQuantity) {
          this.errorMsg = `Sản phẩm ${product.name} đã đạt giới hạn tồn kho tối đa (${product.stockQuantity})!`;
          return;
        }

        existingItem.quantity++;
        this.clearVoucherLocal(true);
        return;
      }

      this.cart.push({
        product,
        quantity: 1,
      });

      this.clearVoucherLocal(true);
    },

    async handleBarcodeScan(sku: string) {
      this.errorMsg = "";

      if (this.cashPaid > 0 || this.activePendingPaymentOrderId) {
        this.errorMsg =
          "Đơn đã nhận tiền/tạo thanh toán online, không được quét thêm sản phẩm.";
        return;
      }

      const cleanSku = normalizeText(sku);

      if (!cleanSku) return;

      const localProduct = this.allProducts.find(
        (p) => p.sku.toLowerCase() === cleanSku.toLowerCase()
      );

      if (localProduct) {
        this.addToCart(localProduct);
        return;
      }

      try {
        const { data } = await api.get("/admin/pos/product", {
          params: {
            sku: cleanSku,
          },
        });

        const mappedProduct = mapPosProductFromBackend(
          {
            ...data,
            sku: data.sku || cleanSku,
          },
          data,
          data.imageUrl
        );

        this.addToCart(mappedProduct);
      } catch (error: any) {
        this.errorMsg = getBackendMessage(
          error,
          `Mã vạch SKU "${cleanSku}" không tồn tại hoặc không bán được!`
        );
      }
    },

    updateQuantity(sku: string, qty: number) {
      this.errorMsg = "";

      if (this.cashPaid > 0 || this.activePendingPaymentOrderId) {
        this.errorMsg =
          "Đơn đã nhận tiền/tạo thanh toán online, không được sửa số lượng.";
        return;
      }

      const item = this.cart.find((i) => i.product.sku === sku);

      if (!item) return;

      const unavailableReason = getProductUnavailableReason(item.product);

      if (unavailableReason) {
        this.errorMsg = unavailableReason;
        return;
      }

      if (qty <= 0) {
        this.removeFromCart(sku);
        return;
      }

      if (qty > item.product.stockQuantity) {
        this.errorMsg = `Không thể chỉnh sửa. Kho chỉ còn tối đa ${item.product.stockQuantity} sản phẩm!`;
        item.quantity = item.product.stockQuantity;
        this.clearVoucherLocal(true);
        return;
      }

      item.quantity = qty;
      this.clearVoucherLocal(true);
    },

    removeFromCart(sku: string) {
      this.errorMsg = "";

      if (this.cashPaid > 0 || this.activePendingPaymentOrderId) {
        this.errorMsg =
          "Đơn đã nhận tiền/tạo thanh toán online, không được xóa sản phẩm.";
        return;
      }

      this.cart = this.cart.filter((item) => item.product.sku !== sku);
      this.clearVoucherLocal(true);
    },

    markCustomerUnsaved() {
      if (this.activeHeldOrderId || this.activePendingPaymentOrderId) return;
      this.customerSavedKey = "";
    },

    markCustomerSaved(customer?: PosCustomer | null) {
      const targetCustomer = customer || this.customer;

      if (!targetCustomer) {
        this.customerSavedKey = "";
        return;
      }

      this.customerSavedKey = buildCustomerSavedKey(targetCustomer);
    },

    async searchCustomer(phone: string) {
      const cleanPhone = normalizePhone(phone);

      if (this.activeHeldOrderId || this.activePendingPaymentOrderId) {
        this.errorMsg = this.activePendingPaymentOrderId
          ? "Hóa đơn đang chờ thanh toán, không được đổi khách hàng."
          : "Đang mở phiếu treo, không được đổi khách hàng.";
        return;
      }

      if (this.cashPaid > 0 || this.activePendingPaymentOrderId) {
        this.errorMsg =
          "Đơn đã nhận tiền/tạo thanh toán online, không được đổi khách hàng.";
        return;
      }

      if (!cleanPhone) {
        this.customer = null;
        this.customerSavedKey = "";
        this.errorMsg = "Vui lòng nhập số điện thoại khách hàng.";
        return;
      }

      if (!isValidVietnamPhone(cleanPhone)) {
        this.customer = null;
        this.customerSavedKey = "";
        this.errorMsg =
          "Số điện thoại không hợp lệ. Vui lòng nhập 10 số, bắt đầu bằng 03, 05, 07, 08 hoặc 09.";
        return;
      }

      this.isLoading = true;
      this.errorMsg = "";

      try {
        const { data } = await api.get("/admin/pos/customer", {
          params: {
            phone: cleanPhone,
          },
        });

        if (data && data.found && data.customer) {
          this.customer = {
            customerId: data.customer.customerId,
            name: data.customer.name || "",
            phone: data.customer.phone || cleanPhone,
            email: data.customer.email || "",
            customerRank: data.customer.customerRank || "BRONZE",
            loyaltyPoints: Number(data.customer.loyaltyPoints || 0),
          };

          this.markCustomerSaved();
          this.errorMsg = "";
          return;
        }

        this.customer = {
          name: "",
          phone: cleanPhone,
          email: "",
          customerRank: "BRONZE",
          loyaltyPoints: 0,
        };

        this.customerSavedKey = "";
        this.errorMsg =
          data?.message ||
          "Không tìm thấy khách hàng. Vui lòng nhập họ tên và email. Hệ thống sẽ lưu khách khi treo phiếu hoặc thanh toán.";
      } catch (error: any) {
        this.customer = null;
        this.customerSavedKey = "";
        this.errorMsg = getBackendMessage(
          error,
          "Không thể kiểm tra số điện thoại khách hàng."
        );
      } finally {
        this.isLoading = false;
      }
    },

    async saveCustomerForPos() {
      this.errorMsg = "";

      if (this.activeHeldOrderId || this.activePendingPaymentOrderId) {
        this.errorMsg = this.activePendingPaymentOrderId
          ? "Hóa đơn đang chờ thanh toán, không được đổi/lưu khách hàng."
          : "Đang mở phiếu treo, không được đổi/lưu khách hàng.";
        return false;
      }

      if (this.cashPaid > 0 || this.activePendingPaymentOrderId) {
        this.errorMsg =
          "Đơn đã nhận tiền/tạo thanh toán online, không được đổi/lưu khách hàng.";
        return false;
      }

      if (!this.customer) {
        this.errorMsg = "Vui lòng nhập thông tin khách hàng.";
        return false;
      }

      const cleanPhone = normalizePhone(this.customer.phone);
      const customerName = normalizeText(this.customer.name);
      const customerEmail = normalizeEmail(this.customer.email);

      if (!cleanPhone) {
        this.errorMsg = "Số điện thoại khách hàng không được để trống.";
        return false;
      }

      if (!isValidVietnamPhone(cleanPhone)) {
        this.errorMsg =
          "Số điện thoại khách hàng không hợp lệ. Vui lòng kiểm tra lại.";
        return false;
      }

      if (!customerName) {
        this.errorMsg = "Họ tên khách hàng không được để trống.";
        return false;
      }

      if (customerName.length < 2 || customerName.length > 100) {
        this.errorMsg = "Họ tên khách hàng phải từ 2 đến 100 ký tự.";
        return false;
      }

      if (!isValidCustomerName(customerName)) {
        this.errorMsg =
          "Họ tên chỉ được chứa chữ cái và khoảng trắng, không được chứa số hoặc ký tự đặc biệt.";
        return false;
      }

      if (!customerEmail) {
        this.errorMsg = "Email khách hàng không được để trống.";
        return false;
      }

      if (!isValidEmail(customerEmail)) {
        this.errorMsg = "Email khách hàng không đúng định dạng.";
        return false;
      }

      this.isLoading = true;
      this.errorMsg = "";

      try {
        const { data } = await api.post("/admin/pos/customer", {
          phone: cleanPhone,
          name: customerName,
          email: customerEmail,
        });

        const savedCustomer = data?.customer;

        this.customer = {
          customerId: savedCustomer?.customerId,
          name: savedCustomer?.name || customerName,
          phone: savedCustomer?.phone || cleanPhone,
          email: savedCustomer?.email || customerEmail,
          customerRank: savedCustomer?.customerRank || "BRONZE",
          loyaltyPoints: Number(savedCustomer?.loyaltyPoints || 0),
        };

        this.markCustomerSaved();
        this.errorMsg = data?.message || "Đã lưu thông tin khách hàng.";

        return {
          success: true,
          data,
        };
      } catch (error: any) {
        this.customerSavedKey = "";
        this.errorMsg = getBackendMessage(
          error,
          "Lưu khách hàng thất bại. Vui lòng kiểm tra lại thông tin."
        );

        return false;
      } finally {
        this.isLoading = false;
      }
    },

    validateCustomerForCheckout(): boolean {
      if (!this.customer) {
        this.errorMsg =
          "Vui lòng nhập thông tin khách hàng trước khi thanh toán.";
        return false;
      }

      const cleanPhone = normalizePhone(this.customer.phone);
      const customerName = normalizeText(this.customer.name);
      const customerEmail = normalizeEmail(this.customer.email);

      if (!cleanPhone) {
        this.errorMsg = "Số điện thoại khách hàng không được để trống.";
        return false;
      }

      if (!isValidVietnamPhone(cleanPhone)) {
        this.errorMsg =
          "Số điện thoại khách hàng không hợp lệ. Vui lòng kiểm tra lại.";
        return false;
      }

      if (!customerName) {
        this.errorMsg = "Họ tên khách hàng không được để trống.";
        return false;
      }

      if (customerName.length < 2 || customerName.length > 100) {
        this.errorMsg = "Họ tên khách hàng phải từ 2 đến 100 ký tự.";
        return false;
      }

      if (!isValidCustomerName(customerName)) {
        this.errorMsg =
          "Họ tên chỉ được chứa chữ cái và khoảng trắng, không được chứa số hoặc ký tự đặc biệt.";
        return false;
      }

      if (!customerEmail) {
        this.errorMsg = "Email khách hàng không được để trống.";
        return false;
      }

      if (!isValidEmail(customerEmail)) {
        this.errorMsg = "Email khách hàng không đúng định dạng.";
        return false;
      }

      this.customer.phone = cleanPhone;
      this.customer.name = customerName;
      this.customer.email = customerEmail;

      this.errorMsg = "";

      return true;
    },

    validateCartCanCheckout(): boolean {
      if (this.cart.length === 0) {
        this.errorMsg = "Giỏ hàng đang trống.";
        return false;
      }

      const invalidItem = this.cart.find((item) =>
        Boolean(getProductUnavailableReason(item.product))
      );

      if (invalidItem) {
        this.errorMsg =
          getProductUnavailableReason(invalidItem.product) ||
          "Giỏ hàng có sản phẩm không đủ điều kiện bán.";
        return false;
      }

      return true;
    },

    registerPartialCashPayment(amount: number) {
      this.errorMsg = "";

      const money = Number(amount || 0);

      if (!this.validateCartCanCheckout()) return false;
      if (!this.validateCustomerForCheckout()) return false;

      if (money <= 0) {
        this.errorMsg = "Tiền khách đưa phải lớn hơn 0.";
        return false;
      }

      if (money >= this.remainingAmount) {
        this.errorMsg =
          "Số tiền đã đủ thanh toán, vui lòng hoàn tất bằng tiền mặt.";
        return false;
      }

      this.cashPaid += money;
      this.paymentMethod = "VIETQR";
      this.transferProvider = "VIETQR";

      this.errorMsg = `Đã nhận tiền mặt ${formatMoney(
        this.cashPaid
      )} ₫. Còn thiếu ${formatMoney(
        this.remainingAmount
      )} ₫, vui lòng chọn VNPay hoặc VietQR để thanh toán phần còn lại.`;

      return true;
    },

    buildCheckoutPayload(
      selectedPaymentMethod: PosCheckoutPaymentMethod,
      cashGiven: number,
      transferAmount: number,
      transferProvider: PosTransferProvider = ""
    ) {
      const cleanTransferProvider =
        selectedPaymentMethod === "MIXED"
          ? transferProvider || "VIETQR"
          : selectedPaymentMethod === "VNPAY"
          ? "VNPAY"
          : selectedPaymentMethod === "VIETQR"
          ? "VIETQR"
          : null;

      return {
        paymentMethod: selectedPaymentMethod,
        transferProvider:
          selectedPaymentMethod === "MIXED" ? cleanTransferProvider : null,

        customerPhone: normalizePhone(this.customer?.phone),
        customerName: normalizeText(this.customer?.name),
        customerEmail: normalizeEmail(this.customer?.email),
        voucherCode: normalizeText(this.voucherCode) || null,

        cashGiven:
          selectedPaymentMethod === "CASH" || selectedPaymentMethod === "MIXED"
            ? cashGiven
            : null,

        transferAmount:
          selectedPaymentMethod === "VNPAY" ||
          selectedPaymentMethod === "VIETQR" ||
          selectedPaymentMethod === "MIXED"
            ? transferAmount || null
            : null,

        items: this.cart.map((item) => ({
          sku: item.product.sku,
          quantity: Number(item.quantity),
        })),
      };
    },

    buildCurrentOrderPayload() {
      return {
        customerPhone: normalizePhone(this.customer?.phone),
        customerName: normalizeText(this.customer?.name),
        customerEmail: normalizeEmail(this.customer?.email),
        voucherCode: normalizeText(this.voucherCode) || null,
        items: this.cart.map((item) => ({
          sku: item.product.sku,
          quantity: Number(item.quantity),
        })),
      };
    },

    validatePaymentInput(
      selectedPaymentMethod: PosCheckoutPaymentMethod,
      cashGiven: number,
      transferAmount: number
    ) {
      if (selectedPaymentMethod === "CASH") {
        if (cashGiven <= 0) {
          this.errorMsg = "Vui lòng nhập số tiền khách đưa.";
          return false;
        }

        if (transferAmount > 0) {
          this.errorMsg =
            "Thanh toán tiền mặt không được gửi tiền chuyển khoản.";
          return false;
        }

        if (cashGiven < this.finalAmount) {
          this.errorMsg =
            "Tiền khách đưa chưa đủ. Vui lòng chọn thêm phương thức thanh toán.";
          return false;
        }
      }

      if (selectedPaymentMethod === "VNPAY") {
        if (cashGiven > 0) {
          this.errorMsg =
            "Thanh toán VNPay toàn phần không được gửi tiền mặt. Nếu khách đưa một phần tiền mặt, hệ thống sẽ dùng MIXED + VNPAY.";
          return false;
        }
      }

      if (selectedPaymentMethod === "VIETQR") {
        if (cashGiven > 0) {
          this.errorMsg =
            "Thanh toán VietQR toàn phần không được gửi tiền mặt. Nếu khách đưa một phần tiền mặt, hệ thống sẽ dùng MIXED + VIETQR.";
          return false;
        }

        if (transferAmount <= 0) {
          transferAmount = this.finalAmount;
        }

        if (transferAmount !== this.finalAmount) {
          this.errorMsg =
            "Số tiền VietQR phải bằng đúng số tiền cần thanh toán.";
          return false;
        }
      }

      if (selectedPaymentMethod === "MIXED") {
        if (cashGiven <= 0) {
          this.errorMsg = "Thanh toán hỗn hợp phải có tiền mặt đã nhận.";
          return false;
        }

        if (cashGiven >= this.finalAmount) {
          this.errorMsg =
            "Tiền mặt đã đủ thanh toán, vui lòng chọn phương thức tiền mặt.";
          return false;
        }

        if (transferAmount <= 0) {
          transferAmount = this.finalAmount - cashGiven;
        }

        const totalPaid = cashGiven + transferAmount;

        if (totalPaid < this.finalAmount) {
          this.errorMsg = `Khách còn thiếu ${formatMoney(
            this.finalAmount - totalPaid
          )} ₫.`;
          return false;
        }

        if (totalPaid > this.finalAmount) {
          this.errorMsg =
            "Tổng tiền mặt và chuyển khoản đang vượt quá số tiền cần thanh toán.";
          return false;
        }
      }

      return true;
    },

    async processCheckout(extra?: {
      paymentMethod?: PosCheckoutPaymentMethod;
      transferProvider?: PosTransferProvider;
      cashGiven?: number;
      transferAmount?: number;
    }) {
      if (!this.validateCartCanCheckout()) return false;

      let selectedPaymentMethod: PosCheckoutPaymentMethod =
        extra?.paymentMethod || this.paymentMethod;

      let transferProvider: PosTransferProvider = extra?.transferProvider || "";
      let cashGiven = Number(extra?.cashGiven || 0);
      let transferAmount = Number(extra?.transferAmount || 0);

      /*
       * BE hiện tại:
       * - /checkout: tạo hóa đơn POS mới
       * - /held-orders/{id}/checkout: chỉ thanh toán phiếu treo HOLD
       * - /orders/{id}/retry-payment: thanh toán lại hóa đơn PENDING_PAYMENT
       */
      if (selectedPaymentMethod === "VNPAY") {
        transferProvider = "VNPAY";

        if (this.cashPaid > 0) {
          selectedPaymentMethod = "MIXED";
          cashGiven = this.cashPaid;
          transferAmount = this.remainingAmount;
        }
      }

      if (selectedPaymentMethod === "VIETQR") {
        transferProvider = "VIETQR";

        if (this.cashPaid > 0) {
          selectedPaymentMethod = "MIXED";
          cashGiven = this.cashPaid;
          transferAmount = this.remainingAmount;
        }
      }

      if (selectedPaymentMethod === "VIETQR" && transferAmount <= 0) {
        transferAmount = this.finalAmount;
      }

      if (selectedPaymentMethod === "MIXED") {
        if (!transferProvider) {
          transferProvider =
            this.paymentMethod === "VNPAY" ? "VNPAY" : "VIETQR";
        }

        if (transferAmount <= 0) {
          transferAmount = this.finalAmount - cashGiven;
        }
      }

      if (
        !this.validatePaymentInput(
          selectedPaymentMethod,
          cashGiven,
          transferAmount
        )
      ) {
        return false;
      }

      const retryPaymentOrderId = this.activePendingPaymentOrderId || null;

      const paymentOnlyPayload = {
        paymentMethod: selectedPaymentMethod,
        transferProvider:
          selectedPaymentMethod === "MIXED" ? transferProvider : null,
        cashGiven:
          selectedPaymentMethod === "CASH" || selectedPaymentMethod === "MIXED"
            ? cashGiven
            : null,
        transferAmount:
          selectedPaymentMethod === "VNPAY" ||
          selectedPaymentMethod === "VIETQR" ||
          selectedPaymentMethod === "MIXED"
            ? transferAmount || null
            : null,
      };

      this.isLoading = true;
      this.errorMsg = "";
      this.vnpayUrl = "";
      this.vietQrImageUrl = "";
      this.vietQrContent = "";
      this.pendingVietQrOrderId = null;
      this.pendingVietQrAmount = 0;
      this.transferProvider = transferProvider;

      try {
        let data: any;

        if (retryPaymentOrderId) {
          const response = await api.post(
            `/admin/pos/orders/${retryPaymentOrderId}/retry-payment`,
            paymentOnlyPayload
          );

          data = response.data;
        } else if (this.activeHeldOrderId) {
          /*
           * Chỉ gọi endpoint phiếu treo khi order hiện tại thật sự là HOLD.
           * Nếu đã tạo QR/VNPay rồi thì phải đi nhánh retryPaymentOrderId ở trên.
           */
          if (this.cashPaid <= 0) {
            await api.patch(
              `/admin/pos/held-orders/${this.activeHeldOrderId}`,
              this.buildCurrentOrderPayload()
            );
          }

          const response = await api.post(
            `/admin/pos/held-orders/${this.activeHeldOrderId}/checkout`,
            paymentOnlyPayload
          );

          data = response.data;
        } else {
          if (!this.validateCustomerForCheckout()) return false;

          const payload = this.buildCheckoutPayload(
            selectedPaymentMethod,
            cashGiven,
            transferAmount,
            transferProvider
          );

          const response = await api.post("/admin/pos/checkout", payload);
          data = response.data;
        }

        const orderId =
          data?.orderId ||
          data?.id ||
          data?.code ||
          data?.orderCode ||
          data?.invoiceCode ||
          retryPaymentOrderId ||
          this.activeHeldOrderId ||
          null;

        this.lastOrderId = orderId;

        const responseTransferProvider = String(
          data?.transferProvider || transferProvider || ""
        ).toUpperCase();

        if (
          selectedPaymentMethod === "VNPAY" ||
          (selectedPaymentMethod === "MIXED" &&
            responseTransferProvider === "VNPAY")
        ) {
          if (data.vnpayPaymentUrl) {
            const redirectUrl = data.vnpayPaymentUrl;

            this.activePendingPaymentOrderId = orderId;
            this.activePendingPaymentTransferProvider = "VNPAY";
            this.activeHeldOrderId = null;
            this.activeHeldOrderCashierName = "";

            this.savePendingCheckoutDraft();
            await this.fetchHeldOrders();

            window.location.href = redirectUrl;

            return {
              success: true,
              data,
            };
          }

          this.errorMsg = "Không nhận được link thanh toán VNPay từ máy chủ!";
          return false;
        }

        if (
          selectedPaymentMethod === "VIETQR" ||
          (selectedPaymentMethod === "MIXED" &&
            responseTransferProvider === "VIETQR")
        ) {
          if (data.vietQrImageUrl) {
            this.vietQrImageUrl = data.vietQrImageUrl;
            this.vietQrContent = data.vietQrContent || "";
            this.pendingVietQrOrderId = orderId;
            this.pendingVietQrAmount = Number(
              data.transferAmount || data.remainingAmount || transferAmount || 0
            );

            this.activePendingPaymentOrderId = orderId;
            this.activePendingPaymentTransferProvider = "VIETQR";
            this.activeHeldOrderId = null;
            this.activeHeldOrderCashierName = "";

            return {
              success: true,
              data,
            };
          }

          this.errorMsg = "Không nhận được mã VietQR từ máy chủ.";
          return false;
        }

        this.activePendingPaymentOrderId = null;
        this.activePendingPaymentTransferProvider = "";

        return {
          success: true,
          data,
        };
      } catch (error: any) {
        this.errorMsg = getBackendMessage(
          error,
          "Thanh toán thất bại. Vui lòng kiểm tra lại!"
        );

        return false;
      } finally {
        this.isLoading = false;
      }
    },

    async confirmVietQrPayment(orderId?: number | string | null) {
      const targetOrderId =
        orderId || this.pendingVietQrOrderId || this.lastOrderId;

      if (!targetOrderId) {
        this.errorMsg = "Không xác định được hóa đơn VietQR cần xác nhận.";
        return false;
      }

      const beforeConfirmCashPaid = Number(this.cashPaid || 0);
      const beforeConfirmTransferAmount = Number(
        this.pendingVietQrAmount || this.remainingAmount || 0
      );
      const beforeConfirmPaymentMethod =
        beforeConfirmCashPaid > 0 ? "MIXED" : "VIETQR";
      const beforeConfirmTransferProvider: PosTransferProvider = "VIETQR";
      const beforeConfirmOrderId = targetOrderId;

      this.isLoading = true;
      this.errorMsg = "";

      try {
        const { data } = await api.post(
          `/admin/pos/orders/${targetOrderId}/vietqr/confirm`
        );

        const fixedData = {
          ...data,
          orderId: data?.orderId || beforeConfirmOrderId,
          paymentMethod: data?.paymentMethod || beforeConfirmPaymentMethod,
          transferProvider:
            data?.transferProvider || beforeConfirmTransferProvider,
        } as any;

        if (beforeConfirmCashPaid > 0) {
          fixedData.paymentMethod = "MIXED";
          fixedData.transferProvider = "VIETQR";
          fixedData.cashGiven = beforeConfirmCashPaid;
          fixedData.transferAmount = beforeConfirmTransferAmount;
          fixedData.paidAmount = Number(
            data?.finalAmount || this.finalAmount || 0
          );
          fixedData.remainingAmount = 0;
          fixedData.changeAmount = Number(data?.changeAmount || 0);
        }

        this.lastOrderId = fixedData.orderId || targetOrderId;
        this.vietQrImageUrl = "";
        this.vietQrContent = "";
        this.pendingVietQrOrderId = null;
        this.pendingVietQrAmount = 0;
        this.activePendingPaymentOrderId = null;
        this.activePendingPaymentTransferProvider = "";

        this.resetLocalOrderOnly();
        await this.fetchProducts();
        await this.fetchHeldOrders();

        return {
          success: true,
          data: fixedData,
        };
      } catch (error: any) {
        this.errorMsg = getBackendMessage(
          error,
          "Xác nhận thanh toán VietQR thất bại. Vui lòng kiểm tra tiền đã vào tài khoản chưa."
        );

        return false;
      } finally {
        this.isLoading = false;
      }
    },

    async holdCurrentOrder() {
      this.errorMsg = "";

      if (this.cashPaid > 0 || this.activePendingPaymentOrderId) {
        this.errorMsg =
          "Đơn đã nhận tiền/tạo thanh toán online, không được treo/cập nhật phiếu.";
        return false;
      }

      if (!this.validateCartCanCheckout()) return false;

      if (!this.activeHeldOrderId && !this.validateCustomerForCheckout()) {
        return false;
      }

      this.isLoading = true;
      this.errorMsg = "";

      try {
        const payload = this.buildCurrentOrderPayload();

        let data: any;

        if (this.activeHeldOrderId) {
          const response = await api.patch(
            `/admin/pos/held-orders/${this.activeHeldOrderId}`,
            payload
          );
          data = response.data;
        } else {
          const response = await api.post("/admin/pos/hold", payload);
          data = response.data;
        }

        const savedOrderId = data?.orderId || this.activeHeldOrderId || null;
        const wasUpdatingHeldOrder = Boolean(this.activeHeldOrderId);

        this.lastOrderId = savedOrderId;

        this.resetLocalOrderOnly();
        await this.fetchHeldOrders();

        this.errorMsg = wasUpdatingHeldOrder
          ? `Đã cập nhật phiếu treo #${savedOrderId || ""} thành công.`
          : `Đã treo phiếu #${savedOrderId || ""} thành công.`;

        return {
          success: true,
          data,
        };
      } catch (error: any) {
        this.errorMsg = getBackendMessage(
          error,
          this.activeHeldOrderId
            ? "Cập nhật phiếu treo thất bại. Vui lòng kiểm tra lại."
            : "Treo phiếu thất bại. Vui lòng kiểm tra lại."
        );

        return false;
      } finally {
        this.isLoading = false;
      }
    },

    mapOrderItemToCartItem(item: any): CartItem {
      const sku = String(item.sku || item.productSku || "");
      const latestProduct = this.allProducts.find(
        (product) => product.sku.toLowerCase() === sku.toLowerCase()
      );

      if (latestProduct) {
        return {
          product: { ...latestProduct },
          quantity: Number(item.quantity || 1),
        };
      }

      const productName = String(item.productName || item.name || "Sản phẩm");
      const subName = [item.capacityLabel, item.bottleTypeName]
        .filter(Boolean)
        .join(" - ");

      return {
        product: {
          id: Number(item.variantId || item.productVariantId || 0),
          sku,
          name: productName,
          subName: subName || item.variantName || "Biến thể mặc định",
          price: Number(item.unitPrice || item.price || 0),
          stockQuantity: Number(
            item.stockQuantity ||
              item.availableStock ||
              item.productStock ||
              item.quantity ||
              1
          ),
          image: `https://ui-avatars.com/api/?name=${encodeURIComponent(
            productName
          )}&background=random&color=fff&size=200`,
          category: item.brandName || item.categoryName || "Phiếu treo",
          manufacturingDate: toDateOnly(item.manufacturingDate),
          expirationDate: toDateOnly(item.expirationDate),
          status: item.variantStatus ?? item.status ?? null,
          expired: Boolean(
            item.expired ?? isDateBeforeToday(item.expirationDate)
          ),
          sellable: item.sellable ?? true,
          unavailableReason: item.unavailableReason || null,
        },
        quantity: Number(item.quantity || 1),
      };
    },

    async openHeldOrder(orderId: number) {
      if (!orderId) {
        this.errorMsg = "Mã phiếu treo không hợp lệ.";
        return false;
      }

      if (this.cashPaid > 0) {
        this.errorMsg =
          "Đơn hiện tại đã nhận tiền mặt một phần, không được mở phiếu treo.";
        return false;
      }

      this.isLoading = true;
      this.errorMsg = "";

      try {
        const { data } = await api.get(`/admin/pos/held-orders/${orderId}`);

        /*
         * Mở phiếu treo phải là trạng thái chỉnh sửa sạch:
         * - Được thêm/sửa/xóa sản phẩm và voucher
         * - Không bị khóa bởi trạng thái thanh toán/VNPay/VietQR cũ
         */
        this.clearPendingCheckoutDraft();
        this.cashPaid = 0;
        this.paymentMethod = "CASH";
        this.transferProvider = "";
        this.vnpayUrl = "";
        this.vietQrImageUrl = "";
        this.vietQrContent = "";
        this.pendingVietQrOrderId = null;
        this.pendingVietQrAmount = 0;
        this.activePendingPaymentOrderId = null;
        this.activePendingPaymentTransferProvider = "";
        this.errorMsg = "";

        const items = Array.isArray(data.items) ? data.items : [];

        this.cart = items.map((item: any) => this.mapOrderItemToCartItem(item));

        this.customer = {
          customerId: data.customerId,
          name: data.customerName || "",
          phone: data.customerPhone || "",
          email: data.customerEmail || "",
          customerRank: data.customerRank || "BRONZE",
          loyaltyPoints: Number(data.customerLoyaltyPointsAfter || 0),
        };

        this.markCustomerSaved();

        const heldVoucherCode = String(
          data.voucherCode ||
            data.voucher_code ||
            data.code ||
            data.voucher?.code ||
            data.voucher?.voucherCode ||
            ""
        )
          .trim()
          .toUpperCase();

        this.voucherCode = heldVoucherCode;
        this.discountAmount = Number(
          data.discountAmount ||
            data.discount_amount ||
            data.voucherDiscountAmount ||
            0
        );

        this.paymentMethod = "CASH";
        this.transferProvider = "";
        this.cashPaid = 0;
        this.vnpayUrl = "";
        this.vietQrImageUrl = "";
        this.vietQrContent = "";
        this.pendingVietQrOrderId = null;
        this.pendingVietQrAmount = 0;
        this.activePendingPaymentOrderId = null;
        this.activePendingPaymentTransferProvider = "";
        this.lastOrderId = data.orderId || orderId;
        this.activeHeldOrderId = Number(data.orderId || orderId);
        this.activeHeldOrderCashierName = data.cashierName || "";
        this.showHeldOrdersPanel = false;
        this.activeHeldOrderOwnOrder =
          data.ownOrder !== false && data.isOwnOrder !== false;

        this.activeHeldOrderCanOpen = data.canOpen !== false;
        this.activeHeldOrderCanCheckout = data.canCheckout !== false;
        this.activeHeldOrderCanTransfer = data.canTransfer !== false;
        this.activeHeldOrderCanCancel = data.canCancel !== false;
        if (this.cart.length > 0) {
          await this.fetchAvailableVouchers();
        }

        this.errorMsg = `Đã mở phiếu treo #${this.activeHeldOrderId}. Có thể thêm/sửa sản phẩm và voucher, khách hàng được giữ nguyên.`;

        return {
          success: true,
          data,
        };
      } catch (error: any) {
        this.errorMsg = getBackendMessage(
          error,
          "Không thể mở phiếu treo. Phiếu có thể thuộc nhân viên khác hoặc đã thanh toán."
        );

        return false;
      } finally {
        this.isLoading = false;
      }
    },

    closeHeldOrderLocal() {
      this.resetLocalOrderOnly();
      this.errorMsg = "";
    },

    async transferHeldOrder(orderId: number, targetEmployeeId: number) {
      if (!orderId) {
        this.errorMsg = "Mã phiếu treo không hợp lệ.";
        return false;
      }

      if (!targetEmployeeId || targetEmployeeId <= 0) {
        this.errorMsg = "Vui lòng chọn nhân viên nhận phiếu.";
        return false;
      }

      this.isLoading = true;
      this.errorMsg = "";

      try {
        const { data } = await api.patch(
          `/admin/pos/held-orders/${orderId}/transfer`,
          {
            targetEmployeeId,
          }
        );

        /*
         * Xóa tạm khỏi danh sách ngay để UI biến mất nhanh,
         * sau đó fetch lại từ backend.
         */
        this.heldOrders = this.heldOrders.filter(
          (held) => Number(held.orderId) !== Number(orderId)
        );

        if (Number(this.activeHeldOrderId || 0) === Number(orderId)) {
          this.resetLocalOrderOnly();
        }

        await this.fetchHeldOrders();

        this.errorMsg = `Đã chuyển phiếu #${orderId} cho ${
          data.cashierName || "nhân viên được chọn"
        }.`;

        return {
          success: true,
          data,
        };
      } catch (error: any) {
        this.errorMsg = getBackendMessage(
          error,
          "Chuyển phiếu thất bại. Bạn chỉ được chuyển phiếu của mình, trừ khi là MANAGER/OWNER."
        );

        return false;
      } finally {
        this.isLoading = false;
      }
    },

    async cancelHeldOrder(orderId: number) {
      if (!orderId) {
        this.errorMsg = "Mã phiếu treo không hợp lệ.";
        return false;
      }

      if (this.cashPaid > 0) {
        this.errorMsg =
          "Đơn hiện tại đã nhận tiền mặt một phần, không được hủy phiếu treo.";
        return false;
      }

      this.isLoading = true;
      this.errorMsg = "";

      try {
        const { data } = await api.patch(
          `/admin/pos/held-orders/${orderId}/cancel`
        );

        if (this.activeHeldOrderId === orderId) {
          this.resetLocalOrderOnly();
        }

        await this.fetchHeldOrders();
        await this.fetchProducts();

        this.errorMsg = `Đã hủy phiếu treo #${orderId}.`;

        return {
          success: true,
          data,
        };
      } catch (error: any) {
        this.errorMsg = getBackendMessage(
          error,
          "Hủy phiếu treo thất bại. Phiếu có thể đã thanh toán, đã bị hủy hoặc không thuộc quyền của bạn."
        );

        return false;
      } finally {
        this.isLoading = false;
      }
    },

    startNewOrder() {
      this.resetLocalOrderOnly();
      this.fetchProducts();
      this.fetchHeldOrders();
    },
  },
});
