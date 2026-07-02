import { defineStore } from 'pinia'
import api from '@/common/api' // Đảm bảo file api.ts của mày đã cấu hình baseURL có chứa '/api'

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
    // 1. Tải danh sách sản phẩm thật từ Backend (Đã sửa link chuẩn theo AdminProductController)
    async fetchProducts() {
      this.isLoading = true;
      try {
        // Backend đang là: @RequestMapping("/api/admin/products")
        const { data } = await api.get('/admin/products');
        
        // Spring Boot có thể trả về Page (có .content) hoặc trả thẳng list, hoặc bọc trong .data
        const productList = data.data || data.content || data;
        this.allProducts = productList;
        
        // Tự động gom nhóm các Category từ sản phẩm thật về
        const dynamicCategories = ['Tất cả', ...new Set(productList.map((p: any) => p.category).filter(Boolean))] as string[];
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

    // 3. Nghiệp vụ súng bắn mã vạch quét SKU (Đã sửa link chuẩn theo PublicProductController)
    async handleBarcodeScan(sku: string) {
      this.errorMsg = '';
      const cleanSku = sku.trim();
      if (!cleanSku) return;

      // Tìm nhanh trong danh sách sản phẩm đã tải về máy
      const localProduct = this.allProducts.find(p => p.sku.toLowerCase() === cleanSku.toLowerCase());
      if (localProduct) {
        this.addToCart(localProduct);
        return;
      }

      // Nếu không thấy, gọi API check real-time: @GetMapping("/variants/sku/{sku}")
      try {
        const { data } = await api.get(`/products/variants/sku/${cleanSku}`);
        // Response trả về có dạng Map.of("data", variant...)
        if (data && data.data) {
          this.addToCart(data.data);
        }
      } catch (error: any) {
        this.errorMsg = `Mã vạch SKU "${cleanSku}" không tồn tại trên hệ thống!`;
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

    // 5. API Tìm kiếm khách hàng bằng số điện thoại
    async searchCustomer(phone: string) {
      if (!phone.trim()) return;
      try {
        // LƯU Ý: BE chưa có API này, tao cấu hình chuẩn RESTful, mày bảo BE viết API này nhé!
        const { data } = await api.get(`/admin/customers/phone/${phone}`);
        if (data && data.data) {
          this.customer = data.data;
        } else {
          this.customer = null;
          this.errorMsg = 'Không tìm thấy thông tin thành viên, hóa đơn sẽ tính là khách vãng lai.';
        }
      } catch (error) {
        this.customer = null;
        this.errorMsg = 'Không tìm thấy khách hàng hoặc lỗi kết nối!';
      }
    },

    // 6. API Gọi lệnh Thanh toán (Checkout) gửi lên Backend
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
        // LƯU Ý: BE cũng chưa có API tạo đơn này! Mày bảo BE viết thêm POST /api/admin/orders/checkout nhé!
        const { data } = await api.post('/admin/orders/checkout', payload);

        if (this.paymentMethod === 'VNPAY') {
          if (data.vnpayPaymentUrl) {
            this.vnpayUrl = data.vnpayPaymentUrl;
            window.open(data.vnpayPaymentUrl, '_blank'); // Bật tab mới quét mã QR VNPay
          } else {
            this.errorMsg = 'Không nhận được link thanh toán VNPay từ máy chủ!';
          }
        } else {
          // Trả tiền mặt thành công -> In hóa đơn và dọn giỏ
          alert('Thanh toán tiền mặt thành công! Hệ thống đang in hóa đơn...');
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