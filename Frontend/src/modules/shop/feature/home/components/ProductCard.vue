<template>
  <div
    class="product-card h-100 position-relative overflow-hidden"
    @click="goToDetail"
  >
    <button
      type="button"
      class="btn-favorite"
      :class="{ active: isFavorited }"
      :disabled="favoriteLoading"
      @click.stop="handleToggleFavorite"
      :title="isFavorited ? 'Bỏ yêu thích' : 'Thêm vào yêu thích'"
    >
      <i v-if="favoriteLoading" class="spinner-border spinner-border-sm"></i>
      <i
        v-else
        class="bi"
        :class="isFavorited ? 'bi-heart-fill' : 'bi-heart'"
      ></i>
    </button>

    <button
      type="button"
      class="btn-compare-card"
      :class="{ active: isInCompare(activeProduct) }"
      @click.stop="toggleCompare(activeProduct)"
      :title="isInCompare(activeProduct) ? 'Bỏ so sánh' : 'Thêm vào so sánh'"
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M16 3l4 4-4 4" />
        <path d="M20 7H4" />
        <path d="M8 21l-4-4 4-4" />
        <path d="M4 17h16" />
      </svg>
    </button>

    <div class="product-image-wrapper">
      <span v-if="cardDiscountPercent > 0" class="discount-badge">
        -{{ cardDiscountPercent }}%
      </span>

      <img
        v-if="hasProductImage"
        :src="productImage"
        :alt="activeProduct.name"
        class="product-real-image"
        @error="handleProductImageError"
      />
      <div
        v-else
        class="product-bottle"
        :style="getBottleStyle(activeProduct.color)"
      >
        <div class="product-bottle-cap"></div>
        <div class="product-bottle-neck"></div>
        <div class="product-bottle-body">
          <div class="product-bottle-label">
            <strong>{{ shortBrand }}</strong>
            <span>PERFUME</span>
          </div>
        </div>
      </div>
    </div>

    <div class="product-content">
      <p class="product-brand mb-1">{{ activeProduct.brand }}</p>
      <h3 class="product-name text-truncate mb-2">{{ activeProduct.name }}</h3>

      <div class="rating-row d-flex align-items-center gap-2 mb-2">
        <span class="stars">{{ starsDisplay }}</span>
        <span class="review-count"
          >{{ ratingDisplay }} | {{ normalizedReviewCount }} đánh giá</span
        >
      </div>

      <div class="price-row d-flex align-items-end gap-2 mb-3 flex-wrap">
        <span v-if="hasVariantPriceRange" class="price-prefix">Từ</span>
        <span class="sale-price">{{ formatCurrency(cardSalePrice) }}</span>
        <span
          v-if="cardDiscountPercent > 0 && cardOriginalPrice > cardSalePrice"
          class="original-price text-decoration-line-through"
        >
          {{ formatCurrency(cardOriginalPrice) }}
        </span>
      </div>

      <div class="product-actions" style="position: relative; z-index: 10">
        <button
          type="button"
          class="btn buy-now-btn"
          @click.stop="openVariantModal('BUY')"
        >
          <i class="bi bi-lightning-charge me-2"></i> Mua ngay
        </button>
        <button
          type="button"
          class="btn add-cart-btn"
          @click.stop="openVariantModal('CART')"
        >
          <i class="bi bi-bag-plus me-2"></i> Thêm sản phẩm
        </button>
      </div>
    </div>

    <!-- MODAL MUA NHANH / THÊM GIỎ HÀNG CHỌN BIẾN THỂ -->
    <Teleport to="body">
      <div
        v-if="showVariantModal"
        class="custom-modal-overlay"
        @click.self="showVariantModal = false"
      >
        <div class="variant-modal-box">
          <div class="vm-header">
            <h5>Chọn Phân Loại</h5>
            <button class="vm-close" @click="showVariantModal = false">
              <svg
                viewBox="0 0 24 24"
                width="20"
                height="20"
                stroke="currentColor"
                stroke-width="2"
                fill="none"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </div>

          <div class="vm-product-info">
            <div class="vm-img-box">
              <img
                :src="modalImage"
                alt="Product Image"
                @error="handleModalImageError"
              />
            </div>
            <div class="vm-details">
              <h6>{{ currentTargetProduct?.name || activeProduct?.name }}</h6>
              <div class="d-flex align-items-center gap-2 flex-wrap">
                <p class="vm-price mb-0">
                  {{
                    formatCurrency(
                      selectedVariant
                        ? selectedVariant.salePrice ??
                            selectedVariant.price ??
                            0
                        : cardSalePrice
                    )
                  }}
                </p>
                <span
                  v-if="
                    selectedVariant &&
                    (selectedVariant.salePrice ?? selectedVariant.price ?? 0) <
                      (selectedVariant.originalPrice ??
                        selectedVariant.oldPrice ??
                        0)
                  "
                  class="text-decoration-line-through text-muted small"
                >
                  {{
                    formatCurrency(
                      selectedVariant.originalPrice ??
                        selectedVariant.oldPrice ??
                        0
                    )
                  }}
                </span>
                <span
                  v-if="calculatedDiscountPercent > 0"
                  class="flash-sale-badge ms-2"
                  >-{{ calculatedDiscountPercent }}%</span
                >
              </div>
            </div>
          </div>

          <div class="vm-variants">
            <p class="vm-label">TÙY CHỌN PHÂN LOẠI:</p>
            <div v-if="isLoadingVariants" class="text-center py-4">
              <span
                class="spinner-border spinner-border-sm me-2"
                style="color: #b78d52"
              ></span>
              <span style="color: #718096; font-size: 13px"
                >Đang tải thông tin...</span
              >
            </div>
            <div v-else class="vm-grid">
              <button
                v-for="v in fullVariants"
                :key="v.productVariantId || v.id"
                class="vm-variant-btn"
                :class="{
                  selected:
                    (selectedVariant?.productVariantId ||
                      selectedVariant?.id) === (v.productVariantId || v.id),
                  disabled: !isVariantSellable(v),
                }"
                :disabled="!isVariantSellable(v)"
                @click="
                  selectedVariant = v;
                  quantity = 1;
                "
              >
                <span class="vm-v-name">{{
                  v.displayCapacity || formatVariantName(v)
                }}</span>
              </button>
            </div>

            <hr class="variant-divider" v-if="selectedVariant" />
            <div class="quantity-section" v-if="selectedVariant">
              <p class="vm-label mb-0">SỐ LƯỢNG:</p>
              <div class="quantity-control">
                <div class="qty-wrapper">
                  <button
                    type="button"
                    @click="quantity > 1 ? quantity-- : null"
                    :disabled="quantity <= 1"
                  >
                    −
                  </button>
                  <input
                    type="number"
                    v-model="quantity"
                    @input="validateQuantity"
                    @blur="validateQuantity"
                    @keyup.enter="validateQuantity"
                  />
                  <button type="button" @click="increaseQuantity">+</button>
                </div>
                <span class="stock-info"> Kho: {{ maxQuantity }} </span>
              </div>
            </div>
          </div>

          <div class="vm-actions">
            <button
              class="vm-btn-cart"
              :disabled="
                !selectedVariant ||
                !isVariantSellable(selectedVariant) ||
                actionLoading ||
                isLoadingVariants
              "
              @click="confirmActionSpecific('CART')"
            >
              <span
                v-if="actionLoading && actionType === 'CART'"
                class="spinner-border spinner-border-sm me-2"
              ></span>
              <i class="bi bi-bag-plus me-1"></i> THÊM VÀO GIỎ
            </button>
            <button
              class="vm-btn-buy"
              :disabled="
                !selectedVariant ||
                !isVariantSellable(selectedVariant) ||
                actionLoading ||
                isLoadingVariants
              "
              @click="confirmActionSpecific('BUY')"
            >
              <span
                v-if="actionLoading && actionType === 'BUY'"
                class="spinner-border spinner-border-sm me-2"
              ></span>
              <i class="bi bi-lightning-charge me-1"></i> MUA NGAY
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- FLOATING COMPARE BAR -->
    <Teleport to="body">
      <div
        class="compare-bar"
        :class="{ show: isCompareUIRenderer && sharedCompareList.length > 0 }"
      >
        <div class="cb-container">
          <div class="cb-left">
            <span class="cb-title"
              >So sánh sản phẩm ({{ sharedCompareList.length }}/3)</span
            >
            <div class="cb-slots">
              <div
                v-for="index in 3"
                :key="'slot-' + index"
                class="cb-slot"
                :class="{
                  filled: sharedCompareList[index - 1],
                  'empty-slot': !sharedCompareList[index - 1],
                }"
                @click="!sharedCompareList[index - 1] && openPickerModal()"
              >
                <template v-if="sharedCompareList[index - 1]">
                  <img
                    :src="getGlobalProductImage(sharedCompareList[index - 1])"
                    alt="Img"
                  />
                  <div class="cb-slot-info">
                    <p>{{ sharedCompareList[index - 1].name }}</p>
                    <span>{{
                      formatCurrency(
                        getComparePrice(sharedCompareList[index - 1])
                      )
                    }}</span>
                  </div>
                  <button
                    class="btn-remove-cb"
                    @click.stop="
                      removeFromCompare(sharedCompareList[index - 1])
                    "
                  >
                    <i class="bi bi-x"></i>
                  </button>
                </template>
                <template v-else>
                  <i class="bi bi-plus-lg me-1"></i> Thêm sản phẩm
                </template>
              </div>
            </div>
          </div>
          <div class="cb-right">
            <button class="cb-btn-clear" @click="clearCompareList">
              Xóa tất cả
            </button>
            <button
              class="cb-btn-compare"
              :disabled="sharedCompareList.length < 2"
              @click="sharedShowCompareModal = true"
            >
              So sánh ngay ({{ sharedCompareList.length }})
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- BẢNG POPUP SO SÁNH CHUYÊN NGHIỆP CÓ AI -->
    <Teleport to="body">
      <div
        class="compare-modal-overlay"
        v-if="isCompareUIRenderer && sharedShowCompareModal"
        @click.self="sharedShowCompareModal = false"
      >
        <div class="compare-modal-box">
          <div class="cm-header">
            <h3>So sánh thông số</h3>
            <button class="cm-close" @click="sharedShowCompareModal = false">
              ✕
            </button>
          </div>
          <div class="cm-body">
            <!-- BẮT ĐẦU: KHỐI TÍNH NĂNG AI -->
            <div class="ai-compare-toolbar">
              <div class="ai-compare-toolbar-text">
                <div class="ai-compare-title">
                  <i class="bi bi-stars"></i>
                  So sánh bằng AI
                </div>
                <div class="ai-compare-subtitle">
                  AI sẽ phân tích thêm độ lưu hương, phong cách, hoàn cảnh sử
                  dụng và gợi ý lựa chọn dựa trên dữ liệu sản phẩm.
                </div>
              </div>

              <button
                type="button"
                class="cm-btn-ai"
                :disabled="
                  sharedCompareAiLoading || sharedCompareList.length < 2
                "
                @click="handleAiCompare"
              >
                <span
                  v-if="sharedCompareAiLoading"
                  class="spinner-border spinner-border-sm me-2"
                ></span>
                <i v-else class="bi bi-stars me-2"></i>
                {{
                  sharedCompareAiLoading
                    ? "Đang phân tích..."
                    : "So sánh bằng AI"
                }}
              </button>
            </div>

            <div v-if="sharedCompareAiError" class="ai-compare-error">
              <i class="bi bi-exclamation-circle me-2"></i>
              {{ sharedCompareAiError }}
            </div>

            <div
              v-if="sharedCompareAnalysis || sharedCompareRecommendation"
              class="ai-compare-result"
            >
              <div v-if="sharedCompareAnalysis" class="ai-result-block">
                <div class="ai-result-label">
                  <i class="bi bi-lightbulb me-2"></i>
                  Nhận xét từ AI
                </div>
                <p>{{ sharedCompareAnalysis }}</p>
              </div>

              <div
                v-if="sharedCompareRecommendation"
                class="ai-result-block ai-result-recommendation"
              >
                <div class="ai-result-label">
                  <i class="bi bi-check2-circle me-2"></i>
                  Gợi ý lựa chọn
                </div>
                <p>{{ sharedCompareRecommendation }}</p>
              </div>
            </div>
            <!-- KẾT THÚC: KHỐI TÍNH NĂNG AI -->

            <table class="table-compare">
              <thead class="sticky-header">
                <tr>
                  <th class="spec-label-col">Sản phẩm</th>
                  <td
                    v-for="p in sharedCompareList"
                    :key="'img' + getProductIdNum(p)"
                    class="spec-value-col"
                  >
                    <button
                      class="btn-remove-from-table"
                      @click="removeFromCompare(p)"
                      title="Xóa khỏi so sánh"
                    >
                      <i class="bi bi-x"></i>
                    </button>
                    <div
                      class="cm-img-wrapper clickable-item"
                      @click="goToDetailFromCompare(p)"
                      title="Xem chi tiết"
                    >
                      <img :src="getGlobalProductImage(p)" class="cm-img" />
                    </div>
                    <h4
                      class="cm-name clickable-item"
                      @click="goToDetailFromCompare(p)"
                      title="Xem chi tiết"
                    >
                      {{ p.name }}
                    </h4>
                    <div class="cm-header-price">
                      {{ formatCurrency(getComparePrice(p)) }}
                      <span
                        v-if="getCompareDiscount(p) > 0"
                        class="flash-sale-badge ms-2"
                        >-{{ getCompareDiscount(p) }}%</span
                      >
                    </div>
                  </td>
                  <td
                    v-for="i in Math.max(0, 3 - sharedCompareList.length)"
                    :key="'empty-th-' + i"
                    class="spec-value-col empty-col"
                  >
                    <div
                      class="empty-product-slot"
                      @click="openPickerModal"
                      title="Chọn thêm sản phẩm so sánh"
                    >
                      <i class="bi bi-plus-circle-dotted"></i
                      ><span>Thêm sản phẩm</span>
                    </div>
                  </td>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td colspan="4" class="group-header">Thông tin cơ bản</td>
                </tr>
                <tr>
                  <th class="spec-label-col">Thương hiệu</th>
                  <td
                    v-for="p in sharedCompareList"
                    :key="'brand' + getProductIdNum(p)"
                    class="fw-bold text-dark"
                  >
                    {{ getCompareValue(p, "brand") }}
                  </td>
                  <td
                    v-for="i in Math.max(0, 3 - sharedCompareList.length)"
                    :key="'empty-brand-' + i"
                  ></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Phân loại dung tích</th>
                  <td
                    v-for="p in sharedCompareList"
                    :key="'var' + getProductIdNum(p)"
                  >
                    <select
                      v-if="p.variants && p.variants.length > 0"
                      class="compare-select"
                      v-model="sharedCompareVariantIds[getProductIdNum(p)]"
                    >
                      <option
                        v-for="v in getSortedVariants(p)"
                        :key="v.productVariantId || v.variantId || v.id"
                        :value="v.productVariantId || v.variantId || v.id"
                      >
                        {{ formatVariantName(v) }}
                      </option>
                    </select>
                    <span v-else class="text-muted">Mặc định</span>
                  </td>
                  <td
                    v-for="i in Math.max(0, 3 - sharedCompareList.length)"
                    :key="'empty-var-' + i"
                  ></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Loại chai</th>
                  <td
                    v-for="p in sharedCompareList"
                    :key="'bottle' + getProductIdNum(p)"
                  >
                    {{ getCompareBottleType(p) }}
                  </td>
                  <td
                    v-for="i in Math.max(0, 3 - sharedCompareList.length)"
                    :key="'empty-bottle-' + i"
                  ></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Giá ưu đãi</th>
                  <td
                    v-for="p in sharedCompareList"
                    :key="'price' + getProductIdNum(p)"
                  >
                    <div class="cm-price-val">
                      {{ formatCurrency(getComparePrice(p)) }}
                      <span
                        v-if="getCompareDiscount(p) > 0"
                        class="flash-sale-badge ms-2"
                        >-{{ getCompareDiscount(p) }}%</span
                      >
                    </div>
                    <div
                      v-if="getCompareOriginalPrice(p) > getComparePrice(p)"
                      class="text-decoration-line-through text-muted small mt-1"
                    >
                      {{ formatCurrency(getCompareOriginalPrice(p)) }}
                    </div>
                  </td>
                  <td
                    v-for="i in Math.max(0, 3 - sharedCompareList.length)"
                    :key="'empty-price-' + i"
                  ></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Đơn giá / 1ml</th>
                  <td
                    v-for="p in sharedCompareList"
                    :key="'priceml' + getProductIdNum(p)"
                  >
                    <div class="fw-bold" style="color: #b78d52">
                      {{ formatPricePerMl(p) }}
                    </div>
                    <span
                      v-if="isBestValue(p)"
                      class="badge bg-warning bg-opacity-10 text-warning border border-warning mt-2 d-inline-block px-2 py-1"
                      >Tiết kiệm nhất</span
                    >
                  </td>
                  <td
                    v-for="i in Math.max(0, 3 - sharedCompareList.length)"
                    :key="'empty-priceml-' + i"
                  ></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Tình trạng kho</th>
                  <td
                    v-for="p in sharedCompareList"
                    :key="'stock' + getProductIdNum(p)"
                  >
                    <span
                      :class="
                        getCompareStock(p) > 0
                          ? 'text-success fw-bold'
                          : 'text-danger fw-bold'
                      "
                    >
                      {{ getCompareStock(p) > 0 ? "Còn hàng" : "Hết hàng" }}
                    </span>
                  </td>
                  <td
                    v-for="i in Math.max(0, 3 - sharedCompareList.length)"
                    :key="'empty-stock-' + i"
                  ></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Đánh giá</th>
                  <td
                    v-for="p in sharedCompareList"
                    :key="'rating' + getProductIdNum(p)"
                    class="cm-rating"
                  >
                    <div
                      class="d-flex align-items-center gap-1"
                      style="justify-content: center"
                    >
                      <span class="score">{{ getRatingScore(p) }} ★</span>
                      <span class="count">({{ getReviewCount(p) }})</span>
                    </div>
                  </td>
                  <td
                    v-for="i in Math.max(0, 3 - sharedCompareList.length)"
                    :key="'empty-rating-' + i"
                  ></td>
                </tr>

                <tr>
                  <td colspan="4" class="group-header">Đặc tính sản phẩm</td>
                </tr>
                <tr>
                  <th class="spec-label-col">Nhóm hương chính</th>
                  <td
                    v-for="p in sharedCompareList"
                    :key="'scent' + getProductIdNum(p)"
                  >
                    {{ getCompareValue(p, "scent") }}
                  </td>
                  <td
                    v-for="i in Math.max(0, 3 - sharedCompareList.length)"
                    :key="'empty-scent-' + i"
                  ></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Nồng độ</th>
                  <td
                    v-for="p in sharedCompareList"
                    :key="'con' + getProductIdNum(p)"
                    class="fw-bold"
                  >
                    {{ getCompareValue(p, "concentration") }}
                  </td>
                  <td
                    v-for="i in Math.max(0, 3 - sharedCompareList.length)"
                    :key="'empty-con-' + i"
                  ></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Độ lưu hương</th>
                  <td
                    v-for="p in sharedCompareList"
                    :key="'long' + getProductIdNum(p)"
                    style="color: #b78d52; font-weight: 600"
                  >
                    {{ getLongevityDisplay(p) }}
                  </td>
                  <td
                    v-for="i in Math.max(0, 3 - sharedCompareList.length)"
                    :key="'empty-long-' + i"
                  ></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Đối tượng (Giới tính)</th>
                  <td
                    v-for="p in sharedCompareList"
                    :key="'gen' + getProductIdNum(p)"
                  >
                    {{ getCompareValue(p, "gender") }}
                  </td>
                  <td
                    v-for="i in Math.max(0, 3 - sharedCompareList.length)"
                    :key="'empty-gen-' + i"
                  ></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Phong cách</th>
                  <td
                    v-for="p in sharedCompareList"
                    :key="'style' + getProductIdNum(p)"
                  >
                    {{ getCompareValue(p, "style") }}
                  </td>
                  <td
                    v-for="i in Math.max(0, 3 - sharedCompareList.length)"
                    :key="'empty-style-' + i"
                  ></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Hoàn cảnh khuyên dùng</th>
                  <td
                    v-for="p in sharedCompareList"
                    :key="'occ' + getProductIdNum(p)"
                  >
                    {{ getCompareValue(p, "occasion") }}
                  </td>
                  <td
                    v-for="i in Math.max(0, 3 - sharedCompareList.length)"
                    :key="'empty-occ-' + i"
                  ></td>
                </tr>

                <tr>
                  <td
                    colspan="4"
                    class="group-header bg-white border-bottom-0 pt-4"
                  ></td>
                </tr>
                <tr>
                  <th class="spec-label-col border-bottom-0"></th>
                  <td
                    v-for="p in sharedCompareList"
                    :key="'act' + getProductIdNum(p)"
                    class="border-bottom-0 pb-4"
                  >
                    <button
                      class="cm-btn-buy"
                      :disabled="isCompareBuyDisabled(p)"
                      @click="buyFromCompare(p)"
                    >
                      <i class="bi bi-cart-plus me-1"></i>
                      {{
                        isCompareBuyDisabled(p)
                          ? "Tạm hết hàng"
                          : "Xem sản phẩm"
                      }}
                    </button>
                  </td>
                  <td
                    v-for="i in Math.max(0, 3 - sharedCompareList.length)"
                    :key="'empty-act-' + i"
                    class="border-bottom-0"
                  ></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- MODAL CHỌN NHANH SẢN PHẨM CÓ ĐẦY ĐỦ GIÁ -->
    <Teleport to="body">
      <div
        class="compare-modal-overlay"
        v-if="isCompareUIRenderer && sharedShowPickerModal"
        @click.self="sharedShowPickerModal = false"
      >
        <div class="product-picker-box">
          <div class="cm-header">
            <h3>Chọn sản phẩm so sánh</h3>
            <button class="cm-close" @click="sharedShowPickerModal = false">
              ✕
            </button>
          </div>
          <div class="picker-search-bar">
            <input
              type="text"
              v-model="pickerSearchKeyword"
              placeholder="Tìm kiếm theo tên sản phẩm hoặc thương hiệu..."
              class="picker-input"
            />
          </div>
          <div class="picker-body">
            <div v-if="pickerLoading" class="text-center py-5">
              <span class="spinner-border text-warning"></span>
            </div>
            <div
              v-else-if="filteredPickerProducts.length === 0"
              class="text-center py-5 text-muted"
            >
              Không tìm thấy sản phẩm phù hợp.
            </div>
            <div v-else class="picker-grid">
              <div
                v-for="item in filteredPickerProducts"
                :key="getProductIdNum(item)"
                class="picker-item"
                :class="{ selected: isInCompare(item) }"
                @click="toggleItemInPicker(item)"
              >
                <img :src="getGlobalProductImage(item)" :alt="item.name" />
                <div class="picker-info">
                  <span class="brand">{{ getBrandNameGlobal(item) }}</span>
                  <h5 class="name" :title="item.name">{{ item.name }}</h5>
                  <div class="picker-price">
                    {{ formatCurrency(getComparePrice(item)) }}
                    <span
                      v-if="getCompareDiscount(item) > 0"
                      class="flash-sale-badge ms-1"
                      style="font-size: 10px; padding: 2px 6px"
                      >-{{ getCompareDiscount(item) }}%</span
                    >
                  </div>
                </div>
                <div class="picker-check">
                  <i
                    :class="
                      isInCompare(item)
                        ? 'bi bi-check-circle-fill text-success'
                        : 'bi bi-circle text-muted'
                    "
                  ></i>
                </div>
              </div>
            </div>
          </div>
          <div class="picker-footer">
            <button
              class="btn btn-secondary btn-sm px-4"
              @click="sharedShowPickerModal = false"
            >
              Đóng
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script lang="ts">
import { ref } from "vue";

