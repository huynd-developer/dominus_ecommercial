<template>
  <div class="detail-view-container">
    <nav class="breadcrumb">
      <span class="back-link" @click="emit('back')" title="Quay lại">
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          style="width: 14px; margin-right: 4px; vertical-align: middle"
        >
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
          <polyline points="9 22 9 12 15 12 15 22" />
        </svg>
      </span>
      <span class="divider">/</span>
      <span class="breadcrumb-item clickable" @click="navigateToShop('all')"
        >Nước hoa</span
      >
      <template v-if="genderText && genderText !== 'Đang cập nhật'">
        <span class="divider">/</span>
        <span
          class="breadcrumb-item clickable"
          @click="navigateToShop('gender', product?.gender)"
          >{{ genderText }}</span
        >
      </template>
      <template v-if="brandText && brandText !== 'Đang cập nhật'">
        <span class="divider">/</span>
        <span
          class="breadcrumb-item clickable"
          @click="navigateToShop('brand', product?.brand)"
          >{{ brandText }}</span
        >
      </template>
      <span class="divider">/</span>
      <span class="active">
        {{ product?.name || "Đang cập nhật" }}
      </span>
    </nav>
    <div class="product-content" v-if="product">
      <div class="product-gallery">
        <div class="main-image-wrapper">
          <img
            :src="productImage"
            class="main-image"
            :alt="product?.name || 'Sản phẩm'"
            @error="handleImageError"
          />
          <button
            class="btn-heart"
            type="button"
            :class="{ active: isFavorited }"
            :disabled="isFavoriteLoading || !selectedVariant"
            @click="toggleFavorite"
            :title="isFavorited ? 'Bỏ yêu thích' : 'Thêm vào yêu thích'"
          >
            <i
              v-if="isFavoriteLoading"
              class="spinner-border spinner-border-sm"
            ></i>
            <i
              v-else
              class="bi"
              :class="isFavorited ? 'bi-heart-fill' : 'bi-heart'"
            ></i>
          </button>
          
          <!-- NÚT SO SÁNH -->
          <button
            v-if="product"
            class="btn-compare"
            type="button"
            :class="{ active: isInCompare(product) }"
            @click="toggleCompare(product)"
            :title="isInCompare(product) ? 'Bỏ so sánh' : 'Thêm vào so sánh'"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M16 3l4 4-4 4" />
              <path d="M20 7H4" />
              <path d="M8 21l-4-4 4-4" />
              <path d="M4 17h16" />
            </svg>
          </button>
        </div>

        <div v-if="productGalleryImages.length > 0" class="thumbnail-list">
          <button
            v-for="(image, index) in productGalleryImages"
            :key="`${image}-${index}`"
            type="button"
            class="thumb"
            :class="{ active: image === productImage }"
            @click="selectGalleryImage(image)"
          >
            <img
              :src="image"
              :alt="`${product?.name || 'Sản phẩm'} - ảnh ${index + 1}`"
              @error="handleImageError"
            />
          </button>
        </div>
      </div>

      <div class="product-info">
        <div class="header-info">
          <div class="brand">
            {{ brandText }}
          </div>
        </div>

        <h1 class="title">
          {{ product?.name || "Tên sản phẩm đang cập nhật" }}
        </h1>

        <div class="rating">
          <span class="stars">
            <i
              v-for="star in 5"
              :key="star"
              class="bi"
              :class="star <= roundedAverage ? 'bi-star-fill' : 'bi-star'"
            ></i>
          </span>

          <span class="score">
            {{ averageRatingText }}
          </span>

          <span class="divider-line">|</span>

          <span class="reviews"> {{ reviewCount }} đánh giá </span>
        </div>

        <div class="price-box">
          <span class="current-price">
            {{
              selectedDisplayPrice > 0
                ? formatCurrency(selectedDisplayPrice)
                : "Liên hệ"
            }}
          </span>

          <span class="old-price" v-if="selectedHasFlashSale">
            {{ formatCurrency(selectedOriginalPrice) }}
          </span>

          <span v-if="selectedHasFlashSale" class="flash-sale-badge">
            -{{ formatDiscount(selectedDiscountPercent) }}%
          </span>
        </div>

        <div class="save-badge" v-if="selectedHasFlashSale">
          Flash Sale đang diễn ra - tiết kiệm
          {{ formatCurrency(selectedOriginalPrice - selectedDisplayPrice) }}
        </div>

        <div class="desc-divider"></div>

        <p class="desc">
          {{
            product?.description ||
            "Chưa có thông tin mô tả chi tiết cho sản phẩm này."
          }}
        </p>

        <div class="product-specs">
          <h4>Thông tin sản phẩm</h4>

          <div class="spec-grid">
            <div class="spec-item">
              <span>Thương hiệu</span>
              <strong>{{ brandText }}</strong>
            </div>

            <div class="spec-item">
              <span>Giới tính</span>
              <strong>{{ genderText }}</strong>
            </div>

            <div class="spec-item">
              <span>Nồng độ</span>
              <strong>{{ concentrationText }}</strong>
            </div>

            <div class="spec-item">
              <span>Nhóm hương</span>
              <strong>{{ fragranceFamilyText }}</strong>
            </div>

            <div class="spec-item">
              <span>Dung tích đang chọn</span>
              <strong>{{ selectedCapacityText }}</strong>
            </div>

            <div class="spec-item">
              <span>Loại chai</span>
              <strong>{{ bottleTypeText }}</strong>
            </div>

            <div class="spec-item">
              <span>Tình trạng</span>
              <strong
                :class="isVariantOutOfStock ? 'text-danger' : 'text-success'"
              >
                {{ stockStatusText }}
              </strong>
            </div>
          </div>
        </div>

        <div class="desc-divider"></div>

        <div class="variant-selection">
          <h4>Chọn dung tích</h4>

          <div
            class="capacity-options"
            v-if="product?.variants && product.variants.length > 0"
          >
            <button
              v-for="variant in product.variants"
              :key="getVariantIdFromVariant(variant)"
              type="button"
              :class="[
                'cap-btn',
                {
                  active:
                    getVariantIdFromVariant(selectedVariant) ===
                    getVariantIdFromVariant(variant),
                },
              ]"
              :disabled="!isVariantSellable(variant)"
              @click="selectVariant(variant)"
            >
              <span>{{ getCapacityText(variant) }}</span>
              <span
                v-if="
                  getVariantIdFromVariant(selectedVariant) ===
                  getVariantIdFromVariant(variant)
                "
                class="check-icon"
              >
                ✓
              </span>
            </button>
          </div>
          <div
            v-else
            style="color: #e53e3e; font-size: 14px; margin-bottom: 20px"
          >
            Sản phẩm hiện chưa có dung tích nào
          </div>
        </div>
        <div class="stock-status mb-3" v-if="selectedVariant">
          <span
            v-if="normalizeStock(selectedVariant) > 0"
            style="color: #2e7d32; font-size: 14px; font-weight: 500"
          >
            <i class="bi bi-box-seam me-1"></i>
            Kho còn: {{ normalizeStock(selectedVariant) }} sản phẩm
          </span>
          <span
            v-else
            style="color: #d32f2f; font-size: 14px; font-weight: 500"
          >
            <i class="bi bi-x-circle me-1"></i>
            Đã hết hàng
          </span>
        </div>

        <div
          class="quantity-section"
          v-if="selectedVariant"
          style="
            display: flex;
            align-items: center;
            gap: 20px;
            margin-bottom: 30px;
            flex-wrap: wrap;
          "
        >
          <h4 style="margin: 0; min-width: 80px">Số lượng</h4>
          <!-- Ghi đè margin-bottom: 0 để input không bị đẩy lên cao -->
          <div class="qty-control" style="margin-bottom: 0">
            <button
              type="button"
              @click="decreaseQty"
              :disabled="quantity <= 1"
            >
              -
            </button>
            <input
              type="number"
              v-model="quantity"
              @input="validateQuantity"
              @blur="validateQuantity"
              @keyup.enter="validateQuantity"
            />
            <button type="button" @click="increaseQty">+</button>
          </div>
        </div>
        <div class="actions">
          <button
            class="btn-add-cart"
            type="button"
            @click="addToCart"
            :disabled="
              isAdding ||
              !selectedVariant ||
              isVariantOutOfStock ||
              isVariantInvalidPrice
            "
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              class="btn-icon"
            >
              <path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z" />
              <line x1="3" y1="6" x2="21" y2="6" />
              <path d="M16 10a4 4 0 01-8 0" />
            </svg>
            {{
              isVariantInvalidPrice
                ? "LIÊN HỆ ĐỂ MUA"
                : isVariantOutOfStock
                ? "TẠM HẾT HÀNG"
                : isAdding
                ? "ĐANG THÊM..."
                : "THÊM VÀO GIỎ HÀNG"
            }}
          </button>
          <button
            class="btn-buy-now"
            type="button"
            @click="buyNow"
            :disabled="
              isAdding ||
              !selectedVariant ||
              isVariantOutOfStock ||
              isVariantInvalidPrice
            "
          >
            {{
              isVariantInvalidPrice
                ? "LIÊN HỆ ĐỂ MUA"
                : isVariantOutOfStock
                ? "TẠM HẾT HÀNG"
                : "MUA NGAY"
            }}
            <svg
              v-if="
                selectedVariant &&
                !isVariantInvalidPrice &&
                !isVariantOutOfStock
              "
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              class="btn-icon-right"
            >
              <line x1="5" y1="12" x2="19" y2="12" />
              <polyline points="12 5 19 12 12 19" />
            </svg>
          </button>
        </div>
        <div class="policy-footer">
          <div class="policy-item">
            <svg
              class="icon-policy"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.5"
            >
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
            <div>
              <strong>Cam kết chính hãng</strong><br />
              100% Authentic
            </div>
          </div>
          <div class="policy-item">
            <svg
              class="icon-policy"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.5"
            >
              <polyline points="1 4 1 10 7 10" />
              <polyline points="23 20 23 14 17 14" />
              <path
                d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 0 1 3.51 15"
              />
            </svg>
            <div>
              <strong>Đổi trả dễ dàng</strong><br />
              Trong 7 ngày
            </div>
          </div>
          <div class="policy-item">
            <svg
              class="icon-policy"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.5"
            >
              <rect x="1" y="3" width="15" height="13" />
              <polygon points="16 8 20 8 23 11 23 16 16 16 16 8" />
              <circle cx="5.5" cy="18.5" r="2.5" />
              <circle cx="18.5" cy="18.5" r="2.5" />
            </svg>
            <div>
              <strong>Giao hàng miễn phí</strong><br />
              Đơn từ 1.000.000đ
            </div>
          </div>
        </div>
      </div>
    </div>
    <ProductReviews
      v-if="product?.id"
      :product-id="Number(product.id)"
      @summary-loaded="handleReviewSummaryLoaded"
    />
    <div class="luxury-toast" :class="{ show: showToast }">
      <div class="toast-content">
        <div class="icon-circle-toast">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.5"
          >
            <polyline points="20 6 9 17 4 12" />
          </svg>
        </div>
        <div class="toast-text">
          <strong>Thêm thành công</strong>
          <span>Đã thêm {{ lastAddedQuantity }} sản phẩm vào giỏ.</span>
        </div>
      </div>
      <button class="toast-action" type="button" @click="goToCart">
        Xem giỏ hàng ➔
      </button>
    </div>

    <!-- THANH NỔI (FLOATING BAR) CHỌN SO SÁNH -->
    <div class="compare-bar" :class="{ show: compareList.length > 0 }">
      <div class="cb-container">
        <div class="cb-left">
          <div class="cb-title">So sánh ({{ compareList.length }}/3)</div>
          <div class="cb-slots">
            <div class="cb-slot filled" v-for="p in compareList" :key="p.id || p.productId">
              <img :src="getProductImageCompare(p)" :alt="p.name" />
              <div class="cb-slot-info">
                <p>{{ p.name }}</p>
                <span>{{ formatCurrency(getComparePrice(p)) }}</span>
              </div>
              <button class="btn-remove-cb" @click="removeFromCompare(p)">✕</button>
            </div>
            <div class="cb-slot empty-slot cursor-pointer" v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty' + i" @click="openPickerModal" title="Chọn thêm sản phẩm so sánh">
              <i class="bi bi-plus-circle me-2"></i> Thêm sản phẩm
            </div>
          </div>
        </div>
        <div class="cb-right">
          <button class="cb-btn-clear" @click="clearCompareList">Xóa hết</button>
          <button class="cb-btn-compare" :disabled="compareList.length < 2" @click="showCompareModal = true">So sánh ngay</button>
        </div>
      </div>
    </div>

    <!-- BẢNG POPUP SO SÁNH CHUYÊN NGHIỆP CÓ AI -->
    <Teleport to="body">
      <div class="compare-modal-overlay" v-if="showCompareModal" @click.self="showCompareModal = false">
        <div class="compare-modal-box">
          <div class="cm-header">
            <h3>So sánh thông số</h3>
            <button class="cm-close" @click="showCompareModal = false">✕</button>
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
                  AI sẽ phân tích thêm độ lưu hương, phong cách, hoàn cảnh sử dụng và gợi ý lựa chọn dựa trên dữ liệu sản phẩm.
                </div>
              </div>

              <button
                type="button"
                class="cm-btn-ai"
                :disabled="compareAiLoading || compareList.length < 2"
                @click="handleAiCompare"
              >
                <span
                  v-if="compareAiLoading"
                  class="spinner-border spinner-border-sm me-2"
                ></span>
                <i v-else class="bi bi-stars me-2"></i>
                {{ compareAiLoading ? "Đang phân tích..." : "So sánh bằng AI" }}
              </button>
            </div>

            <div v-if="compareAiError" class="ai-compare-error">
              <i class="bi bi-exclamation-circle me-2"></i>
              {{ compareAiError }}
            </div>

            <div
              v-if="compareAnalysis || compareRecommendation"
              class="ai-compare-result"
            >
              <div v-if="compareAnalysis" class="ai-result-block">
                <div class="ai-result-label">
                  <i class="bi bi-lightbulb me-2"></i>
                  Nhận xét từ AI
                </div>
                <p>{{ compareAnalysis }}</p>
              </div>

              <div v-if="compareRecommendation" class="ai-result-block ai-result-recommendation">
                <div class="ai-result-label">
                  <i class="bi bi-check2-circle me-2"></i>
                  Gợi ý lựa chọn
                </div>
                <p>{{ compareRecommendation }}</p>
              </div>
            </div>
            <!-- KẾT THÚC: KHỐI TÍNH NĂNG AI -->

            <table class="table-compare">
              <thead class="sticky-header">
                <tr>
                  <th class="spec-label-col">Sản phẩm</th>
                  <td v-for="p in compareList" :key="'img' + (p.id || p.productId)" class="spec-value-col">
                    <button class="btn-remove-from-table" @click="removeFromCompare(p)" title="Xóa khỏi so sánh"><i class="bi bi-x"></i></button>
                    <div class="cm-img-wrapper clickable-item" @click="goToDetailFromCompare(p)" title="Xem chi tiết">
                      <img :src="getProductImageCompare(p)" class="cm-img" />
                    </div>
                    <h4 class="cm-name clickable-item" @click="goToDetailFromCompare(p)" title="Xem chi tiết">{{ p.name }}</h4>
                    <div class="cm-header-price">
                      {{ formatCurrency(getComparePrice(p)) }}
                      <span v-if="getCompareDiscount(p) > 0" class="flash-sale-badge ms-2">-{{ getCompareDiscount(p) }}%</span>
                    </div>
                  </td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-th-' + i" class="spec-value-col empty-col">
                    <div class="empty-product-slot" @click="openPickerModal" title="Chọn thêm sản phẩm so sánh">
                      <i class="bi bi-plus-circle-dotted"></i><span>Thêm sản phẩm</span>
                    </div>
                  </td>
                </tr>
              </thead>
              <tbody>
                <tr><td colspan="4" class="group-header">Thông tin cơ bản</td></tr>
                <tr>
                  <th class="spec-label-col">Thương hiệu</th>
                  <td v-for="p in compareList" :key="'brand' + (p.id || p.productId)" class="fw-bold text-dark">{{ getCompareValue(p, "brand") }}</td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-brand-' + i"></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Phân loại dung tích</th>
                  <td v-for="p in compareList" :key="'var' + (p.id || p.productId)">
                    <select v-if="p.variants && p.variants.length > 0" class="compare-select" v-model="compareVariantIds[p.id || p.productId]">
                      <option v-for="v in getSortedVariantsCompare(p)" :key="v.productVariantId || v.variantId || v.id" :value="v.productVariantId || v.variantId || v.id">
                        {{ formatVariantNameCompare(v) }}
                      </option>
                    </select>
                    <span v-else class="text-muted">Mặc định</span>
                  </td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-var-' + i"></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Loại chai</th>
                  <td v-for="p in compareList" :key="'bottle' + (p.id || p.productId)">{{ getCompareBottleType(p) }}</td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-bottle-' + i"></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Giá ưu đãi</th>
                  <td v-for="p in compareList" :key="'price' + (p.id || p.productId)">
                    <div class="cm-price-val">
                      {{ formatCurrency(getComparePrice(p)) }}
                      <span v-if="getCompareDiscount(p) > 0" class="flash-sale-badge ms-2">-{{ getCompareDiscount(p) }}%</span>
                    </div>
                    <div v-if="getCompareOriginalPrice(p) > getComparePrice(p)" class="text-decoration-line-through text-muted small mt-1">
                      {{ formatCurrency(getCompareOriginalPrice(p)) }}
                    </div>
                  </td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-price-' + i"></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Đơn giá / 1ml</th>
                  <td v-for="p in compareList" :key="'priceml' + (p.id || p.productId)">
                    <div class="fw-bold" style="color: #b78d52;">{{ formatPricePerMl(p) }}</div>
                    <span v-if="isBestValue(p)" class="badge bg-warning bg-opacity-10 text-warning border border-warning mt-2 d-inline-block px-2 py-1">Tiết kiệm nhất</span>
                  </td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-priceml-' + i"></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Tình trạng kho</th>
                  <td v-for="p in compareList" :key="'stock' + (p.id || p.productId)">
                    <span :class="getCompareStock(p) > 0 ? 'text-success fw-bold' : 'text-danger fw-bold'">
                      {{ getCompareStock(p) > 0 ? "Còn hàng" : "Hết hàng" }}
                    </span>
                  </td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-stock-' + i"></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Đánh giá</th>
                  <td v-for="p in compareList" :key="'rating' + (p.id || p.productId)" class="cm-rating">
                    <div class="d-flex align-items-center gap-1" style="justify-content: center;">
                      <span class="score">{{ getRatingScoreCompare(p) }} ★</span>
                      <span class="count">({{ getReviewCountCompare(p) }})</span>
                    </div>
                  </td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-rating-' + i"></td>
                </tr>

                <tr><td colspan="4" class="group-header">Đặc tính sản phẩm</td></tr>
                <tr>
                  <th class="spec-label-col">Nhóm hương chính</th>
                  <td v-for="p in compareList" :key="'scent' + (p.id || p.productId)">{{ getCompareValue(p, "scent") }}</td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-scent-' + i"></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Nồng độ</th>
                  <td v-for="p in compareList" :key="'con' + (p.id || p.productId)" class="fw-bold">{{ getCompareValue(p, "concentration") }}</td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-con-' + i"></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Độ lưu hương</th>
                  <td v-for="p in compareList" :key="'long' + (p.id || p.productId)" style="color: #b78d52; font-weight: 600;">{{ getLongevityDisplay(p) }}</td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-long-' + i"></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Đối tượng (Giới tính)</th>
                  <td v-for="p in compareList" :key="'gen' + (p.id || p.productId)">{{ getCompareValue(p, "gender") }}</td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-gen-' + i"></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Phong cách</th>
                  <td v-for="p in compareList" :key="'style' + (p.id || p.productId)">{{ getCompareValue(p, "style") }}</td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-style-' + i"></td>
                </tr>
                <tr>
                  <th class="spec-label-col">Hoàn cảnh khuyên dùng</th>
                  <td v-for="p in compareList" :key="'occ' + (p.id || p.productId)">{{ getCompareValue(p, "occasion") }}</td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-occ-' + i"></td>
                </tr>

                <tr><td colspan="4" class="group-header bg-white border-bottom-0 pt-4"></td></tr>
                <tr>
                  <th class="spec-label-col border-bottom-0"></th>
                  <td v-for="p in compareList" :key="'act' + (p.id || p.productId)" class="border-bottom-0 pb-4">
                    <button class="cm-btn-buy" :disabled="isCompareBuyDisabled(p)" @click="buyFromCompare(p)">
                      <i class="bi bi-cart-plus me-1"></i> {{ isCompareBuyDisabled(p) ? "Tạm hết hàng" : "Xem sản phẩm" }}
                    </button>
                  </td>
                  <td v-for="i in Math.max(0, 3 - compareList.length)" :key="'empty-act-' + i" class="border-bottom-0"></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- MODAL CHỌN NHANH SẢN PHẨM SO SÁNH -->
    <Teleport to="body">
      <div class="compare-modal-overlay" v-if="showPickerModal" @click.self="showPickerModal = false">
        <div class="product-picker-box">
          <div class="cm-header">
            <h3>Chọn sản phẩm so sánh</h3>
            <button class="cm-close" @click="showPickerModal = false">✕</button>
          </div>
          <div class="picker-search-bar">
            <input type="text" v-model="pickerSearchKeyword" placeholder="Tìm kiếm theo tên sản phẩm hoặc thương hiệu..." class="picker-input" />
          </div>
          <div class="picker-body">
            <div v-if="pickerLoading" class="text-center py-5">
              <span class="spinner-border text-warning"></span>
            </div>
            <div v-else-if="filteredPickerProducts.length === 0" class="text-center py-5 text-muted">
              Không tìm thấy sản phẩm phù hợp.
            </div>
            <div v-else class="picker-grid">
              <div v-for="item in filteredPickerProducts" :key="item.id || item.productId" class="picker-item" :class="{ selected: isInCompare(item) }" @click="toggleItemInPicker(item)">
                <img :src="getProductImageCompare(item)" :alt="item.name" />
                <div class="picker-info">
                  <span class="brand">{{ getBrandName(item) }}</span>
                  <h5 class="name" :title="item.name">{{ item.name }}</h5>
                  <div class="picker-price">{{ formatCurrency(getComparePrice(item)) }}</div>
                </div>
                <div class="picker-check">
                  <i :class="isInCompare(item) ? 'bi bi-check-circle-fill text-success' : 'bi bi-circle text-muted'"></i>
                </div>
              </div>
            </div>
          </div>
          <div class="picker-footer">
            <button class="btn btn-secondary btn-sm px-4" @click="showPickerModal = false">Đóng</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRouter } from "vue-router";
