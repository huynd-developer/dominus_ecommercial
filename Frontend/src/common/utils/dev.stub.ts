// frontend/src/common/utils/dev.stub.ts
export const DEV_STUB = {
  // STUB: Hardcode 1 User có thật trong DB của bạn (VD ID = 1)
  MOCK_USER: {
    id: 1, 
    fullName: 'Nguyễn Kiều Diễm Ly',
    phone: '0901234567',
    email: 'diemly@gmail.com'
  },
  AVAILABLE_VOUCHERS: [
    { code: 'LATRA100K', description: 'Giảm 100K cho đơn từ 5 Triệu' },
    { code: 'FREESHIP', description: 'Miễn phí vận chuyển (Tối đa 30K)' },
    { code: 'NEWYEAR26', description: 'Giảm 5% tối đa 500K' }
  ],
  LOCATIONS: {
    provinces: ['Hà Nội', 'TP. Hồ Chí Minh', 'Đà Nẵng'],
    districts: ['Quận 1', 'Quận 3', 'Quận Cầu Giấy', 'Quận Đống Đa'],
    wards: ['Phường 1', 'Phường 2', 'Phường Bến Nghé', 'Phường Dịch Vọng']
  }
};