export const sharedCompareList = ref<any[]>([]);
export const sharedShowCompareModal = ref(false);
export const sharedShowPickerModal = ref(false);
export const sharedCompareVariantIds = ref<Record<number, number>>({});
export const allProductsStore = ref<any[]>([]);
export const pickerLoading = ref(false);
export const pickerSearchKeyword = ref("");

export type CompareInsight = {
  productId: number;
  longevity?: string;
  style?: string;
  occasion?: string;
};

export const sharedCompareInsights = ref<Record<number, CompareInsight>>({});
export const sharedCompareAiLoading = ref(false);
export const sharedCompareAnalysis = ref("");
export const sharedCompareRecommendation = ref("");
export const sharedCompareAiError = ref("");

let currentCompareRendererId: string | null = null;

let sharedFavoriteLoadPromise: Promise<any> | null = null;
</script>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, watch } from "vue";
import { useRouter } from "vue-router";
import Swal from "sweetalert2";
import api from "@/common/api";
import { favoriteService } from "@/modules/shop/feature/product/services/favorite.service";

interface ProductVariant {
  id?: number;
  Id?: number;
  variantId?: number;
  productVariantId?: number;
  price?: number | string;
  originalPrice?: number | string;
  oldPrice?: number | string;
  salePrice?: number | string;
  promotionPrice?: number | string;
  flashSalePrice?: number | string;
  currentPrice?: number | string;
  displayPrice?: number | string;
  finalPrice?: number | string;
  minPrice?: number | string;
  discountPercent?: number | string;
  stock?: number;
  stockQuantity?: number;
  sellableQuantity?: number;
  availableQuantity?: number;
  quantity?: number;
  status?: number;
  capacity?: string | number | any;
  capacityName?: string | number;
  capacityValue?: string | number;
  volume?: string | number;
  bottleType?: string | any;
  bottleTypeName?: string;
  imageUrl?: string;
  ImageUrl?: string;
  image?: string;
  Image?: string;
  mainImage?: string;
  mainImageUrl?: string;
  thumbnailUrl?: string;
  manufacturingDate?: string;
  mfgDate?: string;
  expirationDate?: string;
  expDate?: string;
  images?: any[];
  Images?: any[];
  imageList?: any[];
  productImages?: any[];
  ProductImages?: any[];
  productImageList?: any[];
  ProductImageList?: any[];
}