import Swal from "sweetalert2";
import api from "@/common/api";
import ProductReviews from "./ProductReviews.vue";
import type { ProductReviewSummaryResponse } from "../types/product-review.type";
import { favoriteService } from "../services/favorite.service";

const router = useRouter();
const props = defineProps<{
  product: any;
}>();
const emit = defineEmits<{
  (e: "back"): void;
  (e: "buy-now"): void;
  (e: "filter-category", filter: { type: string; value: any }): void;
  (e: "navigate-shop", filter: { type: string; value: any }): void;
}>();

const selectedVariant = ref<any>(null);
const quantity = ref<number>(1);
const lastAddedQuantity = ref<number>(1);
const showToast = ref(false);
const isAdding = ref(false);
const reviewSummary = ref<ProductReviewSummaryResponse | null>(null);
const isFavorited = ref(false);
const isFavoriteLoading = ref(false);
const selectedImageUrl = ref("");
const BACKEND_URL = "http://localhost:8080";

const getCurrentRole = () => {
  return String(
    localStorage.getItem("role") || localStorage.getItem("userRole") || ""
  )
    .replace("ROLE_", "")
    .toUpperCase()
    .trim();
};

const hasToken = () => {
  return Boolean(localStorage.getItem("token"));
};

const isCustomerLoggedIn = () => {
  return hasToken() && getCurrentRole() === "USER";
};

