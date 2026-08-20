package org.example.datn_sd69.modules.product.service.impl;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.product.dto.response.AiProductCompareResponse;
import org.example.datn_sd69.modules.product.dto.response.ProductResponse;
import org.example.datn_sd69.modules.product.service.AiProductCompareService;
import org.example.datn_sd69.modules.product.service.ProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final JsonMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-2.5-flash-lite}")
    private String geminiModel;

    @Override
    public AiProductCompareResponse compareProducts(List<Integer> productIds) {

        List<Integer> normalizedProductIds = validateProductIds(productIds);

        List<ProductResponse> products = normalizedProductIds.stream()
                .map(productService::getProductById)
                .toList();

        for (ProductResponse product : products) {
            if (product.getStatus() == null || product.getStatus() != 1) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Chỉ được phân tích sản phẩm đang bán."
                );
            }
        }

        String prompt = buildComparisonPrompt(products);
        String geminiOutput = callGemini(prompt);

        return parseStructuredResponse(
                normalizedProductIds,
                geminiOutput
        );
    }

    private List<Integer> validateProductIds(List<Integer> productIds) {

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

        Set<Integer> uniqueIds = new LinkedHashSet<>(productIds);

        if (uniqueIds.size() != productIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không được chọn trùng sản phẩm để so sánh."
            );
        }

        return new ArrayList<>(uniqueIds);
    }

    private String buildComparisonPrompt(List<ProductResponse> products) {

        List<Map<String, Object>> productData = products.stream()
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
                Bạn là bộ máy phân tích dữ liệu cho chức năng so sánh nước hoa.

                CHỈ sử dụng dữ liệu JSON được cung cấp bên dưới.
                KHÔNG sử dụng kiến thức bên ngoài về tên sản phẩm hoặc thương hiệu.

                MỤC TIÊU:
                - Tạo dữ liệu có cấu trúc để frontend điền trực tiếp vào bảng so sánh.
                - longevity: ước lượng độ lưu hương từ dữ liệu hiện có.
                - style: suy luận phong cách sử dụng từ dữ liệu hiện có.
                - occasion: suy luận hoàn cảnh khuyên dùng từ dữ liệu hiện có.

                QUY TẮC BẮT BUỘC:
                1. Không bịa tầng hương, thành phần, nguyên liệu hoặc thông số không có trong JSON.
                2. Ba field longevity, style và occasion là dữ liệu tư vấn được phép SUY LUẬN THẬN TRỌNG
                   từ description, concentration, gender và fragranceFamilies đang có trong JSON.
                3. Nếu concentration có dữ liệu thì longevity PHẢI đưa ra một mức ước lượng hợp lý;
                   không được trả "Chưa có đủ dữ liệu" chỉ vì JSON không có field longevity gốc.
                4. Nếu ít nhất một trong các field description, concentration, gender hoặc fragranceFamilies
                   có dữ liệu thì style và occasion PHẢI đưa ra gợi ý phù hợp từ dữ liệu đó.
                5. Chỉ trả đúng "Chưa có đủ dữ liệu" cho một field khi toàn bộ dữ liệu liên quan
                   thực sự trống hoặc không đủ bất kỳ căn cứ nào để suy luận.
                6. style phải mô tả PHONG CÁCH, ví dụ như:
                   "Thanh lịch, tinh tế", "Trẻ trung, năng động", "Sang trọng, trưởng thành".
                   style TUYỆT ĐỐI KHÔNG được chỉ trả "Nam", "Nữ", "Unisex",
                   "EDT", "EDP", "Parfum" hoặc tên nồng độ.
                7. occasion phải mô tả HOÀN CẢNH SỬ DỤNG, ví dụ như:
                   "Đi làm, gặp gỡ hằng ngày", "Hẹn hò, đi tiệc", "Dạo phố, hoạt động ban ngày".
                   Không được dùng giới tính thay cho occasion.
                8. longevity phải mô tả thời gian hoặc mức độ lưu hương dễ hiểu bằng tiếng Việt,
                   ví dụ "Khoảng 4 - 6 tiếng (Vừa phải)" khi có đủ căn cứ từ concentration.
                9. Không dùng ProductVariant.stockQuantity.
                10. Nếu đề cập tồn kho chỉ được dựa trên sellableQuantity.
                11. Không coi hết hàng là sản phẩm kém hơn; đó chỉ là trạng thái tồn kho.
                12. Không đổi productId.
                13. Mỗi productId đầu vào phải có đúng một phần tử trong insights.
                14. Kết quả của từng sản phẩm phải dựa trên chính dữ liệu của productId đó,
                    không sao chép máy móc insight của sản phẩm khác.
                15. Chỉ trả JSON, không markdown, không ``` và không giải thích ngoài JSON.

                JSON OUTPUT BẮT BUỘC:
                {
                  "analysis": "Tóm tắt rất ngắn về khác biệt dựa trên dữ liệu đã cho",
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

    private Map<String, Object> toAiProductData(ProductResponse product) {

        Map<String, Object> data = new LinkedHashMap<>();

        data.put("productId", product.getId());
        data.put("name", product.getName());
        data.put("description", product.getDescription());
        data.put("brand", product.getBrandName());
        data.put("category", product.getCategoryName());
        data.put("concentration", product.getConcentrationName());
        data.put("gender", genderLabel(product.getGender()));
        data.put("niche", Boolean.TRUE.equals(product.getIsNiche()));
        data.put("rating", product.getRating());
        data.put("reviewCount", product.getReviewCount());

        List<String> fragranceFamilies =
                product.getFragranceFamilies() == null
                        ? List.of()
                        : product.getFragranceFamilies()
                        .stream()
                        .map(ProductResponse.FragranceFamilyDTO::getName)
                        .filter(name -> name != null && !name.isBlank())
                        .sorted()
                        .toList();

        data.put("fragranceFamilies", fragranceFamilies);

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

        data.put("variants", variants);

        return data;
    }

    private Map<String, Object> toAiVariantData(ProductResponse.VariantDTO variant) {

        Map<String, Object> data = new LinkedHashMap<>();

        data.put("variantId", variant.getId());
        data.put("capacity", variant.getCapacityName());
        data.put("bottleType", variant.getBottleTypeName());
        data.put("sku", variant.getSku());
        data.put("price", variant.getPrice());

        /*
         * InventoryLot là nguồn tồn thật.
         * Chỉ gửi sellableQuantity.
         * Không gửi stockQuantity / manufacturingDate / expirationDate legacy.
         */
        data.put(
                "sellableQuantity",
                variant.getSellableQuantity() == null
                        ? 0L
                        : variant.getSellableQuantity()
        );

        data.put("status", variant.getStatus());

        return data;
    }

    private String genderLabel(Integer gender) {

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

        String json = stripCodeFence(rawOutput);

        try {
            JsonNode root = objectMapper.readTree(json);

            String analysis = root
                    .path("analysis")
                    .asText("")
                    .trim();

            JsonNode insightsNode = root.path("insights");

            if (!insightsNode.isArray()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI không trả về danh sách insights hợp lệ."
                );
            }

            Map<Integer, AiProductCompareResponse.ProductInsight> insightByProductId =
                    new LinkedHashMap<>();

            for (JsonNode item : insightsNode) {

                int productId = item.path("productId").asInt(0);

                if (!productIds.contains(productId)
                        || insightByProductId.containsKey(productId)) {
                    continue;
                }

                insightByProductId.put(
                        productId,
                        AiProductCompareResponse.ProductInsight.builder()
                                .productId(productId)
                                .longevity(readInsightText(item, "longevity"))
                                .style(readInsightText(item, "style"))
                                .occasion(readInsightText(item, "occasion"))
                                .build()
                );
            }

            List<AiProductCompareResponse.ProductInsight> orderedInsights =
                    productIds.stream()
                            .map(productId ->
                                    insightByProductId.getOrDefault(
                                            productId,
                                            AiProductCompareResponse.ProductInsight.builder()
                                                    .productId(productId)
                                                    .longevity(NO_DATA)
                                                    .style(NO_DATA)
                                                    .occasion(NO_DATA)
                                                    .build()
                                    )
                            )
                            .toList();

            return AiProductCompareResponse.builder()
                    .productIds(productIds)
                    .analysis(analysis.isBlank() ? null : analysis)
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

    private String readInsightText(JsonNode item, String field) {

        String value = item
                .path(field)
                .asText("")
                .trim();

        return value.isBlank() ? NO_DATA : value;
    }

    private String stripCodeFence(String value) {

        if (value == null) {
            return "";
        }

        String text = value.trim();

        if (!text.startsWith("```")) {
            return text;
        }

        int firstNewLine = text.indexOf('\n');
        int lastFence = text.lastIndexOf("```");

        if (firstNewLine >= 0 && lastFence > firstNewLine) {
            return text
                    .substring(firstNewLine + 1, lastFence)
                    .trim();
        }

        return text;
    }

    private String callGemini(String prompt) {

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Chưa cấu hình Gemini API key."
            );
        }

        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("role", "user");
        content.put("parts", List.of(textPart));

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("maxOutputTokens", 1200);
        generationConfig.put("responseMimeType", "application/json");

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("contents", List.of(content));
        requestBody.put("generationConfig", generationConfig);

        try {
            RestClient restClient =
                    RestClient.builder()
                            .baseUrl("https://generativelanguage.googleapis.com")
                            .defaultHeader("x-goog-api-key", geminiApiKey)
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

            return extractOutputText(responseBody);

        } catch (RestClientResponseException ex) {

            int status = ex.getStatusCode().value();

            if (status == 400) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Gemini từ chối yêu cầu. Hãy kiểm tra model hoặc dữ liệu gửi lên."
                );
            }

            if (status == 401 || status == 403) {
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

    private String extractOutputText(String responseBody) {

        if (responseBody == null || responseBody.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Dịch vụ AI không trả về nội dung."
            );
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            StringBuilder result = new StringBuilder();

            JsonNode candidates = root.path("candidates");

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
                                result.append(System.lineSeparator());
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