interface Product {
  id: number;
  productId?: number;
  productVariantId?: number;
  variantId?: number;
  name: string;
  brand: string;
  color?: string;
  price?: number | string;
  oldPrice?: number | string;
  promotionPrice?: number | string;
  flashSalePrice?: number | string;
  currentPrice?: number | string;
  displayPrice?: number | string;
  finalPrice?: number | string;
  minPrice?: number | string;
  salePrice?: number | string;
  originalPrice?: number | string;
  discountPercent?: number | string;
  rating: number;
  averageRating?: number;
  reviewCount: number;
  imageUrl?: string;
  ImageUrl?: string;
  image?: string;
  Image?: string;
  mainImage?: string;
  MainImage?: string;
  mainImageUrl?: string;
  MainImageUrl?: string;
  thumbnailUrl?: string;
  ThumbnailUrl?: string;
  images?: any[];
  Images?: any[];
  imageList?: any[];
  ImageList?: any[];
  galleryImages?: any[];
  GalleryImages?: any[];
  productImages?: any[];
  ProductImages?: any[];
  productImageList?: any[];
  ProductImageList?: any[];
  stock?: number;
  stockQuantity?: number;
  sellableQuantity?: number;
  availableQuantity?: number;
  status?: number;
  isFlashSale?: boolean;
  manufacturingDate?: string;
  mfgDate?: string;
  expirationDate?: string;
  expDate?: string;
  variants?: ProductVariant[];
}

const props = withDefaults(
  defineProps<{
    product: Product;
    preloadDetail?: boolean;
  }>(),
  {
    preloadDetail: true,
  }
);
const router = useRouter();
const instanceId = Math.random().toString(36).substring(2, 9);
const isCompareUIRenderer = ref(false);

const fullProductData = ref<any>(null);

const activeProduct = computed(() => {
  const p = props.product as any;
  const full = fullProductData.value;

  const pVariants =
    p?.variants || p?.productVariants || p?.productVariantList || [];

  if (!full) {
    return {
      ...p,
      variants: pVariants,
    };
  }

  const fVariants =
    full?.variants || full?.productVariants || full?.productVariantList || [];

  /*
   * Nếu card đã được parent xác định là Flash Sale thì dữ liệu
   * giá khuyến mãi từ parent phải được giữ nguyên.
   *
   * /v1/products/{id} chỉ dùng để bổ sung thông tin đầy đủ
   * của sản phẩm, không được ghi đè giá Flash Sale theo SKU.
   */
  if (Boolean(p?.isFlashSale)) {
    return {
      ...p,
      ...full,

      /*
       * Giữ nguyên dữ liệu Flash Sale chính xác mà Home đã
       * tổng hợp từ /promotions/flash-sale.
       */
      isFlashSale: true,
      salePrice: p.salePrice,
      originalPrice: p.originalPrice,
      discountPercent: p.discountPercent,
      flashSalePrice: p.flashSalePrice,
      promotionPrice: p.promotionPrice,

      expirationDate:
        full.expirationDate || full.expDate || p.expirationDate || p.expDate,

      manufacturingDate:
        full.manufacturingDate ||
        full.mfgDate ||
        p.manufacturingDate ||
        p.mfgDate,

      /*
       * Quan trọng nhất:
       * không để variants của /v1/products/{id}
       * đè mất salePrice từng SKU từ Flash Sale.
       */
      variants: pVariants.length > 0 ? pVariants : fVariants,
    };
  }

  /*
   * Sản phẩm thường giữ nguyên logic cũ 100%.
   */
  return {
    ...p,
    ...full,

    expirationDate:
      full.expirationDate || full.expDate || p.expirationDate || p.expDate,

    manufacturingDate:
      full.manufacturingDate ||
      full.mfgDate ||
      p.manufacturingDate ||
      p.mfgDate,

    variants: fVariants.length > 0 ? fVariants : pVariants,
  };
});

const getSafeNumber = (val: any) => {
  if (val === null || val === undefined || val === "") return 0;
  if (typeof val === "number") return isNaN(val) ? 0 : val;
  if (typeof val === "string") {
    const cleanStr = val.replace(/[^\d]/g, "");
    return parseInt(cleanStr, 10) || 0;
  }
  return 0;
};

const getProductPrices = (item: any, parentProduct: any = null) => {
  if (!item) return { sale: 0, orig: 0, percent: 0 };

  const basePrice = getSafeNumber(item.price ?? item.Price ?? item.basePrice);
  const flashSale = getSafeNumber(
    item.flashSalePrice ??
      item.salePrice ??
      item.promotionPrice ??
      item.discountPrice ??
      item.specialPrice ??
      item.currentPrice
  );
  let explicitOrig = getSafeNumber(
    item.originalPrice ?? item.oldPrice ?? item.listPrice ?? item.regularPrice
  );
  const percent = getSafeNumber(
    item.discountPercent ??
      item.discount ??
      item.salePercent ??
      parentProduct?.discountPercent ??
      parentProduct?.discount ??
      0
  );

  let sale = 0;
  let orig = 0;

  if (flashSale > 0) {
    sale = flashSale;
    orig =
      explicitOrig > flashSale
        ? explicitOrig
        : basePrice > flashSale
        ? basePrice
        : 0;
  } else if (basePrice > 0) {
    sale = basePrice;
    orig = explicitOrig > basePrice ? explicitOrig : 0;
  }

  if (percent > 0 && sale > 0 && (orig === 0 || orig <= sale)) {
    orig = sale;
    sale = Math.round(orig * (1 - percent / 100));
  }

  if (orig > 0 && sale > orig) {
    const temp = sale;
    sale = orig;
    orig = temp;
  }

  return { sale, orig, percent };
};

const getCheapestVariant = (p: any) => {
  const variants =
    p?.variants || p?.productVariants || p?.productVariantList || [];
  if (!variants || variants.length === 0) return null;

  const activePricedVariants = variants.filter((v: any) => {
    const status = Number(v?.status ?? v?.variantStatus ?? 1);
    return status === 1 && getProductPrices(v, p).sale > 0;
  });

  const sellableVariants = activePricedVariants.filter((v: any) =>
    isVariantSellable(v)
  );

  const validVariants =
    sellableVariants.length > 0 ? sellableVariants : activePricedVariants;

  if (validVariants.length === 0) return null;

  return validVariants.reduce((min: any, v: any) => {
    const vSale = getProductPrices(v, p).sale;
    const minSale = getProductPrices(min, p).sale;
    return vSale < minSale ? v : min;
  }, validVariants[0]);
};

const cardSalePrice = computed(() => {
  const p = activeProduct.value as any;
  const cheapestV = getCheapestVariant(p);
  if (cheapestV) {
    const { sale } = getProductPrices(cheapestV, p);
    if (sale > 0) return sale;
  }
  const { sale } = getProductPrices(p);
  return sale;
});

const cardOriginalPrice = computed(() => {
  const p = activeProduct.value as any;
  const cheapestV = getCheapestVariant(p);
  const sale = cardSalePrice.value;
  if (cheapestV) {
    const { orig } = getProductPrices(cheapestV, p);
    if (orig > sale) return orig;
  }
  const { orig } = getProductPrices(p);
  return orig > sale ? orig : sale;
});

const cardDiscountPercent = computed(() => {
  const p = activeProduct.value as any;
  let percent = getSafeNumber(p.discountPercent ?? p.discount ?? p.salePercent);
  if (percent > 0) return Math.round(percent);
  const sale = cardSalePrice.value;
  const orig = cardOriginalPrice.value;
  if (orig > sale && sale > 0) return Math.round(((orig - sale) / orig) * 100);
  return 0;
});

const cardRepresentativeVariantId = computed(() => {
  const p = activeProduct.value as any;
  const cheapestV = getCheapestVariant(p);
  if (cheapestV)
    return Number(
      cheapestV.productVariantId || cheapestV.variantId || cheapestV.id || 0
    );
  return Number(p.productVariantId || p.variantId || p.id || 0);
});

const hasVariantPriceRange = computed(() => {
  const p = activeProduct.value as any;
  const variants = Array.isArray(p?.variants) ? p.variants : [];
  if (variants.length <= 1) return false;

  const activeVariants = variants.filter(
    (v: any) => Number(v?.status ?? v?.variantStatus ?? 1) === 1
  );
  const sellableVariants = activeVariants.filter((v: any) =>
    isVariantSellable(v)
  );
  const priceVariants =
    sellableVariants.length > 0 ? sellableVariants : activeVariants;

  const prices = priceVariants
    .map((v: any) => getProductPrices(v, p).sale)
    .filter((price: number) => price > 0);

  return new Set(prices).size > 1;
});

const quantity = ref(1);
const actionLoading = ref(false);
const actionType = ref<"CART" | "BUY">("CART");
const favoriteLoading = ref(false);
const isFavorited = ref(false);
const favoritedMap = ref<Record<number, boolean>>({});
const showVariantModal = ref(false);
const isLoadingVariants = ref(false);
const selectedVariant = ref<any>(null);
const fullVariants = ref<any[]>([]);
const currentTargetProduct = ref<any>(null);
const imageLoadError = ref(false);

const BACKEND_URL = "http://localhost:8080";
const brandMap: Record<string, string> = {
  Chanel: "CHANEL",
  Dior: "DIOR",
  "Yves Saint Laurent": "YSL",
  "Giorgio Armani": "ARMANI",
  Givenchy: "GIVENCHY",
  Creed: "CREED",
  Byredo: "BYREDO",
  "Tom Ford": "TOM FORD",
  "Maison Francis Kurkdjian": "MFK",
  "Le Labo": "LE LABO",
  "Paco Rabanne": "PACO",
};
const shortBrand = computed(
  () =>
    brandMap[activeProduct.value.brand] ||
    String(activeProduct.value.brand || "AURA")
      .slice(0, 8)
      .toUpperCase()
);

const MAX_RATING = 5;
const DEFAULT_RATING = 5;
const syncedRating = ref<number | null>(null);
const syncedReviews = ref<number | null>(null);

const normalizedReviewCount = computed(() => {
  if (syncedReviews.value !== null && syncedReviews.value > 0)
    return syncedReviews.value;
  const p = activeProduct.value as any;
  return Math.max(
    0,
    Math.floor(Number(p?.reviewCount || p?.reviews || p?.totalReviews || 0))
  );
});

const rawAverageRating = computed(() => {
  if (syncedRating.value !== null && syncedRating.value > 0)
    return syncedRating.value;
  const p = activeProduct.value as any;
  return Math.min(
    MAX_RATING,
    Math.max(0, Number(p?.averageRating || p?.avgRating || p?.rating || 0))
  );
});

const ratingValue = computed(() =>
  normalizedReviewCount.value > 0 || rawAverageRating.value > 0
    ? rawAverageRating.value
    : DEFAULT_RATING
);
const ratingDisplay = computed(() => ratingValue.value.toFixed(1));
const starsDisplay = computed(() => {
  const rounded = Math.round(ratingValue.value);
  const filled = Math.max(0, Math.min(MAX_RATING, rounded));
  return "★".repeat(filled) + "☆".repeat(MAX_RATING - filled);
});

