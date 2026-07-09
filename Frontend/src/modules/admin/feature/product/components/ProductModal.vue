<script setup lang="ts">
import { ref, onMounted } from 'vue'
import Swal from 'sweetalert2'

import type {
  Product,
  ProductRequestDTO,
  ProductVariant,
  Brand,
  Category,
  Concentration,
  Capacity,
  BottleType,
  FragranceFamily
} from '../types/product.type'

import { productService } from '../services/productService'
import { useAppStore } from '@/common/store/app.store'

const props = defineProps<{
  productSelected: Product | null

  brandList: Brand[]

  categoryList: Category[]

  concentrationList: Concentration[]

  capacityList: Capacity[]

  bottleTypeList: BottleType[]

  fragranceFamilyList: FragranceFamily[]
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'refresh'): void
}>()

const appStore = useAppStore()

const isEdit = ref(false)

const formData = ref<ProductRequestDTO>({
  name: '',
  description: '',
  brandId: 0,
  categoryId: 0,
  concentrationId: 0,
  gender: 3,
  isNiche: false,
  status: 1,
  fragranceFamilyIds: [],
  variants: []
})

onMounted(() => {
  if (!props.productSelected) {
    addVariant()
    return
  }

  isEdit.value = true

  formData.value = {
    name: props.productSelected.name,
    description: props.productSelected.description ?? '',

    brandId: props.productSelected.brandId,

    categoryId: props.productSelected.categoryId,

    concentrationId:
      props.productSelected.concentrationId,

    gender:
      props.productSelected.gender ?? 3,

    isNiche:
      props.productSelected.isNiche ?? false,

    status:
      props.productSelected.status ?? 1,

    fragranceFamilyIds:
      props.productSelected.fragranceFamilies?.map(
        item => item.id
      ) || [],

    variants:
      props.productSelected.variants?.map(v => ({
        id: v.id,

        capacityId: v.capacityId,

        bottleTypeId: v.bottleTypeId,

        sku: v.sku,

        price: v.price,

        stockQuantity: v.stockQuantity,

        status: v.status ?? 1
      })) || []
  }

  if (formData.value.variants.length === 0) {
    addVariant()
  }
})

const addVariant = () => {
  const variant: ProductVariant = {
    capacityId: 0,
    bottleTypeId: 0,
    sku: '',
    price: 0,
    stockQuantity: 0,
    status: 1
  }

  formData.value.variants.push(variant)
}

const removeVariant = (index: number) => {
  formData.value.variants.splice(index, 1)

  if (formData.value.variants.length === 0) {
    addVariant()
  }
}

const saveData = async () => {
  try {
    appStore.startLoading()

    if (
      isEdit.value &&
      props.productSelected
    ) {
      await productService.updateProduct(
        props.productSelected.id,
        formData.value
      )
    } else {
      await productService.createProduct(
        formData.value
      )
    }

    appStore.notifySuccess(
      isEdit.value
        ? 'Cập nhật sản phẩm thành công'
        : 'Thêm sản phẩm thành công'
    )

    emit('refresh')
    emit('close')
  } catch (error: any) {
    Swal.fire({
      icon: 'error',
      title: 'Lỗi',
      text:
        error?.response?.data?.message ||
        'Không thể lưu sản phẩm'
    })
  } finally {
    appStore.stopLoading()
  }
}
</script>