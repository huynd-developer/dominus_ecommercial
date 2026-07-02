import { defineStore } from 'pinia'
import api from '@/common/api' 

// Khai báo kiểu dữ liệu chuẩn
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
  paymentMethod: 'CASH' | 'VNPAY';
  vnpayUrl: string;
  isLoading: boolean;
  errorMsg: string;
}

export const usePosStore = defineStore('posStore', {
  state: (): PosStoreState => ({
    allProducts: [],
    categories: ['Tất cả'],
    selectedCategory: 'Tất cả',
    searchQuery: '',
    cart: [],
    customer: null,
    voucherCode: '',
    discountAmount: 0,
    paymentMethod: 'CASH',
    vnpayUrl: '',
    isLoading: false,
    errorMsg: ''
  }),

  getters: {
    // Bộ lọc sản phẩm theo danh mục và ô tìm kiếm tên/SKU
    filteredProducts(state) {
      return state.allProducts.filter(product => {
        const matchesCategory = state.selectedCategory === 'Tất cả' || product.category === state.selectedCategory;
        const matchesSearch = product.name.toLowerCase().includes(state.searchQuery.toLowerCase()) || 
                              product.sku.toLowerCase().includes(state.searchQuery.toLowerCase());
        return matchesCategory && matchesSearch;
      });
    },

    // Tổng tiền hàng gốc chưa giảm giá
    totalAmount(state): number {
      return state.cart.reduce((total, item) => total + (item.product.price * item.quantity), 0);
    },

    // Số tiền khách thực tế phải trả sau khi trừ voucher
    finalAmount(): number {
      const final = this.totalAmount - this.discountAmount;
      return final > 0 ? final : 0;
    }
  },

  actions: {
    // 1. Tải danh sách sản phẩm và đập phẳng Biến thể ra
    async fetchProducts() {
      this.isLoading = true;
      try {
        const { data } = await api.get('/admin/products');
        const rawProducts = data.data || data.content || data;
        const flatProducts: PosProduct[] = [];

        rawProducts.forEach((p: any) => {
          let imgUrl = p.images && p.images.length > 0 ? p.images[0].imageUrl : '';
          if (!imgUrl || !imgUrl.startsWith('http')) {
            imgUrl = `https://ui-avatars.com/api/?name=${encodeURIComponent(p.name)}&background=random&color=fff&size=200`;
          }

          if (p.variants && p.variants.length > 0) {
            p.variants.forEach((v: any) => {
              flatProducts.push({
                id: p.id,
                sku: v.sku, 
                name: p.name,
                subName: `${v.capacityValue}ml - ${v.bottleTypeName}`, 
                price: v.price,               
                stockQuantity: v.stockQuantity, 
                image: imgUrl,
                category: p.categoryName || 'Tất cả'
              });
            });
          }
        });

        this.allProducts = flatProducts;
        const dynamicCategories = ['Tất cả', ...new Set(flatProducts.map((p: any) => p.category).filter(Boolean))] as string[];
        this.categories = dynamicCategories;

      } catch (error) {
        this.errorMsg = 'Không thể tải danh sách sản phẩm từ máy chủ!';
      } finally {
        this.isLoading = false;
      }
    },

    // 2. Thêm sản phẩm vào giỏ hàng
    addToCart(product: PosProduct) {
      this.errorMsg = '';
      const existingItem = this.cart.find(item => item.product.sku === product.sku);

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

    // 3. SỬA: Quét mã vạch (Gọi chuẩn endpoint PosController: /api/admin/pos/product?sku=...)
    async handleBarcodeScan(sku: string) {
      this.errorMsg = '';
      const cleanSku = sku.trim();
      if (!cleanSku) return;

      // Check nhanh dưới local trước
      const localProduct = this.allProducts.find(p => p.sku.toLowerCase() === cleanSku.toLowerCase());
      if (localProduct) {
        this.addToCart(localProduct);
        return;
      }

      // Nếu không thấy thì gọi API check real-time qua PosController
      try {
        const { data } = await api.get('/admin/pos/product', { params: { sku: cleanSku } });
        if (data) {
          const mappedProduct: PosProduct = {
            id: data.productId,
            sku: data.sku,
            name: data.productName,
            subName: `${data.capacityValue}ml - ${data.bottleTypeName}`,
            price: data.price,
            stockQuantity: data.stockQuantity,
            image: data.image || `https://ui-avatars.com/api/?name=${encodeURIComponent(data.productName)}&background=random&color=fff`,
            category: data.categoryName || 'Tất cả'
          };
          this.addToCart(mappedProduct);
        }
      } catch (error: any) {
        this.errorMsg = error.response?.data?.message || `Mã vạch SKU "${cleanSku}" không tồn tại trên hệ thống!`;
      }
    },

    // 4. Cập nhật số lượng thủ công bằng nút + / -
    updateQuantity(sku: string, qty: number) {
      const item = this.cart.find(i => i.product.sku === sku);
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
      this.cart = this.cart.filter(item => item.product.sku !== sku);
    },

    // 5. SỬA: Tìm kiếm khách hàng bằng SĐT (Gọi chuẩn PosController: /api/admin/pos/customer?phone=...)
    async searchCustomer(phone: string) {
      if (!phone.trim()) return;
      this.errorMsg = '';
      try {
        const { data } = await api.get('/admin/pos/customer', { params: { phone } });
        if (data && data.found) {
          this.customer = data.customer;
        } else {
          this.customer = null;
          this.errorMsg = data.message || 'Hóa đơn sẽ tính dưới dạng khách vãng lai.';
        }
      } catch (error) {
        this.customer = null;
        this.errorMsg = 'Lỗi kết nối khi tìm kiếm khách hàng!';
      }
    },

    // 6. SỬA: Gọi lệnh Thanh toán (Mở trên cùng 1 tab duy nhất)
    async processCheckout() {
      if (this.cart.length === 0) {
        this.errorMsg = 'Giỏ hàng đang trống, không thể thanh toán!';
        return;
      }

      this.isLoading = true;
      this.errorMsg = '';

      const payload = {
        customerPhone: this.customer?.phone || null,
        voucherCode: this.voucherCode || null,
        paymentMethod: this.paymentMethod,
        items: this.cart.map(item => ({
          sku: item.product.sku,
          quantity: item.quantity
        }))
      };

      try {
        const { data } = await api.post('/admin/pos/checkout', payload);

        if (this.paymentMethod === 'VNPAY') {
          if (data.vnpayPaymentUrl) {
            this.vnpayUrl = data.vnpayPaymentUrl;
            
            // 🔥 SỬA TẠI ĐÂY: Điều hướng ngay trên tab hiện tại, đóng luồng POS cũ lại
            window.location.href = data.vnpayPaymentUrl; 
          } else {
            this.errorMsg = 'Không nhận được link thanh toán VNPay từ máy chủ!';
          }
        } else {
          // Trả tiền mặt thành công -> In hóa đơn và dọn giỏ
          alert('Thanh toán tiền mặt thành công! Hệ thống đang xử lý hóa đơn...');
          this.startNewOrder();
        }
      } catch (error: any) {
        this.errorMsg = error.response?.data?.message || 'Thanh toán thất bại. Vui lòng kiểm tra lại!';
      } finally {
        this.isLoading = false;
      }
    },

    // 7. Reset toàn bộ giỏ hàng để đón khách tiếp theo
    startNewOrder() {
      this.cart = [];
      this.customer = null;
      this.voucherCode = '';
      this.discountAmount = 0;
      this.vnpayUrl = '';
      this.errorMsg = '';
      this.fetchProducts(); // Tải lại để cập nhật số lượng tồn kho mới nhất
    }
  }
})