const syncProductData = async () => {
  const p = props.product as any;
  const productId = Number(p?.productId || p?.id || 0);

  // BỎ chặn !fullProductData.value để thẻ luôn lấy giá mới nhất khi được focus
  if (productId > 0) {
    try {
      const res = await api.get(`/v1/products/${productId}?t=${Date.now()}`);
      const data = res.data?.data || res.data;

      // Bắt trường hợp sản phẩm vừa bị Ẩn hoặc Xóa bên Admin -> Tự F5 trang để đồng bộ
      if (
        !data ||
        data.isDeleted === true ||
        data.deleted === true ||
        Number(data.status) === 0
      ) {
        if (!window.sessionStorage.getItem("isReloading")) {
          window.sessionStorage.setItem("isReloading", "true");
          setTimeout(
            () => window.sessionStorage.removeItem("isReloading"),
            3000
          );
          window.location.reload();
        }
        return;
      }

      if (data) {
        fullProductData.value = data;
        syncedRating.value = Number(
          data.averageRating || data.avgRating || data.rating || 0
        );
        syncedReviews.value = Number(
          data.reviewCount || data.reviews || data.totalReviews || 0
        );
      }
    } catch (error) {
      // Bắt lỗi 404 Not Found (sản phẩm đã bị xóa cứng khỏi DB)
      if (!window.sessionStorage.getItem("isReloading")) {
        window.sessionStorage.setItem("isReloading", "true");
        setTimeout(() => window.sessionStorage.removeItem("isReloading"), 3000);
        window.location.reload();
      }
    }
  }
};

const formatCurrency = (value: number) =>
  new Intl.NumberFormat("vi-VN").format(Number(value || 0)) + " đ";

const getSellableQuantity = (variant: any) => {
  const value = Number(variant?.sellableQuantity ?? 0);
  if (!Number.isFinite(value) || value <= 0) return 0;
  return Math.trunc(value);
};

const isVariantSellable = (variant: any) => {
  if (!variant) return false;
  const status = Number(variant?.status ?? variant?.variantStatus ?? 1);
  return status === 1 && getSellableQuantity(variant) > 0;
};

const formatVariantName = (v: any) => {
  if (!v) return "Loại";
  const capCandidates = [
    v.capacityName,
    v.capacityValue,
    v.volume,
    v.capacity?.value,
    v.capacity?.name,
    typeof v.capacity === "string" || typeof v.capacity === "number"
      ? v.capacity
      : null,
  ];
  let cap = capCandidates.find((c) => c != null && c !== "");
  let capString = "";
  if (cap != null) {
    const text = String(cap).trim();
    const numeric = parseFloat(text.toLowerCase().replace("ml", ""));
    if (!Number.isNaN(numeric) && numeric > 0) capString = `${numeric}ml`;
    else capString = text.toLowerCase().includes("ml") ? text : `${text}ml`;
  }
  const bottleCandidates = [
    v.bottleTypeName,
    v.bottleType?.name,
    typeof v.bottleType === "string" ? v.bottleType : null,
  ];
  let bottleString = bottleCandidates.find((b) => b != null && b !== "");
  if (capString && bottleString) return `${capString} - ${bottleString}`;
  if (capString) return capString;
  if (bottleString) return bottleString;
  return "Loại " + (v.productVariantId || v.variantId || v.id || "");
};

const getSortedVariants = (p: any) => {
  if (!p || !p.variants || !Array.isArray(p.variants)) return [];
  return [...p.variants].sort((a: any, b: any) => {
    const getCap = (v: any) => {
      let cap =
        v.capacityName ||
        v.capacityValue ||
        v.volume ||
        v.capacity?.value ||
        v.capacity?.name ||
        v.capacity;
      const text = String(cap || "")
        .toLowerCase()
        .replace(/\s+/g, "");
      const numeric = parseFloat(text.replace("ml", ""));
      return Number.isNaN(numeric) ? 0 : numeric;
    };
    return getCap(a) - getCap(b);
  });
};

const normalizeImageUrl = (url: unknown) => {
  const rawUrl = String(url || "").trim();
  if (!rawUrl) return "";
  if (
    rawUrl.startsWith("http://") ||
    rawUrl.startsWith("https://") ||
    rawUrl.startsWith("data:image") ||
    rawUrl.startsWith("blob:")
  )
    return rawUrl;
  if (rawUrl.startsWith("/")) return `${BACKEND_URL}${rawUrl}`;
  return `${BACKEND_URL}/${rawUrl}`;
};

const getImageUrlFromObject = (value: any) => {
  if (!value) return "";
  if (typeof value === "string") return normalizeImageUrl(value);
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
  if (imageUrl && !images.includes(imageUrl)) images.push(imageUrl);
};

