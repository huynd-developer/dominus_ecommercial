<template>
  <div class="page-wrapper">
    <ShopHeader />

    <div class="product-layout">
      <SidebarFilter @filter-change="handleFilterChange" />

      <main class="product-main">
        <div class="product-toolbar">
          <div>
            <h2 class="toolbar-title">Danh sách sản phẩm</h2>
            <p class="toolbar-subtitle">
              {{ totalElements }} sản phẩm
            </p>
          </div>

          <select
            v-model="pageSize"
            class="page-size-select"
            @change="fetchProducts(0)"
          >
            <option :value="9">9 sản phẩm</option>
            <option :value="12">12 sản phẩm</option>
            <option :value="18">18 sản phẩm</option>
          </select>
        </div>

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

        <div
          v-if="!isLoading && totalPages > 1"
          class="pagination-container"
        >
          <button
            class="page-btn"
            :disabled="pageNumber <= 0"
            @click="fetchProducts(pageNumber - 1)"
          >
            &lt;
          </button>

          <button
            v-for="page in visiblePages"
            :key="page"
            class="page-btn"
            :class="{ active: page === pageNumber }"
            @click="fetchProducts(page)"
          >
            {{ page + 1 }}
          </button>

          <button
            class="page-btn"
            :disabled="pageNumber + 1 >= totalPages"
            @click="fetchProducts(pageNumber + 1)"
          >
            &gt;
          </button>
        </div>
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

const pageNumber = ref(0);
const pageSize = ref(9);
const totalPages = ref(0);
const totalElements = ref(0);

const activeFilters = ref({
  genders: [] as string[],
  fragranceFamilies: [] as string[],
  capacities: [] as string[],
  bottleTypes: [] as string[],
  concentrations: [] as string[],
});

const keyword = computed(() => {
  return String(route.query.keyword || "").trim();
});

const visiblePages = computed(() => {
  const pages: number[] = [];

  const start = Math.max(0, pageNumber.value - 2);
  const end = Math.min(totalPages.value - 1, pageNumber.value + 2);

  for (let i = start; i <= end; i++) {
    pages.push(i);
  }

  return pages;
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
  if (Array.isArray(data)) {
    totalPages.value = 1;
    totalElements.value = data.length;
    pageNumber.value = 0;
    return data;
  }

  if (Array.isArray(data?.data?.content)) {
    pageNumber.value = data.data.page?.number ?? data.data.number ?? 0;
    totalPages.value = data.data.page?.totalPages ?? data.data.totalPages ?? 0;
    totalElements.value =
      data.data.page?.totalElements ?? data.data.totalElements ?? 0;
    return data.data.content;
  }

  if (Array.isArray(data?.content)) {
    pageNumber.value = data.page?.number ?? data.number ?? 0;
    totalPages.value = data.page?.totalPages ?? data.totalPages ?? 0;
    totalElements.value = data.page?.totalElements ?? data.totalElements ?? 0;
    return data.content;
  }

  if (Array.isArray(data?.data)) {
    totalPages.value = 1;
    totalElements.value = data.data.length;
    pageNumber.value = 0;
    return data.data;
  }

  totalPages.value = 0;
  totalElements.value = 0;
  pageNumber.value = 0;
  return [];
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
    id: Number(variant?.id || variant?.Id || variant?.productVariantId || 0),
    productVariantId: Number(
      variant?.productVariantId || variant?.variantId || variant?.id || variant?.Id || 0
    ),
    variantId: Number(
      variant?.productVariantId || variant?.variantId || variant?.id || variant?.Id || 0
    ),
    sku: variant?.sku || variant?.SKU || "",
    capacity: extractCapacity(variant),
    price: Number(variant?.price || variant?.Price || 0),
    stock,
    stockQuantity: stock,
    status: Number(variant?.status ?? 1),
    bottleType:
      typeof variant?.bottleType === "object"
        ? variant?.bottleType?.name
        : variant?.bottleTypeName || variant?.bottleType || "",
  };
};

const resolveGenderText = (item: any) => {
  if (item?.gender === 1 || item?.gender === "1") return "Nam";
  if (item?.gender === 2 || item?.gender === "2") return "Nữ";
  if (item?.gender === 0 || item?.gender === "0") return "Unisex";

  return typeof item?.gender === "object"
    ? item?.gender?.name || ""
    : item?.gender || "";
};

