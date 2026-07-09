<template>
  <div class="page-wrapper">
    <ShopHeader />

    <main class="single-product-main">
      <div
        v-if="isLoading"
        style="padding: 100px; text-align: center; color: #666;"
      >
        Đang tải thông tin sản phẩm...
      </div>

      <div
        v-else-if="!product"
        style="padding: 100px; text-align: center; color: #e53e3e;"
      >
        Không tìm thấy sản phẩm!
      </div>

      <ProductDetail
        v-else
        :product="product"
        @back="goBack"
        @buy-now="handleBuyNow"
      />
    </main>

    <ShopFooter />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import Swal from "sweetalert2";
import api from "@/common/api";

import ShopHeader from "@/modules/shop/layout/ShopHeader.vue";
import ProductDetail from "@/modules/shop/feature/product/components/ProductDetail.vue";
import ShopFooter from "@/modules/shop/layout/ShopFooter.vue";

const route = useRoute();
const router = useRouter();

const product = ref<any>(null);
const isLoading = ref(true);

const extractArrayData = (data: any) => {
  return data?.data?.content || data?.data || data?.content || data || [];
};

const extractObjectData = (data: any) => {
  return data?.data || data || null;
};

const extractCapacity = (variant: any) => {
  if (!variant) return "N/A";

  let value = null;

  if (variant.capacity && typeof variant.capacity === "object") {
    value = variant.capacity.value ?? variant.capacity.name;
  } else if (variant.capacityValue != null) {
    value = variant.capacityValue;
  } else if (variant.volume != null) {
    value = variant.volume;
  } else if (variant.capacity != null) {
    value = variant.capacity;
  }

  if (value == null || value === "") {
    return "N/A";
  }

  const numeric = Number(value);

  if (!Number.isNaN(numeric)) {
    return `${numeric}ml`;
  }

  const text = String(value);

  return text.toLowerCase().includes("ml") ? text : `${text}ml`;
};

const normalizeStock = (variant: any) => {
  return Number(
    variant?.stock ??
      variant?.stockQuantity ??
      variant?.StockQuantity ??
      variant?.quantity ??
      variant?.availableQuantity ??
      0
  );
};

const mapVariant = (variant: any) => {
  const stock = normalizeStock(variant);

  return {
    ...variant,
    id: variant?.id || variant?.Id,
    sku: variant?.sku || variant?.SKU || "",
    capacity: extractCapacity(variant),
    price: Number(variant?.price || variant?.Price || 0),
    stock,
    stockQuantity: stock,
    bottleType:
      typeof variant?.bottleType === "object"
        ? variant?.bottleType?.name
        : variant?.bottleTypeName || variant?.bottleType || "",
  };
};

const mapProduct = (p: any, rawVariants: any[]) => {
  const mappedVariants = Array.isArray(rawVariants)
    ? rawVariants.map(mapVariant).sort((a: any, b: any) => {
        const valA = parseFloat(String(a.capacity).replace("ml", "")) || 0;
        const valB = parseFloat(String(b.capacity).replace("ml", "")) || 0;
        return valA - valB;
      })
    : [];

  return {
    ...p,
    id: p?.id || p?.Id,
    name: p?.name || p?.Name || "Sản phẩm",
    brand:
      typeof p?.brand === "object"
        ? p?.brand?.name
        : p?.brandName || p?.brand || "Premium",
    image:
      p?.imageUrl ||
      p?.ImageUrl ||
      p?.image ||
      "data:image/svg+xml;utf8," +
        encodeURIComponent(`
          <svg xmlns="http://www.w3.org/2000/svg" width="300" height="300">
            <rect width="100%" height="100%" fill="#f3f4f6"/>
            <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle"
              fill="#9ca3af" font-family="Arial" font-size="20">
              No Image
            </text>
          </svg>
        `),
    description: p?.description || p?.Description || "",
    rating: Number(p?.rating || 0),
    variants: mappedVariants,
    price:
      mappedVariants.length > 0
        ? mappedVariants[0].price
        : Number(p?.price || p?.Price || 0),
  };
};

const loadProductDetail = async () => {
  const productId = Number(route.params.id);

  if (!productId || Number.isNaN(productId)) {
    product.value = null;
    isLoading.value = false;
    return;
  }

  try {
    isLoading.value = true;
    product.value = null;

    const productRes = await api.get(`/products/${productId}`);
    const productData = extractObjectData(productRes.data);

    let variantData: any[] = [];

    try {
      const variantRes = await api.get(`/products/${productId}/variants`);
      variantData = extractArrayData(variantRes.data);
    } catch (variantError) {
      variantData =
        productData?.variants ||
        productData?.productVariants ||
        productData?.productVariantList ||
        [];
    }

    product.value = mapProduct(productData, variantData);
  } catch (error: any) {
    console.error("Lỗi tải chi tiết sản phẩm:", error);

    product.value = null;

    await Swal.fire({
      icon: "error",
      title: "Không tải được sản phẩm",
      text:
        error?.response?.data?.message ||
        "Sản phẩm không tồn tại hoặc đã ngừng kinh doanh.",
      confirmButtonText: "Quay lại danh sách",
      confirmButtonColor: "#bd9a5f",
    });

    router.replace({
      name: "ProductList",
    });
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  loadProductDetail();
});

watch(
  () => route.params.id,
  () => {
    loadProductDetail();
  }
);

const goBack = () => {
  const previousPath = window.history.state?.back;

  if (previousPath) {
    router.back();
    return;
  }

  router.push({
    name: "ProductList",
  });
};

const handleBuyNow = () => {
  router.push({
    name: "Checkout",
  });
};
</script>

<style scoped>
.page-wrapper {
  font-family: "Inter", sans-serif;
  background-color: #ffffff;
  min-height: 100vh;
}

.single-product-main {
  max-width: 1320px;
  margin: 40px auto;
  padding: 0 20px;
  min-height: 50vh;
}
</style>