const appendImageList = (images: string[], value: any) => {
  if (!value) return;
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

const productImages = computed(() => {
  const images: string[] = [];
  const p = activeProduct.value as any;
  const addUnique = (url: unknown) => {
    const formatted = getImageUrlFromObject(url);
    if (formatted && !images.includes(formatted)) images.push(formatted);
  };
  const imageArrays = [
    p?.images,
    p?.productImages,
    p?.galleryImages,
    p?.imageList,
  ];
  for (const arr of imageArrays) {
    if (Array.isArray(arr)) {
      const primaryObj = arr.find((img: any) =>
        Boolean(img?.isPrimary || img?.is_primary || img?.primary)
      );
      if (primaryObj)
        addUnique(primaryObj?.imageUrl || primaryObj?.url || primaryObj);
    }
  }
  addUnique(p?.primaryImageUrl);
  addUnique(p?.mainImage);
  addUnique(p?.MainImage);
  addUnique(p?.imageUrl);
  addUnique(p?.ImageUrl);
  addUnique(p?.image);
  addUnique(p?.thumbnailUrl);
  imageArrays.forEach((arr) => {
    appendImageList(images, arr);
  });
  if (Array.isArray(p?.variants)) {
    p.variants.forEach((variant: any) => {
      getVariantImageList(variant).forEach((imageUrl: any) => {
        addUnique(imageUrl);
      });
    });
  }
  return images;
});

const productImage = computed(() => productImages.value[0] || "");
const hasProductImage = computed(
  () => Boolean(productImage.value) && !imageLoadError.value
);
const getPlaceholderImage = () => {
  return (
    "data:image/svg+xml;utf8," +
    encodeURIComponent(
      `<svg xmlns="http://www.w3.org/2000/svg" width="300" height="300"><rect width="100%" height="100%" fill="#f3f4f6"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="#9ca3af" font-family="Arial" font-size="20">No Image</text></svg>`
    )
  );
};

const getGlobalProductImage = (p: any) => {
  if (!p) return getPlaceholderImage();
  const images: string[] = [];
  const addUnique = (url: unknown) => {
    const formatted = getImageUrlFromObject(url);
    if (formatted && !images.includes(formatted)) images.push(formatted);
  };
  addUnique(p?.primaryImageUrl);
  addUnique(p?.mainImage);
  addUnique(p?.imageUrl);
  if (images.length > 0) return images[0];
  if (Array.isArray(p?.images) && p.images.length > 0)
    return getImageUrlFromObject(p.images[0]?.imageUrl || p.images[0]);
  return getPlaceholderImage();
};

const modalImage = computed(() => {
  if (selectedVariant.value) {
    const variantImage = getVariantImageList(selectedVariant.value)[0];
    if (variantImage) return variantImage;
  }
  if (currentTargetProduct.value)
    return getGlobalProductImage(currentTargetProduct.value);
  return productImage.value || getPlaceholderImage();
});

const handleProductImageError = () => {
  imageLoadError.value = true;
};
const handleModalImageError = (event: Event) => {
  const target = event.target as HTMLImageElement | null;
  if (!target) return;
  target.onerror = null;
  target.src = getPlaceholderImage();
};
const getBottleStyle = (color?: string): Record<string, string> => ({
  "--bottle-color": color || "#0a192f",
});

const showToast = (
  type: "success" | "warning" | "error",
  title: string,
  message: string
) => {
  Swal.fire({
    toast: true,
    position: "top-end",
    icon: type,
    title: title,
    text: message,
    showConfirmButton: false,
    timer: 2000,
    didOpen: () => {
      const container = Swal.getContainer();
      if (container) {
        container.style.setProperty("z-index", "9999999", "important");
      }
    },
  });
};

const checkLoginBeforeAction = () => {
  const token = localStorage.getItem("token");
  const rawRole =
    localStorage.getItem("role") || localStorage.getItem("userRole") || "";
  const role = rawRole.replace("ROLE_", "").toUpperCase().trim();

  if (!token) {
    Swal.fire({
      icon: "info",
      title: "Bạn chưa đăng nhập",
      text: "Vui lòng đăng nhập để tiếp tục trải nghiệm mua sắm tại Dominus.",
      showCancelButton: true,
      confirmButtonText: "Đăng nhập ngay",
      cancelButtonText: "Ở lại xem tiếp",
      confirmButtonColor: "#bd9a5f",
      cancelButtonColor: "#6b7280",
      didOpen: () => {
        const container = Swal.getContainer();
        if (container)
          container.style.setProperty("z-index", "9999999", "important");
      },
    }).then((result) => {
      if (result.isConfirmed)
        router.push({
          name: "Login",
          query: { redirect: router.currentRoute.value.fullPath },
        });
    });
    return false;
  }
  if (role !== "USER" && role !== "CUSTOMER") {
    Swal.fire({
      icon: "warning",
      title: "Từ chối thao tác",
      text: "Chức năng này chỉ dành cho tài khoản Khách hàng.",
      confirmButtonColor: "#bd9a5f",
      didOpen: () => {
        const container = Swal.getContainer();
        if (container)
          container.style.setProperty("z-index", "9999999", "important");
      },
    });
    return false;
  }
  return true;
};

const getProductIdNum = (item: any) => Number(item?.id || item?.productId || 0);

const getPrimaryVariantObject = (item: any) => {
  if (!item) return null;

  if (Array.isArray(item?.variants) && item.variants.length > 0) {
    const activeVariants = item.variants.filter(
      (variant: any) =>
        Number(variant?.status ?? variant?.variantStatus ?? 1) === 1
    );

    const sellableVariant = activeVariants.find(
      (variant: any) =>
        isVariantSellable(variant) && getProductPrices(variant, item).sale > 0
    );

    if (sellableVariant) return sellableVariant;

    return (
      activeVariants.find(
        (variant: any) => getProductPrices(variant, item).sale > 0
      ) ||
      activeVariants[0] ||
      item.variants[0]
    );
  }

  return item;
};

const getCompareVariant = (p: any) => {
  const pId = getProductIdNum(p);
  const vId = sharedCompareVariantIds.value[pId];
  if (vId && p.variants && Array.isArray(p.variants)) {
    const found = p.variants.find(
      (v: any) => Number(v.productVariantId || v.variantId || v.id) === vId
    );
    if (found) return found;
  }
  return getPrimaryVariantObject(p);
};

const getComparePrice = (p: any) => {
  const v = getCompareVariant(p);
  const { sale } = getProductPrices(v || p, p);
  if (sale > 0) return sale;
  return getSafeNumber(p?.price ?? p?.Price ?? p?.minPrice ?? p?.maxPrice);
};

const getCompareOriginalPrice = (p: any) => {
  const v = getCompareVariant(p);
  const sale = getComparePrice(p);
  const { orig } = getProductPrices(v || p, p);
  if (orig > sale) return orig;

  const pOrig = getSafeNumber(
    p?.originalPrice ??
      p?.oldPrice ??
      p?.listPrice ??
      p?.price ??
      p?.maxPrice ??
      p?.minPrice
  );
  return pOrig > sale ? pOrig : sale;
};

const getCompareStock = (p: any) => getSellableQuantity(getCompareVariant(p));

const getCompareDiscount = (p: any) => {
  const v = getCompareVariant(p);
  const percent = getSafeNumber(
    v?.discountPercent ?? v?.discount ?? p?.discountPercent ?? p?.discount
  );
  if (percent > 0) return Math.round(percent);
  const sale = getComparePrice(p);
  const orig = getCompareOriginalPrice(p);
  if (orig > sale && sale > 0) return Math.round(((orig - sale) / orig) * 100);
  return 0;
};

const isCompareBuyDisabled = (p: any) =>
  getCompareStock(p) <= 0 || getComparePrice(p) <= 0;

const confirmActionSpecific = async (type: "CART" | "BUY") => {
  actionType.value = type;

  if (!selectedVariant.value) return;
  const targetIdToUse = currentTargetProduct.value
    ? getProductIdNum(currentTargetProduct.value)
    : getProductIdNum(activeProduct.value);
  const variantId = Number(
    selectedVariant.value.productVariantId ||
      selectedVariant.value.variantId ||
      selectedVariant.value.id ||
      targetIdToUse
  );

  try {
    actionLoading.value = true;

    const cartRes = await api
      .get("/v1/customer/cart/my-cart")
      .catch(() => null);
    let currentQty = 0;

    if (cartRes && cartRes.data) {
      const payload = cartRes.data;
      const candidates = [
        payload,
        payload?.data,
        payload?.content,
        payload?.items,
        payload?.cartItems,
        payload?.data?.content,
        payload?.data?.items,
        payload?.data?.cartItems,
      ];
      let currentCartItems = [];
      for (const c of candidates) {
        if (Array.isArray(c)) {
          currentCartItems = c;
          break;
        }
      }
      const existingItem = currentCartItems.find(
        (i: any) =>
          Number(i?.productVariantId || i?.variantId || i?.id) === variantId
      );
      if (existingItem) currentQty = Number(existingItem.quantity || 0);
    }

    const stock = getSellableQuantity(selectedVariant.value);

    if (!isVariantSellable(selectedVariant.value) || stock <= 0) {
      showToast(
        "warning",
        "Tạm hết hàng",
        "Phân loại này hiện không còn tồn có thể bán."
      );
      return;
    }

    const maxAllow = Math.min(stock, 10);

    if (currentQty + quantity.value > maxAllow) {
      if (currentQty >= maxAllow) {
        showToast(
          "warning",
          "Giới hạn mua",
          `Giỏ hàng đã đạt giới hạn ${maxAllow} sản phẩm này!`
        );
      } else {
        showToast(
          "warning",
          "Giới hạn mua",
          `Giỏ hàng đang có sẵn ${currentQty} cái. Chỉ được thêm tối đa ${
            maxAllow - currentQty
          } cái nữa!`
        );
      }
      return;
    }

    if (actionType.value === "CART") {
      await api.post("/v1/customer/cart/add", {
        productVariantId: variantId,
        quantity: quantity.value,
      });
      window.dispatchEvent(new Event("cart-updated"));
      showVariantModal.value = false;
      showToast("success", "Thành công", "Đã thêm sản phẩm vào giỏ hàng.");
    } else {
      await api.post("/v1/customer/cart/add", {
        productVariantId: variantId,
        quantity: quantity.value,
      });
      window.dispatchEvent(new Event("cart-updated"));
      showVariantModal.value = false;
      router.push({ name: "Checkout" });
    }
  } catch (error: any) {
    showToast(
      "error",
      "Lỗi",
      error?.response?.data?.message || "Không thể thực hiện yêu cầu."
    );
  } finally {
    actionLoading.value = false;
  }
};

const buyFromCompare = (p: any) => {
  const id = p.id || p.productId;
  sharedShowCompareModal.value = false;
  if (id) {
    router.push({ name: "SingleProduct", params: { id: id } });
  }
};

const toCompareText = (value: any): string => {
  if (value === null || value === undefined || value === "") return "";

  if (Array.isArray(value)) {
    return value
      .map((item: any) => toCompareText(item))
      .filter(Boolean)
      .join(", ");
  }

  if (typeof value === "object") {
    return String(
      value?.name ?? value?.value ?? value?.label ?? value?.displayName ?? ""
    ).trim();
  }

  return String(value).trim();
};

const getGenderText = (item: any) => {
  const raw = item?.genderName ?? item?.gender;
  const g = toCompareText(raw);
  const normalized = g.toLowerCase();

  if (g === "1" || normalized === "nam" || normalized === "male") return "Nam";
  if (
    g === "2" ||
    normalized === "nữ" ||
    normalized === "nu" ||
    normalized === "female"
  )
    return "Nữ";
  if (g === "0" || normalized === "unisex") return "Unisex";

  return g || "Đang cập nhật";
};

const getAttributeText = (item: any, field: string) => {
  if (!item) return "Đang cập nhật";

  const candidates = [item?.[`${field}Name`], item?.[field]];

  for (const candidate of candidates) {
    const text = toCompareText(candidate);
    if (text) return text;
  }

  return "Đang cập nhật";
};

const getFragranceFamily = (item: any) => {
  if (!item) return "Đang cập nhật";

  const candidates = [
    item?.scents,
    item?.fragranceFamilies,
    item?.scentGroups,
    item?.scentGroup,
    item?.fragranceFamily,
    item?.fragranceFamilyName,
    item?.scent,
    item?.scentName,
    item?.mainScent,
    item?.mainScentName,
  ];

  for (const candidate of candidates) {
    const text = toCompareText(candidate);
    if (text) return text;
  }

  return "Đang cập nhật";
};

const getOccasionText = (item: any) => {
  const directCandidates = [
    item?.occasions,
    item?.occasionName,
    item?.occasion,
  ];

  for (const candidate of directCandidates) {
    const text = toCompareText(candidate);
    if (text) return text;
  }

  return (
    sharedCompareInsights.value[getProductIdNum(item)]?.occasion ||
    "Chưa có dữ liệu"
  );
};

const getStyleText = (item: any) => {
  const directCandidates = [item?.styles, item?.styleName, item?.style];

  for (const candidate of directCandidates) {
    const text = toCompareText(candidate);
    if (text) return text;
  }

  return (
    sharedCompareInsights.value[getProductIdNum(item)]?.style ||
    "Chưa có dữ liệu"
  );
};

const getBrandNameGlobal = (item: any) => {
  if (typeof item?.brand === "object") return item?.brand?.name || "Premium";
  return item?.brandName || item?.brand || "Premium";
};

const getRatingScore = (item: any) => {
  const raw = Number(
    item?.averageRating || item?.avgRating || item?.rating || 0
  );
  if (raw > 0) return Math.min(5, Math.max(0, raw)).toFixed(1);
  return (5.0).toFixed(1);
};

const getReviewCount = (item: any) =>
  Number(item?.reviewCount || item?.reviews || item?.totalReviews || 0);

const getPricePerMl = (p: any) => {
  const v = getCompareVariant(p);
  const price = getComparePrice(p);
  let cap =
    v?.capacityName ||
    v?.capacityValue ||
    v?.volume ||
    v?.capacity?.value ||
    v?.capacity?.name ||
    p?.capacity ||
    "";
  const numericCap = parseFloat(String(cap).replace(/[^0-9.]/g, ""));
  if (numericCap > 0 && price > 0) {
    return price / numericCap;
  }
  return 0;
};

const formatPricePerMl = (p: any) => {
  const val = getPricePerMl(p);
  if (val > 0) {
    return new Intl.NumberFormat("vi-VN").format(Math.round(val)) + "đ/ml";
  }
  return "-";
};

const isBestValue = (p: any) => {
  const validValues = sharedCompareList.value
    .map((item) => getPricePerMl(item))
    .filter((v) => v > 0);
  if (validValues.length === 0) return false;
  const minValue = Math.min(...validValues);
  return getPricePerMl(p) === minValue && minValue > 0;
};

const getLongevityDisplay = (p: any) => {
  const v = getCompareVariant(p);

  const actualLongevity = [
    p?.longevityName,
    p?.longevity,
    p?.lastingTime,
    p?.lastingDuration,
    v?.longevityName,
    v?.longevity,
    v?.lastingTime,
    v?.lastingDuration,
  ]
    .map((value) => toCompareText(value))
    .find(Boolean);

  if (actualLongevity) return actualLongevity;

  return (
    sharedCompareInsights.value[getProductIdNum(p)]?.longevity ||
    "Chưa có dữ liệu"
  );
};

const getCompareBottleType = (p: any) => {
  const v = getCompareVariant(p);
  const bottleCandidates = [
    v?.bottleTypeName,
    v?.bottleType,
    p?.bottleTypeName,
    p?.bottleType,
  ];

  for (const candidate of bottleCandidates) {
    const text = toCompareText(candidate);
    if (text) return text;
  }

  return "Đang cập nhật";
};

const getCompareValue = (p: any, type: string) => {
  const v = getCompareVariant(p);

  if (type === "brand") return getBrandNameGlobal(p);
  if (type === "scent") return getFragranceFamily(p);

  if (type === "concentration") {
    const productValue = getAttributeText(p, "concentration");
    return productValue !== "Đang cập nhật"
      ? productValue
      : getAttributeText(v, "concentration");
  }

  if (type === "gender") {
    const productValue = getGenderText(p);
    return productValue !== "Đang cập nhật" ? productValue : getGenderText(v);
  }

  if (type === "occasion") return getOccasionText(p);
  if (type === "style") return getStyleText(p);

  return "Đang cập nhật";
};

const openPickerModal = async () => {
  sharedShowPickerModal.value = true;
  if (allProductsStore.value.length === 0) {
    pickerLoading.value = true;
    try {
      const res = await api.get("/v1/products", {
        params: { size: 100, page: 0, t: Date.now() },
      });
      let list = [];
      if (res.data?.data?.content) list = res.data.data.content;
      else if (Array.isArray(res.data?.data)) list = res.data.data;
      else if (res.data?.content) list = res.data.content;
      else if (Array.isArray(res.data)) list = res.data;
      if (list.length > 0) allProductsStore.value = list;
    } catch (e) {
      console.error("Lỗi lấy danh sách sản phẩm:", e);
    } finally {
      pickerLoading.value = false;
    }
  }
};

const filteredPickerProducts = computed(() => {
  if (!pickerSearchKeyword.value.trim()) return allProductsStore.value;
  const kw = pickerSearchKeyword.value.toLowerCase();
  return allProductsStore.value.filter((item: any) => {
    const name = String(item.name || "").toLowerCase();
    const brandObj = item.brand;
    const brandName = typeof brandObj === "object" ? brandObj?.name : brandObj;
    const brandStr = String(brandName || item.brandName || "").toLowerCase();
    return name.includes(kw) || brandStr.includes(kw);
  });
});

const isInCompare = (item: any) => {
  const targetId = getProductIdNum(item);
  if (!targetId) return false;
  return sharedCompareList.value.some(
    (p: any) => getProductIdNum(p) === targetId
  );
};

const resetCompareAiResult = () => {
  sharedCompareInsights.value = {};
  sharedCompareAnalysis.value = "";
  sharedCompareRecommendation.value = "";
  sharedCompareAiError.value = "";
};

const clearCompareList = () => {
  sharedCompareList.value = [];
  sharedCompareVariantIds.value = {};
  resetCompareAiResult();
};

const toggleCompare = (item: any) => {
  const targetId = getProductIdNum(item);
  if (!targetId) return;

  if (isInCompare(item)) {
    removeFromCompare(item);
  } else {
    if (sharedCompareList.value.length >= 3) {
      showToast("warning", "Giới hạn", "Chỉ được so sánh tối đa 3 sản phẩm!");
      return;
    }

    sharedCompareList.value.push(item);
    resetCompareAiResult();
  }
};

const removeFromCompare = (item: any) => {
  const targetId = getProductIdNum(item);
  if (!targetId) return;

  sharedCompareList.value = sharedCompareList.value.filter(
    (p: any) => getProductIdNum(p) !== targetId
  );

  if (sharedCompareVariantIds.value) {
    delete sharedCompareVariantIds.value[targetId];
  }

  resetCompareAiResult();
};

const toggleItemInPicker = (item: any) => {
  const targetId = getProductIdNum(item);
  if (!targetId) return;

  if (isInCompare(item)) {
    removeFromCompare(item);
  } else {
    if (sharedCompareList.value.length >= 3) {
      showToast("warning", "Giới hạn", "Chỉ được so sánh tối đa 3 sản phẩm!");
      return;
    }

    sharedCompareList.value.push(item);
    resetCompareAiResult();
  }
};

const getCompareProductIds = () => {
  return Array.from(
    new Set(
      sharedCompareList.value
        .map((item: any) => getProductIdNum(item))
        .filter((id: number) => Number.isFinite(id) && id > 0)
    )
  );
};

const loadStructuredCompareInsights = async (): Promise<boolean> => {
  const productIds = getCompareProductIds();

  if (productIds.length < 2 || productIds.length > 3) {
    resetCompareAiResult();
    return false;
  }

  try {
    const res = await api.post("/v1/products/compare/ai", {
      productIds,
    });

    const data = res?.data?.data ?? res?.data;
    const rows = Array.isArray(data?.insights) ? data.insights : [];

    const map: Record<number, CompareInsight> = {};

    rows.forEach((row: any) => {
      const productId = Number(row?.productId);

      if (!Number.isFinite(productId) || productId <= 0) {
        return;
      }

      map[productId] = {
        productId,
        longevity: String(row?.longevity || "").trim() || "Chưa có dữ liệu",
        style: String(row?.style || "").trim() || "Chưa có dữ liệu",
        occasion: String(row?.occasion || "").trim() || "Chưa có dữ liệu",
      };
    });

    sharedCompareInsights.value = map;
    sharedCompareAnalysis.value = String(data?.analysis || "").trim();
    sharedCompareRecommendation.value = String(
      data?.recommendation || ""
    ).trim();
    sharedCompareAiError.value = "";

    return true;
  } catch (error: any) {
    const message =
      error?.response?.data?.message ||
      error?.message ||
      "Không thể phân tích sản phẩm bằng AI lúc này.";

    console.warn("Không lấy được kết quả so sánh AI:", message);

    sharedCompareInsights.value = {};
    sharedCompareAnalysis.value = "";
    sharedCompareRecommendation.value = "";
    sharedCompareAiError.value = String(message);

    return false;
  }
};

const handleAiCompare = async () => {
  if (sharedCompareAiLoading.value) return;

  if (!checkLoginBeforeAction()) return;

  const productIds = getCompareProductIds();

  if (productIds.length < 2 || productIds.length > 3) {
    showToast(
      "warning",
      "Chưa đủ sản phẩm",
      "Vui lòng chọn từ 2 đến 3 sản phẩm để so sánh bằng AI."
    );
    return;
  }

  sharedCompareAiLoading.value = true;
  sharedCompareAiError.value = "";

  try {
    await loadStructuredCompareInsights();
  } finally {
    sharedCompareAiLoading.value = false;
  }
};

const goToDetailFromCompare = (p: any) => {
  sharedShowCompareModal.value = false;
  const productId = getProductIdNum(p);
  if (productId > 0)
    router.push({ name: "SingleProduct", params: { id: productId } });
  else router.push("/product");
};

watch(sharedShowCompareModal, async (val) => {
  if (val) {
    sharedCompareList.value.forEach((p) => {
      const id = getProductIdNum(p);
      if (!sharedCompareVariantIds.value[id]) {
        const primaryV = getPrimaryVariantObject(p);
        sharedCompareVariantIds.value[id] = Number(
          primaryV?.productVariantId ?? primaryV?.variantId ?? primaryV?.id ?? 0
        );
      }
    });
    const updatedList = [...sharedCompareList.value];
    for (let i = 0; i < updatedList.length; i++) {
      const p = updatedList[i];
      const id = getProductIdNum(p);

      const variantSaleMap = new Map<number, number>();
      const variantOrigMap = new Map<number, number>();
      const discountMap = new Map<number, number>();

      if (p.variants && Array.isArray(p.variants)) {
        p.variants.forEach((pv: any) => {
          const vId = Number(pv.productVariantId || pv.variantId || pv.id);
          if (vId) {
            const sale = Number(
              pv.salePrice ??
                pv.promotionPrice ??
                pv.flashSalePrice ??
                pv.price ??
                0
            );
            const orig = Number(
              pv.originalPrice ?? pv.oldPrice ?? pv.price ?? sale
            );
            const disc = Number(pv.discountPercent ?? pv.discount ?? 0);
            if (sale > 0) variantSaleMap.set(vId, sale);
            if (orig > 0) variantOrigMap.set(vId, orig);
            if (disc > 0) discountMap.set(vId, disc);
          }
        });
      }

      try {
        const res = await api.get(`/v1/products/${id}?t=${Date.now()}`);
        const fullData = res.data?.data || res.data;
        if (fullData) {
          let rawVariants =
            fullData.variants ||
            fullData.productVariants ||
            fullData.productVariantList;
          if (rawVariants && Array.isArray(rawVariants)) {
            fullData.variants = rawVariants.map((v: any) => {
              const vId = Number(v.productVariantId || v.variantId || v.id);

              const apiSale = Number(
                v.salePrice ??
                  v.promotionPrice ??
                  v.flashSalePrice ??
                  v.price ??
                  0
              );
              const apiOrig = Number(
                v.originalPrice ?? v.oldPrice ?? v.price ?? apiSale
              );

              let finalSale = variantSaleMap.get(vId) ?? apiSale;
              let finalOrig = variantOrigMap.get(vId) ?? apiOrig;
              let finalDisc =
                discountMap.get(vId) ??
                Number(v.discountPercent ?? fullData?.discountPercent ?? 0);

              if (finalDisc > 0 && finalOrig <= finalSale && finalSale > 0) {
                finalOrig = finalSale;
                finalSale = Math.round(finalOrig * (1 - finalDisc / 100));
              }

              return {
                ...v,
                salePrice: finalSale,
                originalPrice: finalOrig,
                discountPercent: finalDisc,
              };
            });
          }
          updatedList[i] = {
            ...p,
            ...fullData,
            flashSalePrice: p.flashSalePrice ?? fullData.flashSalePrice,
            salePrice: p.salePrice ?? fullData.salePrice,
            discountPercent: p.discountPercent ?? fullData.discountPercent,
            originalPrice: p.originalPrice ?? fullData.originalPrice,
            oldPrice: p.oldPrice ?? fullData.oldPrice,
            variants: fullData.variants || p.variants,
          };
        }
      } catch (e) {}
    }
    sharedCompareList.value = updatedList;
    resetCompareAiResult();
  }
});

const openVariantModal = async (
  type: "CART" | "BUY",
  customProduct: any = null,
  preselectedVariantId?: number
) => {
  if (!checkLoginBeforeAction()) return;

  actionType.value = type;
  currentTargetProduct.value = customProduct || activeProduct.value;
  selectedVariant.value = null;
  fullVariants.value = [];
  quantity.value = 1;
  showVariantModal.value = true;
  isLoadingVariants.value = true;

  try {
    const tp = currentTargetProduct.value;
    const tId = getProductIdNum(tp);

    const salePriceMap = new Map<number, number>();
    const origPriceMap = new Map<number, number>();
    const discountMap = new Map<number, number>();

    if (tp?.variants && Array.isArray(tp.variants)) {
      tp.variants.forEach((v: any) => {
        const vId = Number(v.productVariantId || v.variantId || v.id);
        if (vId) {
          const sale = Number(
            v.salePrice ?? v.promotionPrice ?? v.flashSalePrice ?? v.price ?? 0
          );
          const orig = Number(v.originalPrice ?? v.oldPrice ?? v.price ?? sale);
          const disc = Number(v.discountPercent ?? v.discount ?? 0);
          if (sale > 0) salePriceMap.set(vId, sale);
          if (orig > 0) origPriceMap.set(vId, orig);
          if (disc > 0) discountMap.set(vId, disc);
        }
      });
    }

    const res = await api.get(`/v1/products/${tId}?t=${Date.now()}`);
    const data = res.data?.data || res.data;

    // SỬA DÒNG IF THÀNH THẾ NÀY:
    if (
      !data ||
      data.isDeleted === true ||
      data.deleted === true ||
      Number(data.status) === 0
    ) {
      showToast("error", "Lỗi", "Sản phẩm đã bị ẩn hoặc không còn tồn tại!");
      showVariantModal.value = false;
      isLoadingVariants.value = false;
      return;
    }

    let rawVariants =
      data?.variants || data?.productVariants || data?.productVariantList || [];

    if (!rawVariants || rawVariants.length === 0) {
      showToast("error", "Lỗi", "Sản phẩm không có phân loại hợp lệ!");
      showVariantModal.value = false;
      isLoadingVariants.value = false;
      return;
    }

    const processedVariants = rawVariants.map((v: any) => {
      const vId = Number(v.productVariantId || v.variantId || v.id);
      const displayCap = formatVariantName(v);

      let capObj = v.capacityName || v.capacityValue || v.volume || v.capacity;
      if (typeof capObj === "object") capObj = capObj?.value ?? capObj?.name;
      const numericCap =
        parseFloat(String(capObj || "").replace("ml", "")) || 0;

      const apiSale = Number(
        v.salePrice ?? v.promotionPrice ?? v.flashSalePrice ?? v.price ?? 0
      );
      const apiOrig = Number(
        v.originalPrice ?? v.oldPrice ?? v.price ?? apiSale
      );

      let finalSale = apiSale;
      let finalOrig = apiOrig;
      let finalDisc = Number(
        v.discountPercent ?? tp?.discountPercent ?? tp?.discount ?? 0
      );

      if (finalDisc > 0 && finalOrig <= finalSale && finalSale > 0) {
        finalOrig = finalSale;
        finalSale = Math.round(finalOrig * (1 - finalDisc / 100));
      }

      return {
        ...v,
        productVariantId: vId,
        id: vId,
        salePrice: finalSale,
        originalPrice: finalOrig,
        price: finalSale > 0 ? finalSale : finalOrig,
        discountPercent: finalDisc,
        displayCapacity: displayCap,
        numericCapacity: numericCap,
      };
    });

    processedVariants.sort(
      (a: any, b: any) => a.numericCapacity - b.numericCapacity
    );
    fullVariants.value = processedVariants;

    if (fullVariants.value.length > 0) {
      const targetVId =
        preselectedVariantId || cardRepresentativeVariantId.value;
      const targetVariant = fullVariants.value.find(
        (v: any) => v.productVariantId === targetVId
      );

      selectedVariant.value =
        (targetVariant && isVariantSellable(targetVariant)
          ? targetVariant
          : null) ||
        fullVariants.value.find((v: any) => isVariantSellable(v)) ||
        targetVariant ||
        fullVariants.value[0];
    }
  } catch (error) {
    console.error("Lỗi lấy chi tiết biến thể:", error);
    showToast("error", "Lỗi", "Sản phẩm đã bị xóa hoặc không còn tồn tại!");
    showVariantModal.value = false;
  } finally {
    isLoadingVariants.value = false;
  }
};

const getPrimaryVariantId = () => {
  const sortedVariants = getSortedVariants(activeProduct.value);
  if (sortedVariants.length > 0) {
    const v = sortedVariants[0];
    return Number(v?.productVariantId || v?.variantId || v?.id || 0);
  }
  return Number(
    activeProduct.value.productVariantId ||
      activeProduct.value.variantId ||
      activeProduct.value.id ||
      0
  );
};

const loadFavoriteStatus = async () => {
  const token = localStorage.getItem("token");
  const rawRole =
    localStorage.getItem("role") || localStorage.getItem("userRole") || "";
  const role = rawRole.replace("ROLE_", "").toUpperCase().trim();
  if (!token || (role !== "USER" && role !== "CUSTOMER")) {
    favoritedMap.value = {};
    isFavorited.value = false;
    return;
  }

  try {
    if (!sharedFavoriteLoadPromise) {
      sharedFavoriteLoadPromise = favoriteService.getFavorites().finally(() => {
        sharedFavoriteLoadPromise = null;
      });
    }

    const res = await sharedFavoriteLoadPromise;
    const list = Array.isArray(res.data) ? res.data : [];
    const nextMap: Record<number, boolean> = {};
    list.forEach((item: any) => {
      const variantId = Number(item?.productVariantId || 0);
      if (variantId > 0) nextMap[variantId] = true;
    });
    favoritedMap.value = nextMap;
    const primaryId = getPrimaryVariantId();
    let matched = primaryId ? Boolean(nextMap[primaryId]) : false;
    if (
      !matched &&
      activeProduct.value?.variants &&
      Array.isArray(activeProduct.value.variants)
    ) {
      matched = activeProduct.value.variants.some((v: any) => {
        const vId = Number(v?.productVariantId || v?.variantId || v?.id || 0);
        return vId && nextMap[vId];
      });
    }
    isFavorited.value = matched;
  } catch (error) {
    favoritedMap.value = {};
    isFavorited.value = false;
  }
};

const handleToggleFavorite = async () => {
  const variantId = getPrimaryVariantId();
  if (!variantId || Number.isNaN(variantId)) return;
  if (!checkLoginBeforeAction()) return;

  try {
    favoriteLoading.value = true;
    const res = await favoriteService.toggleFavorite(variantId);
    const favorited = Boolean(res.data?.favorited);
    favoritedMap.value = { ...favoritedMap.value, [variantId]: favorited };
    isFavorited.value = favorited;
    window.dispatchEvent(
      new CustomEvent("favorite-updated", {
        detail: { productVariantId: variantId, favorited },
      })
    );
    showToast(
      favorited ? "success" : "warning",
      favorited ? "Đã thêm yêu thích" : "Đã bỏ yêu thích",
      res.data?.message || ""
    );
  } catch (error: any) {
    showToast("error", "Lỗi", "Không thể xử lý yêu thích");
  } finally {
    favoriteLoading.value = false;
  }
};

const handleFavoriteUpdated = (event: Event) => {
  const customEvent = event as CustomEvent<{
    productVariantId?: number;
    favorited?: boolean;
  }>;
  const variantId = Number(customEvent.detail?.productVariantId || 0);
  const favorited = Boolean(customEvent.detail?.favorited);
  if (!variantId) return;
  favoritedMap.value = { ...favoritedMap.value, [variantId]: favorited };
  const primaryId = getPrimaryVariantId();
  let matched = primaryId === variantId ? favorited : isFavorited.value;
  if (
    !matched &&
    activeProduct.value?.variants &&
    Array.isArray(activeProduct.value.variants)
  ) {
    matched = activeProduct.value.variants.some((v: any) => {
      const vId = Number(v?.productVariantId || v?.variantId || v?.id || 0);
      return vId && favoritedMap.value[vId];
    });
  }
  isFavorited.value = matched;
};

const goToDetail = () => {
  const productId = getProductIdNum(activeProduct.value);
  if (productId > 0)
    router.push({ name: "SingleProduct", params: { id: productId } });
  else router.push("/product");
};

const maxQuantity = computed(() =>
  selectedVariant.value ? getSellableQuantity(selectedVariant.value) : 0
);

const calculatedDiscountPercent = computed(() => {
  if (selectedVariant.value) {
    if (selectedVariant.value.discountPercent)
      return selectedVariant.value.discountPercent;
    const original = getSafeNumber(
      selectedVariant.value.originalPrice ||
        selectedVariant.value.oldPrice ||
        cardOriginalPrice.value
    );
    const sale = getSafeNumber(
      selectedVariant.value.salePrice ||
        selectedVariant.value.price ||
        cardSalePrice.value
    );
    if (original && sale && original > sale)
      return Math.round(((original - sale) / original) * 100);
    return 0;
  }
  return cardDiscountPercent.value;
});

const validateQuantity = () => {
  let val = Number(quantity.value);
  if (Number.isNaN(val) || val < 1) quantity.value = 1;
  else if (val > 10) {
    quantity.value = 10;
    showToast(
      "warning",
      "Giới hạn mua",
      "Bạn chỉ được mua tối đa 10 sản phẩm cho mỗi phân loại."
    );
  } else if (val > maxQuantity.value) {
    quantity.value = maxQuantity.value;
    showToast(
      "warning",
      "Giới hạn tồn kho",
      `Sản phẩm chỉ còn ${maxQuantity.value} trong kho.`
    );
  } else quantity.value = Math.floor(val);
};

const increaseQuantity = () => {
  if (quantity.value >= 10) {
    showToast(
      "warning",
      "Giới hạn mua",
      "Bạn chỉ được mua tối đa 10 sản phẩm cho mỗi phân loại."
    );
    return;
  }
  if (quantity.value >= maxQuantity.value) {
    showToast(
      "warning",
      "Giới hạn tồn kho",
      `Sản phẩm chỉ còn ${maxQuantity.value} trong kho.`
    );
    return;
  }
  quantity.value++;
};

const handleFocus = () => {
  syncProductData();
};

onMounted(() => {
  if (!currentCompareRendererId) {
    currentCompareRendererId = instanceId;
    isCompareUIRenderer.value = true;
  }
  window.addEventListener("favorite-updated", handleFavoriteUpdated);
  window.addEventListener("focus", handleFocus); // BỔ SUNG LẮNG NGHE CHUYỂN TAB
  loadFavoriteStatus();

  if (props.preloadDetail) {
    syncProductData();
  }
});

onBeforeUnmount(() => {
  if (currentCompareRendererId === instanceId) {
    currentCompareRendererId = null;
  }
  window.removeEventListener("favorite-updated", handleFavoriteUpdated);
  window.removeEventListener("focus", handleFocus); // DỌN DẸP SỰ KIỆN CHUYỂN TAB
});

// ĐÃ TRẢ LẠI NGUYÊN BẢN CHO BẠN: Khôi phục theo dõi bằng ID để tránh bị lỗi tự nhận diện load ở Component Cha
watch(
  () =>
    Number(
      (props.product as any)?.productId || (props.product as any)?.id || 0
    ),
  (newProductId, oldProductId) => {
    if (newProductId === oldProductId) return;

    fullProductData.value = null;
    loadFavoriteStatus();

    if (props.preloadDetail) {
      syncProductData();
    }
  }
);
</script>

<style scoped>
.product-card {
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid rgba(26, 26, 26, 0.055);
  box-shadow: 0 8px 28px rgba(5, 16, 36, 0.045);
  transition: all 0.28s ease;
  cursor: pointer;
}
.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 22px 48px rgba(5, 16, 36, 0.105);
}

