export interface Brand {
  id: number

  brandName?: string

  name?: string
}

export interface Category {
  id: number

  categoryName?: string

  name?: string
}

export interface Concentration {
  id: number

  concentrationName?: string

  name?: string
}

export interface BottleType {
  id: number

  bottleTypeName?: string

  name?: string
}

export interface Capacity {
  id: number

  capacityValue?: string

  value?: string

  name?: string
}

export interface FragranceFamily {
  id: number

  familyName?: string

  name?: string
}

export interface ProductVariant {
  id?: number

  capacityId: number
  capacityName?: string

  bottleTypeId: number
  bottleTypeName?: string

  sku: string

  price: number

  stockQuantity: number

  manufacturingDate: string

  expirationDate: string

  status: number
}

export interface Product {

  id: number

  name: string

  description?: string

  brandId: number

  brandName?: string

  categoryId: number

  categoryName?: string

  concentrationId: number

  concentrationName?: string

  gender: number

  isNiche: boolean

  status: number

  primaryImageUrl?: string

  fragranceFamilies: FragranceFamily[]

  variants: ProductVariant[]

}

export interface ProductRequestDTO {
  name: string

  description?: string

  brandId: number

  categoryId: number

  concentrationId: number

  gender?: number

  isNiche?: boolean

  status?: number

  fragranceFamilyIds: number[]

  variants: ProductVariant[]
}

export interface ProductResponse extends Product {}