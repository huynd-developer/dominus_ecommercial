<template>
  <div class="page-wrapper">
    <ShopHeader />
    <ShopNavbar />

    <div class="product-layout">
      <SidebarFilter @filter-change="handleFilterChange" />

      <main class="product-main">
        <div
          v-if="isLoading"
          style="padding: 50px; text-align: center; color: #666"
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
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import api from "@/common/api";

import ShopHeader from "@/modules/shop/layout/ShopHeader.vue";
import ShopNavbar from "@/modules/shop/layout/ShopNavbar.vue";
import SidebarFilter from "@/modules/shop/feature/product/components/SidebarFilter.vue";
import ProductGrid from "@/modules/shop/feature/product/components/ProductGrid.vue";
import ShopFooter from "@/modules/shop/layout/ShopFooter.vue";

const router = useRouter();
const route = useRoute();

const productList = ref<any[]>([]);
const isLoading = ref(false);

const BACKEND_URL = "http://localhost:8080";

const toNumber = (value: unknown, fallback = 0) => {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : fallback;
};

const getRouteQueryValue = (value: unknown) => {
  if (Array.isArray(value)) {
    return value[0] || "";
  }

  return value ? String(value) : "";
};

const normalizeText = (value: unknown) => {
  return String(value ?? "")
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .trim();
};

const normalizeCapacityKey = (value: unknown) => {
  // Cắt chữ ml và khoảng trắng
  const text = normalizeText(value).replace(/ml/g, "").replace(/\s+/g, "").trim();
  // Ép về số để 10.0 hay 10 đều thành 10
  const num = parseFloat(text);
  
  return !Number.isNaN(num) ? String(num) : text;
};

const getPlaceholderImage = () => {
  return (
    "data:image/svg+xml;utf8," +
    encodeURIComponent(`
      <svg xmlns="http://www.w3.org/2000/svg" width="300" height="300">
        <rect width="100%" height="100%" fill="#f3f4f6"/>
        <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle"
          fill="#9ca3af" font-family="Arial" font-size="20">
          No Image
        </text>
      </svg>
    `)
  );
};

const normalizeImageUrl = (url: unknown) => {
  const rawUrl = String(url || "").trim();

  if (!rawUrl) {
    return "";
  }

  if (
    rawUrl.startsWith("http://") ||
    rawUrl.startsWith("https://") ||
    rawUrl.startsWith("data:image") ||
    rawUrl.startsWith("blob:")
  ) {
    return rawUrl;
  }

  if (rawUrl.startsWith("/")) {
    return `${BACKEND_URL}${rawUrl}`;
  }

  return `${BACKEND_URL}/${rawUrl}`;
};

const getImageUrlFromObject = (value: any) => {
  if (!value) {
    return "";
  }

  if (typeof value === "string") {
    return normalizeImageUrl(value);
  }

  return normalizeImageUrl(
    value?.imageUrl ??
      value?.ImageUrl ??
      value?.url ??
      value?.Url ??
      value?.mediaUrl ??
      value?.MediaUrl ??
      value?.path ??
      value?.Path ??
      value?.fileUrl ??
      value?.FileUrl ??
      value?.thumbnailUrl ??
      value?.ThumbnailUrl ??
      value?.mainImageUrl ??
      value?.MainImageUrl ??
      value?.mainImage ??
      value?.MainImage ??
      ""
  );
};

const appendImage = (images: string[], value: any) => {
  const imageUrl = getImageUrlFromObject(value);

  if (imageUrl && !images.includes(imageUrl)) {
    images.push(imageUrl);
  }
};

const appendImageList = (images: string[], value: any) => {
  if (!value) {
    return;
  }

  if (Array.isArray(value)) {
    value.forEach((item) => appendImage(images, item));
    return;
  }

  appendImage(images, value);
};

