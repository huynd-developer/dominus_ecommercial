<template>
  <div class="page-wrapper">
    <ShopHeader />

    <div class="product-layout">
      <SidebarFilter @filter-change="handleFilterChange" />

      <main class="product-main">
        <div
          v-if="isLoading"
          style="padding: 50px; text-align: center; color: #666;"
        >
          Đang tải danh sách sản phẩm...
        </div>

        <ProductGrid
          v-else
          :product-list="filteredProductList"
          @open-detail="handleOpenDetail"
        />
      </main>
    </div>

    <ShopFooter />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import api from "@/common/api";

import ShopHeader from "@/modules/shop/layout/ShopHeader.vue";
import SidebarFilter from "@/modules/shop/feature/product/components/SidebarFilter.vue";
import ProductGrid from "@/modules/shop/feature/product/components/ProductGrid.vue";
import ShopFooter from "@/modules/shop/layout/ShopFooter.vue";

const router = useRouter();
const route = useRoute();

const productList = ref<any[]>([]);
const isLoading = ref(false);

// === STATE CHO PHÂN TRANG ===
const currentPage = ref(1);
const itemsPerPage = ref(6); // Số sản phẩm trên 1 trang (M có thể đổi thành 12, 16 tùy ý)

const activeFilters = ref<any>({
  genders: [],
  fragranceFamilies: [],
  capacities: [],
  bottleTypes: [],
  concentrations: [],
});

const handleFilterChange = (filters: any) => {
  activeFilters.value = {
    genders: filters?.genders || [],
    fragranceFamilies: filters?.fragranceFamilies || [],
    capacities: filters?.capacities || [],
    bottleTypes: filters?.bottleTypes || [],
    concentrations: filters?.concentrations || [],
  };
};