.discount-badge {
  position: absolute;
  top: 15px;
  left: 15px;
  z-index: 3;
  background: #e53e3e;
  color: #ffffff;
  border-radius: 4px;
  padding: 4px 8px;
  font-size: 11px;
  font-weight: bold;
}
.expiry-warning-badge {
  position: absolute;
  top: 15px;
  left: 15px;
  z-index: 3;
  background: #d97706;
  color: white;
  font-size: 11px;
  font-weight: 800;
  padding: 4px 8px;
  border-radius: 999px;
  box-shadow: 0 4px 10px rgba(217, 119, 6, 0.3);
  display: inline-flex;
  align-items: center;
}
.discount-badge ~ .expiry-warning-badge {
  top: 48px;
}

.btn-favorite {
  position: absolute;
  top: 14px;
  right: 14px;
  z-index: 5;
  width: 38px;
  height: 38px;
  border-radius: 999px;
  border: 1px solid rgba(189, 154, 95, 0.35);
  background: rgba(255, 255, 255, 0.94);
  color: #8c8c8c;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.22s ease;
  box-shadow: 0 8px 18px rgba(5, 16, 36, 0.08);
}
.btn-favorite:hover:not(:disabled) {
  color: #dc2626;
  border-color: #dc2626;
  transform: scale(1.05);
}
.btn-favorite.active {
  color: #dc2626;
  border-color: #dc2626;
  background: #fff5f5;
}
.btn-favorite:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}
.btn-compare-card {
  position: absolute;
  top: 60px;
  right: 14px;
  z-index: 5;
  width: 38px;
  height: 38px;
  border-radius: 999px;
  border: 1px solid rgba(189, 154, 95, 0.35);
  background: rgba(255, 255, 255, 0.94);
  color: #8c8c8c;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.22s ease;
  box-shadow: 0 8px 18px rgba(5, 16, 36, 0.08);
}
.btn-compare-card:hover {
  color: #bd9a5f;
  border-color: #bd9a5f;
  transform: scale(1.05);
}
.btn-compare-card.active {
  color: #bd9a5f;
  border-color: #bd9a5f;
  background: #fdfaf6;
}
.btn-compare-card svg {
  width: 17px;
  height: 17px;
}