const getPlaceholderImage = () => {
  return (
    "data:image/svg+xml;utf8," +
    encodeURIComponent(`
      <svg xmlns="http://www.w3.org/2000/svg" width="400" height="400">
        <rect width="100%" height="100%" fill="#f3f4f6"/>
        <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle"
          fill="#9ca3af" font-family="Arial" font-size="24">
          Không có ảnh
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
  appendImage(images, variant?.thumbnailUrl);
  appendImage(images, variant?.imageUrl);
  appendImage(images, variant?.ImageUrl);
  appendImage(images, variant?.image);
  appendImageList(images, variant?.images);
  appendImageList(images, variant?.Images);
  appendImageList(images, variant?.imageList);
  appendImageList(images, variant?.ImageList);
  appendImageList(images, variant?.productImages);
  appendImageList(images, variant?.ProductImages);
  return images;
};

const productGalleryImages = computed(() => {
  const images: string[] = [];
  const productData = props.product;
  const addUnique = (url: unknown) => {
    const formatted = getImageUrlFromObject(url);
    if (formatted && !images.includes(formatted)) {
      images.push(formatted);
    }
  };
  if (productData?.primaryImageUrl) {
    addUnique(productData.primaryImageUrl);
  }
  if (Array.isArray(productData?.images)) {
    const primaryObj = productData.images.find((img: any) =>
      Boolean(img?.isPrimary)
    );
    if (primaryObj) {
      addUnique(primaryObj?.imageUrl || primaryObj);
    }
    productData.images.forEach((img: any) => {
      addUnique(img?.imageUrl || img);
    });
  }
  addUnique(productData?.mainImage);
  addUnique(productData?.mainImageUrl);
  addUnique(productData?.thumbnailUrl);
  addUnique(productData?.imageUrl);
  addUnique(productData?.image);
  appendImageList(images, productData?.galleryImages);
  appendImageList(images, productData?.imageList);
  appendImageList(images, productData?.productImages);
  if (selectedVariant.value) {
    getVariantImageList(selectedVariant.value).forEach((imageUrl) =>
      addUnique(imageUrl)
    );
  }
  if (Array.isArray(productData?.variants)) {
    productData.variants.forEach((variant: any) => {
      getVariantImageList(variant).forEach((imageUrl) => addUnique(imageUrl));
    });
  }
  return images;
});

const productImage = computed(() => {
  if (
    selectedImageUrl.value &&
    productGalleryImages.value.includes(selectedImageUrl.value)
  ) {
    return selectedImageUrl.value;
  }
  return productGalleryImages.value[0] || getPlaceholderImage();
});

const selectGalleryImage = (imageUrl: string) => {
  selectedImageUrl.value = imageUrl;
};

const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement | null;
  if (!target) {
    return;
  }
  target.onerror = null;
  target.src = getPlaceholderImage();
};

const navigateToShop = (type: string, value: any = null) => {
  emit("back"); 
  emit("filter-category", { type, value }); 
  emit("navigate-shop", { type, value });
};

const brandText = computed(() => {
  if (typeof props.product?.brand === "object") {
    return props.product?.brand?.name || "Đang cập nhật";
  }
  return props.product?.brandName || props.product?.brand || "Đang cập nhật";
});

const genderText = computed(() => {
  const gender = props.product?.gender;
  if (gender === 1 || gender === "1") return "Nam";
  if (gender === 2 || gender === "2") return "Nữ";
  if (gender === 0 || gender === "0") return "Unisex";
  if (typeof gender === "object") {
    return gender?.name || "Đang cập nhật";
  }
  return gender || "Đang cập nhật";
});

const concentrationText = computed(() => {
  if (typeof props.product?.concentration === "object") {
    return props.product?.concentration?.name || "Đang cập nhật";
  }
  return (
    props.product?.concentrationName ||
    props.product?.concentration ||
    "Đang cập nhật"
  );
});

const fragranceFamilyText = computed(() => {
  if (Array.isArray(props.product?.scents) && props.product.scents.length > 0) {
    return props.product.scents.join(", ");
  }
  if (Array.isArray(props.product?.fragranceFamilies)) {
    const names = props.product.fragranceFamilies
      .map((item: any) => (typeof item === "object" ? item?.name : item))
      .filter(Boolean);
    if (names.length > 0) {
      return names.join(", ");
    }
  }
  if (typeof props.product?.fragranceFamily === "object") {
    return props.product?.fragranceFamily?.name || "Đang cập nhật";
  }
  return (
    props.product?.fragranceFamilyName ||
    props.product?.fragranceFamily ||
    "Đang cập nhật"
  );
});

const selectedCapacityText = computed(() => {
  if (!selectedVariant.value) {
    return "Chưa chọn";
  }
  return getCapacityText(selectedVariant.value);
});

const bottleTypeText = computed(() => {
  if (!selectedVariant.value) {
    return "Đang cập nhật";
  }
  return getBottleTypeText(selectedVariant.value);
});

const selectedSkuText = computed(() => {
  return (
    selectedVariant.value?.sku || selectedVariant.value?.SKU || "Đang cập nhật"
  );
});

const DEFAULT_RATING = 5;
const MAX_RATING = 5;

const toFiniteNumber = (value: unknown, fallback = 0) => {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : fallback;
};

const clampRating = (value: unknown) => {
  return Math.min(MAX_RATING, Math.max(0, toFiniteNumber(value, 0)));
};

const reviewCount = computed(() => {
  const count = toFiniteNumber(
    reviewSummary.value?.reviewCount ?? props.product?.reviewCount,
    0
  );
  return Math.max(0, Math.floor(count));
});

const rawAverageRating = computed(() => {
  return clampRating(
    reviewSummary.value?.averageRating ??
      props.product?.averageRating ??
      props.product?.rating ??
      0
  );
});

const hasReviewCountSource = computed(() => {
  return (
    (reviewSummary.value?.reviewCount !== null &&
      reviewSummary.value?.reviewCount !== undefined) ||
    (props.product?.reviewCount !== null &&
      props.product?.reviewCount !== undefined)
  );
});

const hasActualReviews = computed(() => {
  if (hasReviewCountSource.value) {
    return reviewCount.value > 0;
  }
  return rawAverageRating.value > 0;
});

const averageRating = computed(() => {
  return hasActualReviews.value ? rawAverageRating.value : DEFAULT_RATING;
});

const averageRatingText = computed(() => {
  return averageRating.value.toFixed(1);
});

const roundedAverage = computed(() => {
  return Math.min(MAX_RATING, Math.max(0, Math.round(averageRating.value)));
});

const normalizeStock = (variant: any) => {
  const value = Number(variant?.sellableQuantity ?? 0);
  if (!Number.isFinite(value) || value <= 0) {
    return 0;
  }
  return Math.trunc(value);
};

const isVariantSellable = (variant: any) => {
  if (!variant) return false;
  const status = Number(variant?.status ?? variant?.variantStatus ?? 1);
  return status === 1 && normalizeStock(variant) > 0;
};

const getVariantIdFromVariant = (variant: any) => {
  return Number(
    variant?.productVariantId ??
      variant?.variantId ??
      variant?.id ??
      variant?.Id ??
      0
  );
};

const isEmptyDisplayValue = (value: any) => {
  if (value === null || value === undefined) {
    return true;
  }
  const text = String(value).trim();
  return (
    text === "" ||
    text.toUpperCase() === "N/A" ||
    text.toUpperCase() === "NULL" ||
    text.toUpperCase() === "UNDEFINED" ||
    text === "-"
  );
};

const formatCapacityNumber = (value: any) => {
  const text = String(value || "").trim();
  if (!text) {
    return "";
  }
  const lowerText = text.toLowerCase().replace(/\s+/g, "");
  if (lowerText.endsWith("ml")) {
    const numberPart = lowerText.replace("ml", "");
    const numberValue = Number(numberPart);
    if (Number.isFinite(numberValue)) {
      return `${
        Number.isInteger(numberValue) ? numberValue : numberValue.toString()
      }ml`;
    }
    return text;
  }
  const numberValue = Number(text);
  if (Number.isFinite(numberValue)) {
    return `${
      Number.isInteger(numberValue) ? numberValue : numberValue.toString()
    }ml`;
  }
  return text.toLowerCase().includes("ml") ? text : `${text}ml`;
};

const getCapacityText = (variant: any) => {
  const candidates = [
    variant?.capacityName,
    variant?.capacityText,
    variant?.capacityValue,
    variant?.volume,
    variant?.volumeValue,
    variant?.capacity?.name,
    variant?.capacity?.value,
    variant?.capacity,
  ];
  for (const value of candidates) {
    if (!isEmptyDisplayValue(value)) {
      return formatCapacityNumber(value);
    }
  }
  const sku = String(variant?.sku || variant?.SKU || "").toUpperCase();
  const match = sku.match(/-(\d+(?:\.\d+)?)-/);
  if (match?.[1]) {
    return formatCapacityNumber(match[1]);
  }
  return "Đang cập nhật";
};

const getBottleTypeText = (variant: any) => {
  const candidates = [
    variant?.bottleTypeName,
    variant?.bottleTypeText,
    variant?.variantBottleType,
    variant?.bottleName,
    variant?.bottleType?.name,
    variant?.bottleType,
    props.product?.bottleTypeName,
    props.product?.bottleType,
  ];
  for (const value of candidates) {
    if (!isEmptyDisplayValue(value)) {
      return String(value).trim();
    }
  }
  const sku = String(variant?.sku || variant?.SKU || "").toUpperCase();
  if (sku.includes("FULL")) {
    return "Chai gốc Fullbox";
  }
  if (sku.includes("CHIET") || sku.includes("DECANT")) {
    return "Chai chiết";
  }
  return "Đang cập nhật";
};

const getVariantId = () => {
  return getVariantIdFromVariant(selectedVariant.value);
};

const isVariantFlashSale = (variant: any) => {
  const originalPrice = Number(
    variant?.originalPrice ?? variant?.oldPrice ?? variant?.price ?? 0
  );
  const salePrice = Number(variant?.salePrice ?? variant?.price ?? 0);
  const discountPercent = Number(variant?.discountPercent ?? 0);
  return (
    Boolean(variant?.isFlashSale || variant?.hasPromotion) &&
    discountPercent > 0 &&
    originalPrice > salePrice
  );
};

const selectedOriginalPrice = computed(() => {
  if (!selectedVariant.value) {
    return 0;
  }
  return Number(
    selectedVariant.value.originalPrice ??
      selectedVariant.value.oldPrice ??
      selectedVariant.value.price ??
      0
  );
});

const selectedDisplayPrice = computed(() => {
  if (!selectedVariant.value) {
    return 0;
  }
  if (isVariantFlashSale(selectedVariant.value)) {
    return Number(
      selectedVariant.value.salePrice ?? selectedVariant.value.price ?? 0
    );
  }
  return Number(selectedVariant.value.price ?? 0);
});

const selectedDiscountPercent = computed(() => {
  return Number(selectedVariant.value?.discountPercent ?? 0);
});

const selectedHasFlashSale = computed(() => {
  return isVariantFlashSale(selectedVariant.value);
});

const isVariantOutOfStock = computed(() => {
  if (!selectedVariant.value) {
    return true;
  }
  return !isVariantSellable(selectedVariant.value);
});

const stockStatusText = computed(() => {
  if (!selectedVariant.value) {
    return "Chưa chọn dung tích";
  }
  return isVariantOutOfStock.value ? "Hết hàng" : "Còn hàng";
});

const isVariantInvalidPrice = computed(() => {
  if (!selectedVariant.value) {
    return true;
  }
  return selectedDisplayPrice.value <= 0;
});

const formatCurrency = (value: number) => {
  if (value == null || Number.isNaN(Number(value))) {
    return "0 đ";
  }
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(Number(value));
};

const formatDiscount = (value?: number | null) => {
  const numberValue = Number(value || 0);
  if (Number.isInteger(numberValue)) {
    return String(numberValue);
  }
  return numberValue.toFixed(2).replace(/\.?0+$/, "");
};

const getErrorMessage = (error: any) => {
  const data = error?.response?.data;
  if (!data) {
    return "Có lỗi xảy ra, vui lòng thử lại.";
  }
  if (typeof data === "string") {
    return data;
  }
  if (data.message) {
    return data.message;
  }
  if (data.errors && typeof data.errors === "object") {
    const firstError = Object.values(data.errors)[0];
    if (firstError) {
      return String(firstError);
    }
  }
  return "Có lỗi xảy ra, vui lòng thử lại.";
};

const handleReviewSummaryLoaded = (summary: ProductReviewSummaryResponse) => {
  reviewSummary.value = summary;
};

const selectVariant = (variant: any) => {
  if (!isVariantSellable(variant)) {
    return;
  }
  selectedVariant.value = variant;
  quantity.value = 1;
  const variantImages = getVariantImageList(variant);
  const firstVariantImage = variantImages.find((imageUrl): imageUrl is string =>
    Boolean(imageUrl)
  );
  if (firstVariantImage) {
    selectedImageUrl.value = firstVariantImage;
  } else if (
    selectedImageUrl.value &&
    !productGalleryImages.value.includes(selectedImageUrl.value)
  ) {
    selectedImageUrl.value = productGalleryImages.value[0] || "";
  }
  loadFavoriteStatus();
};

const validateQuantity = () => {
  let val = Number(quantity.value);

  const stock = selectedVariant.value
    ? normalizeStock(selectedVariant.value)
    : 0;

  if (Number.isNaN(val) || val < 1) {
    quantity.value = 1;
  } else if (val > 10) {
    quantity.value = 10;
    Swal.fire({ icon: 'warning', title: 'Vượt quá giới hạn', text: 'Bạn chỉ có thể mua tối đa 10 sản phẩm cho mỗi phân loại.', confirmButtonColor: '#bd9a5f', didOpen: () => { const c = Swal.getContainer(); if(c) c.style.setProperty('z-index', '9999999', 'important'); } });
  } else if (val > stock) {
    quantity.value = stock;
    Swal.fire({ icon: 'warning', title: 'Vượt quá tồn kho', text: `Sản phẩm chỉ còn ${stock} trong kho.`, confirmButtonColor: '#bd9a5f', didOpen: () => { const c = Swal.getContainer(); if(c) c.style.setProperty('z-index', '9999999', 'important'); } });
  } else {
    quantity.value = Math.floor(val);
  }
};

const decreaseQty = () => {
  if (quantity.value > 1) {
    quantity.value--;
  }
};

const increaseQty = () => {
  if (!selectedVariant.value) {
    return;
  }
  const stock = normalizeStock(selectedVariant.value);
  if (quantity.value >= 10) {
    Swal.fire({ icon: 'warning', title: 'Vượt quá giới hạn', text: 'Bạn chỉ có thể mua tối đa 10 sản phẩm cho mỗi phân loại.', confirmButtonColor: '#bd9a5f', didOpen: () => { const c = Swal.getContainer(); if(c) c.style.setProperty('z-index', '9999999', 'important'); } });
    return;
  }
  if (quantity.value >= stock) {
    Swal.fire({ icon: 'warning', title: 'Vượt quá tồn kho', text: `Sản phẩm chỉ còn ${stock} trong kho.`, confirmButtonColor: '#bd9a5f', didOpen: () => { const c = Swal.getContainer(); if(c) c.style.setProperty('z-index', '9999999', 'important'); } });
    return;
  }
  quantity.value++;
};

const askLogin = async (
  message = "Vui lòng đăng nhập để sử dụng chức năng này."
) => {
  const result = await Swal.fire({
    icon: "info",
    title: "Bạn chưa đăng nhập",
    text: message,
    showCancelButton: true,
    confirmButtonText: "Đăng nhập ngay",
    cancelButtonText: "Ở lại xem tiếp",
    confirmButtonColor: "#bd9a5f",
    cancelButtonColor: "#6b7280",
    didOpen: () => {
      const container = Swal.getContainer();
      if (container) container.style.setProperty('z-index', '9999999', 'important');
    }
  });
  if (result.isConfirmed) {
    router.push({
      name: "Login",
      query: {
        redirect: router.currentRoute.value.fullPath,
      },
    });
  }
};

const loadFavoriteStatus = async () => {
  const variantId = getVariantId();
  if (!variantId || Number.isNaN(variantId)) {
    isFavorited.value = false;
    return;
  }
  if (!isCustomerLoggedIn()) {
    isFavorited.value = false;
    return;
  }
  try {
    const res = await favoriteService.checkFavorite(variantId);
    isFavorited.value = Boolean(res.data?.favorited);
  } catch (error) {
    console.error("Lỗi kiểm tra yêu thích:", error);
    isFavorited.value = false;
  }
};

const toggleFavorite = async () => {
  const variantId = getVariantId();
  if (!variantId || Number.isNaN(variantId)) {
    await Swal.fire({ icon: 'warning', title: 'Chưa chọn dung tích', text: 'Vui lòng chọn dung tích trước khi thêm yêu thích.', confirmButtonColor: '#bd9a5f', didOpen: () => { const c = Swal.getContainer(); if(c) c.style.setProperty('z-index', '9999999', 'important'); } });
    return;
  }
  if (!hasToken()) {
    await askLogin("Vui lòng đăng nhập để thêm sản phẩm vào danh sách yêu thích.");
    return;
  }
  if (!isCustomerLoggedIn()) {
    await Swal.fire({ icon: 'warning', title: 'Không thể sử dụng chức năng này', text: 'Chỉ tài khoản khách hàng mới được thêm sản phẩm yêu thích.', confirmButtonColor: '#bd9a5f', didOpen: () => { const c = Swal.getContainer(); if(c) c.style.setProperty('z-index', '9999999', 'important'); } });
    return;
  }
  try {
    isFavoriteLoading.value = true;
    const res = await favoriteService.toggleFavorite(variantId);
    isFavorited.value = Boolean(res.data?.favorited);
    window.dispatchEvent(
      new CustomEvent("favorite-updated", {
        detail: {
          productVariantId: variantId,
          favorited: isFavorited.value,
        },
      })
    );
    await Swal.fire({
      toast: true,
      position: "top-end",
      icon: isFavorited.value ? "success" : "info",
      title:
        res.data?.message ||
        (isFavorited.value ? "Đã thêm vào yêu thích" : "Đã bỏ yêu thích"),
      showConfirmButton: false,
      timer: 1600,
      timerProgressBar: true,
      didOpen: () => {
        const container = Swal.getContainer();
        if (container) container.style.setProperty('z-index', '9999999', 'important');
      }
    });
  } catch (error: any) {
    console.error("Lỗi yêu thích sản phẩm:", error);
    await Swal.fire({ icon: 'error', title: 'Không thể xử lý yêu thích', text: getErrorMessage(error), confirmButtonColor: '#bd9a5f', didOpen: () => { const c = Swal.getContainer(); if(c) c.style.setProperty('z-index', '9999999', 'important'); } });
  } finally {
    isFavoriteLoading.value = false;
  }
};

const getCurrentCartQuantity = async (productVariantId: number) => {
  try {
    const res = await api.get("/v1/customer/cart/my-cart");
    const items = Array.isArray(res.data) ? res.data : [];
    const cartItem = items.find(
      (item: any) => Number(item.productVariantId) === Number(productVariantId)
    );
    return Number(cartItem?.quantity || 0);
  } catch (error) {
    console.error("Không kiểm tra được giỏ hàng hiện tại:", error);
    return 0;
  }
};

const validateBeforeCartAction = async () => {
  if (!selectedVariant.value) {
    await Swal.fire({ icon: 'warning', title: 'Chưa chọn dung tích', text: 'Vui lòng chọn dung tích trước khi mua hàng.', confirmButtonColor: '#bd9a5f', didOpen: () => { const c = Swal.getContainer(); if(c) c.style.setProperty('z-index', '9999999', 'important'); } });
    return false;
  }
  const variantId = getVariantId();
  if (!variantId || Number.isNaN(variantId)) {
    await Swal.fire({ icon: 'error', title: 'Biến thể không hợp lệ', text: 'Không xác định được biến thể sản phẩm. Vui lòng tải lại trang.', confirmButtonColor: '#bd9a5f', didOpen: () => { const c = Swal.getContainer(); if(c) c.style.setProperty('z-index', '9999999', 'important'); } });
    return false;
  }
  if (isVariantInvalidPrice.value) {
    await Swal.fire({ icon: 'warning', title: 'Sản phẩm chưa có giá', text: 'Sản phẩm chưa có giá bán. Vui lòng liên hệ cửa hàng.', confirmButtonColor: '#bd9a5f', didOpen: () => { const c = Swal.getContainer(); if(c) c.style.setProperty('z-index', '9999999', 'important'); } });
    return false;
  }
  if (isVariantOutOfStock.value) {
    await Swal.fire({ icon: 'warning', title: 'Tạm hết hàng', text: 'Sản phẩm này hiện đã hết hàng.', confirmButtonColor: '#bd9a5f', didOpen: () => { const c = Swal.getContainer(); if(c) c.style.setProperty('z-index', '9999999', 'important'); } });
    return false;
  }
  if (!hasToken()) {
    await askLogin("Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng.");
    return false;
  }
  if (!isCustomerLoggedIn()) {
    await Swal.fire({ icon: 'warning', title: 'Không thể mua hàng', text: 'Chỉ tài khoản khách hàng mới được thêm sản phẩm vào giỏ hàng.', confirmButtonColor: '#bd9a5f', didOpen: () => { const c = Swal.getContainer(); if(c) c.style.setProperty('z-index', '9999999', 'important'); } });
    return false;
  }
  const stockQuantity = normalizeStock(selectedVariant.value);
  const quantityToAdd = Number(quantity.value || 1);
  if (quantityToAdd <= 0) {
    await Swal.fire({ icon: 'warning', title: 'Số lượng không hợp lệ', text: 'Số lượng phải lớn hơn 0.', confirmButtonColor: '#bd9a5f', didOpen: () => { const c = Swal.getContainer(); if(c) c.style.setProperty('z-index', '9999999', 'important'); } });
    return false;
  }
  if (quantityToAdd > stockQuantity) {
    await Swal.fire({ icon: 'warning', title: 'Vượt quá tồn kho', text: `Sản phẩm chỉ còn ${stockQuantity} trong kho.`, confirmButtonColor: '#bd9a5f', didOpen: () => { const c = Swal.getContainer(); if(c) c.style.setProperty('z-index', '9999999', 'important'); } });
    return false;
  }
  const currentCartQuantity = await getCurrentCartQuantity(variantId);
  const totalAfterAdd = currentCartQuantity + quantityToAdd;
  if (totalAfterAdd > 10) {
    await Swal.fire({ icon: 'warning', title: 'Vượt quá giới hạn', html: `Bạn chỉ có thể mua tối đa <b>10</b> sản phẩm cho mỗi phân loại.<br/>Trong giỏ hàng của bạn hiện đã có <b>${currentCartQuantity}</b> sản phẩm.`, confirmButtonColor: '#bd9a5f', didOpen: () => { const c = Swal.getContainer(); if(c) c.style.setProperty('z-index', '9999999', 'important'); } });
    return false;
  }
  if (totalAfterAdd > stockQuantity) {
    await Swal.fire({ icon: 'warning', title: 'Vượt quá tồn kho', html: `Sản phẩm này chỉ còn <b>${stockQuantity}</b> trong kho.<br/>Trong giỏ hàng của bạn hiện đã có <b>${currentCartQuantity}</b> sản phẩm.<br/>Bạn chỉ có thể thêm tối đa <b>${Math.max(stockQuantity - currentCartQuantity, 0)}</b> sản phẩm nữa.`, confirmButtonColor: '#bd9a5f', didOpen: () => { const c = Swal.getContainer(); if(c) c.style.setProperty('z-index', '9999999', 'important'); } });
    return false;
  }
  return true;
};

const addToCart = async () => {
  const valid = await validateBeforeCartAction();
  if (!valid) {
    return;
  }
  try {
    isAdding.value = true;
    const variantId = getVariantId();
    const quantityToAdd = Number(quantity.value || 1);
    await api.post("/v1/customer/cart/add", {
      productVariantId: variantId,
      quantity: quantityToAdd,
    });
    lastAddedQuantity.value = quantityToAdd;
    window.dispatchEvent(new Event("cart-updated"));
    showToast.value = true;
    window.setTimeout(() => {
      showToast.value = false;
    }, 3000);
  } catch (error: any) {
    console.error("Lỗi khi thêm vào giỏ hàng:", error);
    await Swal.fire({ icon: 'error', title: 'Không thể thêm vào giỏ', text: getErrorMessage(error), confirmButtonColor: '#bd9a5f', didOpen: () => { const c = Swal.getContainer(); if(c) c.style.setProperty('z-index', '9999999', 'important'); } });
  } finally {
    isAdding.value = false;
  }
};

const buyNow = async () => {
  const valid = await validateBeforeCartAction();
  if (!valid) {
    return;
  }
  try {
    isAdding.value = true;
    const variantId = getVariantId();
    const quantityToAdd = Number(quantity.value || 1);
    await api.post("/v1/customer/cart/add", {
      productVariantId: variantId,
      quantity: quantityToAdd,
    });
    lastAddedQuantity.value = quantityToAdd;
    window.dispatchEvent(new Event("cart-updated"));
    emit("buy-now");
  } catch (error: any) {
    console.error("Lỗi khi xử lý Mua ngay:", error);
    await Swal.fire({ icon: 'error', title: 'Không thể mua ngay', text: getErrorMessage(error), confirmButtonColor: '#bd9a5f', didOpen: () => { const c = Swal.getContainer(); if(c) c.style.setProperty('z-index', '9999999', 'important'); } });
  } finally {
    isAdding.value = false;
  }
};

const goToCart = () => {
  router.push("/cart");
};

// ==========================================
// CÁC HÀM SO SÁNH VÀ AI SO SÁNH
// ==========================================
type CompareInsight = {
  productId: number;
  longevity?: string;
  style?: string;
  occasion?: string;
};

const MAX_COMPARE = 3;
const compareList = ref<any[]>([]);
const showCompareModal = ref(false);
const showPickerModal = ref(false);
const pickerLoading = ref(false);
const pickerSearchKeyword = ref("");
const allProductsStore = ref<any[]>([]);
const compareVariantIds = ref<Record<number, number>>({});

const compareInsights = ref<Record<number, CompareInsight>>({});
const compareAiLoading = ref(false);
const compareAnalysis = ref("");
const compareRecommendation = ref("");
const compareAiError = ref("");

const resetCompareAiResult = () => {
  compareInsights.value = {};
  compareAnalysis.value = "";
  compareRecommendation.value = "";
  compareAiError.value = "";
};

const isInCompare = (item: any) => { const id = item?.id || item?.productId; return compareList.value.some((p: any) => (p.id || p.productId) === id); };

const clearCompareList = () => {
  compareList.value = [];
  compareVariantIds.value = {};
  resetCompareAiResult();
};

const toggleCompare = (item: any) => {
  if(!item) return;
  const id = item.id || item.productId;
  if (isInCompare(item)) { 
    compareList.value = compareList.value.filter((p: any) => (p.id || p.productId) !== id); 
    delete compareVariantIds.value[id]; 
    resetCompareAiResult();
  }
  else {
    if (compareList.value.length >= MAX_COMPARE) { Swal.fire({ icon: 'warning', title: 'Giới hạn', text: `Chỉ được so sánh tối đa ${MAX_COMPARE} sản phẩm!`, confirmButtonColor: '#bd9a5f', didOpen: () => { const c = Swal.getContainer(); if(c) c.style.setProperty('z-index', '9999999', 'important'); } }); return; }
    compareList.value.push(item);
    resetCompareAiResult();
  }
};

const removeFromCompare = (item: any) => { 
  const id = item?.id || item?.productId; 
  compareList.value = compareList.value.filter((p: any) => (p.id || p.productId) !== id); 
  delete compareVariantIds.value[id]; 
  resetCompareAiResult();
};

const openPickerModal = async () => {
  showPickerModal.value = true;
  if (allProductsStore.value.length === 0) {
    pickerLoading.value = true;
    try {
      const res = await api.get("/v1/products", { params: { size: 100, page: 0 } });
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

const getBrandName = (item: any) => {
  if (typeof item?.brand === "object") return item?.brand?.name || "Premium";
  return item?.brandName || item?.brand || "Premium";
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

const toggleItemInPicker = (item: any) => {
  if (isInCompare(item)) removeFromCompare(item);
  else toggleCompare(item);
};

const goToDetailFromCompare = (p: any) => {
  showCompareModal.value = false;
  const id = p.id || p.productId;
  if(id) {
      router.push({ name: "SingleProduct", params: { id: id } });
  }
};

const getPrimaryVariantObject = (item: any) => {
  if (!item) return null;
  if (Array.isArray(item?.variants) && item.variants.length > 0) {
    const activeVariants = item.variants.filter((variant: any) => Number(variant?.status ?? variant?.variantStatus ?? 1) === 1);
    const sellableVariant = activeVariants.find((variant: any) => isVariantSellable(variant));
    if (sellableVariant) return sellableVariant;
    return activeVariants[0] || item.variants[0];
  }
  return item;
};

const getSafeNumber = (val: any) => {
  if (val === null || val === undefined || val === '') return 0;
  if (typeof val === 'number') return isNaN(val) ? 0 : val;
  if (typeof val === 'string') {
    const cleanStr = val.replace(/[^\d]/g, '');
    return parseInt(cleanStr, 10) || 0;
  }
  return 0;
};

const getProductPrices = (item: any) => {
  if (!item) return { sale: 0, orig: 0 };
  
  const basePrice = getSafeNumber(item.price ?? item.Price ?? item.basePrice);
  const flashSale = getSafeNumber(item.flashSalePrice ?? item.salePrice ?? item.promotionPrice ?? item.discountPrice ?? item.specialPrice ?? item.currentPrice);
  const explicitOrig = getSafeNumber(item.originalPrice ?? item.oldPrice ?? item.listPrice ?? item.regularPrice);
  const percent = getSafeNumber(item.discountPercent ?? item.discount ?? item.salePercent);

  let sale = 0;
  let orig = 0;

  if (flashSale > 0) {
    sale = flashSale;
    orig = explicitOrig > flashSale ? explicitOrig : (basePrice > flashSale ? basePrice : 0);
  } else if (basePrice > 0) {
    sale = basePrice;
    orig = explicitOrig > basePrice ? explicitOrig : 0;
  }

  if (orig === 0 && percent > 0 && sale > 0) {
    orig = Math.round(sale / (1 - percent / 100));
  }

  if (orig > 0 && sale > orig) {
    const temp = sale;
    sale = orig;
    orig = temp;
  }

  return { sale, orig };
};

const getCompareVariant = (p: any) => {
  const pId = p.id || p.productId;
  const vId = compareVariantIds.value[pId];
  if (vId && p.variants && Array.isArray(p.variants)) {
    const found = p.variants.find((v: any) => (v.productVariantId || v.variantId || v.id) === vId);
    if (found) return found;
  }
  return getPrimaryVariantObject(p);
};

const getCompareBottleType = (p: any) => {
  const v = getCompareVariant(p);
  if (v) {
    const bottleCandidates = [ v.bottleTypeName, v.bottleType?.name, typeof v.bottleType === "string" ? v.bottleType : null ];
    let bottleString = bottleCandidates.find((b) => b != null && b !== "");
    return bottleString || "Đang cập nhật";
  }
  return "Đang cập nhật";
};

const getComparePrice = (p: any) => {
  const v = getCompareVariant(p);
  let vSale = Number(v?.salePrice ?? v?.promotionPrice ?? v?.flashSalePrice ?? v?.currentPrice ?? v?.price ?? v?.Price ?? 0);
  if (vSale > 0) return vSale;
  return Number(p?.salePrice ?? p?.promotionPrice ?? p?.flashSalePrice ?? p?.currentPrice ?? p?.price ?? p?.Price ?? p?.minPrice ?? p?.maxPrice ?? p?.basePrice ?? 0);
};

const getCompareOriginalPrice = (p: any) => {
  const v = getCompareVariant(p);
  const sale = getComparePrice(p);
  let vOrig = Number(v?.originalPrice ?? v?.oldPrice ?? v?.listPrice ?? v?.price ?? v?.Price ?? 0);
  if (vOrig > sale) return vOrig;
  let pOrig = Number(p?.originalPrice ?? p?.oldPrice ?? p?.listPrice ?? p?.price ?? p?.Price ?? p?.maxPrice ?? p?.minPrice ?? 0);
  return pOrig > sale ? pOrig : sale;
};

const getCompareStock = (p: any) => normalizeStock(getCompareVariant(p));

const getCompareDiscount = (p: any) => {
  const sale = getComparePrice(p); const orig = getCompareOriginalPrice(p);
  if (orig > sale && sale > 0) return Math.round(((orig - sale) / orig) * 100); return 0;
};
const isCompareBuyDisabled = (p: any) => getCompareStock(p) <= 0 || getComparePrice(p) <= 0;

const buyFromCompare = (p: any) => {
  const id = p.id || p.productId;
  showCompareModal.value = false;
  if(id) {
      router.push({ name: "SingleProduct", params: { id: id } });
  }
};

// Hàm xử lý Text an toàn cho Comparison
const toCompareText = (value: any): string => {
  if (value === null || value === undefined || value === "") return "";
  if (Array.isArray(value)) {
    return value.map((item: any) => toCompareText(item)).filter(Boolean).join(", ");
  }
  if (typeof value === "object") {
    return String(value?.name ?? value?.value ?? value?.label ?? value?.displayName ?? "").trim();
  }
  return String(value).trim();
};

const getGenderTextCompare = (item: any) => {
  const raw = item?.genderName ?? item?.gender;
  const g = toCompareText(raw);
  const normalized = g.toLowerCase();

  if (g === "1" || normalized === "nam" || normalized === "male") return "Nam";
  if (g === "2" || normalized === "nữ" || normalized === "nu" || normalized === "female") return "Nữ";
  if (g === "0" || normalized === "unisex") return "Unisex";

  return g || "Đang cập nhật";
};

const getAttributeTextCompare = (item: any, field: string) => {
  if (!item) return "Đang cập nhật";
  const candidates = [item?.[`${field}Name`], item?.[field]];
  for (const candidate of candidates) {
    const text = toCompareText(candidate);
    if (text) return text;
  }
  return "Đang cập nhật";
};

const getFragranceFamilyCompare = (item: any) => {
  if (!item) return "Đang cập nhật";
  const candidates = [
    item?.scents, item?.fragranceFamilies, item?.scentGroups, item?.scentGroup,
    item?.fragranceFamily, item?.fragranceFamilyName, item?.scent, item?.scentName, item?.mainScent, item?.mainScentName,
  ];
  for (const candidate of candidates) {
    const text = toCompareText(candidate);
    if (text) return text;
  }
  return "Đang cập nhật";
};

const getCompareInsight = (item: any): CompareInsight | null => {
  const productId = item?.id || item?.productId;
  if (!productId) return null;
  return compareInsights.value[productId] || null;
};

const getOccasionTextCompare = (item: any) => {
  const directCandidates = [item?.occasions, item?.occasionName, item?.occasion];
  for (const candidate of directCandidates) {
    const text = toCompareText(candidate);
    if (text) return text;
  }
  return getCompareInsight(item)?.occasion || "Chưa có dữ liệu";
};

const getStyleTextCompare = (item: any) => {
  const directCandidates = [item?.styles, item?.styleName, item?.style];
  for (const candidate of directCandidates) {
    const text = toCompareText(candidate);
    if (text) return text;
  }
  return getCompareInsight(item)?.style || "Chưa có dữ liệu";
};

const getRatingScoreCompare = (item: any) => {
  const raw = Number(item?.averageRating || item?.avgRating || item?.rating || 0);
  if (raw > 0) return Math.min(5, Math.max(0, raw)).toFixed(1);
  return (5.0).toFixed(1);
};

const getReviewCountCompare = (item: any) => Number(item?.reviewCount || item?.reviews || item?.totalReviews || 0);

const getCompareValue = (p: any, type: string) => {
  const v = getCompareVariant(p);
  if (type === 'brand') return getBrandName(p); 
  if (type === 'scent') return getFragranceFamilyCompare(p);
  if (type === 'concentration') {
    const productValue = getAttributeTextCompare(p, 'concentration');
    return productValue !== "Đang cập nhật" ? productValue : getAttributeTextCompare(v, 'concentration');
  }
  if (type === 'gender') {
    const productValue = getGenderTextCompare(p);
    return productValue !== "Đang cập nhật" ? productValue : getGenderTextCompare(v);
  }
  if (type === 'occasion') return getOccasionTextCompare(p);
  if (type === 'style') return getStyleTextCompare(p);
  return '';
};

const getPricePerMl = (p: any) => {
  const v = getCompareVariant(p);
  const price = getComparePrice(p);
  let cap = v?.capacityName || v?.capacityValue || v?.volume || v?.capacity?.value || v?.capacity?.name || p?.capacity || "";
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
  const validValues = compareList.value.map(item => getPricePerMl(item)).filter(v => v > 0);
  if (validValues.length === 0) return false;
  const minValue = Math.min(...validValues);
  return getPricePerMl(p) === minValue && minValue > 0;
};

const getLongevityDisplay = (p: any) => {
  const v = getCompareVariant(p);
  const actualLongevity = [
    p?.longevityName, p?.longevity, p?.lastingTime, p?.lastingDuration,
    v?.longevityName, v?.longevity, v?.lastingTime, v?.lastingDuration,
  ]
    .map((value) => toCompareText(value))
    .find(Boolean);

  if (actualLongevity) return actualLongevity;
  return getCompareInsight(p)?.longevity || "Chưa có dữ liệu";
};

const getProductImageCompare = (item: any) => {
  if(item?.primaryImageUrl) return getImageUrlFromObject(item.primaryImageUrl);
  if(item?.mainImage) return getImageUrlFromObject(item.mainImage);
  if(item?.imageUrl) return getImageUrlFromObject(item.imageUrl);
  if(Array.isArray(item?.images) && item.images.length > 0) {
    const prim = item.images.find((i:any) => i?.isPrimary);
    if(prim) return getImageUrlFromObject(prim.imageUrl || prim);
    return getImageUrlFromObject(item.images[0].imageUrl || item.images[0]);
  }
  return getPlaceholderImage();
};

const formatVariantNameCompare = (v: any) => {
  if (!v) return "Loại";
  const capCandidates = [ v.capacityName, v.capacityValue, v.volume, v.capacity?.value, v.capacity?.name, typeof v.capacity === "string" || typeof v.capacity === "number" ? v.capacity : null ];
  let cap = capCandidates.find((c) => c != null && c !== "");
  let capString = "";
  if (cap != null) {
    const text = String(cap).trim();
    const numeric = parseFloat(text.toLowerCase().replace("ml", ""));
    if (!Number.isNaN(numeric) && numeric > 0) capString = `${numeric}ml`;
    else capString = text.toLowerCase().includes("ml") ? text : `${text}ml`;
  }
  const bottleCandidates = [ v.bottleTypeName, v.bottleType?.name, typeof v.bottleType === "string" ? v.bottleType : null ];
  let bottleString = bottleCandidates.find((b) => b != null && b !== "");
  if (capString && bottleString) return `${capString} - ${bottleString}`;
  if (capString) return capString;
  if (bottleString) return bottleString;
  return "Loại " + (v.productVariantId || v.variantId || v.id || "");
};

const getSortedVariantsCompare = (p: any) => {
  if (!p || !p.variants || !Array.isArray(p.variants)) return [];
  return [...p.variants].sort((a: any, b: any) => {
    const getCap = (v: any) => {
      let cap = v.capacityName || v.capacityValue || v.volume || v.capacity?.value || v.capacity?.name || v.capacity;
      const text = String(cap || "").toLowerCase().replace(/\s+/g, "");
      const numeric = parseFloat(text.replace("ml", ""));
      return Number.isNaN(numeric) ? 0 : numeric;
    };
    return getCap(a) - getCap(b);
  });
};

const getProductIdNum = (item: any) => Number(item?.id || item?.productId || 0);

const getCompareProductIds = () => {
  return Array.from(
    new Set(
      compareList.value
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
    const res = await api.post("/v1/products/compare/ai", { productIds });
    const data = res?.data?.data ?? res?.data;
    const rows = Array.isArray(data?.insights) ? data.insights : [];
    const map: Record<number, CompareInsight> = {};

    rows.forEach((row: any) => {
      const productId = Number(row?.productId);
      if (!Number.isFinite(productId) || productId <= 0) return;
      map[productId] = {
        productId,
        longevity: String(row?.longevity || "").trim() || "Chưa có dữ liệu",
        style: String(row?.style || "").trim() || "Chưa có dữ liệu",
        occasion: String(row?.occasion || "").trim() || "Chưa có dữ liệu",
      };
    });

    compareInsights.value = map;
    compareAnalysis.value = String(data?.analysis || "").trim();
    compareRecommendation.value = String(data?.recommendation || "").trim();
    compareAiError.value = "";
    return true;
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || "Không thể phân tích sản phẩm bằng AI lúc này.";
    console.warn("Không lấy được kết quả so sánh AI:", message);
    compareInsights.value = {};
    compareAnalysis.value = "";
    compareRecommendation.value = "";
    compareAiError.value = String(message);
    return false;
  }
};

const handleAiCompare = async () => {
  if (compareAiLoading.value) return;
  if (!hasToken()) {
    await askLogin("Vui lòng đăng nhập để sử dụng tính năng AI.");
    return;
  }

  const productIds = getCompareProductIds();
  if (productIds.length < 2 || productIds.length > 3) {
    await Swal.fire({ icon: 'warning', title: 'Chưa đủ sản phẩm', text: 'Vui lòng chọn từ 2 đến 3 sản phẩm để so sánh bằng AI.', confirmButtonColor: '#bd9a5f', didOpen: () => { const c = Swal.getContainer(); if(c) c.style.setProperty('z-index', '9999999', 'important'); } });
    return;
  }

  compareAiLoading.value = true;
  compareAiError.value = "";

  try {
    await loadStructuredCompareInsights();
  } finally {
    compareAiLoading.value = false;
  }
};

watch(showCompareModal, async (val) => {
  if(!val) { resetCompareAiResult(); } 
  if (val) {
    compareList.value.forEach((p) => {
      const id = getProductIdNum(p);
      if (!compareVariantIds.value[id]) {
        const primaryV = getPrimaryVariantObject(p);
        compareVariantIds.value[id] = Number(primaryV?.productVariantId ?? primaryV?.variantId ?? primaryV?.id ?? 0);
      }
    });
    const updatedList = [...compareList.value];
    for (let i = 0; i < updatedList.length; i++) {
      const p = updatedList[i];
      const id = getProductIdNum(p);

      const variantSaleMap = new Map<number, number>();
      const variantOrigMap = new Map<number, number>();
      if (p.variants && Array.isArray(p.variants)) {
        p.variants.forEach((pv: any) => {
          const vId = Number(pv.productVariantId || pv.variantId || pv.id);
          if (vId) {
            const prices = getProductPrices(pv);
            if (prices.sale > 0) variantSaleMap.set(vId, prices.sale);
            if (prices.orig > 0) variantOrigMap.set(vId, prices.orig);
          }
        });
      }

      try {
        const res = await api.get(`/v1/products/${id}`);
        const fullData = res.data?.data || res.data;
        if (fullData) {
          let rawVariants = fullData.variants || fullData.productVariants || fullData.productVariantList;
          if (rawVariants && Array.isArray(rawVariants)) {
            fullData.variants = rawVariants.map((v: any) => {
              const vId = Number(v.productVariantId || v.variantId || v.id);
              const prices = getProductPrices(v);
              return { 
                ...v, 
                salePrice: variantSaleMap.get(vId) ?? (prices.sale > 0 ? prices.sale : v.salePrice), 
                originalPrice: variantOrigMap.get(vId) ?? (prices.orig > 0 ? prices.orig : v.originalPrice) 
              };
            });
          }
          updatedList[i] = { 
            ...fullData,
            ...p,
            flashSalePrice: p.flashSalePrice ?? fullData.flashSalePrice,
            salePrice: p.salePrice ?? fullData.salePrice,
            discountPercent: p.discountPercent ?? fullData.discountPercent,
            originalPrice: p.originalPrice ?? fullData.originalPrice,
            oldPrice: p.oldPrice ?? fullData.oldPrice,
            variants: fullData.variants || p.variants
          };
        }
      } catch (e) {}
    }
    compareList.value = updatedList;
    resetCompareAiResult();
  }
});

watch(
  () => props.product,

  (newProduct) => {
    if (newProduct) {
      window.scrollTo({
        top: 0,

        behavior: "smooth",
      });
    }

    reviewSummary.value = null;

    isFavorited.value = false;

    if (
      newProduct &&
      Array.isArray(newProduct.variants) &&
      newProduct.variants.length > 0
    ) {
      selectedVariant.value =
        newProduct.variants.find((variant: any) =>
          isVariantSellable(variant)
        ) || newProduct.variants[0];

      quantity.value = 1;
    } else {
      selectedVariant.value = null;

      quantity.value = 1;
    }

    let primaryUrl = "";

    if (newProduct?.primaryImageUrl) {
      primaryUrl = getImageUrlFromObject(newProduct.primaryImageUrl);
    } else if (Array.isArray(newProduct?.images)) {
      const primaryObj = newProduct.images.find((img: any) =>
        Boolean(img?.isPrimary)
      );

      if (primaryObj) {
        primaryUrl = getImageUrlFromObject(primaryObj?.imageUrl || primaryObj);
      } else if (newProduct.images.length > 0) {
        primaryUrl = getImageUrlFromObject(
          newProduct.images[0]?.imageUrl || newProduct.images[0]
        );
      }
    }

    selectedImageUrl.value = primaryUrl || productGalleryImages.value[0] || "";
  },

  {
    immediate: true,
  }
);

watch(
  () => getVariantId(),

  () => {
    loadFavoriteStatus();
  },

  {
    immediate: true,
  }
);
</script>

<style scoped>
.detail-view-container {
  display: flex;

  flex-direction: column;

  width: 100%;
}

.breadcrumb {
  font-size: 13px;

  color: #718096;

  margin-bottom: 25px;

  display: flex;

  align-items: center;

  gap: 12px;
}

.back-link {
  font-weight: 600;

  color: #0a142f;

  cursor: pointer;

  transition: 0.2s;

  display: flex;

  align-items: center;
}

.back-link:hover {
  color: #c69c6d;
}

.breadcrumb-item.clickable {
  cursor: pointer;

  color: #4a5568;

  font-weight: 500;

  transition: color 0.2s;
}

.breadcrumb-item.clickable:hover {
  color: #c69c6d;

  text-decoration: underline;
}

.divider {
  color: #cbd5e0;
}

.breadcrumb .active {
  color: #c69c6d;

  font-weight: 500;
}

.product-content {
  display: flex;

  gap: 50px;

  background: #ffffff;

  padding: 40px;

  border-radius: 16px;

  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
}

.product-gallery {
  flex: 1;

  max-width: 480px;
}

.main-image-wrapper {
  background: #f8f9fa;

  border-radius: 16px;

  overflow: hidden;

  position: relative;

  margin-bottom: 15px;

  aspect-ratio: 1 / 1;

  display: flex;

  align-items: center;

  justify-content: center;

  border: 1px solid #eaeaea;
}

.main-image {
  width: 100%;

  height: 100%;

  object-fit: cover;
}

.btn-heart {
  position: absolute;

  top: 15px;

  right: 15px;

  background: #ffffff;

  border: 1px solid #eaeaea;

  width: 44px;

  height: 44px;

  border-radius: 50%;

  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);

  font-size: 22px;

  cursor: pointer;

  color: #718096;

  display: flex;

  align-items: center;

  justify-content: center;

  transition: 0.2s;

  z-index: 2;
}

.btn-heart:hover {
  color: #e53e3e;

  border-color: #e53e3e;
}

.btn-heart.active {
  color: #e53e3e;

  border-color: #e53e3e;

  background: #fff5f5;
}

.btn-heart:disabled {
  opacity: 0.65;

  cursor: not-allowed;
}

.btn-compare {
  position: absolute;
  top: 70px;
  right: 15px;
  background: #ffffff;
  border: 1px solid #eaeaea;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  font-size: 20px;
  cursor: pointer;
  color: #718096;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: 0.2s;
  z-index: 2;
}

.btn-compare:hover {
  color: #c69c6d;
  border-color: #c69c6d;
}

.btn-compare.active {
  color: #c69c6d;
  border-color: #c69c6d;
  background: #fdfaf6;
}

.btn-compare svg {
  width: 22px;
  height: 22px;
}

.thumbnail-list {
  display: flex;

  gap: 15px;

  overflow-x: auto;

  padding-bottom: 4px;
}

.thumb {
  width: calc(25% - 11.25px);

  min-width: 92px;

  border-radius: 8px;

  border: 1px solid #eaeaea;

  cursor: pointer;

  padding: 5px;

  background: #f8f9fa;

  transition: 0.2s;

  aspect-ratio: 1 / 1;

  display: flex;

  align-items: center;

  justify-content: center;
}

.thumb img {
  max-width: 100%;

  max-height: 100%;

  object-fit: contain;

  mix-blend-mode: multiply;

  pointer-events: none;
}

.thumb.active {
  border: 2px solid #0a142f;
}

.thumb:hover {
  border-color: #c69c6d;
}

.product-info {
  flex: 1.2;
}

.header-info {
  display: flex;

  justify-content: space-between;

  align-items: center;

  margin-bottom: 5px;
}

.brand {
  color: #c69c6d;

  font-weight: 600;

  font-size: 16px;
}

.title {
  font-size: 32px;

  font-weight: 700;

  margin: 0 0 15px 0;

  color: #0a142f;

  letter-spacing: -0.5px;
}

.rating {
  font-size: 14px;

  color: #718096;

  margin-bottom: 25px;

  display: flex;

  align-items: center;

  gap: 10px;
}

.rating .stars {
  display: inline-flex;

  align-items: center;

  gap: 2px;

  color: #ecc94b;
}

.rating .stars i {
  font-size: 14px;
}

.score {
  font-weight: bold;

  color: #0a142f;
}

.divider-line {
  color: #cbd5e0;
}

.price-box {
  margin-bottom: 5px;

  display: flex;

  align-items: baseline;

  gap: 15px;

  flex-wrap: wrap;
}

.current-price {
  font-size: 28px;

  font-weight: bold;

  color: #0a142f;
}

.old-price {
  font-size: 16px;

  color: #a0aec0;

  text-decoration: line-through;

  border-left: 1px solid #cbd5e0;

  padding-left: 15px;
}

.save-badge {
  color: #c69c6d;

  font-size: 13px;

  font-weight: 500;

  margin-bottom: 25px;
}

.flash-sale-badge {
  display: inline-flex;

  align-items: center;

  justify-content: center;

  background: #dc2626;

  color: #ffffff;

  border-radius: 999px;

  padding: 5px 10px;

  font-size: 13px;

  font-weight: 800;
}

.desc-divider {
  height: 1px;

  background: #eaeaea;

  margin: 25px 0;
}

.desc {
  color: #4a5568;

  line-height: 1.6;

  font-size: 14px;

  white-space: pre-line;
}

.product-specs {
  margin: 24px 0;

  padding: 20px;

  border: 1px solid #eaeaea;

  border-radius: 14px;

  background: #fafafa;
}

.product-specs h4 {
  margin: 0 0 16px;

  font-size: 15px;

  font-weight: 700;

  color: #0a142f;

  text-transform: uppercase;

  letter-spacing: 0.5px;
}

.spec-grid {
  display: grid;

  grid-template-columns: repeat(2, 1fr);

  gap: 14px 20px;
}

.spec-item {
  display: flex;

  flex-direction: column;

  gap: 4px;

  min-width: 0;
}

.spec-item span {
  font-size: 12px;

  color: #718096;
}

.spec-item strong {
  font-size: 14px;

  color: #0a142f;

  word-break: break-word;
}

.text-danger {
  color: #dc2626 !important;
}

.text-success {
  color: #16a34a !important;
}

.variant-selection {
  margin-bottom: 30px;
}

.variant-selection h4,
.quantity-section h4 {
  font-size: 14px;

  font-weight: 600;

  color: #0a142f;

  margin: 0 0 15px 0;

  text-transform: uppercase;

  letter-spacing: 0.5px;
}

.capacity-options {
  display: flex;

  flex-wrap: wrap;

  gap: 12px;
}

.cap-btn {
  flex: 0 0 auto;

  min-width: 80px;

  padding: 10px 15px;

  border: 1px solid #cbd5e0;

  background: #ffffff;

  border-radius: 6px;

  cursor: pointer;

  position: relative;

  font-size: 14px;

  font-weight: 600;

  color: #4a5568;

  transition: all 0.2s ease;

  text-align: center;
}

.cap-btn:hover {
  border-color: #c69c6d;

  color: #c69c6d;
}
.cap-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  background: #f5f5f5;
  color: #9ca3af;
  border-color: #e5e7eb;
}

.cap-btn:disabled:hover {
  border-color: #e5e7eb;
  color: #9ca3af;
}
.cap-btn.active {
  border-color: #0a142f;

  background: #0a142f;

  color: #ffffff;
}

.variant-sale-chip {
  display: block;

  margin-top: 4px;

  font-size: 10px;

  font-weight: 800;

  color: #dc2626;
}

.cap-btn.active .variant-sale-chip {
  color: #ffffff;
}

.check-icon {
  position: absolute;

  top: -6px;

  right: -6px;

  background: #c69c6d;

  color: #ffffff;

  width: 18px;

  height: 18px;

  border-radius: 50%;

  font-size: 10px;

  display: flex;

  align-items: center;

  justify-content: center;

  border: 2px solid #ffffff;

  font-weight: bold;
}

.stock-info {
  color: #718096;

  font-size: 13px;

  margin-bottom: 10px;
}

.stock-info strong {
  color: #0a142f;
}

.qty-control {
  display: inline-flex;

  border: 1px solid #cbd5e0;

  border-radius: 8px;

  margin-bottom: 40px;

  overflow: hidden;
}

.qty-control button {
  width: 45px;

  height: 45px;

  border: none;

  background: #ffffff;

  cursor: pointer;

  font-size: 18px;

  color: #4a5568;
}

.qty-control button:disabled {
  color: #cbd5e0;

  cursor: not-allowed;
}

.qty-control input {
  width: 60px;

  text-align: center;

  border: none;

  outline: none;

  font-weight: 600;

  font-size: 15px;

  border-left: 1px solid #cbd5e0;

  border-right: 1px solid #cbd5e0;
}

.actions {
  display: flex;

  gap: 15px;

  margin-bottom: 40px;
}

.btn-add-cart,
.btn-buy-now {
  flex: 1;

  padding: 16px;

  border-radius: 8px;

  font-weight: bold;

  cursor: pointer;

  border: none;

  transition: 0.2s;

  display: flex;

  justify-content: center;

  align-items: center;

  gap: 10px;

  font-size: 14px;
}

.btn-add-cart {
  background: #0a142f;

  color: #ffffff;
}

.btn-add-cart:hover:not(:disabled) {
  background: #13275a;
}

.btn-add-cart:disabled,
.btn-buy-now:disabled {
  background: #718096;

  cursor: not-allowed;
}

.btn-buy-now {
  background: #b78d52;

  color: #ffffff;
}

.btn-buy-now:hover:not(:disabled) {
  background: #c69c6d;
}

.btn-icon,
.btn-icon-right {
  width: 18px;

  height: 18px;
}

.policy-footer {
  display: flex;

  justify-content: space-between;

  border-top: 1px solid #eaeaea;

  padding-top: 25px;
}

.policy-item {
  display: flex;

  align-items: flex-start;

  gap: 12px;

  font-size: 12px;

  color: #718096;

  line-height: 1.5;
}

.icon-policy {
  width: 24px;

  height: 24px;

  color: #b78d52;

  flex-shrink: 0;
}

.policy-item strong {
  color: #4a5568;

  font-size: 13px;

  font-weight: 600;
}

.luxury-toast {
  position: fixed;

  bottom: 40px;

  right: 40px;

  background: #0a142f;

  color: #ffffff;

  padding: 16px 24px;

  border-radius: 12px;

  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);

  display: flex;

  align-items: center;

  justify-content: space-between;

  gap: 30px;

  min-width: 380px;

  transform: translateY(100px);

  opacity: 0;

  visibility: hidden;

  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);

  z-index: 1000;

  border: 1px solid rgba(198, 156, 109, 0.3);
}

.luxury-toast.show {
  transform: translateY(0);

  opacity: 1;

  visibility: visible;
}

.toast-content {
  display: flex;

  align-items: center;

  gap: 15px;
}

.icon-circle-toast {
  width: 36px;

  height: 36px;

  border-radius: 50%;

  background: rgba(198, 156, 109, 0.15);

  display: flex;

  align-items: center;

  justify-content: center;

  color: #c69c6d;
}

.icon-circle-toast svg {
  width: 20px;

  height: 20px;
}

.toast-text {
  display: flex;

  flex-direction: column;
}

.toast-text strong {
  font-size: 15px;

  margin-bottom: 2px;

  letter-spacing: 0.5px;
}

.toast-text span {
  font-size: 13px;

  color: #a0aec0;
}

.toast-action {
  background: transparent;

  border: none;

  color: #c69c6d;

  font-weight: bold;

  font-size: 13px;

  cursor: pointer;

  letter-spacing: 0.5px;

  text-transform: uppercase;

  padding: 0;

  transition: 0.2s;
}

.toast-action:hover {
  color: #e8c499;

  text-decoration: underline;
}

@media (max-width: 992px) {
  .product-content {
    flex-direction: column;

    padding: 24px;
  }

  .product-gallery {
    max-width: 100%;
  }

  .actions,
  .policy-footer {
    flex-direction: column;
  }

  .luxury-toast {
    left: 16px;

    right: 16px;

    bottom: 20px;

    min-width: auto;
  }
}

@media (max-width: 576px) {
  .spec-grid {
    grid-template-columns: 1fr;
  }

  .title {
    font-size: 26px;
  }

  .current-price {
    font-size: 24px;
  }
}

/* Ẩn mũi tên tăng giảm mặc định của trình duyệt */

.qty-control input[type="number"]::-webkit-inner-spin-button,
.qty-control input[type="number"]::-webkit-outer-spin-button {
  -webkit-appearance: none;

  margin: 0;
}

.qty-control input[type="number"] {
  appearance: textfield;

  -moz-appearance: textfield;
}

/* === CSS CHO SO SÁNH SẢN PHẨM === */
.compare-bar { position: fixed; bottom: 0; left: 0; width: 100%; background: #ffffff; box-shadow: 0 -4px 20px rgba(0,0,0,0.1); transform: translateY(100%); transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1); z-index: 1040; padding: 15px 0; border-top: 2px solid #bd9a5f; }
.compare-bar.show { transform: translateY(0); }
.cb-container { max-width: 1200px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center; padding: 0 20px; }
.cb-left { display: flex; align-items: center; gap: 24px; flex-wrap: wrap; }
.cb-title { font-weight: 800; font-size: 15px; color: #0a142f; text-transform: uppercase; letter-spacing: 0.5px; }
.cb-slots { display: flex; gap: 15px; }
.cb-slot { width: 230px; height: 60px; border: 1px dashed #cbd5e0; border-radius: 8px; display: flex; align-items: center; padding: 5px; gap: 10px; background: #f8fafc; position: relative; }
.cb-slot.filled { border-style: solid; border-color: #eaeaea; background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.cb-slot img { width: 48px; height: 48px; object-fit: cover; border-radius: 6px; }
.cb-slot-info { flex: 1; overflow: hidden; }
.cb-slot-info p { margin: 0; font-size: 13px; font-weight: 700; color: #0a142f; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.cb-slot-info span { font-size: 12px; color: #bd9a5f; font-weight: 600; }
.btn-remove-cb { position: absolute; top: -8px; right: -8px; background: #e53e3e; color: white; border: none; border-radius: 50%; width: 22px; height: 22px; font-size: 12px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: 0.2s; box-shadow: 0 2px 5px rgba(229,62,62,0.3); }
.btn-remove-cb:hover { transform: scale(1.1); }
.cb-slot.empty-slot { justify-content: center; color: #a0aec0; font-size: 13px; cursor: pointer; transition: 0.2s; }
.cb-slot.empty-slot:hover { color: #bd9a5f; border-color: #bd9a5f; background: #fdfaf6; }
.cb-right { display: flex; gap: 15px; align-items: center; }
.cb-btn-clear { background: transparent; border: none; color: #718096; font-weight: 600; cursor: pointer; font-size: 14px; transition: 0.2s; }
.cb-btn-clear:hover { color: #e53e3e; text-decoration: underline; }
.cb-btn-compare { background: #0a142f; color: white; border: none; padding: 12px 28px; border-radius: 8px; font-weight: 700; text-transform: uppercase; cursor: pointer; transition: 0.2s; letter-spacing: 0.5px; }
.cb-btn-compare:disabled { opacity: 0.5; cursor: not-allowed; }
.cb-btn-compare:hover:not(:disabled) { background: #bd9a5f; box-shadow: 0 4px 12px rgba(189,154,95,0.3); }

/* COMPARE MODAL */
.compare-modal-overlay { 
  position: fixed; 
  inset: 0; 
  background: rgba(0,0,0,0.7); 
  z-index: 1050; 
  display: flex; 
  align-items: center; 
  justify-content: center; 
  backdrop-filter: blur(6px); 
}
.compare-modal-box { background: white; width: 95%; max-width: 1100px; max-height: 90vh; border-radius: 16px; display: flex; flex-direction: column; overflow: hidden; animation: modalFadeIn 0.3s ease; box-shadow: 0 20px 60px rgba(0,0,0,0.2); }
.cm-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid #eaeaea; background: #fdfaf6; }
.cm-header h3 { margin: 0; font-family: "Playfair Display", serif; font-size: 22px; font-weight: 800; color: #0a142f; }
.cm-close { background: none; border: none; font-size: 24px; cursor: pointer; color: #a0aec0; transition: 0.2s; padding: 0; line-height: 1; }
.cm-close:hover { color: #e53e3e; transform: rotate(90deg); }
.cm-body { padding: 0; overflow-y: auto; }

.ai-compare-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 24px;
  background: #fffdf8;
  border-bottom: 1px solid #eee3d3;
}
.ai-compare-toolbar-text { min-width: 0; }
.ai-compare-title { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; color: #0a142f; font-size: 16px; font-weight: 800; }
.ai-compare-title i { color: #b78d52; }
.ai-compare-subtitle { color: #718096; font-size: 13px; line-height: 1.5; }

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
.cm-btn-ai:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 14px rgba(183, 141, 82, 0.24); }
.cm-btn-ai:disabled { opacity: 0.6; cursor: not-allowed; }

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
.ai-result-block { padding: 15px 16px; }
.ai-result-block + .ai-result-block { border-top: 1px solid #eadfcf; }
.ai-result-label { margin-bottom: 6px; color: #0a142f; font-size: 14px; font-weight: 800; }
.ai-result-label i { color: #b78d52; }
.ai-result-block p { margin: 0; color: #4a5568; font-size: 14px; line-height: 1.65; }
.ai-result-recommendation { background: #fffcf7; }

@media (max-width: 768px) {
  .ai-compare-toolbar { align-items: stretch; flex-direction: column; }
  .cm-btn-ai { width: 100%; }
}

.table-compare { width: 100%; border-collapse: collapse; text-align: left; }
.sticky-header th, .sticky-header td { position: sticky; top: 0; background: white; z-index: 10; border-bottom: 2px solid #eaeaea; padding-top: 25px; padding-bottom: 20px; }
.table-compare th, .table-compare td { padding: 18px 24px; border-bottom: 1px solid #eaeaea; vertical-align: middle; }

.spec-label-col { width: 18%; background: #f8fafc; font-weight: 700; color: #4a5568; font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px; border-right: 1px solid #eaeaea; }
.spec-value-col { width: 27.33%; font-size: 15px; color: #0a142f; text-align: center; position: relative; }
.spec-value-col:not(:last-child) { border-right: 1px solid #eaeaea; }

.cm-img-wrapper { text-align: center; margin-bottom: 15px; cursor: pointer; display: flex; justify-content: center; }
.cm-img-wrapper img { width: 140px; height: 140px; object-fit: cover; border-radius: 12px; border: 1px solid #eaeaea; box-shadow: 0 4px 15px rgba(0,0,0,0.04); transition: transform 0.2s; }
.cm-img-wrapper:hover img { transform: scale(1.05); }

.cm-name { font-family: "Playfair Display", serif; font-size: 18px; font-weight: 800; color: #0a142f; margin: 0 0 5px; text-align: center; line-height: 1.3; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; cursor: pointer; transition: color 0.2s; }
.cm-name:hover { color: #bd9a5f; }

.btn-remove-from-table { position: absolute; top: 10px; right: 10px; background: #e53e3e; color: white; border: none; width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; cursor: pointer; opacity: 0; transition: 0.2s; z-index: 15; }
.spec-value-col:hover .btn-remove-from-table { opacity: 1; }

.cm-header-price { text-align: center; font-size: 16px; font-weight: 800; color: #e53e3e; }
.group-header { background: #f8fafc; font-weight: 800; color: #0a142f; font-size: 16px; padding: 12px 24px; border-top: 2px solid #eaeaea; border-bottom: 1px solid #eaeaea; }

.empty-product-slot { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; min-height: 180px; border: 2px dashed #cbd5e0; border-radius: 12px; color: #a0aec0; cursor: pointer; transition: 0.2s; background: #f8fafc; }
.empty-product-slot i { font-size: 32px; margin-bottom: 10px; }
.empty-product-slot span { font-weight: 600; font-size: 14px; }
.empty-product-slot:hover { border-color: #bd9a5f; color: #bd9a5f; background: #fffcf7; }

.compare-select { width: 80%; padding: 8px 10px; border: 1px solid #cbd5e0; border-radius: 6px; font-size: 13px; font-weight: 600; color: #0a142f; outline: none; cursor: pointer; margin: 0 auto; display: block; }
.compare-select:focus { border-color: #bd9a5f; }

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

.cm-btn-buy { width: 80%; margin: 0 auto; display: flex; justify-content: center; align-items: center; padding: 12px; background: #0a142f; color: white; border: none; border-radius: 8px; font-weight: 700; cursor: pointer; transition: 0.2s; text-transform: uppercase; letter-spacing: 0.5px; font-size: 13px; }
.cm-btn-buy:hover:not(:disabled) { background: #bd9a5f; box-shadow: 0 4px 15px rgba(189,154,95,0.3); }
.cm-btn-buy:disabled { opacity: 0.6; cursor: not-allowed; background: #718096; }

/* PICKER MODAL */
.product-picker-box { background: white; width: 95%; max-width: 700px; max-height: 80vh; border-radius: 16px; display: flex; flex-direction: column; overflow: hidden; animation: modalFadeIn 0.3s ease; }
.picker-search-bar { padding: 15px 24px; border-bottom: 1px solid #eaeaea; background: #f8fafc; }
.picker-input { width: 100%; padding: 12px 15px; border: 1px solid #cbd5e0; border-radius: 8px; font-size: 14px; outline: none; transition: 0.2s; }
.picker-input:focus { border-color: #bd9a5f; box-shadow: 0 0 0 3px rgba(189,154,95,0.1); }
.picker-body { flex: 1; overflow-y: auto; padding: 20px 24px; }
.picker-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 15px; }

.picker-item { display: flex; flex-direction: column; align-items: center; text-align: center; padding: 15px; border: 1px solid #eaeaea; border-radius: 12px; cursor: pointer; transition: 0.2s; position: relative; justify-content: space-between; height: 100%; }
.picker-item:hover { border-color: #bd9a5f; transform: translateY(-3px); box-shadow: 0 6px 15px rgba(0,0,0,0.05); }
.picker-item.selected { border-color: #bd9a5f; background: #fdfaf6; }
.picker-item img { width: 80px; height: 80px; object-fit: cover; border-radius: 8px; margin-bottom: 12px; flex-shrink: 0; }
.picker-info { display: flex; flex-direction: column; align-items: center; width: 100%; flex: 1; justify-content: space-between; }
.picker-info .brand { font-size: 11px; color: #bd9a5f; font-weight: 700; text-transform: uppercase; margin-bottom: 4px; }
.picker-info .name { font-size: 13px; font-weight: 700; color: #0a142f; margin: 0 0 8px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; line-height: 1.3; }
.picker-price { font-size: 15px; font-weight: 800; color: #e53e3e; margin-top: auto; }

.picker-check { position: absolute; top: 10px; right: 10px; font-size: 18px; }
.picker-footer { padding: 15px 24px; border-top: 1px solid #eaeaea; display: flex; justify-content: flex-end; background: #f8fafc; }
@keyframes modalFadeIn { from { opacity: 0; transform: translateY(20px) scale(0.98); } to { opacity: 1; transform: translateY(0) scale(1); } }
</style>