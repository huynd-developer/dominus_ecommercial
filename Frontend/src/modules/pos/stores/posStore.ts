import { defineStore } from "pinia";
import api from "../../../common/api";
import {
  MOCK_PRODUCTS,
  MOCK_CATEGORIES,
} from "../../../common/constants/mockData";
import type {
  PosProduct,
  CartItem,
  CustomerLookupResult,
  CustomerPosResponse,
  PosCheckoutRequest,
  PosOrderResponse,
  ApiErrorBody,
} from "../../../common/types/pos";

// Helper lấy message lỗi từ response BE (format cố định: { message: string })
// BE luôn trả ResponseEntity.badRequest().body(Map.of("message", e.getMessage()))
// nên mọi lỗi nghiệp vụ (hết hàng, SKU sai, voucher hết hạn...) đều đi qua đây
function extractErrorMessage(err: any, fallback: string): string {
  const body = err?.response?.data as ApiErrorBody | undefined;
  return body?.message || fallback;
}

export const usePosStore = defineStore("pos", {
  state: () => ({
    // ---- Danh sách sản phẩm hiển thị ở ProductGrid ----
    // TODO: đang dùng MOCK_PRODUCTS. Khi BE có API list sản phẩm POS,
    // gọi fetchProducts() trong onMounted của PosView và xoá MOCK_PRODUCTS ở đây
    allProducts: [...MOCK_PRODUCTS] as PosProduct[],
    categories: [...MOCK_CATEGORIES] as string[],
    activeCategory: "Tất cả",
    searchQuery: "",
    isLoadingProducts: false,

    // ---- Giỏ hàng ----
    cart: [] as CartItem[],

    // ---- Khách hàng đang được áp cho đơn (null = khách vãng lai) ----
    selectedCustomer: null as CustomerPosResponse | null,
    customerPhoneInput: "",
    isSearchingCustomer: false,
    customerLookupMessage: "",

    // ---- Voucher ----
    voucherCode: "",
    appliedVoucherCode: "", // mã đã thực sự gửi kèm lúc checkout thành công lần áp dụng gần nhất

    // ---- Trạng thái thanh toán ----
    isCheckingOut: false,
    checkoutError: "",
    lastOrder: null as PosOrderResponse | null,
  }),

  getters: {
    // Lọc theo category + searchQuery, dùng cho ProductGrid
    filteredProducts(state): PosProduct[] {
      let list = state.allProducts;

      if (state.activeCategory && state.activeCategory !== "Tất cả") {
        list = list.filter((p) => p.category === state.activeCategory);
      }

      const q = state.searchQuery.trim().toLowerCase();
      if (q) {
        list = list.filter(
          (p) =>
            p.name.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q)
        );
      }
      return list;
    },

    cartTotal(state): number {
      return state.cart.reduce(
        (sum, item) => sum + item.price * item.quantity,
        0
      );
    },

    cartItemCount(state): number {
      return state.cart.reduce((sum, item) => sum + item.quantity, 0);
    },
  },

  actions: {
    // ============================================================
    // SẢN PHẨM — MOCK TẠM, thay bằng API thật ở đây sau này
    // ============================================================
    async fetchProducts() {
      // Placeholder cho lúc BE xong API. Ví dụ tương lai:
      // this.isLoadingProducts = true
      // try {
      //   const { data } = await api.get('/admin/pos/products')
      //   this.allProducts = data // map lại field nếu BE trả tên khác PosProduct
      // } finally {
      //   this.isLoadingProducts = false
      // }
      this.allProducts = [...MOCK_PRODUCTS];
    },

    // Thêm sản phẩm vào giỏ — dùng khi bấm card ở ProductGrid (mock data)
    addToCart(product: PosProduct | CartItem) {
      if (this.cart.length === 0 && this.lastOrder) {
        this.startNewOrder();
      }
      const existing = this.cart.find((i) => i.id === product.id);
      const stock = "stockQuantity" in product ? product.stockQuantity : 0;

      if (existing) {
        if (stock && existing.quantity >= stock) {
          this.checkoutError = `Sản phẩm [${existing.name}] chỉ còn ${stock} trong kho hiển thị`;
          return;
        }
        existing.quantity += 1;
        return;
      }

      this.cart.push({
        id: product.id,
        sku: product.sku,
        name: "name" in product ? product.name : "",
        price: product.price,
        quantity: 1,
        stockQuantity: stock,
        imageUrl: "image" in product ? product.image : null,
      });
    },

    // ============================================================
    // TÌM SẢN PHẨM THEO SKU/BARCODE — GỌI API THẬT
    // GET /api/admin/pos/product?sku=...
    // Dùng cho ô search ở PosHeader (F4) khi bắn mã vạch hoặc gõ SKU chính xác
    // ============================================================
    async addToCartBySku(
      sku: string
    ): Promise<{ success: boolean; message?: string }> {
      const trimmed = sku.trim();
      if (!trimmed)
        return { success: false, message: "Vui lòng nhập mã sản phẩm" };

      try {
        const { data } = await api.get("/admin/pos/product", {
          params: { sku: trimmed },
        });

        const existing = this.cart.find((i) => i.id === data.variantId);
        if (existing) {
          if (existing.quantity >= data.stockQuantity) {
            return {
              success: false,
              message: `Sản phẩm [${data.productName}] chỉ còn ${data.stockQuantity} trong kho`,
            };
          }
          existing.quantity += 1;
        } else {
          this.cart.push({
            id: data.variantId,
            sku: data.sku,
            name: data.productName,
            price: data.price,
            quantity: 1,
            stockQuantity: data.stockQuantity,
            imageUrl: data.imageUrl,
          });
        }
        return { success: true };
      } catch (err: any) {
        return {
          success: false,
          message: extractErrorMessage(
            err,
            "Không tìm thấy sản phẩm với mã này"
          ),
        };
      }
    },

    increaseQuantity(itemId: number) {
      const item = this.cart.find((i) => i.id === itemId);
      if (!item) return;
      if (item.stockQuantity && item.quantity >= item.stockQuantity) return;
      item.quantity += 1;
    },

    decreaseQuantity(itemId: number) {
      const item = this.cart.find((i) => i.id === itemId);
      if (!item) return;
      item.quantity -= 1;
      if (item.quantity <= 0) {
        this.removeItem(itemId);
      }
    },

    removeItem(itemId: number) {
      this.cart = this.cart.filter((i) => i.id !== itemId);
    },

    clearCart() {
      this.cart = [];
      this.selectedCustomer = null;
      this.customerPhoneInput = "";
      this.voucherCode = "";
      this.checkoutError = "";
      // KHÔNG reset lastOrder & appliedVoucherCode ở đây nữa —
      // để CartSideBar còn hiển thị được dòng giảm giá của đơn CASH vừa thanh toán xong.
      // 2 field này được dọn riêng ở startNewOrder() khi cashier thực sự bắt đầu đơn mới.
    },
    startNewOrder() {
      this.lastOrder = null;
      this.appliedVoucherCode = "";
    },
    // ============================================================
    // TÌM KHÁCH THEO SĐT — GỌI API THẬT
    // GET /api/admin/pos/customer?phone=...
    // found=false vẫn là kết quả hợp lệ -> nghĩa là khách vãng lai,
    // KHÔNG coi đó là lỗi (BE trả 200 OK cho cả 2 trường hợp)
    // ============================================================
    async searchCustomerByPhone(): Promise<CustomerLookupResult> {
      const phone = this.customerPhoneInput.trim();
      if (!phone) {
        this.customerLookupMessage = "Vui lòng nhập số điện thoại khách";
        return { found: false, message: this.customerLookupMessage };
      }

      this.isSearchingCustomer = true;
      this.customerLookupMessage = "";
      try {
        const { data } = await api.get<CustomerLookupResult>(
          "/admin/pos/customer",
          {
            params: { phone },
          }
        );

        if (data.found && data.customer) {
          this.selectedCustomer = data.customer;
          this.customerLookupMessage = `Đã chọn khách: ${data.customer.name}`;
        } else {
          this.selectedCustomer = null;
          this.customerLookupMessage = data.message || "Khách vãng lai";
        }
        return data;
      } catch (err: any) {
        this.selectedCustomer = null;
        this.customerLookupMessage = extractErrorMessage(
          err,
          "Không tìm được thông tin khách hàng"
        );
        return { found: false, message: this.customerLookupMessage };
      } finally {
        this.isSearchingCustomer = false;
      }
    },

    clearSelectedCustomer() {
      this.selectedCustomer = null;
      this.customerPhoneInput = "";
      this.customerLookupMessage = "";
    },

    // ============================================================
    // VOUCHER
    // Theo nghiệp vụ thực tế của BE: voucher chỉ được validate thật
    // (đúng hạn, đủ điều kiện đơn tối thiểu, tính đúng % / tiền giảm)
    // ngay trong lúc gọi /checkout — KHÔNG có endpoint preview riêng.
    // Vì vậy ở đây chỉ lưu mã người dùng nhập, còn việc giảm bao nhiêu
    // sẽ chỉ biết chính xác sau khi BE trả PosOrderResponse.discountAmount.
    // ============================================================
    setVoucherCode(code: string) {
      this.voucherCode = code;
    },

    clearVoucher() {
      this.voucherCode = "";
      this.appliedVoucherCode = "";
    },

    // ============================================================
    // CHECKOUT — GỌI API THẬT
    // POST /api/admin/pos/checkout
    // CASH  -> status COMPLETED ngay, BE đã trừ kho + tích điểm + giữ voucher
    // VNPAY -> status PENDING_PAYMENT, BE trả vnpayPaymentUrl để FE mở QR,
    //          kho/voucher đã bị giữ ngay lúc này (rollback qua IPN nếu thất bại)
    // ============================================================
    async checkout(
      paymentMethod: "CASH" | "VNPAY"
    ): Promise<{
      success: boolean;
      order?: PosOrderResponse;
      message?: string;
    }> {
      if (this.cart.length === 0) {
        this.checkoutError = "Giỏ hàng đang trống";
        return { success: false, message: this.checkoutError };
      }

      this.isCheckingOut = true;
      this.checkoutError = "";

      const payload: PosCheckoutRequest = {
        items: this.cart.map((item) => ({
          sku: item.sku,
          quantity: item.quantity,
        })),
        paymentMethod,
        customerPhone:
          this.selectedCustomer?.phone ||
          this.customerPhoneInput.trim() ||
          null,
        voucherCode: this.voucherCode.trim() || null,
      };

      try {
        const { data } = await api.post<PosOrderResponse>(
          "/admin/pos/checkout",
          payload
        );

        this.lastOrder = data;
        this.appliedVoucherCode = payload.voucherCode || "";

        // CASH hoàn tất ngay -> dọn giỏ hàng để bắt đầu đơn mới
        // VNPAY phải đợi khách quét QR xong (qua /return hoặc IPN) mới nên dọn giỏ,
        // nên giữ nguyên cart ở đây, để PosView tự quyết định khi nào clearCart()
        if (paymentMethod === "CASH") {
          this.clearCart();
        }

        return { success: true, order: data };
      } catch (err: any) {
        this.checkoutError = extractErrorMessage(
          err,
          "Thanh toán thất bại, vui lòng thử lại"
        );
        return { success: false, message: this.checkoutError };
      } finally {
        this.isCheckingOut = false;
      }
    },
  },
});
