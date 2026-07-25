<template>
  <nav class="shop-navbar">
    <div class="container">
      <ul
        class="nav justify-content-center align-items-center flex-nowrap overflow-auto h-100"
      >
        <!-- Brands Dropdown -->
        <li class="nav-item nav-dropdown-wrapper">
          <RouterLink to="/products" class="nav-link menu-link">
            Thương hiệu
            <i class="bi bi-chevron-down ms-1 nav-chevron"></i>
          </RouterLink>

          <div class="brand-dropdown">
            <div class="brand-dropdown-header">
              <p class="brand-dropdown-kicker mb-1">Luxury Brands</p>
              <h3 class="brand-dropdown-title mb-0">Thương hiệu nổi bật</h3>
            </div>

            <div class="brand-grid">
              <RouterLink
                v-for="brand in brands"
                :key="brand.id"
                :to="{
                  path: '/products',
                  query: { brandId: String(brand.id) },
                }"
                class="brand-item"
              >
                <span class="brand-logo-circle">
                  <img
                    v-if="getBrandLogoUrl(brand)"
                    :src="getBrandLogoUrl(brand)"
                    :alt="brand.name"
                    class="brand-logo-img"
                  />

                  <span v-else class="brand-monogram">
                    {{ getBrandMonogram(brand.name) }}
                  </span>
                </span>

                <span class="brand-name">{{ brand.name }}</span>
                <i class="bi bi-arrow-right-short"></i>
              </RouterLink>
            </div>
          </div>
        </li>

        <!-- Gender Dropdown -->
        <li class="nav-item nav-dropdown-wrapper">
          <RouterLink to="/products" class="nav-link menu-link">
            Giới tính
            <i class="bi bi-chevron-down ms-1 nav-chevron"></i>
          </RouterLink>

          <div class="simple-dropdown">
            <RouterLink
              :to="{ path: '/products', query: { gender: 'Nam' } }"
              class="simple-dropdown-item"
            >
              Nước hoa Nam
            </RouterLink>

            <RouterLink
              :to="{ path: '/products', query: { gender: 'Nữ' } }"
              class="simple-dropdown-item"
            >
              Nước hoa Nữ
            </RouterLink>

            <RouterLink
              :to="{ path: '/products', query: { gender: 'Unisex' } }"
              class="simple-dropdown-item"
            >
              Nước hoa Unisex
            </RouterLink>
          </div>
        </li>

        <li class="nav-item">
          <RouterLink
            :to="{ path: '/products', query: { niche: 'true' } }"
            class="nav-link menu-link"
          >
            Niche
          </RouterLink>
        </li>

        <li class="nav-item">
          <RouterLink
            :to="{ path: '/products', query: { bottleType: 'chiet' } }"
            class="nav-link menu-link"
          >
            Chiết
          </RouterLink>
        </li>

        <li class="nav-item">
          <RouterLink
            :to="{ path: '/products', query: { flashSale: 'true' } }"
            class="nav-link menu-link flash-sale-link"
          >
            Flash Sale
          </RouterLink>
        </li>
      </ul>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import api from "@/common/api";

interface Brand {
  id: number;
  name: string;
  logoUrl?: string | null;
}

const brands = ref<Brand[]>([]);

const extractBrandContent = (responseData: any): Brand[] => {
  if (Array.isArray(responseData)) {
    return responseData;
  }

  if (Array.isArray(responseData?.data)) {
    return responseData.data;
  }

  if (Array.isArray(responseData?.content)) {
    return responseData.content;
  }

  if (Array.isArray(responseData?.data?.content)) {
    return responseData.data.content;
  }

  return [];
};

const getTotalPages = (responseData: any): number => {
  return Number(
    responseData?.data?.totalPages ?? responseData?.totalPages ?? 1
  );
};

const fetchBrands = async () => {
  try {
    const size = 100;
    let page = 0;
    let totalPages = 1;
    const allBrands: Brand[] = [];

    do {
      const res = await api.get("/brands", {
        params: {
          page,
          size,
        },
      });

      const pageBrands = extractBrandContent(res.data);
      allBrands.push(...pageBrands);

      totalPages = getTotalPages(res.data);
      page++;
    } while (page < totalPages);

    brands.value = allBrands;
  } catch (error) {
    console.error("Lỗi khi tải danh sách thương hiệu từ BE:", error);
    brands.value = [];
  }
};

