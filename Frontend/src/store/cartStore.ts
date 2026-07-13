import { defineStore } from "pinia";
import api from "@/common/api";

export interface CartItem {
  cartItemId: number;
  productVariantId: number;

  sku?: string;
  productName?: string;
  capacity?: string;

  quantity: number;
  price: number;
  stockQuantity: number;

  note?: string | null;
  imageUrl?: string | null;
  thumbnailUrl?: string | null;

  available?: boolean;
  unavailableReason?: string | null;
}

export interface AddToCartPayload {
  productVariantId?: number;
  id?: number | string;
  quantity?: number;
  note?: string | null;
  thumbnailUrl?: string | null;
  image?: string | null;
  imageUrl?: string | null;
}

const getCurrentRole = () => {
  return String(localStorage.getItem("role") || "")
    .toUpperCase()
    .trim();
};

const isCustomerUser = () => {
  const token = localStorage.getItem("token");
  const role = getCurrentRole();

  return Boolean(token) && role === "USER";
};

export const useCartStore = defineStore("cart", {
  state: () => ({
    items: [] as CartItem[],
    isLoading: false,
    isOpen: false,
  }),

  getters: {
    cartCount: (state) => {
      return state.items.reduce((count, item) => {
        return count + Number(item.quantity || 0);
      }, 0);
    },

    cartTotal: (state) => {
      return state.items.reduce((total, item) => {
        return total + Number(item.price || 0) * Number(item.quantity || 0);
      }, 0);
    },

    hasInvalidItem: (state) => {
      return state.items.some((item) => {
        const quantity = Number(item.quantity || 0);
        const stockQuantity = Number(item.stockQuantity || 0);

        return (
          item.available === false ||
          quantity <= 0 ||
          stockQuantity <= 0 ||
          quantity > stockQuantity
        );
      });
    },
  },

  actions: {
    toggleCart() {
      this.isOpen = !this.isOpen;
    },

    openCart() {
      this.isOpen = true;
    },

    closeCart() {
      this.isOpen = false;
    },

    clearCartLocal() {
      this.items = [];
      this.isOpen = false;
    },

    async loadCart() {
      if (!isCustomerUser()) {
        this.clearCartLocal();
        return;
      }

      try {
        this.isLoading = true;

        const res = await api.get("/v1/customer/cart/my-cart");

        this.items = Array.isArray(res.data) ? res.data : [];
      } catch (error: any) {
        console.error("Lỗi đồng bộ giỏ hàng:", error);

        if (error?.response?.status === 401 || error?.response?.status === 403) {
          this.clearCartLocal();
        }
      } finally {
        this.isLoading = false;
      }
    },

    async fetchRealCart() {
      await this.loadCart();
    },

    async addToCart(payload: AddToCartPayload, quantityArg?: number) {
      if (!isCustomerUser()) {
        throw new Error("Vui lòng đăng nhập bằng tài khoản khách hàng để thêm vào giỏ.");
      }

      const productVariantId = Number(payload.productVariantId ?? payload.id ?? 0);
      const quantity = Number(payload.quantity ?? quantityArg ?? 1);

      if (!productVariantId || productVariantId <= 0) {
        throw new Error("Biến thể sản phẩm không hợp lệ.");
      }

      if (!quantity || quantity <= 0) {
        throw new Error("Số lượng phải lớn hơn 0.");
      }

      await api.post("/v1/customer/cart/add", {
        productVariantId,
        quantity,
        note: payload.note || null,
        thumbnailUrl: payload.thumbnailUrl || payload.imageUrl || payload.image || null,
      });

      await this.loadCart();

      this.isOpen = true;

      window.dispatchEvent(new Event("cart-updated"));
    },

    async updateQuantity(
      cartItemId: number,
      quantityOrCurrentQty: number,
      change?: number,
      stockQty?: number
    ) {
      if (!cartItemId || cartItemId <= 0) {
        throw new Error("Mã sản phẩm trong giỏ không hợp lệ.");
      }

      const newQuantity =
        typeof change === "number"
          ? Number(quantityOrCurrentQty || 0) + change
          : Number(quantityOrCurrentQty || 0);

      if (newQuantity < 1) {
        return;
      }

      if (typeof stockQty === "number" && newQuantity > stockQty) {
        throw new Error(`Sản phẩm này chỉ còn tối đa ${stockQty} cái trong kho.`);
      }

      await api.put(`/v1/customer/cart/update/${cartItemId}`, {
        quantity: newQuantity,
      });

      await this.loadCart();

      window.dispatchEvent(new Event("cart-updated"));
    },

    async removeItem(cartItemId: number) {
      if (!cartItemId || cartItemId <= 0) {
        throw new Error("Mã sản phẩm trong giỏ không hợp lệ.");
      }

      await api.delete(`/v1/customer/cart/remove/${cartItemId}`);

      this.items = this.items.filter((item) => item.cartItemId !== cartItemId);

      window.dispatchEvent(new Event("cart-updated"));
    },
  },
});