.product-image-wrapper {
  width: 100%;
  aspect-ratio: 1 / 1;
  background-color: #f9fafb;
  border-radius: 16px 16px 0 0;
  overflow: hidden;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}
.product-real-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
  transition: transform 0.4s cubic-bezier(0.165, 0.84, 0.44, 1);
}
.product-card:hover .product-real-image {
  transform: scale(1.08);
}
.product-bottle {
  width: 120px;
  height: 190px;
  display: flex;
  flex-direction: column;
  align-items: center;
  align-self: center;
  filter: drop-shadow(0 18px 24px rgba(5, 16, 36, 0.18));
  transition: transform 0.28s ease;
}
.product-card:hover .product-bottle {
  transform: scale(1.05);
}
.product-bottle-cap {
  width: 58px;
  height: 34px;
  border-radius: 15px 15px 6px 6px;
  background: linear-gradient(135deg, #f1d08a, #9b6f2e);
}
.product-bottle-neck {
  width: 32px;
  height: 22px;
  background: linear-gradient(135deg, #d2ad68, #8b642c);
}
.product-bottle-body {
  width: 120px;
  height: 134px;
  border-radius: 16px 16px 24px 24px;
  background: linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.32),
      transparent 32%
    ),
    linear-gradient(145deg, var(--bottle-color), #080808 86%);
  border: 2px solid rgba(255, 255, 255, 0.24);
  display: flex;
  align-items: center;
  justify-content: center;
}
.product-bottle-label {
  width: 82px;
  height: 70px;
  border: 1px solid var(--aura-gold);
  background: rgba(5, 16, 36, 0.88);
  color: var(--aura-gold);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.product-bottle-label strong {
  font-family: var(--aura-serif);
  font-size: 14px;
  letter-spacing: 1.5px;
  line-height: 1;
}
.product-bottle-label span {
  margin-top: 5px;
  font-size: 7px;
  letter-spacing: 2px;
  font-family: var(--aura-sans);
}

.product-content {
  padding: 18px 20px 20px;
}
.product-brand {
  color: #8c8c8c;
  font-family: var(--aura-sans);
  font-size: 11px;
  letter-spacing: 1px;
  text-transform: uppercase;
  font-weight: 700;
}
.product-name {
   font-family: Arial, "Segoe UI", sans-serif;
  font-size: 17px;
  font-weight: 700;
  color: var(--aura-black);
  line-height: 1.28;
  margin: 0 0 10px;

  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.stars {
  color: var(--aura-gold);
  font-size: 13px;
  letter-spacing: 1px;
}
.review-count {
  color: #777777;
  font-size: 12px;
  font-weight: 500;
}
.price-prefix {
  color: #718096;
  font-size: 12px;
  font-weight: 700;
  align-self: center;
}
.sale-price {
  color: #111111;
  font-size: 18px;
  font-weight: 800;
  font-family: var(--aura-sans);
}
.original-price {
  color: #a8a8a8;
  font-size: 13px;
  font-weight: 500;
}

.product-actions {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 10px;
  width: 100%;
}
.buy-now-btn,
.add-cart-btn {
  font-size: 13px;
  font-weight: 800;
  border-radius: 7px;
  padding: 10px;
  transition: all 0.22s ease;
  min-height: 42px;
  display: flex;
  justify-content: center;
  align-items: center;
}
.buy-now-btn {
  border: none;
  background: var(--aura-gold);
  color: #ffffff;
}
.buy-now-btn:hover:not(:disabled) {
  background: #a3824d;
  color: #ffffff;
}
.add-cart-btn {
  border: 1px solid var(--aura-gold);
  color: var(--aura-gold);
  background: #ffffff;
}
.add-cart-btn:hover:not(:disabled) {
  background: var(--aura-gold);
  color: #ffffff;
}
.buy-now-btn:disabled,
.add-cart-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

/* COMPARE BAR */
.compare-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  background: #ffffff;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.1);
  transform: translateY(100%);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1040;
  padding: 15px 0;
  border-top: 2px solid #bd9a5f;
}
.compare-bar.show {
  transform: translateY(0);
}
.cb-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}
.cb-left {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}
.cb-title {
  font-weight: 800;
  font-size: 15px;
  color: #0a142f;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.cb-slots {
  display: flex;
  gap: 15px;
}
.cb-slot {
  width: 230px;
  height: 60px;
  border: 1px dashed #cbd5e0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  padding: 5px;
  gap: 10px;
  background: #f8fafc;
  position: relative;
}
.cb-slot.filled {
  border-style: solid;
  border-color: #eaeaea;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.cb-slot img {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 6px;
}
.cb-slot-info {
  flex: 1;
  overflow: hidden;
}
.cb-slot-info p {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: #0a142f;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.cb-slot-info span {
  font-size: 12px;
  color: #bd9a5f;
  font-weight: 600;
}
.btn-remove-cb {
  position: absolute;
  top: -8px;
  right: -8px;
  background: #e53e3e;
  color: white;
  border: none;
  border-radius: 50%;
  width: 22px;
  height: 22px;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: 0.2s;
  box-shadow: 0 2px 5px rgba(229, 62, 62, 0.3);
}
.btn-remove-cb:hover {
  transform: scale(1.1);
}
.cb-slot.empty-slot {
  justify-content: center;
  color: #a0aec0;
  font-size: 13px;
  cursor: pointer;
  transition: 0.2s;
}
.cb-slot.empty-slot:hover {
  color: #bd9a5f;
  border-color: #bd9a5f;
  background: #fdfaf6;
}
.cb-right {
  display: flex;
  gap: 15px;
  align-items: center;
}
.cb-btn-clear {
  background: transparent;
  border: none;
  color: #718096;
  font-weight: 600;
  cursor: pointer;
  font-size: 14px;
  transition: 0.2s;
}
.cb-btn-clear:hover {
  color: #e53e3e;
  text-decoration: underline;
}
.cb-btn-compare {
  background: #0a142f;
  color: white;
  border: none;
  padding: 12px 28px;
  border-radius: 8px;
  font-weight: 700;
  text-transform: uppercase;
  cursor: pointer;
  transition: 0.2s;
  letter-spacing: 0.5px;
}
.cb-btn-compare:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.cb-btn-compare:hover:not(:disabled) {
  background: #bd9a5f;
  box-shadow: 0 4px 12px rgba(189, 154, 95, 0.3);
}

/* COMPARE MODAL */
.compare-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  z-index: 1050;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(6px);
}
.compare-modal-box {
  background: white;
  width: 95%;
  max-width: 1100px;
  max-height: 90vh;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: modalFadeIn 0.3s ease;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}
.cm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #eaeaea;
  background: #fdfaf6;
}
.cm-header h3 {
  margin: 0;
  font-family: "Playfair Display", serif;
  font-size: 22px;
  font-weight: 800;
  color: #0a142f;
}
.cm-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #a0aec0;
  transition: 0.2s;
  padding: 0;
  line-height: 1;
}
.cm-close:hover {
  color: #e53e3e;
  transform: rotate(90deg);
}
.cm-body {
  padding: 0;
  overflow-y: auto;
}