const resolveFragranceFamilies = (item: any) => {
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

  return mappedScents.filter(Boolean);
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

  const price =
    mappedVariants.length > 0
      ? mappedVariants[0].price
      : Number(item?.price || item?.Price || 0);

  return {
    ...item,
    id: Number(item?.id || item?.Id || 0),
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
    imageUrl: item?.imageUrl || item?.ImageUrl || item?.image || "",
    brand:
      typeof item?.brand === "object"
        ? item?.brand?.name
        : item?.brandName || item?.brand || "Premium",
    description: item?.description || item?.Description || "",
    price,
    salePrice: Number(item?.salePrice || price),
    originalPrice: Number(item?.originalPrice || price),
    discountPercent: Number(item?.discountPercent || 0),
    rating: Number(item?.rating || 5),
    reviewCount: Number(item?.reviewCount || item?.reviews || 0),
    variants: mappedVariants,
    scents: resolveFragranceFamilies(item),
    gender: resolveGenderText(item),
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

const fetchProducts = async (page = pageNumber.value) => {
  try {
    isLoading.value = true;

    const res = await api.get("/products", {
      params: {
        page,
        size: pageSize.value,
        keyword: keyword.value || undefined,
      },
    });

    const rawData = extractArrayData(res.data);

    productList.value = Array.isArray(rawData)
      ? rawData.map((item: any) => mapProduct(item))
      : [];
  } catch (error) {
    console.error("Lỗi fetch API List:", error);

    productList.value = [];
    totalPages.value = 0;
    totalElements.value = 0;
  } finally {
    isLoading.value = false;
  }
};

const filteredProductList = computed(() => {
  return productList.value.filter((product: any) => {
    if (activeFilters.value.genders.length > 0) {
      const filters = activeFilters.value.genders.map((gender) =>
        gender.toLowerCase()
      );

      const productGender = String(product.gender || "").toLowerCase();

      if (!filters.includes(productGender)) {
        return false;
      }
    }

    if (activeFilters.value.fragranceFamilies.length > 0) {
      const filters = activeFilters.value.fragranceFamilies.map((family) =>
        family.toLowerCase()
      );

      const productScents = (product.scents || []).map((scent: string) =>
        scent.toLowerCase()
      );

      if (!filters.some((family) => productScents.includes(family))) {
        return false;
      }
    }

    if (activeFilters.value.concentrations.length > 0) {
      const filters = activeFilters.value.concentrations.map((concentration) =>
        concentration.toLowerCase()
      );

      const productConcentration = String(product.concentration || "").toLowerCase();

      if (!filters.includes(productConcentration)) {
        return false;
      }
    }

    if (activeFilters.value.bottleTypes.length > 0) {
      const filters = activeFilters.value.bottleTypes.map((bottle) =>
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

        hasMatch = filters.some((filter) => variantBottles.includes(filter));
      }

      if (!hasMatch) {
        return false;
      }
    }

    if (activeFilters.value.capacities.length > 0) {
      if (!Array.isArray(product.variants) || product.variants.length === 0) {
        return false;
      }

      const filters = activeFilters.value.capacities.map((capacity) =>
        capacity.toLowerCase().replace("ml", "").trim()
      );

      const productCapacities = product.variants.map((variant: any) =>
        String(variant.capacity || "")
          .toLowerCase()
          .replace("ml", "")
          .trim()
      );

      if (!filters.some((capacity) => productCapacities.includes(capacity))) {
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
  fetchProducts(0);
});

watch(
  () => route.query.keyword,
  () => {
    fetchProducts(0);
  }
);

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

.product-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.toolbar-title {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  color: #0a142f;
}

.toolbar-subtitle {
  margin: 4px 0 0;
  color: #718096;
  font-size: 14px;
}

.page-size-select {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 9px 14px;
  color: #0a142f;
  outline: none;
}

.pagination-container {
  margin-top: 34px;
  display: flex;
  justify-content: center;
  gap: 8px;
}

.page-btn {
  min-width: 38px;
  height: 38px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  border-radius: 8px;
  color: #0a142f;
  font-weight: 700;
}

.page-btn.active,
.page-btn:hover:not(:disabled) {
  background: #0a142f;
  color: #ffffff;
  border-color: #0a142f;
}

.page-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
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

  .product-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>