const getVariantImageList = (variant: any) => {
  const images: string[] = [];

  appendImage(images, variant?.mainImage);
  appendImage(images, variant?.mainImageUrl);
  appendImage(images, variant?.MainImageUrl);
  appendImage(images, variant?.thumbnailUrl);
  appendImage(images, variant?.ThumbnailUrl);
  appendImage(images, variant?.imageUrl);
  appendImage(images, variant?.ImageUrl);
  appendImage(images, variant?.image);
  appendImage(images, variant?.Image);

  appendImageList(images, variant?.images);
  appendImageList(images, variant?.Images);
  appendImageList(images, variant?.imageList);
  appendImageList(images, variant?.ImageList);
  appendImageList(images, variant?.productImages);
  appendImageList(images, variant?.ProductImages);
  appendImageList(images, variant?.productImageList);
  appendImageList(images, variant?.ProductImageList);

  return images;
};

const getProductImageList = (item: any, variants?: any[]) => {
  const images: string[] = [];

  appendImage(images, item?.mainImage);
  appendImage(images, item?.mainImageUrl);
  appendImage(images, item?.MainImageUrl);
  appendImage(images, item?.thumbnailUrl);
  appendImage(images, item?.ThumbnailUrl);
  appendImage(images, item?.imageUrl);
  appendImage(images, item?.ImageUrl);
  appendImage(images, item?.image);
  appendImage(images, item?.Image);

  appendImageList(images, item?.images);
  appendImageList(images, item?.Images);
  appendImageList(images, item?.galleryImages);
  appendImageList(images, item?.GalleryImages);
  appendImageList(images, item?.imageList);
  appendImageList(images, item?.ImageList);
  appendImageList(images, item?.productImages);
  appendImageList(images, item?.ProductImages);
  appendImageList(images, item?.productImageList);
  appendImageList(images, item?.ProductImageList);

  if (Array.isArray(variants)) {
    variants.forEach((variant) => {
      getVariantImageList(variant).forEach((imageUrl) => {
        if (imageUrl && !images.includes(imageUrl)) {
          images.push(imageUrl);
        }
      });
    });
  }

  return images;
};

const selectedBrandId = computed(() => {
  const rawBrandId = getRouteQueryValue(route.query.brandId);
  const brandId = Number(rawBrandId);

  return Number.isFinite(brandId) && brandId > 0 ? brandId : null;
});

const selectedGender = computed(() => {
  return normalizeText(getRouteQueryValue(route.query.gender));
});

const selectedBottleTypeKeyword = computed(() => {
  return normalizeText(getRouteQueryValue(route.query.bottleType));
});

const selectedCapacityKeyword = computed(() => {
  return normalizeCapacityKey(getRouteQueryValue(route.query.capacity));
});

const selectedNicheOnly = computed(() => {
  const value = normalizeText(getRouteQueryValue(route.query.niche));
  return value === "true" || value === "1";
});

const selectedFlashSaleOnly = computed(() => {
  const value = normalizeText(getRouteQueryValue(route.query.flashSale));
  return value === "true" || value === "1";
});

const currentPage = ref(1);
const itemsPerPage = ref(6);

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

const getVariantCapacityRaw = (variant: any) => {
  if (!variant) return "";

  if (variant.capacity && typeof variant.capacity === "object") {
    return (
      variant.capacity.value ??
      variant.capacity.name ??
      variant.capacity.capacityValue ??
      variant.capacity.capacityName ??
      variant.capacity.label ??
      ""
    );
  }

  return (
    variant.capacityValue ??
    variant.CapacityValue ??
    variant.capacityName ??
    variant.CapacityName ??
    variant.capacityLabel ??
    variant.CapacityLabel ??
    variant.volume ??
    variant.Volume ??
    variant.capacity ??
    variant.Capacity ??
    ""
  );
};

const extractCapacity = (variant: any) => {
  const value = getVariantCapacityRaw(variant);

  if (value == null || value === "") {
    return "N/A";
  }

  const text = String(value).trim();
  const numeric = Number(text.toLowerCase().replace(/ml/g, "").trim());

  if (!Number.isNaN(numeric) && numeric > 0) {
    return `${numeric}ml`;
  }

  return text.toLowerCase().includes("ml") ? text : `${text}ml`;
};

const normalizeStock = (variant: any) => {
  const value = Number(variant?.sellableQuantity ?? 0);

  if (!Number.isFinite(value) || value <= 0) {
    return 0;
  }

  return Math.trunc(value);
};

const getDiscountPercentFromPrice = (
  salePrice: number,
  originalPrice: number
) => {
  if (originalPrice > 0 && salePrice > 0 && salePrice < originalPrice) {
    return Math.round(((originalPrice - salePrice) / originalPrice) * 100);
  }

  return 0;
};

