package org.example.datn_sd69.modules.product.service.impl;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.modules.product.dto.response.AiProductCompareResponse;
import org.example.datn_sd69.modules.product.dto.response.ProductResponse;
import org.example.datn_sd69.modules.product.service.AiProductCompareService;
import org.example.datn_sd69.modules.product.service.AiUsageService;
import org.example.datn_sd69.modules.product.service.ProductService;
import org.example.datn_sd69.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AiProductCompareServiceImpl implements AiProductCompareService {

    private static final String NO_DATA = "Chưa có đủ dữ liệu";

    private final ProductService productService;
    private final UserRepository userRepository;
    private final AiUsageService aiUsageService;
    private final JsonMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-3.5-flash-lite}")
    private String geminiModel;

    @Override
    public AiProductCompareResponse compareProducts(
            List<Integer> productIds
    ) {

        /*
         * Validate request ở Service để vẫn an toàn
         * nếu service được gọi từ caller khác ngoài Controller.
         */
        List<Integer> normalizedProductIds =
                validateProductIds(productIds);

        /*
         * Dùng ProductService hiện tại để lấy dữ liệu sản phẩm.
         * Không query trực tiếp repository để tránh tạo thêm
         * một luồng mapping Product khác.
         */
        List<ProductResponse> products =
                normalizedProductIds.stream()
                        .map(productService::getProductById)
                        .toList();

        /*
         * AI chỉ phân tích sản phẩm hiện đang bán.
         */
        for (ProductResponse product : products) {

            if (product.getStatus() == null
                    || product.getStatus() != 1) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Chỉ được phân tích sản phẩm đang bán."
                );
            }
        }

        /*
         * Lấy UserId thật từ SecurityContext hiện tại.
         * JWT vẫn giữ nguyên subject=email; không sửa cấu trúc token.
         */
        Integer currentUserId =
                getCurrentAuthenticatedCustomerId();

        String prompt = buildComparisonPrompt(products);

        /*
         * Chỉ riêng thao tác AI bị giới hạn 5 lượt/user/ngày.
         * So sánh thường không đi qua service này nên không bị ảnh hưởng.
         *
         * Reserve quota bằng một transaction ngắn rồi commit ngay,
         * sau đó mới gọi Gemini để không giữ lock DB trong lúc chờ API ngoài.
         */
        AiUsageService.Reservation reservation =
                aiUsageService.reserve(currentUserId);

        try {

            String geminiOutput =
                    callGemini(prompt);

            return parseStructuredResponse(
                    normalizedProductIds,
                    geminiOutput
            );

        } catch (RuntimeException ex) {

            /*
             * Gemini hoặc parse lỗi thì hoàn lại đúng lượt vừa reserve.
             * User chỉ mất lượt khi thao tác AI thực sự thành công.
             */
            aiUsageService.release(reservation);

            throw ex;
        }
    }

    private Integer getCurrentAuthenticatedCustomerId() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()
                || "anonymousUser".equalsIgnoreCase(
                authentication.getName()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Bạn cần đăng nhập để sử dụng so sánh bằng AI."
            );
        }

        String email =
                authentication
                        .getName()
                        .trim()
                        .toLowerCase();

        User user =
                userRepository
                        .findWithRoleByEmail(email)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Tài khoản đăng nhập không tồn tại."
                                )
                        );

        if (Boolean.TRUE.equals(user.getIsDeleted())
                || user.getStatus() == null
                || user.getStatus() != 1) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Tài khoản của bạn đã bị khóa hoặc không còn hoạt động."
            );
        }

        String roleName =
                user.getRole() == null
                        || user.getRole().getName() == null
                        ? ""
                        : user.getRole()
                        .getName()
                        .trim()
                        .toUpperCase();

        if (!"USER".equals(roleName)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Chức năng so sánh bằng AI chỉ dành cho tài khoản khách hàng."
            );
        }

        return user.getId();
    }

    private List<Integer> validateProductIds(
            List<Integer> productIds
    ) {

        if (productIds == null
                || productIds.size() < 2
                || productIds.size() > 3) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ được so sánh từ 2 đến 3 sản phẩm."
            );
        }

        for (Integer productId : productIds) {

            if (productId == null || productId <= 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "ProductId không hợp lệ."
                );
            }
        }

        /*
         * Không cho cùng một sản phẩm xuất hiện nhiều lần.
         *
         * LinkedHashSet giữ nguyên thứ tự productIds
         * mà frontend gửi lên.
         */
        Set<Integer> uniqueIds =
                new LinkedHashSet<>(productIds);

        if (uniqueIds.size() != productIds.size()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không được chọn trùng sản phẩm để so sánh."
            );
        }

        return new ArrayList<>(uniqueIds);
    }

    private String buildComparisonPrompt(
            List<ProductResponse> products
    ) {

        /*
         * Chỉ gửi những dữ liệu AI thực sự cần.
         * Không serialize nguyên Entity/ProductResponse.
         */
        List<Map<String, Object>> productData =
                products.stream()
                        .map(this::toAiProductData)
                        .toList();

        final String json;

        try {

            json = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(productData);

        } catch (JacksonException ex) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể chuẩn bị dữ liệu sản phẩm để phân tích."
            );
        }

        return """
                Bạn là trợ lý tư vấn cho chức năng so sánh sản phẩm nước hoa
                của một website thương mại điện tử.

                CHỈ được sử dụng dữ liệu JSON được cung cấp bên dưới.
                KHÔNG sử dụng kiến thức bên ngoài về tên sản phẩm,
                thương hiệu hoặc các thông số không có trong JSON.

                MỤC TIÊU:

                - Phân tích sự khác biệt giữa các sản phẩm.
                - Tạo dữ liệu có cấu trúc để frontend bổ sung vào bảng so sánh.
                - Đưa ra gợi ý lựa chọn dựa trên nhu cầu của người mua.
                - Không khẳng định tuyệt đối một sản phẩm là tốt nhất
                  nếu dữ liệu không đủ căn cứ.

                Ý NGHĨA CÁC FIELD:

                - analysis:
                  Tóm tắt ngắn những khác biệt đáng chú ý giữa các sản phẩm.
                  Khi nhắc tới sản phẩm PHẢI dùng chính xác field "name"
                  của sản phẩm đó trong JSON.
                  Tuyệt đối không sử dụng productId, variantId hoặc SKU
                  trong nội dung analysis.

                - recommendation:
                  Đưa ra lời khuyên lựa chọn có điều kiện.
                  Khi nhắc tới sản phẩm PHẢI dùng chính xác field "name"
                  của sản phẩm đó trong JSON.
                  Không được hiển thị productId, variantId, SKU
                  hoặc bất kỳ ID kỹ thuật nào cho khách hàng.

                  Ví dụ:
                  "Nếu ưu tiên phong cách thanh lịch và dùng khi đi làm,
                  Chanel phù hợp hơn. Nếu ưu tiên phong cách cổ điển,
                  Chanel No 5 đáng cân nhắc hơn."

                  recommendation phải dựa hoàn toàn trên JSON.
                  Không được bịa thông số.

                - longevity:
                  Ước lượng độ lưu hương từ dữ liệu hiện có.

                - style:
                  Phong cách sử dụng phù hợp.

                - occasion:
                  Hoàn cảnh sử dụng phù hợp.

                QUY TẮC BẮT BUỘC:

                1. Không bịa tầng hương, thành phần, nguyên liệu,
                   hiệu năng hoặc thông số không có trong JSON.

                2. longevity, style và occasion là dữ liệu tư vấn
                   được phép SUY LUẬN THẬN TRỌNG từ:
                   description,
                   concentration,
                   gender,
                   fragranceFamilies.

                3. Nếu concentration có dữ liệu thì longevity
                   PHẢI đưa ra một mức ước lượng hợp lý.
                   Không được trả "Chưa có đủ dữ liệu"
                   chỉ vì JSON không có field longevity gốc.

                4. Nếu ít nhất một trong các field:
                   description,
                   concentration,
                   gender,
                   fragranceFamilies
                   có dữ liệu thì style và occasion phải đưa ra
                   gợi ý phù hợp từ dữ liệu đó.

                5. Chỉ trả đúng "Chưa có đủ dữ liệu"
                   khi toàn bộ dữ liệu liên quan thực sự không đủ
                   để đưa ra bất kỳ suy luận hợp lý nào.

                6. style phải mô tả PHONG CÁCH.
                   Ví dụ:
                   "Thanh lịch, tinh tế"
                   "Trẻ trung, năng động"
                   "Sang trọng, trưởng thành"

                   style TUYỆT ĐỐI KHÔNG được chỉ trả:
                   "Nam",
                   "Nữ",
                   "Unisex",
                   "EDT",
                   "EDP",
                   "Parfum"
                   hoặc chỉ tên nồng độ.

                7. occasion phải mô tả HOÀN CẢNH SỬ DỤNG.
                   Ví dụ:
                   "Đi làm, gặp gỡ hằng ngày"
                   "Hẹn hò, đi tiệc"
                   "Dạo phố, hoạt động ban ngày"

                   Không được dùng giới tính thay cho occasion.

                8. longevity phải mô tả thời gian hoặc mức độ
                   lưu hương dễ hiểu bằng tiếng Việt.
                   Ví dụ:
                   "Khoảng 4 - 6 tiếng (Vừa phải)"
                   nếu có căn cứ phù hợp từ concentration.

                9. Có thể sử dụng price để phân tích chênh lệch giá
                   giữa các biến thể được cung cấp.

                10. Có thể sử dụng rating và reviewCount
                    như dữ liệu tham khảo nếu chúng có trong JSON.

                11. Không dùng ProductVariant.stockQuantity.

                12. Nếu đề cập tồn kho,
                    chỉ được dựa trên sellableQuantity.

                13. Trạng thái hết hàng KHÔNG được dùng làm căn cứ
                    để kết luận chất lượng sản phẩm thấp hơn.

                14. Không thay đổi productId.

                15. Mỗi productId đầu vào phải có đúng một
                    phần tử tương ứng trong insights.

                16. Insight của từng sản phẩm phải dựa trên
                    chính dữ liệu của productId đó.

                17. Không sao chép máy móc longevity,
                    style hoặc occasion giữa các sản phẩm.

                18. recommendation phải đưa ra gợi ý có điều kiện
                    theo nhu cầu người dùng thay vì tuyên bố
                    một sản phẩm luôn luôn tốt hơn sản phẩm khác.

                19. Nếu không đủ dữ liệu để xác định một người
                    chắc chắn nên chọn sản phẩm nào,
                    phải nói rõ tiêu chí để người dùng tự lựa chọn.

                20. Trả lời bằng tiếng Việt.

                21. CHỈ trả JSON.
                    Không markdown.
                    Không ```json.
                    Không giải thích bên ngoài JSON.

                22. analysis và recommendation là nội dung
                    HIỂN THỊ TRỰC TIẾP CHO KHÁCH HÀNG.

                23. TUYỆT ĐỐI KHÔNG được hiển thị productId,
                    variantId, SKU hoặc bất kỳ ID kỹ thuật nào
                    trong analysis hoặc recommendation.

                24. Khi nhắc tới một sản phẩm trong analysis
                    hoặc recommendation, PHẢI gọi sản phẩm bằng
                    chính xác field "name" trong JSON.

                25. Không dùng các cách gọi kỹ thuật như:
                    "productId 19", "sản phẩm 19", "variantId 2",
                    "SKU ABC123".

                    Ví dụ:
                    SAI:
                    "Sản phẩm productId 19 phù hợp hơn."

                    ĐÚNG:
                    "Chanel phù hợp hơn."

                26. productId CHỈ được sử dụng trong field productId
                    của từng object trong insights để backend ánh xạ
                    insight về đúng sản phẩm.

                JSON OUTPUT BẮT BUỘC:

                {
                  "analysis": "Tóm tắt khác biệt và luôn gọi sản phẩm bằng tên",
                  "recommendation": "Gợi ý lựa chọn và luôn gọi sản phẩm bằng tên",
                  "insights": [
                    {
                      "productId": 1,
                      "longevity": "string",
                      "style": "string",
                      "occasion": "string"
                    }
                  ]
                }

                DỮ LIỆU SẢN PHẨM:
                """ + json;
    }

    private Map<String, Object> toAiProductData(
            ProductResponse product
    ) {

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "productId",
                product.getId()
        );

        data.put(
                "name",
                product.getName()
        );

        data.put(
                "description",
                product.getDescription()
        );

        data.put(
                "brand",
                product.getBrandName()
        );

        data.put(
                "category",
                product.getCategoryName()
        );

        data.put(
                "concentration",
                product.getConcentrationName()
        );

        data.put(
                "gender",
                genderLabel(product.getGender())
        );

        data.put(
                "niche",
                Boolean.TRUE.equals(product.getIsNiche())
        );

        data.put(
                "rating",
                product.getRating()
        );

        data.put(
                "reviewCount",
                product.getReviewCount()
        );

        /*
         * Chỉ lấy tên nhóm hương.
         * Không gửi object/domain dư thừa sang AI.
         */
        List<String> fragranceFamilies =
                product.getFragranceFamilies() == null
                        ? List.of()
                        : product.getFragranceFamilies()
                        .stream()
                        .map(
                                ProductResponse
                                        .FragranceFamilyDTO::getName
                        )
                        .filter(
                                name ->
                                        name != null
                                                && !name.isBlank()
                        )
                        .sorted()
                        .toList();

        data.put(
                "fragranceFamilies",
                fragranceFamilies
        );

        /*
         * Dữ liệu variant dùng thông tin hiện có trong ProductResponse.
         */
        List<Map<String, Object>> variants =
                product.getVariants() == null
                        ? List.of()
                        : product.getVariants()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        variant ->
                                                variant.getCapacityName() == null
                                                        ? ""
                                                        : variant.getCapacityName()
                                )
                        )
                        .map(this::toAiVariantData)
                        .toList();

        data.put(
                "variants",
                variants
        );

        return data;
    }

    private Map<String, Object> toAiVariantData(
            ProductResponse.VariantDTO variant
    ) {

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "variantId",
                variant.getId()
        );

        data.put(
                "capacity",
                variant.getCapacityName()
        );

        data.put(
                "bottleType",
                variant.getBottleTypeName()
        );

        data.put(
                "sku",
                variant.getSku()
        );

        data.put(
                "price",
                variant.getPrice()
        );

        /*
         * InventoryLot là nguồn tồn kho vật lý thật.
         *
         * AI chỉ nhận sellableQuantity.
         *
         * KHÔNG gửi:
         * - stockQuantity
         * - manufacturingDate
         * - expirationDate
         *
         * vì đây là các field legacy của ProductVariant.
         */
        data.put(
                "sellableQuantity",
                variant.getSellableQuantity() == null
                        ? 0L
                        : variant.getSellableQuantity()
        );

        data.put(
                "status",
                variant.getStatus()
        );

        return data;
    }

    private String genderLabel(
            Integer gender
    ) {

        if (gender == null) {
            return "Chưa có dữ liệu";
        }

        return switch (gender) {
            case 0 -> "Unisex";
            case 1 -> "Nam";
            case 2 -> "Nữ";
            default -> "Chưa có dữ liệu";
        };
    }

    private AiProductCompareResponse parseStructuredResponse(
            List<Integer> productIds,
            String rawOutput
    ) {

        String json =
                stripCodeFence(rawOutput);

        try {

            JsonNode root =
                    objectMapper.readTree(json);

            String analysis =
                    root
                            .path("analysis")
                            .asText("")
                            .trim();

            String recommendation =
                    root
                            .path("recommendation")
                            .asText("")
                            .trim();

            JsonNode insightsNode =
                    root.path("insights");

            if (!insightsNode.isArray()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI không trả về danh sách insights hợp lệ."
                );
            }

            /*
             * Map theo productId để:
             *
             * - bỏ output ID lạ từ AI;
             * - bỏ productId trùng;
             * - trả output theo đúng thứ tự request ban đầu.
             */
            Map<
                    Integer,
                    AiProductCompareResponse.ProductInsight
                    > insightByProductId =
                    new LinkedHashMap<>();

            for (JsonNode item : insightsNode) {

                int productId =
                        item
                                .path("productId")
                                .asInt(0);

                if (!productIds.contains(productId)
                        || insightByProductId.containsKey(productId)) {

                    continue;
                }

                insightByProductId.put(
                        productId,
                        AiProductCompareResponse
                                .ProductInsight
                                .builder()
                                .productId(productId)
                                .longevity(
                                        readInsightText(
                                                item,
                                                "longevity"
                                        )
                                )
                                .style(
                                        readInsightText(
                                                item,
                                                "style"
                                        )
                                )
                                .occasion(
                                        readInsightText(
                                                item,
                                                "occasion"
                                        )
                                )
                                .build()
                );
            }

            /*
             * Luôn trả đúng số phần tử tương ứng productIds.
             * Nếu Gemini thiếu insight thì fallback NO_DATA,
             * không làm FE bị thiếu cột/dòng.
             */
            List<AiProductCompareResponse.ProductInsight>
                    orderedInsights =
                    productIds.stream()
                            .map(
                                    productId ->
                                            insightByProductId
                                                    .getOrDefault(
                                                            productId,
                                                            AiProductCompareResponse
                                                                    .ProductInsight
                                                                    .builder()
                                                                    .productId(productId)
                                                                    .longevity(NO_DATA)
                                                                    .style(NO_DATA)
                                                                    .occasion(NO_DATA)
                                                                    .build()
                                                    )
                            )
                            .toList();

            return AiProductCompareResponse
                    .builder()
                    .productIds(productIds)
                    .analysis(
                            analysis.isBlank()
                                    ? null
                                    : analysis
                    )
                    .recommendation(
                            recommendation.isBlank()
                                    ? NO_DATA
                                    : recommendation
                    )
                    .insights(orderedInsights)
                    .build();

        } catch (ResponseStatusException ex) {

            throw ex;

        } catch (JacksonException ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không đọc được dữ liệu so sánh có cấu trúc từ AI."
            );
        }
    }

    private String readInsightText(
            JsonNode item,
            String field
    ) {

        String value =
                item
                        .path(field)
                        .asText("")
                        .trim();

        return value.isBlank()
                ? NO_DATA
                : value;
    }

    private String stripCodeFence(
            String value
    ) {

        if (value == null) {
            return "";
        }

        String text =
                value.trim();

        /*
         * Gemini đã được yêu cầu trả application/json,
         * tuy nhiên vẫn giữ fallback này để chịu được trường hợp
         * model bọc response bằng markdown code fence.
         */
        if (!text.startsWith("```")) {
            return text;
        }

        int firstNewLine =
                text.indexOf('\n');

        int lastFence =
                text.lastIndexOf("```");

        if (firstNewLine >= 0
                && lastFence > firstNewLine) {

            return text
                    .substring(
                            firstNewLine + 1,
                            lastFence
                    )
                    .trim();
        }

        return text;
    }

    private String callGemini(
            String prompt
    ) {

        if (geminiApiKey == null
                || geminiApiKey.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Chưa cấu hình Gemini API key."
            );
        }

        /*
         * Gemini GenerateContent request.
         */
        Map<String, Object> textPart =
                new LinkedHashMap<>();

        textPart.put(
                "text",
                prompt
        );

        Map<String, Object> content =
                new LinkedHashMap<>();

        content.put(
                "role",
                "user"
        );

        content.put(
                "parts",
                List.of(textPart)
        );

        Map<String, Object> generationConfig =
                new LinkedHashMap<>();

        /*
         * Output khá ngắn nên 1200 token là đủ cho
         * 2-3 sản phẩm và giúp hạn chế token không cần thiết.
         */
        generationConfig.put(
                "maxOutputTokens",
                1200
        );

        /*
         * Yêu cầu Gemini trả JSON thay vì text tự do.
         */
        generationConfig.put(
                "responseMimeType",
                "application/json"
        );

        Map<String, Object> requestBody =
                new LinkedHashMap<>();

        requestBody.put(
                "contents",
                List.of(content)
        );

        requestBody.put(
                "generationConfig",
                generationConfig
        );

        try {

            RestClient restClient =
                    RestClient
                            .builder()
                            .baseUrl(
                                    "https://generativelanguage.googleapis.com"
                            )
                            .defaultHeader(
                                    "x-goog-api-key",
                                    geminiApiKey
                            )
                            .defaultHeader(
                                    "Content-Type",
                                    MediaType.APPLICATION_JSON_VALUE
                            )
                            .build();

            String responseBody =
                    restClient
                            .post()
                            .uri(
                                    "/v1beta/models/{model}:generateContent",
                                    geminiModel
                            )
                            .body(requestBody)
                            .retrieve()
                            .body(String.class);

            return extractOutputText(
                    responseBody
            );

        } catch (RestClientResponseException ex) {

            int status =
                    ex.getStatusCode().value();

            if (status == 400) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Gemini từ chối yêu cầu. Hãy kiểm tra model hoặc dữ liệu gửi lên."
                );
            }

            if (status == 401
                    || status == 403) {

                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Gemini API key không hợp lệ hoặc không có quyền sử dụng."
                );
            }

            if (status == 404) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Không tìm thấy model Gemini đã cấu hình."
                );
            }

            if (status == 429) {

                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Gemini đã đạt giới hạn Free Tier hoặc đang bị giới hạn tần suất."
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không thể nhận phản hồi từ dịch vụ Gemini."
            );

        } catch (ResponseStatusException ex) {

            throw ex;

        } catch (Exception ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không thể kết nối tới dịch vụ Gemini."
            );
        }
    }

    private String extractOutputText(
            String responseBody
    ) {

        if (responseBody == null
                || responseBody.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Dịch vụ AI không trả về nội dung."
            );
        }

        try {

            JsonNode root =
                    objectMapper.readTree(responseBody);

            StringBuilder result =
                    new StringBuilder();

            JsonNode candidates =
                    root.path("candidates");

            if (candidates.isArray()) {

                for (JsonNode candidate : candidates) {

                    JsonNode parts =
                            candidate
                                    .path("content")
                                    .path("parts");

                    if (!parts.isArray()) {
                        continue;
                    }

                    for (JsonNode part : parts) {

                        String text =
                                part
                                        .path("text")
                                        .asText("");

                        if (!text.isBlank()) {

                            if (!result.isEmpty()) {
                                result.append(
                                        System.lineSeparator()
                                );
                            }

                            result.append(text);
                        }
                    }
                }
            }

            if (result.isEmpty()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Dịch vụ AI không trả về nội dung phân tích."
                );
            }

            return result
                    .toString()
                    .trim();

        } catch (JacksonException ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không đọc được phản hồi từ dịch vụ AI."
            );
        }
    }
}