onMounted(() => {
  fetchBrands();
});

const getBrandMonogram = (brandName: string) => {
  if (!brandName) return "";

  return brandName
    .split(" ")
    .map((word) => word.charAt(0))
    .join("")
    .slice(0, 2)
    .toUpperCase();
};

const getBrandLogoUrl = (brand: Brand) => {
  const rawUrl = brand.logoUrl?.trim();

  if (!rawUrl) {
    return "";
  }

  if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
    return rawUrl;
  }

  if (rawUrl.startsWith("/")) {
    return `http://localhost:8080${rawUrl}`;
  }

  return `http://localhost:8080/${rawUrl}`;
};
</script>

<style scoped>
.shop-navbar {
  position: relative;
  z-index: 1000;
  height: 52px;
  background: #ffffff;
  border-bottom: 1px solid rgba(26, 26, 26, 0.08);
}

.nav {
  overflow: visible !important;
}

.nav-item {
  position: relative;
}

.menu-link {
  position: relative;
  color: var(--aura-black);
  font-family: var(--aura-sans);
  font-size: 15px;
  font-weight: 800;
  padding: 15px 34px;
  white-space: nowrap;
  transition: all 0.22s ease;
}

.menu-link::after {
  content: "";
  position: absolute;
  left: 34px;
  right: 34px;
  bottom: 7px;
  height: 1px;
  background: var(--aura-gold);
  opacity: 0;
  transform: scaleX(0);
  transition: all 0.22s ease;
}

.menu-link:hover {
  color: var(--aura-gold);
}

.menu-link:hover::after {
  opacity: 1;
  transform: scaleX(1);
}

.nav-chevron {
  font-size: 11px;
  transition: transform 0.22s ease;
}

.nav-dropdown-wrapper:hover .nav-chevron {
  transform: rotate(180deg);
}

.flash-sale-link {
  color: #b31320;
  font-weight: 900;
}

.flash-sale-link:hover {
  color: #8f0f19;
}

.flash-sale-link::after {
  background: #b31320;
}

/* SIMPLE DROPDOWN */
.simple-dropdown {
  position: absolute;
  top: calc(100% + 1px);
  left: 50%;
  z-index: 2400;
  min-width: 220px;
  padding: 10px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid rgba(189, 154, 95, 0.18);
  box-shadow: 0 20px 50px rgba(5, 16, 36, 0.16);
  opacity: 0;
  visibility: hidden;
  transform: translate(-50%, 12px);
  pointer-events: none;
  transition: all 0.22s ease;
}

.simple-dropdown::before {
  content: "";
  position: absolute;
  top: -8px;
  left: 50%;
  width: 16px;
  height: 16px;
  background: #ffffff;
  border-left: 1px solid rgba(189, 154, 95, 0.18);
  border-top: 1px solid rgba(189, 154, 95, 0.18);
  transform: translateX(-50%) rotate(45deg);
}

.nav-dropdown-wrapper:hover .simple-dropdown {
  opacity: 1;
  visibility: visible;
  transform: translate(-50%, 0);
  pointer-events: auto;
}

.simple-dropdown-item {
  min-height: 42px;
  padding: 10px 12px;
  border-radius: 9px;
  color: var(--aura-black);
  text-decoration: none;
  display: flex;
  align-items: center;
  font-size: 14px;
  font-weight: 800;
  transition: all 0.2s ease;
}

.simple-dropdown-item:hover {
  background: rgba(189, 154, 95, 0.09);
  color: var(--aura-gold);
}

/* BRAND DROPDOWN */
.brand-dropdown {
  position: absolute;
  top: calc(100% + 1px);
  left: 50%;
  z-index: 2500;
  width: 620px;
  padding: 24px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid rgba(189, 154, 95, 0.18);
  box-shadow: 0 28px 70px rgba(5, 16, 36, 0.18);
  opacity: 0;
  visibility: hidden;
  transform: translate(-50%, 12px);
  transition: all 0.24s ease;
  pointer-events: none;
}