const getProductIdValue = (item: any) => {
  return Number(item?.productId ?? item?.id ?? item?.Id ?? 0);
};

const getVariantIdValue = (item: any) => {
  return Number(
    item?.productVariantId ??
      item?.variantId ??
      item?.id ??
      item?.Id ??
      0
  );
};

const buildPriceFields = (
  salePriceInput: unknown,
  originalPriceInput: unknown,
  fallbackInput: unknown = 0
) => {
  const fallback = toNumber(fallbackInput, 0);
  const originalPrice = toNumber(originalPriceInput, fallback);
  const salePrice = toNumber(salePriceInput, originalPrice || fallback);

  const finalSalePrice =
    salePrice > 0 ? salePrice : originalPrice > 0 ? originalPrice : fallback;

  const finalOriginalPrice =
    originalPrice > 0 ? originalPrice : finalSalePrice;

  const discountPercent = getDiscountPercentFromPrice(
    finalSalePrice,
    finalOriginalPrice
  );

  return {
    price: finalSalePrice,
    salePrice: finalSalePrice,
    promotionPrice: finalSalePrice,
    flashSalePrice: finalSalePrice,
    currentPrice: finalSalePrice,
    displayPrice: finalSalePrice,
    finalPrice: finalSalePrice,
    minPrice: finalSalePrice,

    originalPrice: finalOriginalPrice,
    oldPrice: finalOriginalPrice,
    listPrice: finalOriginalPrice,
    basePrice: finalOriginalPrice,

    discountPercent,
  };
};

const mapVariant = (variant: any) => {
  const stock = normalizeStock(variant);
  const rawCapacity = getVariantCapacityRaw(variant);
  const capacity = extractCapacity(variant);
  const variantImages = getVariantImageList(variant);
  const variantMainImage = variantImages[0] || "";

  const originalPrice = toNumber(
    variant?.originalPrice ??
      variant?.oldPrice ??
      variant?.price ??
      variant?.Price,
    0
  );

  const salePrice = toNumber(
    variant?.salePrice ??
      variant?.promotionPrice ??
      variant?.flashSalePrice ??
      variant?.price ??
      variant?.Price,
    originalPrice
  );

  const discountPercent =
    toNumber(
      variant?.discountPercent ??
        variant?.discount ??
        variant?.salePercent ??
        variant?.promotionPercent,
      0
    ) || getDiscountPercentFromPrice(salePrice, originalPrice);

  const hasDiscount =
    discountPercent > 0 && originalPrice > 0 && salePrice < originalPrice;

  return {
    ...variant,
    id: variant?.id || variant?.Id || variant?.productVariantId,
    productVariantId: variant?.productVariantId || variant?.id || variant?.Id,
    variantId:
      variant?.variantId ||
      variant?.productVariantId ||
      variant?.id ||
      variant?.Id,
    sku: variant?.sku || variant?.SKU || "",
    capacity,
    capacityKey: normalizeCapacityKey(rawCapacity || capacity),

    price: hasDiscount ? salePrice : originalPrice || salePrice,
    salePrice: hasDiscount ? salePrice : originalPrice || salePrice,
    promotionPrice: hasDiscount ? salePrice : undefined,
    flashSalePrice: hasDiscount ? salePrice : undefined,
    currentPrice: hasDiscount ? salePrice : originalPrice || salePrice,
    displayPrice: hasDiscount ? salePrice : originalPrice || salePrice,
    finalPrice: hasDiscount ? salePrice : originalPrice || salePrice,
    minPrice: hasDiscount ? salePrice : originalPrice || salePrice,

    originalPrice: originalPrice || salePrice,
    oldPrice: originalPrice || salePrice,
    listPrice: originalPrice || salePrice,
    basePrice: originalPrice || salePrice,

    sellableQuantity: stock,

    // Compatibility cho code cũ.
    stock,
    stockQuantity: stock,
    bottleType:
      typeof variant?.bottleType === "object"
        ? variant?.bottleType?.name
        : variant?.bottleTypeName || variant?.bottleType || "",

    discountPercent: hasDiscount ? discountPercent : 0,
    isFlashSale: Boolean(variant?.isFlashSale || variant?.flashSale),
    flashSale: Boolean(variant?.isFlashSale || variant?.flashSale),

    image: variantMainImage,
    imageUrl: variantMainImage,
    images: variantImages,
    productImages: variantImages.map((imageUrl, index) => ({
      id: index + 1,
      imageUrl,
      url: imageUrl,
    })),
  };
};

