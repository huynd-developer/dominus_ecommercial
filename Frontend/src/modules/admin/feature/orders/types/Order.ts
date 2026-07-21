export interface Order {
  id: number

  customerId?: number
  customerName: string
  customerPhone: string

  cashierId?: number
  cashierName?: string

  orderType: string

  totalAmount: number
  discountAmount: number
  finalAmount: number

  paymentMethod: string

  status: number

  createdAt: string

  completedAt?: string
}