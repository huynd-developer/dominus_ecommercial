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
}

export interface CartItem {
  product: PosProduct;
  quantity: number;
}

export interface PosCustomer {
  name: string;
  phone: string;
  loyaltyPoints: number;
}

export interface PosStoreState {
  allProducts: PosProduct[];
  categories: string[];
  selectedCategory: string;
  searchQuery: string;
  cart: CartItem[];
  customer: PosCustomer | null;
  voucherCode: string;
  discountAmount: number;
  paymentMethod: "CASH" | "VNPAY";
  vnpayUrl: string;
  isLoading: boolean;
  errorMsg: string;

  // Thêm nhẹ để lưu mã đơn sau checkout, không ảnh hưởng logic cũ
  lastOrderId: string | number | null;
}

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
    vnpayUrl: "",
    isLoading: false,
    errorMsg: "",
    lastOrderId: null,
  }),

  getters: {
    filteredProducts(state) {
      return state.allProducts.filter((product) => {
        const matchesCategory =
          state.selectedCategory === "Tất cả" ||
          product.category === state.selectedCategory;

        const matchesSearch =
          product.name.toLowerCase().includes(state.searchQuery.toLowerCase()) ||
          product.sku.toLowerCase().includes(state.searchQuery.toLowerCase());

        return matchesCategory && matchesSearch;
      });
    },

    totalAmount(state): number {
      return state.cart.reduce(
        (total, item) => total + item.product.price * item.quantity,
        0
      );
    },

    finalAmount(): number {
      const final = this.totalAmount - this.discountAmount;
      return final > 0 ? final : 0;
    },
  },

  actions: {
    // 1. Tải danh sách sản phẩm
    async fetchProducts() {
      this.isLoading = true;
      this.errorMsg = "";

      try {
        const { data } = await api.get("/admin/products");
        const rawProducts = data.data || data.content || data;
        const flatProducts: PosProduct[] = [];

        rawProducts.forEach((p: any) => {
          let imgUrl =
            p.images && p.images.length > 0 ? p.images[0].imageUrl : "";

          if (!imgUrl || !imgUrl.startsWith("http")) {
            imgUrl = `https://ui-avatars.com/api/?name=${encodeURIComponent(
              p.name || p.productName || "Product"
            )}&background=random&color=fff&size=200`;
          }

          // Data có mảng variants
          if (p.variants && p.variants.length > 0) {
            p.variants.forEach((v: any) => {
              flatProducts.push({
                id: p.id,
                sku: v.sku,
                name: p.name,
                subName: `${v.capacityValue || ""}ml - ${
                  v.bottleTypeName || ""
                }`,
                price: Number(v.price || 0),
                stockQuantity: Number(v.stockQuantity || 0),
                image: imgUrl,
                category: p.categoryName || "Tất cả",
              });
            });
          }

          // Data dạng biến thể phẳng
          else if (p.sku) {
            flatProducts.push({
              id: p.variantId || p.id || p.productId,
              sku: p.sku,
              name: p.productName || p.name,
              subName: p.capacityLabel || `${p.capacityValue || ""}ml`,
              price: Number(p.price || 0),
              stockQuantity: Number(p.stockQuantity || 0),
              image: p.imageUrl || imgUrl,
              category: p.brandName || p.categoryName || "Tất cả",
            });
          }
        });

        this.allProducts = flatProducts;

        const dynamicCategories = [
          "Tất cả",
          ...new Set(flatProducts.map((p) => p.category).filter(Boolean)),
        ] as string[];

        this.categories = dynamicCategories;
      } catch (error) {
        this.errorMsg = "Không thể tải danh sách sản phẩm từ máy chủ!";
      } finally {
        this.isLoading = false;
      }
    },

    // 2. Thêm sản phẩm vào giỏ
    addToCart(product: PosProduct) {
      this.errorMsg = "";

      const existingItem = this.cart.find(
        (item) => item.product.sku === product.sku
      );

      if (existingItem) {
        if (existingItem.quantity >= product.stockQuantity) {
          this.errorMsg = `Sản phẩm ${product.name} đã đạt giới hạn tồn kho tối đa (${product.stockQuantity})!`;
          return;
        }

        existingItem.quantity++;
      } else {
        if (product.stockQuantity <= 0) {
          this.errorMsg = `Sản phẩm ${product.name} đã hết hàng trong kho!`;
          return;
        }

        this.cart.push({ product, quantity: 1 });
      }
    },

    // 3. Quét mã vạch
    async handleBarcodeScan(sku: string) {
      this.errorMsg = "";

      const cleanSku = sku.trim();
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
          params: { sku: cleanSku },
        });

        if (data) {
          const mappedProduct: PosProduct = {
            id: data.variantId || data.productId,
            sku: data.sku,
            name: data.productName,
            subName: data.capacityLabel || "",
            price: Number(data.price || 0),
            stockQuantity: Number(data.stockQuantity || 0),
            image:
              data.imageUrl ||
              `https://ui-avatars.com/api/?name=${encodeURIComponent(
                data.productName || "Product"
              )}&background=random&color=fff`,
            category: data.brandName || "Tất cả",
          };

          this.addToCart(mappedProduct);
        }
      } catch (error: any) {
        this.errorMsg =
          error.response?.data?.message ||
          `Mã vạch SKU "${cleanSku}" không tồn tại trên hệ thống!`;
      }
    },

    // 4. Cập nhật số lượng
    updateQuantity(sku: string, qty: number) {
      const item = this.cart.find((i) => i.product.sku === sku);
      if (!item) return;

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
      this.cart = this.cart.filter((item) => item.product.sku !== sku);
    },

    // 5. Tìm khách hàng
    async searchCustomer(phone: string) {
      if (!phone.trim()) return;

      this.errorMsg = "";

      try {
        const { data } = await api.get("/admin/pos/customer", {
          params: { phone },
        });

        if (data && data.found) {
          this.customer = data.customer;
        } else {
          this.customer = {
            name: "",
            phone,
            loyaltyPoints: 0,
          };

          this.errorMsg =
            data.message || "Khách hàng mới! Vui lòng nhập thêm tên để tích điểm.";
        }
      } catch (error) {
        this.customer = {
          name: "",
          phone,
          loyaltyPoints: 0,
        };

        this.errorMsg = "Không thể kiểm tra SĐT, đơn sẽ tính là khách mới.";
      }
    },

    // 6. Checkout
    // Sửa chính ở hàm này:
    // - Vẫn giữ logic cũ.
    // - Nhưng return { success, data } để file Cart lấy được orderId thật.
    // - Không reset cart trong này để tránh mất dữ liệu in hóa đơn.
    async processCheckout(extra?: {
      paymentMethod?: "CASH" | "VNPAY";
      cashGiven?: number;
      changeAmount?: number;
    }) {
      if (this.cart.length === 0) {
        this.errorMsg = "Giỏ hàng đang trống, không thể thanh toán!";
        return false;
      }

      this.isLoading = true;
      this.errorMsg = "";

      const selectedPaymentMethod = extra?.paymentMethod || this.paymentMethod;

      const payload = {
        customerPhone: this.customer?.phone?.trim() || null,
        customerName: this.customer?.name?.trim() || null,
        voucherCode: this.voucherCode?.trim() || null,
        paymentMethod: selectedPaymentMethod.toUpperCase(),
        cashGiven: extra?.cashGiven || null,
        changeAmount: extra?.changeAmount || null,
        items: this.cart.map((item) => ({
          sku: item.product.sku,
          quantity: Number(item.quantity),
        })),
      };

      try {
        const { data } = await api.post("/admin/pos/checkout", payload);

        const orderId =
          data?.orderId ||
          data?.id ||
          data?.code ||
          data?.orderCode ||
          data?.invoiceCode ||
          null;

        this.lastOrderId = orderId;

        if (selectedPaymentMethod === "VNPAY") {
          if (data.vnpayPaymentUrl) {
            this.vnpayUrl = data.vnpayPaymentUrl;
            window.location.href = data.vnpayPaymentUrl;

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
        this.errorMsg =
          error.response?.data?.message ||
          "Thanh toán thất bại. Vui lòng kiểm tra lại!";
        return false;
      } finally {
        this.isLoading = false;
      }
    },

    // 7. Reset đơn mới
    startNewOrder() {
      this.cart = [];
      this.customer = null;
      this.voucherCode = "";
      this.discountAmount = 0;
      this.vnpayUrl = "";
      this.errorMsg = "";
      this.lastOrderId = null;
      this.fetchProducts();
    },
  },
});