const mapProduct = (item: any) => {
  const rawVariants =
    item?.variants ||
    item?.Variants ||
    item?.productVariants ||
    item?.ProductVariants ||
    item?.productVariantList ||
    item?.ProductVariantList ||
    item?.productVariantResponses ||
    item?.ProductVariantResponses ||
    item?.productVariantDTOs ||
    item?.ProductVariantDTOs ||
    item?.lstProductVariant ||
    item?.LstProductVariant ||
    item?.items ||
    item?.Items ||
    [];

  const mappedVariants = Array.isArray(rawVariants)
    ? rawVariants.map(mapVariant).sort((a: any, b: any) => {
        const valA = Number(a.capacityKey || 0);
        const valB = Number(b.capacityKey || 0);
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

  const representativeVariant =
    mappedVariants.length > 0 ? mappedVariants[0] : null;

  const productSalePrice = toNumber(
    item?.salePrice ??
      item?.promotionPrice ??
      item?.flashSalePrice ??
      item?.price ??
      item?.Price,
    0
  );

  const productOriginalPrice = toNumber(
    item?.originalPrice ?? item?.oldPrice ?? item?.price ?? item?.Price,
    productSalePrice
  );

  const price =
    representativeVariant?.salePrice ||
    representativeVariant?.price ||
    productSalePrice;

  const originalPrice =
    representativeVariant?.originalPrice ||
    representativeVariant?.oldPrice ||
    productOriginalPrice ||
    price;

  const discountPercent =
    toNumber(
      item?.discountPercent ??
        item?.discount ??
        item?.salePercent ??
        item?.promotionPercent,
      0
    ) || getDiscountPercentFromPrice(price, originalPrice);

  const category =
    typeof item?.category === "object"
      ? item?.category?.name || item?.category?.Name || ""
      : item?.categoryName || item?.category || item?.CategoryName || "";

  const productImages = getProductImageList(item, mappedVariants);
  const mainImage = productImages[0] || getPlaceholderImage();

  return {
    ...item,
    id: item?.id || item?.Id || item?.productId,
    productId: item?.productId || item?.id || item?.Id,
    brandId: Number(
      item?.brandId ??
        item?.BrandId ??
        item?.brand?.id ??
        item?.brand?.Id ??
        0
    ),
    name: item?.name || item?.Name || item?.productName || "Sản phẩm",

    image: mainImage,
    imageUrl: mainImage,
    mainImage,
    images: productImages,
    imageList: productImages,
    galleryImages: productImages,
    productImages: productImages.map((imageUrl, index) => ({
      id: index + 1,
      imageUrl,
      url: imageUrl,
    })),

    brand:
      typeof item?.brand === "object"
        ? item?.brand?.name
        : item?.brandName || item?.brand || item?.promotionName || "Premium",
    category,
    description: item?.description || item?.Description || "",

    price,
    salePrice: price,
    promotionPrice: discountPercent > 0 ? price : undefined,
    flashSalePrice: discountPercent > 0 ? price : undefined,
    currentPrice: price,
    displayPrice: price,
    finalPrice: price,
    minPrice: price,

    originalPrice,
    oldPrice: originalPrice,
    listPrice: originalPrice,
    basePrice: originalPrice,
    discountPercent,

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

const mapFlashSaleGroupedProducts = (rows: any[]) => {
  const productMap = new Map<number, any[]>();

  rows.forEach((item) => {
    const pId = Number(item.productId ?? item.productVariantId ?? 0);

    if (!productMap.has(pId)) {
      productMap.set(pId, []);
    }

    productMap.get(pId)!.push(item);
  });

  const groupedProducts: any[] = [];

  productMap.forEach((variants, productId) => {
    if (!variants || variants.length === 0) return;

    const firstItem = variants[0];

    const productImages = getProductImageList(firstItem, variants);
    const productImage = productImages[0] || getPlaceholderImage();

    const mappedVariants = variants.map((v) => {
      const vId = Number(v.productVariantId || v.variantId || v.id || 0);
      const vSalePrice = toNumber(v.salePrice, 0);
      const vOriginalPrice = toNumber(v.originalPrice, vSalePrice);
      const vStock = toNumber(v.sellableQuantity, 0);
      const vDiscountPercent =
        toNumber(v.discountPercent, 0) ||
        getDiscountPercentFromPrice(vSalePrice, vOriginalPrice);

      return {
        ...v,
        id: vId,
        productVariantId: vId,
        variantId: vId,

        price: vSalePrice,
        salePrice: vSalePrice,
        promotionPrice: vSalePrice,
        flashSalePrice: vSalePrice,
        currentPrice: vSalePrice,
        displayPrice: vSalePrice,
        finalPrice: vSalePrice,
        minPrice: vSalePrice,

        originalPrice: vOriginalPrice,
        oldPrice: vOriginalPrice,
        listPrice: vOriginalPrice,
        basePrice: vOriginalPrice,

        discountPercent: vDiscountPercent,
        isFlashSale: true,
        flashSale: true,

        sellableQuantity: vStock,
        stockQuantity: vStock,
        stock: vStock,
        status: 1,
        capacity: v.capacity,
        capacityKey: normalizeCapacityKey(v.capacity),
        bottleType: v.bottleType,
        imageUrl: getImageUrlFromObject(v) || productImage,
        image: getImageUrlFromObject(v) || productImage,
        images: productImages,
        productImages: productImages.map((imageUrl, index) => ({
          id: index + 1,
          imageUrl,
          url: imageUrl,
        })),
      };
    });

    mappedVariants.sort((a: any, b: any) => {
      const aSale = Number(a.salePrice || a.price || 0);
      const bSale = Number(b.salePrice || b.price || 0);
      return aSale - bSale;
    });

    const representativeVariant = mappedVariants[0];

    groupedProducts.push({
      id: productId,
      productId,
      productVariantId: representativeVariant?.productVariantId,
      variantId: representativeVariant?.productVariantId,

      name: firstItem?.productName || firstItem?.name || "Sản phẩm Flash Sale",
      brand: firstItem?.brandName || firstItem?.brand || firstItem?.promotionName || "Flash Sale",
      color: "#0a192f",

      imageUrl: productImage,
      image: productImage,
      mainImage: productImage,
      images: productImages,
      imageList: productImages,
      galleryImages: productImages,
      productImages: productImages.map((imageUrl, index) => ({
        id: index + 1,
        imageUrl,
        url: imageUrl,
      })),

      price: representativeVariant?.salePrice || 0,
      salePrice: representativeVariant?.salePrice || 0,
      promotionPrice: representativeVariant?.salePrice || 0,
      flashSalePrice: representativeVariant?.salePrice || 0,
      currentPrice: representativeVariant?.salePrice || 0,
      displayPrice: representativeVariant?.salePrice || 0,
      finalPrice: representativeVariant?.salePrice || 0,
      minPrice: representativeVariant?.salePrice || 0,

      originalPrice: representativeVariant?.originalPrice || 0,
      oldPrice: representativeVariant?.originalPrice || 0,
      listPrice: representativeVariant?.originalPrice || 0,
      basePrice: representativeVariant?.originalPrice || 0,

      discountPercent: representativeVariant?.discountPercent || 0,

      rating: 5,
      reviewCount: 0,

      sellableQuantity: mappedVariants.reduce(
        (sum, v) => sum + (v.sellableQuantity || 0),
        0
      ),
      stockQuantity: mappedVariants.reduce(
        (sum, v) => sum + (v.sellableQuantity || 0),
        0
      ),
      isFlashSale: true,
      flashSale: true,
      endDate: firstItem?.endDate,

      variants: mappedVariants,
    });
  });

  return groupedProducts;
};

const fetchActiveFlashSaleGroupedProducts = async () => {
  try {
    const res = await api.get("/promotions/flash-sale", {
      params: {
        page: 0,
        size: 200,
      },
    });

    const rawRows = extractArrayData(res.data);
    return Array.isArray(rawRows) ? mapFlashSaleGroupedProducts(rawRows) : [];
  } catch (error) {
    console.error("Lỗi tải Flash Sale để đồng bộ danh sách:", error);
    return [];
  }
};

const mergeFlashSaleIntoProducts = (
  products: any[],
  flashSaleProducts: any[]
) => {
  if (!Array.isArray(products) || products.length === 0) {
    return [];
  }

  if (!Array.isArray(flashSaleProducts) || flashSaleProducts.length === 0) {
    return products;
  }

  const flashSaleByProductId = new Map<number, any>();
  const flashSaleByVariantId = new Map<number, any>();

  flashSaleProducts.forEach((flashSaleProduct) => {
    const productId = getProductIdValue(flashSaleProduct);

    if (productId > 0) {
      flashSaleByProductId.set(productId, flashSaleProduct);
    }

    if (Array.isArray(flashSaleProduct?.variants)) {
      flashSaleProduct.variants.forEach((variant: any) => {
        const variantId = getVariantIdValue(variant);

        if (variantId > 0) {
          flashSaleByVariantId.set(variantId, {
            ...variant,
            productId,
            endDate: flashSaleProduct?.endDate,
          });
        }
      });
    }
  });

  return products.map((product) => {
    const productId = getProductIdValue(product);
    const matchedFlashSaleProduct = flashSaleByProductId.get(productId);

    if (!matchedFlashSaleProduct) {
      return product;
    }

    const originalVariants = Array.isArray(product?.variants)
      ? product.variants
      : [];

    const flashSaleVariants = Array.isArray(matchedFlashSaleProduct?.variants)
      ? matchedFlashSaleProduct.variants
      : [];

    const mergedVariants =
      originalVariants.length > 0
        ? originalVariants.map((variant: any) => {
            const variantId = getVariantIdValue(variant);
            const flashSaleVariant = flashSaleByVariantId.get(variantId);

            if (!flashSaleVariant) {
              return variant;
            }

            const salePrice = toNumber(
              flashSaleVariant?.salePrice ??
                flashSaleVariant?.price ??
                variant?.salePrice ??
                variant?.price,
              0
            );

            const originalPrice = toNumber(
              flashSaleVariant?.originalPrice ??
                flashSaleVariant?.oldPrice ??
                variant?.originalPrice ??
                variant?.oldPrice ??
                variant?.price,
              salePrice
            );

            const discountPercent =
              toNumber(flashSaleVariant?.discountPercent, 0) ||
              getDiscountPercentFromPrice(salePrice, originalPrice);

            return {
              ...variant,

              price: salePrice,
              salePrice,
              promotionPrice: salePrice,
              flashSalePrice: salePrice,
              currentPrice: salePrice,
              displayPrice: salePrice,
              finalPrice: salePrice,
              minPrice: salePrice,

              originalPrice,
              oldPrice: originalPrice,
              listPrice: originalPrice,
              basePrice: originalPrice,

              discountPercent,
              isFlashSale: true,
              flashSale: true,
              endDate: flashSaleVariant?.endDate,
            };
          })
        : flashSaleVariants;

    const flashSaleRepresentative =
      flashSaleVariants
        .slice()
        .sort(
          (a: any, b: any) =>
            toNumber(a?.salePrice ?? a?.price, 0) -
            toNumber(b?.salePrice ?? b?.price, 0)
        )[0] || matchedFlashSaleProduct;

    const salePrice = toNumber(
      flashSaleRepresentative?.salePrice ??
        flashSaleRepresentative?.price ??
        matchedFlashSaleProduct?.salePrice ??
        matchedFlashSaleProduct?.price,
      0
    );

    const originalPrice = toNumber(
      flashSaleRepresentative?.originalPrice ??
        flashSaleRepresentative?.oldPrice ??
        matchedFlashSaleProduct?.originalPrice ??
        matchedFlashSaleProduct?.oldPrice,
      salePrice
    );

    const discountPercent =
      toNumber(
        flashSaleRepresentative?.discountPercent ??
          matchedFlashSaleProduct?.discountPercent,
        0
      ) || getDiscountPercentFromPrice(salePrice, originalPrice);

    return {
      ...product,

      price: salePrice,
      salePrice,
      promotionPrice: salePrice,
      flashSalePrice: salePrice,
      currentPrice: salePrice,
      displayPrice: salePrice,
      finalPrice: salePrice,
      minPrice: salePrice,

      originalPrice,
      oldPrice: originalPrice,
      listPrice: originalPrice,
      basePrice: originalPrice,

      discountPercent,
      isFlashSale: true,
      flashSale: true,
      endDate: matchedFlashSaleProduct?.endDate,

      productVariantId:
        flashSaleRepresentative?.productVariantId ||
        flashSaleRepresentative?.variantId ||
        product?.productVariantId ||
        matchedFlashSaleProduct?.productVariantId,
      variantId:
        flashSaleRepresentative?.productVariantId ||
        flashSaleRepresentative?.variantId ||
        product?.variantId ||
        matchedFlashSaleProduct?.variantId,

      variants: mergedVariants,
    };
  });
};

const fetchProducts = async () => {
  try {
    isLoading.value = true;
    const isFlashSaleQuery = route.query.flashSale === "true";

    if (isFlashSaleQuery) {
      /*
       * Flash Sale API chỉ dùng metadata khuyến mãi/giá.
       * Tồn bán được luôn lấy từ /v1/products -> InventoryLot.
       */
      const [productRes, flashSaleProducts] = await Promise.all([
        api.get("/v1/products", {
          params: {
            brandId: selectedBrandId.value || undefined,
            size: 200,
          },
        }),
        fetchActiveFlashSaleGroupedProducts(),
      ]);

      const rawData = extractArrayData(productRes.data);
      const normalProducts = Array.isArray(rawData)
        ? rawData.map((item: any) => mapProduct(item))
        : [];

      const flashSaleProductIds = new Set(
        flashSaleProducts
          .map((item: any) => getProductIdValue(item))
          .filter((id: number) => id > 0)
      );

      productList.value = mergeFlashSaleIntoProducts(
        normalProducts,
        flashSaleProducts
      ).filter((item: any) =>
        flashSaleProductIds.has(getProductIdValue(item))
      );
    } else {
      const [productRes, flashSaleProducts] = await Promise.all([
        api.get("/v1/products", {
          params: {
            brandId: selectedBrandId.value || undefined,
          },
        }),
        fetchActiveFlashSaleGroupedProducts(),
      ]);

      const rawData = extractArrayData(productRes.data);
      const normalProducts = Array.isArray(rawData)
        ? rawData.map((item: any) => mapProduct(item))
        : [];

      productList.value = mergeFlashSaleIntoProducts(
        normalProducts,
        flashSaleProducts
      );
    }
  } catch (error) {
    console.error("Lỗi fetch API List:", error);
    productList.value = [];
  } finally {
    isLoading.value = false;
  }
};

const filteredProductList = computed(() => {
  if (!productList.value) return [];

  return productList.value.filter((product: any) => {
    if (selectedBrandId.value) {
      const productBrandId = Number(
        product?.brandId ??
          product?.BrandId ??
          product?.brand?.id ??
          product?.brand?.Id ??
          0
      );

      if (productBrandId > 0 && productBrandId !== selectedBrandId.value) {
        return false;
      }
    }

    if (selectedGender.value) {
      const productGender = normalizeText(product.gender);

      if (productGender !== selectedGender.value) {
        return false;
      }
    }

    if (selectedNicheOnly.value) {
      const productCategory = normalizeText(
        product?.category || product?.categoryName || ""
      );

      const isNiche =
        product?.isNiche === true ||
        product?.niche === true ||
        Number(product?.isNiche) === 1 ||
        Number(product?.niche) === 1 ||
        normalizeText(product?.isNiche) === "true" ||
        normalizeText(product?.niche) === "true" ||
        productCategory.includes("niche");

      if (!isNiche) {
        return false;
      }
    }

    if (selectedBottleTypeKeyword.value) {
      const productBottleType = normalizeText(product.bottleType);

      let hasBottleTypeMatch = productBottleType.includes(
        selectedBottleTypeKeyword.value
      );

      if (!hasBottleTypeMatch && Array.isArray(product.variants)) {
        hasBottleTypeMatch = product.variants.some((variant: any) => {
          const variantBottleType = normalizeText(
            typeof variant.bottleType === "object"
              ? variant.bottleType?.name
              : variant.bottleType || variant.bottleTypeName || ""
          );

          return variantBottleType.includes(selectedBottleTypeKeyword.value);
        });
      }

      if (!hasBottleTypeMatch) {
        return false;
      }
    }

    if (selectedCapacityKeyword.value) {
      if (!Array.isArray(product.variants) || product.variants.length === 0) {
        return false;
      }

      const hasCapacityMatch = product.variants.some((variant: any) => {
        const variantCapacity = normalizeCapacityKey(
          variant.capacityKey ||
            variant.capacity ||
            variant.capacityName ||
            variant.capacityValue ||
            ""
        );

        return variantCapacity === selectedCapacityKeyword.value;
      });

      if (!hasCapacityMatch) {
        return false;
      }
    }

    if (selectedFlashSaleOnly.value) {
      const productDiscount = Number(
        product?.discountPercent ??
          product?.discount ??
          product?.salePercent ??
          product?.promotionPercent ??
          0
      );

      const hasVariantDiscount =
        Array.isArray(product.variants) &&
        product.variants.some((variant: any) => {
          return (
            Number(
              variant?.discountPercent ??
                variant?.discount ??
                variant?.salePercent ??
                variant?.promotionPercent ??
                0
            ) > 0
          );
        });

      const isFlashSale =
        product?.flashSale === true ||
        product?.isFlashSale === true ||
        Number(product?.flashSale) === 1 ||
        Number(product?.isFlashSale) === 1 ||
        normalizeText(product?.flashSale) === "true" ||
        normalizeText(product?.isFlashSale) === "true" ||
        productDiscount > 0 ||
        hasVariantDiscount;

      if (!isFlashSale) {
        return false;
      }
    }

    if (activeFilters.value.genders?.length > 0) {
      const filters = activeFilters.value.genders.map((gender: string) =>
        normalizeText(gender)
      );

      const productGender = normalizeText(product.gender);

      if (!filters.includes(productGender)) {
        return false;
      }
    }

    if (activeFilters.value.fragranceFamilies?.length > 0) {
      const filters = activeFilters.value.fragranceFamilies.map(
        (family: string) => normalizeText(family)
      );

      const productScents = (product.scents || []).map((scent: string) =>
        normalizeText(scent)
      );

      if (!filters.some((family: string) => productScents.includes(family))) {
        return false;
      }
    }

    if (activeFilters.value.concentrations?.length > 0) {
      const filters = activeFilters.value.concentrations.map(
        (concentration: string) => normalizeText(concentration)
      );

      const productConcentration = normalizeText(product.concentration || "");

      if (!filters.includes(productConcentration)) {
        return false;
      }
    }

    if (activeFilters.value.bottleTypes?.length > 0) {
      const filters = activeFilters.value.bottleTypes.map((bottle: string) =>
        normalizeText(bottle)
      );

      const productBottle = normalizeText(product.bottleType);

      let hasMatch = filters.includes(productBottle);

      if (!hasMatch && Array.isArray(product.variants)) {
        const variantBottles = product.variants.map((variant: any) =>
          normalizeText(
            typeof variant.bottleType === "object"
              ? variant.bottleType?.name
              : variant.bottleType || ""
          )
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
        normalizeCapacityKey(capacity)
      );

      const productCapacities = product.variants.map((variant: any) =>
        normalizeCapacityKey(
          variant.capacityKey ||
            variant.capacity ||
            variant.capacityName ||
            variant.capacityValue ||
            ""
        )
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
      id: item.id || item.productId,
    },
  });
};

// BẮT SỰ KIỆN CLICK CHUỘT QUAY LẠI CỬA SỔ ĐỂ TỰ ĐỘNG CẬP NHẬT DANH SÁCH & FLASH SALE MỚI NHẤT
const handleFocus = async () => {
  await fetchProducts();
};

onMounted(() => {
  fetchProducts();
  window.addEventListener("focus", handleFocus);
});

onUnmounted(() => {
  window.removeEventListener("focus", handleFocus);
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

watch(
  () => [
    route.query.brandId,
    route.query.gender,
    route.query.niche,
    route.query.bottleType,
    route.query.capacity,
    route.query.flashSale,
  ],
  () => {
    currentPage.value = 1;
    fetchProducts();
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