.ai-compare-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 24px;
  background: #fffdf8;
  border-bottom: 1px solid #eee3d3;
}
.ai-compare-toolbar-text {
  min-width: 0;
}
.ai-compare-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  color: #0a142f;
  font-size: 16px;
  font-weight: 800;
}
.ai-compare-title i {
  color: #b78d52;
}
.ai-compare-subtitle {
  color: #718096;
  font-size: 13px;
  line-height: 1.5;
}

.cm-btn-ai {
  flex-shrink: 0;
  min-width: 180px;
  padding: 11px 16px;
  border: none;
  border-radius: 10px;
  background: #b78d52;
  color: #ffffff;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition: 0.2s;
}
.cm-btn-ai:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 14px rgba(183, 141, 82, 0.24);
}
.cm-btn-ai:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ai-compare-error {
  margin: 16px 24px 0;
  padding: 12px 14px;
  border: 1px solid #fecaca;
  border-radius: 10px;
  background: #fff5f5;
  color: #b91c1c;
  font-size: 13px;
  font-weight: 600;
}
.ai-compare-result {
  margin: 16px 24px;
  border: 1px solid #eadfcf;
  border-radius: 12px;
  background: #fffdf8;
  overflow: hidden;
}
.ai-result-block {
  padding: 15px 16px;
}
.ai-result-block + .ai-result-block {
  border-top: 1px solid #eadfcf;
}
.ai-result-label {
  margin-bottom: 6px;
  color: #0a142f;
  font-size: 14px;
  font-weight: 800;
}
.ai-result-label i {
  color: #b78d52;
}
.ai-result-block p {
  margin: 0;
  color: #4a5568;
  font-size: 14px;
  line-height: 1.65;
}
.ai-result-recommendation {
  background: #fffcf7;
}

@media (max-width: 768px) {
  .ai-compare-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
  .cm-btn-ai {
    width: 100%;
  }
}

.table-compare {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}
.sticky-header th,
.sticky-header td {
  position: sticky;
  top: 0;
  background: white;
  z-index: 10;
  border-bottom: 2px solid #eaeaea;
  padding-top: 25px;
  padding-bottom: 20px;
}
.table-compare th,
.table-compare td {
  padding: 18px 24px;
  border-bottom: 1px solid #eaeaea;
  vertical-align: middle;
}

.spec-label-col {
  width: 18%;
  background: #f8fafc;
  font-weight: 700;
  color: #4a5568;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-right: 1px solid #eaeaea;
}
.spec-value-col {
  width: 27.33%;
  font-size: 15px;
  color: #0a142f;
  text-align: center;
  position: relative;
}
.spec-value-col:not(:last-child) {
  border-right: 1px solid #eaeaea;
}

.cm-img-wrapper {
  text-align: center;
  margin-bottom: 15px;
  cursor: pointer;
  display: flex;
  justify-content: center;
}
.cm-img-wrapper img {
  width: 140px;
  height: 140px;
  object-fit: cover;
  border-radius: 12px;
  border: 1px solid #eaeaea;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s;
}
.cm-img-wrapper:hover img {
  transform: scale(1.05);
}

.cm-name {
  font-family: "Playfair Display", serif;
  font-size: 18px;
  font-weight: 800;
  color: #0a142f;
  margin: 0 0 5px;
  text-align: center;
  line-height: 1.3;

  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;

  cursor: pointer;
  transition: color 0.2s;
}
.cm-name:hover {
  color: #bd9a5f;
}

.btn-remove-from-table {
  position: absolute;
  top: 10px;
  right: 10px;
  background: #e53e3e;
  color: white;
  border: none;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: 0.2s;
  z-index: 15;
}
.spec-value-col:hover .btn-remove-from-table {
  opacity: 1;
}

.cm-header-price {
  text-align: center;
  font-size: 16px;
  font-weight: 800;
  color: #e53e3e;
}
.group-header {
  background: #f8fafc;
  font-weight: 800;
  color: #0a142f;
  font-size: 16px;
  padding: 12px 24px;
  border-top: 2px solid #eaeaea;
  border-bottom: 1px solid #eaeaea;
}

.empty-product-slot {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 180px;
  border: 2px dashed #cbd5e0;
  border-radius: 12px;
  color: #a0aec0;
  cursor: pointer;
  transition: 0.2s;
  background: #f8fafc;
}
.empty-product-slot i {
  font-size: 32px;
  margin-bottom: 10px;
}
.empty-product-slot span {
  font-weight: 600;
  font-size: 14px;
}
.empty-product-slot:hover {
  border-color: #bd9a5f;
  color: #bd9a5f;
  background: #fffcf7;
}

.compare-select {
  width: 80%;
  padding: 8px 10px;
  border: 1px solid #cbd5e0;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #0a142f;
  outline: none;
  cursor: pointer;
  margin: 0 auto;
  display: block;
}
.compare-select:focus {
  border-color: #bd9a5f;
}

.cm-price-val {
  font-size: 16px;
  font-weight: 800;
  color: #e53e3e;
  display: flex;
  align-items: center;
  justify-content: flex-start;
}
.cm-rating {
  color: #bd9a5f;
  font-weight: 700;
  font-size: 15px;
  text-align: left;
}
.cm-rating .d-flex {
  justify-content: flex-start !important;
}
.cm-btn-buy {
  width: 80%;
  margin: 0 auto;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 12px;
  background: #0a142f;
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
  transition: 0.2s;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-size: 13px;
}
.cm-btn-buy:hover:not(:disabled) {
  background: #bd9a5f;
  box-shadow: 0 4px 15px rgba(189, 154, 95, 0.3);
}
.cm-btn-buy:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: #718096;
}

/* PICKER MODAL */
.product-picker-box {
  background: white;
  width: 95%;
  max-width: 700px;
  max-height: 80vh;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: modalFadeIn 0.3s ease;
}
.picker-search-bar {
  padding: 15px 24px;
  border-bottom: 1px solid #eaeaea;
  background: #f8fafc;
}
.picker-input {
  width: 100%;
  padding: 12px 15px;
  border: 1px solid #cbd5e0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: 0.2s;
}
.picker-input:focus {
  border-color: #bd9a5f;
  box-shadow: 0 0 0 3px rgba(189, 154, 95, 0.1);
}
.picker-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}
.picker-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 15px;
}

.picker-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 15px;
  border: 1px solid #eaeaea;
  border-radius: 12px;
  cursor: pointer;
  transition: 0.2s;
  position: relative;
  justify-content: space-between;
}
.picker-item:hover {
  border-color: #bd9a5f;
  transform: translateY(-3px);
  box-shadow: 0 6px 15px rgba(0, 0, 0, 0.05);
}
.picker-item.selected {
  border-color: #bd9a5f;
  background: #fdfaf6;
}
.picker-item img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  margin-bottom: 12px;
  flex-shrink: 0;
}
.picker-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  flex: 1;
}
.picker-info .brand {
  font-size: 11px;
  color: #bd9a5f;
  font-weight: 700;
  text-transform: uppercase;
  margin-bottom: 4px;
}
.picker-info .name {
  font-size: 13px;
  font-weight: 700;
  color: #0a142f;
  margin: 0 0 8px;

  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.3;
}
.picker-price {
  font-size: 15px;
  font-weight: 800;
  color: #e53e3e;
  margin-top: auto;
}
.picker-check {
  position: absolute;
  top: 10px;
  right: 10px;
  font-size: 18px;
}
.picker-footer {
  padding: 15px 24px;
  border-top: 1px solid #eaeaea;
  display: flex;
  justify-content: flex-end;
  background: #f8fafc;
}

/* MODAL CHỌN BIẾN THỂ */
.custom-modal-overlay {
  backdrop-filter: blur(5px);
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: 1050;
  display: flex;
  align-items: center;
  justify-content: center;
}
.variant-modal-box {
  background: #ffffff;
  width: 100%;
  max-width: 440px;
  border-radius: 20px;
  padding: 28px;
  box-shadow: 0 24px 54px rgba(6, 19, 43, 0.25);
  animation: modalFadeIn 0.3s ease-out forwards;
}
@keyframes modalFadeIn {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
.vm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  border-bottom: 1px solid rgba(189, 154, 95, 0.15);
  padding-bottom: 16px;
}
.vm-header h5 {
  margin: 0;
  font-family: "Playfair Display", serif;
  font-weight: 800;
  color: #06132b;
  font-size: 20px;
  letter-spacing: -0.5px;
}
.vm-close {
  background: transparent;
  border: none;
  padding: 4px;
  color: #a0aec0;
  cursor: pointer;
  transition: 0.2s;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.vm-close:hover {
  color: #e53e3e;
  background: #fff5f5;
  transform: rotate(90deg);
}
.vm-product-info {
  display: flex;
  gap: 18px;
  margin-bottom: 28px;
  align-items: center;
}
.vm-img-box {
  width: 82px;
  height: 82px;
  border-radius: 14px;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid rgba(6, 19, 43, 0.08);
  flex-shrink: 0;
  padding: 6px;
}
.vm-img-box img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.vm-details h6 {
  margin: 0 0 6px 0;
  font-weight: 800;
  font-size: 16px;
  color: #06132b;
  font-family: "Playfair Display", serif;
  line-height: 1.3;

  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.vm-price {
  margin: 0;
  font-weight: 800;
  color: #b78d52;
  font-size: 18px;
}
.vm-label {
  font-weight: 700;
  font-size: 13px;
  color: #4a5568;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.vm-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 28px;
}
.vm-variant-btn {
  background: #ffffff;
  border: 1px solid #cbd5e0;
  border-radius: 12px;
  padding: 12px 6px;
  text-align: center;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.vm-variant-btn:hover:not(.disabled) {
  border-color: #b78d52;
  background: #fffcf7;
  transform: translateY(-2px);
}
.vm-variant-btn.selected {
  border-color: #b78d52;
  background: #fffcf7;
  border-width: 2px;
  box-shadow: 0 6px 14px rgba(183, 141, 82, 0.15);
  padding: 11px 5px;
}
.vm-v-name {
  font-size: 14px;
  font-weight: 800;
  color: #06132b;
}
.vm-variant-btn.disabled {
  opacity: 0.45;
  cursor: not-allowed;
  background: #f1f5f9;
  border-color: #e2e8f0;
}
.vm-variant-btn.disabled .vm-v-name {
  text-decoration: line-through;
  color: #a0aec0;
}
.variant-divider {
  border: 0;
  border-top: 1px dashed #cbd5e0;
  margin: 20px 0 16px 0;
}
.quantity-section {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 24px;
}
.quantity-control {
  display: flex;
  align-items: center;
  gap: 12px;
}
.qty-wrapper {
  display: inline-flex;
  border: 1px solid #cbd5e0;
  border-radius: 6px;
  overflow: hidden;
}
.qty-wrapper button {
  width: 32px;
  height: 32px;
  background: #ffffff;
  border: none;
  cursor: pointer;
  color: #06132b;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: 0.2s;
}
.qty-wrapper button:hover:not(:disabled) {
  background: #f1f5f9;
}
.qty-wrapper button:disabled {
  color: #cbd5e0;
  cursor: not-allowed;
}
.qty-wrapper input {
  width: 44px;
  text-align: center;
  border: none;
  font-size: 15px;
  font-weight: 700;
  outline: none;
  border-left: 1px solid #cbd5e0;
  border-right: 1px solid #cbd5e0;
  color: #06132b;
}
.stock-info {
  font-size: 13px;
  color: #718096;
}
.vm-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}
.vm-btn-cart,
.vm-btn-buy {
  border-radius: 12px;
  padding: 14px;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.5px;
  transition: all 0.25s ease;
  text-transform: uppercase;
  display: flex;
  justify-content: center;
  align-items: center;
  border: none;
  flex-grow: 1;
}
.vm-btn-cart {
  background: #0a142f;
  color: #ffffff;
}
.vm-btn-cart:hover:not(:disabled) {
  background: #13275a;
  transform: translateY(-2px);
  box-shadow: 0 6px 14px rgba(10, 20, 47, 0.2);
}
.vm-btn-buy {
  background: #b78d52;
  color: #ffffff;
}
.vm-btn-buy:hover:not(:disabled) {
  background: #9b7541;
  transform: translateY(-2px);
  box-shadow: 0 6px 14px rgba(183, 141, 82, 0.25);
}
.vm-btn-cart:disabled,
.vm-btn-buy:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}
.qty-wrapper input[type="number"]::-webkit-inner-spin-button,
.qty-wrapper input[type="number"]::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.qty-wrapper input[type="number"] {
  appearance: textfield;
  -moz-appearance: textfield;
}
.flash-sale-badge {
  background: #b31320;
  color: #ffffff;
  border-radius: 999px;
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 800;
  margin-bottom: 2px;
}
.date-info-box {
  background: #fffcf7;
  border-left: 3px solid #d97706;
  padding: 8px 12px;
  border-radius: 4px;
  margin-top: 8px;
}
</style>
