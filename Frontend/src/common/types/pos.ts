// ============================================================
// TYPE KHỚP 1:1 VỚI DTO BACKEND (org.example.datn_sd69.modules.pos.dto.*)
// Suy ra từ PosServiceImpl.builder(...) — nếu BE đổi field thì sửa ở đây trước
// ============================================================

// ---- Response khi tìm SKU/barcode: ProductVariantPosResponse ----
export interface ProductVariantPosResponse {
  variantId: number
  sku: string
  productName: string
  brandName: string
  capacityLabel: string   // ví dụ "50 ml"
  bottleTypeName: string
  price: number            // BigDecimal -> number khi qua JSON
  stockQuantity: number
  imageUrl: string | null
}

// ---- Response tìm khách theo SĐT: CustomerPosResponse ----
export interface CustomerPosResponse {
  customerId: number
  name: string
  phone: string
  email: string
  customerRank: string
  loyaltyPoints: number
}

// Wrapper của GET /api/admin/pos/customer (PosController trả Map thủ công)
export interface CustomerLookupResult {
  found: boolean
  message?: string
  customer?: CustomerPosResponse
}

// ---- Request item trong giỏ: PosItemRequest ----
export interface PosItemRequest {
  sku: string
  quantity: number
}

// ---- Request checkout: PosCheckoutRequest ----
export interface PosCheckoutRequest {
  items: PosItemRequest[]
  paymentMethod: 'CASH' | 'VNPAY'
  customerPhone?: string | null
  voucherCode?: string | null
}

// ---- Response checkout: PosOrderResponse ----
export interface PosOrderInvoiceItem {
  productName: string
  sku: string
  capacityLabel: string
  quantity: number
  unitPrice: number
  lineTotal: number
}

export interface PosOrderResponse {
  orderId: number
  status: 'COMPLETED' | 'PENDING_PAYMENT'
  totalAmount: number
  discountAmount: number
  finalAmount: number
  paymentMethod: 'CASH' | 'VNPAY'
  vnpayPaymentUrl: string | null
  createdAt: string
  cashierName: string | null
  customerName: string | null
  customerPhone: string | null
  items: PosOrderInvoiceItem[]
}

// ---- Lỗi nghiệp vụ trả về dạng { message: string } khi RuntimeException ----
export interface ApiErrorBody {
  message: string
}

// ============================================================
// TYPE DÙNG RIÊNG CHO GIỎ HÀNG Ở FE (CartItem)
// Mở rộng từ ProductVariantPosResponse + quantity đang chọn trong giỏ
// id dùng để key trong v-for = variantId, KHÔNG dùng product.id (đã đổi tên field cho khớp)
// ============================================================
export interface CartItem {
  id: number               // = variantId, dùng làm key + truyền cho decreaseQuantity/removeItem
  sku: string
  name: string              // = productName, đổi tên để khớp với CartSideBar.vue (item.name)
  price: number
  quantity: number
  stockQuantity: number      // để FE tự chặn không cho cộng quá tồn kho hiển thị (tồn kho thật do BE validate lại lúc checkout)
  imageUrl: string | null
}

// ============================================================
// TYPE CHO DỮ LIỆU SẢN PHẨM HIỂN THỊ Ở PRODUCTGRID (MOCK TẠM THỜI)
// Khi BE có API list sản phẩm thật, đổi shape này khớp với response thật
// rồi chỉ cần sửa hàm fetchProducts() trong store, component không đổi gì
// ============================================================
export interface PosProduct {
  id: number          // map sang variantId khi addToCart thật
  sku: string          // QUAN TRỌNG: phải là SKU thật tồn tại trong DB khi nối API thật,
                        // nếu không findVariantBySku ở BE sẽ throw "Không tìm thấy sản phẩm"
  name: string
  subName?: string
  price: number
  stockQuantity: number
  image: string
  category: string
}