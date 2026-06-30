// ============================================================
// DỮ LIỆU GIẢ (MOCK) — DÙNG TẠM CHO ĐẾN KHI BE CÓ API LIST SẢN PHẨM
// ============================================================
// CÁCH GỠ BỎ MOCK SAU NÀY (đọc kỹ trước khi xoá file này):
// 1. BE viết thêm API, ví dụ: GET /api/admin/pos/products
//    -> trả về danh sách ProductVariantPosResponse[] (hoặc tương tự)
// 2. Trong modules/pos/stores/posStore.ts, sửa hàm `fetchProducts()`:
//    - Xoá dòng `this.allProducts = [...MOCK_PRODUCTS]`
//    - Thay bằng: const { data } = await api.get('/admin/pos/products')
//                 this.allProducts = data.map(mapVariantToPosProduct)
// 3. Xoá import MOCK_PRODUCTS, MOCK_CATEGORIES khỏi posStore.ts
// 4. Xoá file này (mockData.ts)
//
// LƯU Ý QUAN TRỌNG: sku ở dưới đây phải khớp với SKU thật trong DB
// (cột product_variants.sku) nếu muốn bấm vào card mà gọi checkout thật
// chạy được — vì BE tìm theo SKU (findBySkuWithDetails), không tìm theo id.
// Nếu SKU không tồn tại trong DB, checkout sẽ bị BE trả lỗi
// "SKU không hợp lệ: ...".
// ============================================================

import type { PosProduct } from '../types/pos'

export const MOCK_CATEGORIES: string[] = [
  'Tất cả',
  'Nước hoa Nam',
  'Nước hoa Nữ',
  'Unisex',
  'Set quà tặng',
  'Mini & Travel size',
]

export const MOCK_PRODUCTS: PosProduct[] = [
  {
    id: 1,
    sku: 'PEF-DIOR-SAUV-100',
    name: 'Dior Sauvage EDP',
    subName: '100ml - Nam tính, lôi cuốn',
    price: 3250000,
    stockQuantity: 12,
    image: 'https://images.unsplash.com/photo-1592945403244-b3fbafd7f539?w=400',
    category: 'Nước hoa Nam',
  },
  {
    id: 2,
    sku: 'PEF-CHANEL-NO5-100',
    name: 'Chanel No.5 EDP',
    subName: '100ml - Hoa cỏ cổ điển',
    price: 4150000,
    stockQuantity: 8,
    image: 'https://images.unsplash.com/photo-1541643600914-78b084683601?w=400',
    category: 'Nước hoa Nữ',
  },
  {
    id: 3,
    sku: 'PEF-YSL-LIBRE-90',
    name: 'YSL Libre EDP',
    subName: '90ml - Hoa oải hương',
    price: 2980000,
    stockQuantity: 15,
    image: 'https://images.unsplash.com/photo-1523293182086-7651a899d37f?w=400',
    category: 'Nước hoa Nữ',
  },
  {
    id: 4,
    sku: 'PEF-VERSACE-ERO-100',
    name: 'Versace Eros EDT',
    subName: '100ml - Gỗ thơm mạnh mẽ',
    price: 2150000,
    stockQuantity: 20,
    image: 'https://images.unsplash.com/photo-1587017539504-67cfbddac569?w=400',
    category: 'Nước hoa Nam',
  },
  {
    id: 5,
    sku: 'PEF-TOMFORD-OUD-50',
    name: 'Tom Ford Oud Wood',
    subName: '50ml - Gỗ trầm sang trọng',
    price: 5450000,
    stockQuantity: 5,
    image: 'https://images.unsplash.com/photo-1615634260167-c8cdede054de?w=400',
    category: 'Unisex',
  },
  {
    id: 6,
    sku: 'PEF-GUCCI-BLOOM-50',
    name: 'Gucci Bloom EDP',
    subName: '50ml - Hoa nhài tươi mới',
    price: 2650000,
    stockQuantity: 10,
    image: 'https://images.unsplash.com/photo-1556228720-195a672e8a03?w=400',
    category: 'Nước hoa Nữ',
  },
  {
    id: 7,
    sku: 'PEF-ARMANI-CODE-100',
    name: 'Armani Code EDT',
    subName: '100ml - Ấm nồng quyến rũ',
    price: 1980000,
    stockQuantity: 18,
    image: 'https://images.unsplash.com/photo-1563170351-be82bc888aa4?w=400',
    category: 'Nước hoa Nam',
  },
  {
    id: 8,
    sku: 'PEF-SET-COUPLE-01',
    name: 'Set quà tặng Couple',
    subName: '2x30ml - Nam & Nữ',
    price: 1750000,
    stockQuantity: 7,
    image: 'https://images.unsplash.com/photo-1547887537-6158d64c35b3?w=400',
    category: 'Set quà tặng',
  },
  {
    id: 9,
    sku: 'PEF-MINI-TRAVEL-10',
    name: 'Mini Travel Set',
    subName: '4x10ml - Tiện mang theo',
    price: 890000,
    stockQuantity: 25,
    image: 'https://images.unsplash.com/photo-1615528480245-7ea58202fbc1?w=400',
    category: 'Mini & Travel size',
  },
]

// Khách hàng giả — dùng khi cần test nhanh không phải gõ SĐT thật mỗi lần
export const MOCK_CUSTOMERS = [
  { phone: '0901111111', name: 'Nguyễn Văn A (mock)' },
  { phone: '0902222222', name: 'Trần Thị B (mock)' },
]