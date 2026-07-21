import type {OrderItem} from "./OrderItem";

export interface OrderDetail{

    id:number

    customerId?:number

    customerName:string

    customerPhone:string

    shippingAddress:string

    cashierId?:number

    cashierName?:string

    orderType:string

    totalAmount:number

    discountAmount:number

    finalAmount:number

    paymentMethod:string

    status:number

    createdAt:string

    completedAt?:string

    items:OrderItem[]

}