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

export type PosPaymentMethod = "CASH" | "VNPAY";
export type PosCheckoutPaymentMethod = "CASH" | "VNPAY" | "MIXED";

export interface PosStoreState {
  allProducts: PosProduct[];
  categories: string[];
  selectedCategory: string;
  searchQuery: string;

  cart: CartItem[];
  customer: PosCustomer | null;

  voucherCode: string;
  discountAmount: number;

  paymentMethod: PosPaymentMethod;
  cashPaid: number;

  vnpayUrl: string;
  isLoading: boolean;
  errorMsg: string;
  lastOrderId: string | number | null;

  heldOrders: PosHeldOrder[];
  transferTargets: PosTransferTarget[];

  activeHeldOrderId: number | null;
  activeHeldOrderCashierName: string;
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

const isValidEmail = (email: string): boolean => {
  return /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email);
};

const formatMoney = (value: number): string => {
  return new Intl.NumberFormat("vi-VN").format(Number(value || 0));
};

const getBackendMessage = (error: any, fallback: string): string => {
  const data = error?.response?.data;

  if (!data) {
    return fallback;
  }

  if (data.errors && typeof data.errors === "object") {
    const firstError = Object.values(data.errors)[0];

    if (firstError) {
      return String(firstError);
    }
  }

  return data.message || fallback;
};

const toDateOnly = (value?: string | null): string | null => {
  if (!value) {
    return null;
  }

  return String(value).substring(0, 10);
};

const isDateBeforeToday = (value?: string | null): boolean => {
  const dateOnly = toDateOnly(value);

  if (!dateOnly) {
    return false;
  }

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const date = new Date(`${dateOnly}T00:00:00`);

  return !Number.isNaN(date.getTime()) && date.getTime() < today.getTime();
};