const extractArrayData = (data: any) => {
  return data?.data?.content || data?.data || data?.content || data || [];
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

const mapProduct = (item: any) => {
  const rawVariants =
    item?.variants ||
    item?.productVariants ||
    item?.productVariantList ||
    [];

  const mappedVariants = Array.isArray(rawVariants)
    ? rawVariants.map(mapVariant).sort((a: any, b: any) => {
        const valA = parseFloat(String(a.capacity).replace("ml", "")) || 0;
        const valB = parseFloat(String(b.capacity).replace("ml", "")) || 0;
        return valA - valB;
      })
    : [];

  let genderText = "";

  if (item?.gender === 1) genderText = "Nam";
  else if (item?.gender === 2) genderText = "Nữ";
  else if (item?.gender === 0) genderText = "Unisex";
  else {
    genderText =
      typeof item?.gender === "object"
        ? item?.gender?.name || ""
        : item?.gender || "";
  }

  let mappedScents: string[] = [];

  if (item?.fragranceFamily) {
    mappedScents.push(
      item.fragranceFamily.name || item.fragranceFamily.Name || ""
    );
  } else if (Array.isArray(item?.fragranceFamilies)) {
    mappedScents = item.fragranceFamilies.map((family: any) =>
      typeof family === "object" ? family.name : family
    );
  }

  const price =
    mappedVariants.length > 0
      ? mappedVariants[0].price
      : Number(item?.price || item?.Price || 0);

  return {
    ...item,
    id: item?.id || item?.Id,
    name: item?.name || item?.Name || "Sản phẩm",
    image:
      item?.imageUrl ||
      item?.ImageUrl ||
      item?.image ||
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
    brand:
      typeof item?.brand === "object"
        ? item?.brand?.name
        : item?.brandName || item?.brand || "Premium",
    description: item?.description || item?.Description || "",
    price,
    variants: mappedVariants,
    scents: mappedScents.filter(Boolean),
    gender: genderText,
    concentration:
      typeof item?.concentration === "object"
        ? item?.concentration?.name
        : item?.concentrationName || item?.concentration || "",
    bottleType:
      typeof item?.bottleType === "object"
        ? item?.bottleType?.name
        : item?.bottleTypeName || item?.bottleType || "",
  };
};

const fetchProducts = async () => {
  try {
    isLoading.value = true;

    const res = await api.get("/v1/products");
    const rawData = extractArrayData(res.data);

    productList.value = Array.isArray(rawData)
      ? rawData.map((item: any) => mapProduct(item))
      : [];
  } catch (error) {
    console.error("Lỗi fetch API List:", error);
  } finally {
    isLoading.value = false;
  }
};

const filteredProductList = computed(() => {
  if (!productList.value) return [];

  return productList.value.filter((product: any) => {
    if (activeFilters.value.genders?.length > 0) {
      const filters = activeFilters.value.genders.map((gender: string) =>
        gender.toLowerCase()
      );

      const productGender = String(product.gender || "").toLowerCase();

      if (!filters.includes(productGender)) {
        return false;
      }
    }

    if (activeFilters.value.fragranceFamilies?.length > 0) {
      const filters = activeFilters.value.fragranceFamilies.map(
        (family: string) => family.toLowerCase()
      );

      const productScents = (product.scents || []).map((scent: string) =>
        scent.toLowerCase()
      );

      if (!filters.some((family: string) => productScents.includes(family))) {
        return false;
      }
    }

    if (activeFilters.value.concentrations?.length > 0) {
      const filters = activeFilters.value.concentrations.map(
        (concentration: string) => concentration.toLowerCase()
      );

      const productConcentration = String(
        product.concentration || ""
      ).toLowerCase();

      if (!filters.includes(productConcentration)) {
        return false;
      }
    }

    if (activeFilters.value.bottleTypes?.length > 0) {
      const filters = activeFilters.value.bottleTypes.map((bottle: string) =>
        bottle.toLowerCase()
      );

      const productBottle = String(product.bottleType || "").toLowerCase();

      let hasMatch = filters.includes(productBottle);

      if (!hasMatch && Array.isArray(product.variants)) {
        const variantBottles = product.variants.map((variant: any) =>
          String(
            typeof variant.bottleType === "object"
              ? variant.bottleType?.name
              : variant.bottleType || ""
          ).toLowerCase()
        );

        hasMatch = filters.some((filter: string) =>
          variantBottles.includes(filter)
        );
      }

      if (!hasMatch) {
        return false;
      }
    }

    if (activeFilters.value.capacities?.length > 0) {
      if (!Array.isArray(product.variants) || product.variants.length === 0) {
        return false;
      }

      const filters = activeFilters.value.capacities.map((capacity: string) =>
        capacity.toLowerCase().replace("ml", "").trim()
      );

      const productCapacities = product.variants.map((variant: any) =>
        String(variant.capacity || "")
          .toLowerCase()
          .replace("ml", "")
          .trim()
      );

      if (
        !filters.some((capacity: string) =>
          productCapacities.includes(capacity)
        )
      ) {
        return false;
      }
    }

    return true;
  });
});

const handleOpenDetail = (item: any) => {
  router.push({
    name: "SingleProduct",
    params: {
      id: item.id,
    },
  });
};

onMounted(() => {
  fetchProducts();
});

watch(
  () => route.query.id,
  (id) => {
    if (id) {
      router.replace({
        name: "SingleProduct",
        params: {
          id: String(id),
        },
      });
    }
  },
  {
    immediate: true,
  }
);
</script>

<style scoped>
.page-wrapper {
  font-family: "Inter", sans-serif;
  background-color: #ffffff;
  min-height: 100vh;
}

.product-layout {
  display: flex;
  flex-direction: row;
  gap: 40px;
  max-width: 1320px;
  margin: 40px auto;
  padding: 0 20px;
}

.product-main {
  flex: 1;
  min-width: 0;
}

:deep(.sidebar-filter) {
  width: 280px;
  flex-shrink: 0;
}

@media (max-width: 991px) {
  .product-layout {
    flex-direction: column;
  }

  :deep(.sidebar-filter) {
    width: 100%;
  }
}
</style>