export interface CustomerAddress {
  id?: number;
  customerId: number;
  recipientName: string;
  phone: string;
  provinceCode: string;
  provinceName: string;
  wardCode: string;
  wardName: string;
  specificAddress: string;
  fullAddress: string;
  isDefault: boolean;
}