const isDateAfterToday = (value?: string | null): boolean => {
  const dateOnly = toDateOnly(value);

  if (!dateOnly) {
    return false;
  }

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
  if (!product) {
    return "Sản phẩm không hợp lệ.";
  }

  if (product.unavailableReason) {
    return product.unavailableReason;
  }

  if (product.sellable === false) {
    return "Sản phẩm hiện không được bán.";
  }

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
    id: Number(raw.variantId || raw.productVariantId || raw.id || parent.id || 0),
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

export const usePosStore = defineStore("posStore", {
  state: (): PosStoreState => ({
    allProducts: [],
    categories: ["Tất cả"],
    selectedCategory: "Tất cả",
    searchQuery: "",

    cart: [],
    customer: null,

    voucherCode: "",
    discountAmount: 0,

    paymentMethod: "CASH",
    cashPaid: 0,

    vnpayUrl: "",
    isLoading: false,
    errorMsg: "",
    lastOrderId: null,

    heldOrders: [],
    transferTargets: [],

    activeHeldOrderId: null,
    activeHeldOrderCashierName: "",
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
      return state.cashPaid > 0 || state.activeHeldOrderId !== null;
    },
  },

  actions: {
    resetLocalOrderOnly() {
      this.cart = [];
      this.customer = null;
      this.voucherCode = "";
      this.discountAmount = 0;
      this.paymentMethod = "CASH";
      this.cashPaid = 0;
      this.vnpayUrl = "";
      this.errorMsg = "";
      this.lastOrderId = null;
      this.activeHeldOrderId = null;
      this.activeHeldOrderCashierName = "";

      sessionStorage.removeItem("pos_pending_checkout_draft");
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

        this.categories = [
          "Tất cả",
          ...new Set(
            flatProducts
              .map((p) => p.category)
              .filter((category) => !!category)
          ),
        ];
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

    addToCart(product: PosProduct) {
      this.errorMsg = "";

      if (this.activeHeldOrderId) {
        this.errorMsg =
          "Đang mở phiếu treo, không được thêm sản phẩm. Hãy thanh toán hoặc đóng phiếu trước.";
        return;
      }

      if (this.cashPaid > 0) {
        this.errorMsg =
          "Đơn đã nhận tiền mặt một phần, không được thêm sản phẩm.";
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
        return;
      }

      this.cart.push({
        product,
        quantity: 1,
      });
    },

    async handleBarcodeScan(sku: string) {
      this.errorMsg = "";

      if (this.activeHeldOrderId) {
        this.errorMsg = "Đang mở phiếu treo, không được quét thêm sản phẩm.";
        return;
      }

      if (this.cashPaid > 0) {
        this.errorMsg =
          "Đơn đã nhận tiền mặt một phần, không được quét thêm sản phẩm.";
        return;
      }

      const cleanSku = normalizeText(sku);

      if (!cleanSku) {
        return;
      }

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

      if (this.activeHeldOrderId) {
        this.errorMsg =
          "Đang mở phiếu treo, không được sửa số lượng. Muốn đổi sản phẩm thì hủy phiếu cũ ở backend và tạo phiếu mới.";
        return;
      }

      if (this.cashPaid > 0) {
        this.errorMsg =
          "Đơn đã nhận tiền mặt một phần, không được sửa số lượng.";
        return;
      }

      const item = this.cart.find((i) => i.product.sku === sku);

      if (!item) {
        return;
      }

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
        return;
      }

      item.quantity = qty;
    },

    removeFromCart(sku: string) {
      this.errorMsg = "";

      if (this.activeHeldOrderId) {
        this.errorMsg =
          "Đang mở phiếu treo, không được xóa sản phẩm. Muốn đổi sản phẩm thì hủy phiếu cũ ở backend và tạo phiếu mới.";
        return;
      }

      if (this.cashPaid > 0) {
        this.errorMsg =
          "Đơn đã nhận tiền mặt một phần, không được xóa sản phẩm.";
        return;
      }

      this.cart = this.cart.filter((item) => item.product.sku !== sku);
    },

    async searchCustomer(phone: string) {
      const cleanPhone = normalizePhone(phone);

      if (this.activeHeldOrderId) {
        this.errorMsg = "Đang mở phiếu treo, không được đổi khách hàng.";
        return;
      }

      if (this.cashPaid > 0) {
        this.errorMsg =
          "Đơn đã nhận tiền mặt một phần, không được đổi khách hàng.";
        return;
      }

      if (!cleanPhone) {
        this.customer = null;
        this.errorMsg = "Vui lòng nhập số điện thoại khách hàng.";
        return;
      }

      if (!isValidVietnamPhone(cleanPhone)) {
        this.customer = null;
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

        this.errorMsg =
          data?.message ||
          "Không tìm thấy khách hàng. Vui lòng nhập họ tên và email để tạo khách mới.";
      } catch (error: any) {
        this.customer = null;
        this.errorMsg = getBackendMessage(
          error,
          "Không thể kiểm tra số điện thoại khách hàng."
        );
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

      if (this.activeHeldOrderId) {
        this.errorMsg =
          "Phiếu treo không xử lý nhận tiền mặt tạm ở FE. Hãy thanh toán trực tiếp bằng CASH hoặc VNPAY/MIXED.";
        return false;
      }

      if (!this.validateCartCanCheckout()) {
        return false;
      }

      if (!this.validateCustomerForCheckout()) {
        return false;
      }

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
      this.paymentMethod = "VNPAY";

      this.errorMsg = `Đã nhận tiền mặt ${formatMoney(
        this.cashPaid
      )} ₫. Còn thiếu ${formatMoney(
        this.remainingAmount
      )} ₫, vui lòng chọn tiếp phương thức thanh toán.`;

      return true;
    },

    buildCheckoutPayload(
      selectedPaymentMethod: PosCheckoutPaymentMethod,
      cashGiven: number,
      transferAmount: number
    ) {
      return {
        paymentMethod: selectedPaymentMethod,
        customerPhone: normalizePhone(this.customer?.phone),
        customerName: normalizeText(this.customer?.name),
        customerEmail: normalizeEmail(this.customer?.email),
        voucherCode: normalizeText(this.voucherCode) || null,

        cashGiven:
          selectedPaymentMethod === "CASH" || selectedPaymentMethod === "MIXED"
            ? cashGiven
            : null,

        transferAmount:
          selectedPaymentMethod === "MIXED"
            ? transferAmount
            : selectedPaymentMethod === "VNPAY"
              ? null
              : null,

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
          this.errorMsg = "Thanh toán VNPay không được gửi tiền mặt.";
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
      cashGiven?: number;
      transferAmount?: number;
    }) {
      if (!this.validateCartCanCheckout()) {
        return false;
      }

      let selectedPaymentMethod: PosCheckoutPaymentMethod =
        extra?.paymentMethod || this.paymentMethod;

      let cashGiven = Number(extra?.cashGiven || 0);
      let transferAmount = Number(extra?.transferAmount || 0);

      if (selectedPaymentMethod === "VNPAY" && this.cashPaid > 0) {
        selectedPaymentMethod = "MIXED";
        cashGiven = this.cashPaid;
        transferAmount = this.remainingAmount;
      }

      if (selectedPaymentMethod === "MIXED" && transferAmount <= 0) {
        transferAmount = this.finalAmount - cashGiven;
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

      this.isLoading = true;
      this.errorMsg = "";

      try {
        let data: any;

        if (this.activeHeldOrderId) {
          const response = await api.post(
            `/admin/pos/held-orders/${this.activeHeldOrderId}/checkout`,
            {
              paymentMethod: selectedPaymentMethod,
              cashGiven:
                selectedPaymentMethod === "CASH" ||
                selectedPaymentMethod === "MIXED"
                  ? cashGiven
                  : null,
              transferAmount:
                selectedPaymentMethod === "MIXED"
                  ? transferAmount
                  : selectedPaymentMethod === "VNPAY"
                    ? null
                    : null,
            }
          );

          data = response.data;
        } else {
          if (!this.validateCustomerForCheckout()) {
            return false;
          }

          const payload = this.buildCheckoutPayload(
            selectedPaymentMethod,
            cashGiven,
            transferAmount
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
          this.activeHeldOrderId ||
          null;

        this.lastOrderId = orderId;

        if (
          selectedPaymentMethod === "VNPAY" ||
          selectedPaymentMethod === "MIXED"
        ) {
          if (data.vnpayPaymentUrl) {
            const redirectUrl = data.vnpayPaymentUrl;

            this.resetLocalOrderOnly();
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

    async holdCurrentOrder() {
      this.errorMsg = "";

      if (this.activeHeldOrderId) {
        this.errorMsg = "Phiếu này đã được treo rồi.";
        return false;
      }

      if (this.cashPaid > 0) {
        this.errorMsg = "Đơn đã nhận tiền mặt một phần, không được treo phiếu.";
        return false;
      }

      if (!this.validateCartCanCheckout()) {
        return false;
      }

      if (!this.validateCustomerForCheckout()) {
        return false;
      }

      this.isLoading = true;
      this.errorMsg = "";

      try {
        const payload = {
          customerPhone: normalizePhone(this.customer?.phone),
          customerName: normalizeText(this.customer?.name),
          customerEmail: normalizeEmail(this.customer?.email),
          voucherCode: normalizeText(this.voucherCode) || null,
          items: this.cart.map((item) => ({
            sku: item.product.sku,
            quantity: Number(item.quantity),
          })),
        };

        const { data } = await api.post("/admin/pos/hold", payload);

        this.lastOrderId = data?.orderId || null;

        this.resetLocalOrderOnly();
        await this.fetchHeldOrders();

        this.errorMsg = `Đã treo phiếu #${data?.orderId || ""} thành công.`;

        return {
          success: true,
          data,
        };
      } catch (error: any) {
        this.errorMsg = getBackendMessage(
          error,
          "Treo phiếu thất bại. Vui lòng kiểm tra lại."
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
          stockQuantity: Number(item.stockQuantity || item.quantity || 1),
          image: `https://ui-avatars.com/api/?name=${encodeURIComponent(
            productName
          )}&background=random&color=fff&size=200`,
          category: item.brandName || item.categoryName || "Phiếu treo",
          manufacturingDate: toDateOnly(item.manufacturingDate),
          expirationDate: toDateOnly(item.expirationDate),
          status: item.variantStatus ?? item.status ?? null,
          expired: Boolean(item.expired ?? isDateBeforeToday(item.expirationDate)),
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

        this.voucherCode = data.voucherCode || "";
        this.discountAmount = Number(data.discountAmount || 0);
        this.paymentMethod = "CASH";
        this.cashPaid = 0;
        this.vnpayUrl = "";
        this.lastOrderId = data.orderId || orderId;
        this.activeHeldOrderId = Number(data.orderId || orderId);
        this.activeHeldOrderCashierName = data.cashierName || "";
        this.showHeldOrdersPanel = false;

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

        await this.fetchHeldOrders();

        if (this.activeHeldOrderId === orderId) {
          this.resetLocalOrderOnly();
        }

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

    startNewOrder() {
      this.resetLocalOrderOnly();
      this.fetchProducts();
      this.fetchHeldOrders();
    },
  },
});