.brand-dropdown::before {
  content: "";
  position: absolute;
  top: -8px;
  left: 50%;
  width: 16px;
  height: 16px;
  background: #ffffff;
  border-left: 1px solid rgba(189, 154, 95, 0.18);
  border-top: 1px solid rgba(189, 154, 95, 0.18);
  transform: translateX(-50%) rotate(45deg);
}

.nav-dropdown-wrapper:hover .brand-dropdown {
  opacity: 1;
  visibility: visible;
  transform: translate(-50%, 0);
  pointer-events: auto;
}

.brand-dropdown-header {
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(26, 26, 26, 0.08);
}

.brand-dropdown-kicker {
  color: var(--aura-gold);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 3px;
  text-transform: uppercase;
}

.brand-dropdown-title {
  color: var(--aura-black);
  font-family: var(--aura-serif);
  font-size: 26px;
  font-weight: 700;
}

.brand-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 18px;
  row-gap: 10px;
  max-height: 360px;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 10px;
}

/* Scrollbar Chrome / Edge */
.brand-grid::-webkit-scrollbar {
  width: 8px;
}

.brand-grid::-webkit-scrollbar-track {
  background: rgba(189, 154, 95, 0.08);
  border-radius: 999px;
}

.brand-grid::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #d8bb76, #9d7939);
  border-radius: 999px;
  border: 2px solid #fffaf2;
}

.brand-grid::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #cda65c, #7c5a24);
}

/* Scrollbar Firefox */
.brand-grid {
  scrollbar-width: thin;
  scrollbar-color: #bd9a5f rgba(189, 154, 95, 0.08);
}

.brand-item {
  min-height: 58px;
  padding: 12px 12px;
  border-radius: 10px;
  color: var(--aura-black);
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 14px;
  transition: all 0.22s ease;
}

.brand-item:hover {
  background: rgba(189, 154, 95, 0.09);
  color: var(--aura-gold);
  transform: translateX(2px);
}

.brand-logo-circle {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  flex-shrink: 0;
  border: 1px solid rgba(189, 154, 95, 0.34);
  background: #fffaf2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.brand-logo-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 6px;
  display: block;
}

.brand-monogram {
  width: 100%;
  height: 100%;
  color: var(--aura-gold);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--aura-serif);
  font-size: 17px;
  font-weight: 800;
}

.brand-name {
  flex: 1;
  font-family: var(--aura-serif);
  font-size: 20px;
  font-weight: 700;
}

.brand-item i {
  color: var(--aura-gold);
  font-size: 20px;
  opacity: 0;
  transform: translateX(-4px);
  transition: all 0.22s ease;
}

.brand-item:hover i {
  opacity: 1;
  transform: translateX(0);
}

@media (max-width: 991.98px) {
  .menu-link {
    padding-left: 24px;
    padding-right: 24px;
  }

  .brand-dropdown {
    left: 0;
    width: 540px;
    transform: translate(0, 12px);
  }

  .nav-dropdown-wrapper:hover .brand-dropdown {
    transform: translate(0, 0);
  }

  .brand-dropdown::before {
    left: 42px;
  }

  .simple-dropdown {
    left: 0;
    transform: translate(0, 12px);
  }

  .nav-dropdown-wrapper:hover .simple-dropdown {
    transform: translate(0, 0);
  }

  .simple-dropdown::before {
    left: 42px;
  }
}

@media (max-width: 767.98px) {
  .menu-link {
    padding-left: 18px;
    padding-right: 18px;
  }

  .menu-link::after {
    left: 18px;
    right: 18px;
  }

  .brand-dropdown {
    width: 320px;
    padding: 18px;
  }

  .brand-grid {
    grid-template-columns: 1fr;
  }

  .brand-dropdown-title {
    font-size: 22px;
  }

  .brand-name {
    font-size: 18px;
  }

  .simple-dropdown {
    min-width: 210px;
  }
}
</style>