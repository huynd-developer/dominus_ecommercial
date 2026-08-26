package org.example.datn_sd69.modules.report.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.ReturnRequestItem;
import org.example.datn_sd69.modules.report.dto.BestSellingProductResponse;
import org.example.datn_sd69.modules.report.dto.ReportSummaryResponse;
import org.example.datn_sd69.modules.report.dto.RevenueChartResponse;
import org.example.datn_sd69.modules.report.enums.ReportFilterType;
import org.example.datn_sd69.modules.report.projection.BestSellingProductProjection;
import org.example.datn_sd69.modules.report.service.OwnerReportService;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
import org.example.datn_sd69.repository.ReturnRequestItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OwnerReportServiceImpl implements OwnerReportService {

    /**
     * Giao dịch bán KHÔNG còn xác định bằng Order.Status = 3.
     * Mốc bán thực tế là Order.CompletedAt != null, vì sau khi hoàn thành đơn
     * có thể chuyển sang 6 (RETURN_REQUESTED) hoặc 7 (RETURN_COMPLETED).
     *
     * ReturnRequestItemRepository chỉ lấy refund đã hoàn tất (Status = 3)
     * và có RefundedAt nằm trong kỳ báo cáo.
     */
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final int MAX_CUSTOM_DAYS = 366;

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern POSITIVE_INTEGER_PATTERN = Pattern.compile("^\\d+$");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReturnRequestItemRepository returnRequestItemRepository;

    @Override
    public ReportSummaryResponse getSummary(
            String filterType,
            String fromDate,
            String toDate
    ) {
        DateRange range = resolveDateRange(filterType, fromDate, toDate);

        /*
         * Doanh thu thuần trong kỳ = doanh thu bán phát sinh trong kỳ
         *                              - tiền hoàn sản phẩm hoàn tất trong kỳ.
         *
         * Hai thời điểm là độc lập:
         * - Sale: Order.CompletedAt
         * - Refund: ReturnRequestItem.RefundedAt
         */
        BigDecimal grossSalesRevenue = moneyOrZero(
                orderRepository.sumGrossSalesRevenueForOwnerReport(
                        range.startDateTime(),
                        range.endDateTimeExclusive()
                )
        );

        BigDecimal completedProductRefund = moneyOrZero(
                returnRequestItemRepository.sumCompletedProductRefundAmountForOwnerReport(
                        range.startDateTime(),
                        range.endDateTimeExclusive()
                )
        );

        BigDecimal totalRevenue = grossSalesRevenue.subtract(completedProductRefund);

        Long totalOrders = orderRepository.countCompletedSalesForOwnerReport(
                range.startDateTime(),
                range.endDateTimeExclusive()
        );

        /*
         * "Sản phẩm đã bán" là gross sold quantity của các giao dịch bán hoàn tất
         * trong kỳ. Hàng trả là một KPI/sự kiện khác, không làm thay đổi số lượng
         * từng được bán ở kỳ gốc.
         */
        Long totalProductsSold = orderItemRepository.sumSoldQuantityForOwnerReport(
                range.startDateTime(),
                range.endDateTimeExclusive()
        );

        List<Object[]> salesBreakdown = orderRepository.getOwnerReportSalesBreakdownByOrderType(
                range.startDateTime(),
                range.endDateTimeExclusive()
        );

        List<Object[]> refundBreakdown = returnRequestItemRepository.getOwnerReportRefundBreakdownByOrderType(
                range.startDateTime(),
                range.endDateTimeExclusive()
        );

        BigDecimal onlineRevenue = BigDecimal.ZERO;
        BigDecimal offlineRevenue = BigDecimal.ZERO;
        Long onlineOrders = 0L;
        Long offlineOrders = 0L;

        /* Bán hàng: cộng doanh thu và đếm số đơn phát sinh bán trong kỳ. */
        for (Object[] row : salesBreakdown) {
            if (row == null || row.length < 3) {
                continue;
            }

            String type = normalizeOrderType(row[0]);
            BigDecimal revenue = objectToMoney(row[1]);
            long count = objectToLong(row[2]);

            if (isOnlineOrderType(type)) {
                onlineRevenue = onlineRevenue.add(revenue);
                onlineOrders += count;
            } else if (isCounterOrderType(type)) {
                offlineRevenue = offlineRevenue.add(revenue);
                offlineOrders += count;
            }
        }

        /* Hoàn tiền: chỉ trừ revenue; tuyệt đối không trừ số đơn đã hoàn thành. */
        for (Object[] row : refundBreakdown) {
            if (row == null || row.length < 2) {
                continue;
            }

            String type = normalizeOrderType(row[0]);
            BigDecimal refund = objectToMoney(row[1]);

            if (isOnlineOrderType(type)) {
                onlineRevenue = onlineRevenue.subtract(refund);
            } else if (isCounterOrderType(type)) {
                offlineRevenue = offlineRevenue.subtract(refund);
            }
        }

        return new ReportSummaryResponse(
                range.filterType(),
                range.fromDate(),
                range.toDate(),
                moneyOrZero(totalRevenue),
                longOrZero(totalOrders),
                longOrZero(totalProductsSold),
                moneyOrZero(onlineRevenue),
                moneyOrZero(offlineRevenue),
                onlineOrders,
                offlineOrders
        );
    }

    private List<RevenueChartResponse> buildRevenueChart(DateRange range) {
        List<Order> sales = orderRepository.findSalesForOwnerReportChart(
                range.startDateTime(),
                range.endDateTimeExclusive()
        );

        List<ReturnRequestItem> refunds =
                returnRequestItemRepository.findCompletedProductRefundsForOwnerReportChart(
                        range.startDateTime(),
                        range.endDateTimeExclusive()
                );

        LinkedHashMap<String, RevenueBucket> buckets = initChartBuckets(range);

        /* Sale ghi nhận vào bucket theo CompletedAt và tăng số đơn. */
        for (Order order : sales) {
            if (order == null || order.getCompletedAt() == null) {
                continue;
            }

            String label = buildChartLabel(order.getCompletedAt(), range.chartGroupType());

            buckets
                    .computeIfAbsent(label, key -> new RevenueBucket())
                    .addSale(calculateSalesRevenue(order));
        }

        /* Refund ghi nhận vào bucket theo RefundedAt; không làm giảm totalOrders. */
        for (ReturnRequestItem refundItem : refunds) {
            if (refundItem == null || refundItem.getRefundedAt() == null) {
                continue;
            }

            String label = buildChartLabel(refundItem.getRefundedAt(), range.chartGroupType());

            buckets
                    .computeIfAbsent(label, key -> new RevenueBucket())
                    .addRefund(moneyOrZero(refundItem.getRefundAmount()));
        }

        return buckets.entrySet()
                .stream()
                .map(entry -> new RevenueChartResponse(
                        entry.getKey(),
                        entry.getValue().revenue(),
                        entry.getValue().totalOrders()
                ))
                .toList();
    }

    @Override
    public List<RevenueChartResponse> getRevenueChart(
            String filterType,
            String fromDate,
            String toDate
    ) {
        DateRange range = resolveDateRange(filterType, fromDate, toDate);
        return buildRevenueChart(range);
    }

    @Override
    public List<RevenueChartResponse> getQuarterlyRevenueChart() {
        LocalDate today = LocalDate.now(VN_ZONE);

        LocalDate fromDate = today.withDayOfYear(1);

        /*
         * Một năm luôn có 4 quý.
         *
         * toDate dùng để tạo đủ bucket Q1, Q2, Q3, Q4.
         * endDateTimeExclusive vẫn chỉ tính dữ liệu đến hết hôm nay,
         * tránh lấy nhầm dữ liệu tương lai nếu có dữ liệu test.
         */
        LocalDate toDate = LocalDate.of(today.getYear(), 12, 31);

        DateRange range = new DateRange(
                ReportFilterType.YEAR,
                fromDate,
                toDate,
                fromDate.atStartOfDay(),
                today.plusDays(1).atStartOfDay(),
                ChartGroupType.QUARTER
        );

        return buildRevenueChart(range);
    }

    @Override
    public List<BestSellingProductResponse> getBestSellingProducts(
            String filterType,
            String fromDate,
            String toDate,
            String limit
    ) {
        DateRange range = resolveDateRange(filterType, fromDate, toDate);
        int safeLimit = parseLimit(limit);

        List<BestSellingProductProjection> projections =
                orderItemRepository.findBestSellingProductsForOwnerReport(
                        range.startDateTime(),
                        range.endDateTimeExclusive()
                );

        return projections
                .stream()
                .limit(safeLimit)
                .map(item -> new BestSellingProductResponse(
                        item.getProductId(),
                        safeText(item.getProductName(), "Sản phẩm"),
                        safeText(item.getBrandName(), "Không rõ thương hiệu"),
                        safeText(item.getCapacityName(), ""),
                        safeText(item.getBottleTypeName(), ""),
                        longOrZero(item.getTotalSold()),
                        moneyOrZero(item.getRevenue()),
                        item.getImageUrl()
                ))
                .toList();
    }



    private DateRange resolveDateRange(
            String rawFilterType,
            String rawFromDate,
            String rawToDate
    ) {
        ReportFilterType filterType = parseFilterType(rawFilterType);

        if (filterType != ReportFilterType.CUSTOM) {
            if (StringUtils.hasText(rawFromDate) || StringUtils.hasText(rawToDate)) {
                throw badRequest("fromDate và toDate chỉ được truyền khi filterType = CUSTOM");
            }
        }

        LocalDate today = LocalDate.now(VN_ZONE);
        LocalDate fromDate;
        LocalDate toDate;
        ChartGroupType chartGroupType;

        switch (filterType) {
            case DAY -> {
                fromDate = today;
                toDate = today;
                chartGroupType = ChartGroupType.HOUR;
            }

            case WEEK -> {
                fromDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                toDate = fromDate.plusDays(6);

                if (toDate.isAfter(today)) {
                    toDate = today;
                }

                chartGroupType = ChartGroupType.DAY;
            }

            case MONTH -> {
                fromDate = today.withDayOfMonth(1);
                toDate = today;
                chartGroupType = ChartGroupType.DAY;
            }

            case QUARTER -> {
                int currentQuarter = (today.getMonthValue() - 1) / 3 + 1;
                int firstMonthOfQuarter = (currentQuarter - 1) * 3 + 1;

                fromDate = LocalDate.of(today.getYear(), firstMonthOfQuarter, 1);
                toDate = today;
                chartGroupType = ChartGroupType.MONTH;
            }

            case YEAR -> {
                fromDate = today.withDayOfYear(1);
                toDate = today;
                chartGroupType = ChartGroupType.MONTH;
            }

            case CUSTOM -> {
                fromDate = parseDate(rawFromDate, "fromDate", true);
                toDate = parseDate(rawToDate, "toDate", true);

                if (fromDate.isAfter(toDate)) {
                    throw badRequest("fromDate không được lớn hơn toDate");
                }

                if (toDate.isAfter(today)) {
                    throw badRequest("toDate không được lớn hơn ngày hiện tại");
                }

                long inclusiveDays = ChronoUnit.DAYS.between(fromDate, toDate) + 1;

                if (inclusiveDays > MAX_CUSTOM_DAYS) {
                    throw badRequest("Khoảng thời gian tùy chỉnh tối đa là " + MAX_CUSTOM_DAYS + " ngày");
                }

                chartGroupType = inclusiveDays <= 31 ? ChartGroupType.DAY : ChartGroupType.MONTH;
            }

            default -> throw badRequest("filterType không hợp lệ");
        }

        return new DateRange(
                filterType,
                fromDate,
                toDate,
                fromDate.atStartOfDay(),
                toDate.plusDays(1).atStartOfDay(),
                chartGroupType
        );
    }

    private ReportFilterType parseFilterType(String rawValue) {
        /*
         * Controller đang để filterType optional.
         * Dashboard thực tế nên có mặc định, tránh vừa vào trang đã 400.
         */
        if (rawValue == null || !StringUtils.hasText(rawValue)) {
            return ReportFilterType.MONTH;
        }

        if (containsWhitespace(rawValue)) {
            throw badRequest("filterType không được chứa khoảng trắng");
        }

        try {
            return ReportFilterType.valueOf(rawValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw badRequest("filterType không hợp lệ. Chỉ nhận: DAY, WEEK, MONTH, QUARTER, YEAR, CUSTOM");
        }
    }

    private LocalDate parseDate(
            String rawValue,
            String fieldName,
            boolean required
    ) {
        if (rawValue == null) {
            if (required) {
                throw badRequest(fieldName + " là bắt buộc khi filterType = CUSTOM");
            }

            return null;
        }

        if (!StringUtils.hasText(rawValue)) {
            throw badRequest(fieldName + " không được để trống");
        }

        if (containsWhitespace(rawValue)) {
            throw badRequest(fieldName + " không được chứa khoảng trắng");
        }

        if (!DATE_PATTERN.matcher(rawValue).matches()) {
            throw badRequest(fieldName + " phải đúng định dạng yyyy-MM-dd");
        }

        try {
            return LocalDate.parse(rawValue);
        } catch (DateTimeParseException ex) {
            throw badRequest(fieldName + " không hợp lệ hoặc ngày không tồn tại");
        }
    }

    private int parseLimit(String rawLimit) {
        if (rawLimit == null) {
            return DEFAULT_LIMIT;
        }

        if (!StringUtils.hasText(rawLimit)) {
            throw badRequest("limit không được để trống");
        }

        if (containsWhitespace(rawLimit)) {
            throw badRequest("limit không được chứa khoảng trắng");
        }

        if (!POSITIVE_INTEGER_PATTERN.matcher(rawLimit).matches()) {
            throw badRequest("limit chỉ được nhập số nguyên dương");
        }

        try {
            int limit = Integer.parseInt(rawLimit);

            if (limit < 1 || limit > MAX_LIMIT) {
                throw badRequest("limit phải nằm trong khoảng từ 1 đến " + MAX_LIMIT);
            }

            return limit;
        } catch (NumberFormatException ex) {
            throw badRequest("limit không hợp lệ");
        }
    }

    private LinkedHashMap<String, RevenueBucket> initChartBuckets(DateRange range) {
        LinkedHashMap<String, RevenueBucket> buckets = new LinkedHashMap<>();

        if (range.chartGroupType() == ChartGroupType.HOUR) {
            for (int hour = 0; hour < 24; hour++) {
                buckets.put(String.format("%02d:00", hour), new RevenueBucket());
            }

            return buckets;
        }

        if (range.chartGroupType() == ChartGroupType.DAY) {
            LocalDate current = range.fromDate();

            while (!current.isAfter(range.toDate())) {
                buckets.put(current.toString(), new RevenueBucket());
                current = current.plusDays(1);
            }

            return buckets;
        }

        if (range.chartGroupType() == ChartGroupType.QUARTER) {
            LocalDate currentQuarter = firstDayOfQuarter(range.fromDate());
            LocalDate endQuarter = firstDayOfQuarter(range.toDate());

            while (!currentQuarter.isAfter(endQuarter)) {
                buckets.put(buildQuarterLabel(currentQuarter), new RevenueBucket());
                currentQuarter = currentQuarter.plusMonths(3);
            }

            return buckets;
        }

        YearMonth currentMonth = YearMonth.from(range.fromDate());
        YearMonth endMonth = YearMonth.from(range.toDate());

        while (!currentMonth.isAfter(endMonth)) {
            buckets.put(currentMonth.toString(), new RevenueBucket());
            currentMonth = currentMonth.plusMonths(1);
        }

        return buckets;
    }

    private String buildChartLabel(
            LocalDateTime createdAt,
            ChartGroupType chartGroupType
    ) {
        return switch (chartGroupType) {
            case HOUR -> String.format("%02d:00", createdAt.getHour());
            case DAY -> createdAt.toLocalDate().toString();
            case MONTH -> YearMonth.from(createdAt.toLocalDate()).toString();
            case QUARTER -> buildQuarterLabel(createdAt.toLocalDate());
        };
    }

    private LocalDate firstDayOfQuarter(LocalDate date) {
        int firstMonthOfQuarter = ((date.getMonthValue() - 1) / 3) * 3 + 1;
        return LocalDate.of(date.getYear(), firstMonthOfQuarter, 1);
    }

    private String buildQuarterLabel(LocalDate date) {
        int quarter = (date.getMonthValue() - 1) / 3 + 1;
        return "Q" + quarter + "/" + date.getYear();
    }

    /**
     * Doanh thu bán của một Order không tính shipping.
     *
     * ONLINE: FinalAmount = tiền hàng sau voucher + shipping.
     * POS/IN_STORE: FinalAmount không chứa shipping, nên dùng nguyên FinalAmount.
     *
     * Tách theo OrderType còn giúp báo cáo lịch sử đúng với các POS row cũ có thể
     * đã lưu Shippingfee mặc định 30.000 dù POS thực tế không thu shipping.
     */
    private BigDecimal calculateSalesRevenue(Order order) {
        if (order == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal finalAmount = moneyOrZero(order.getFinalAmount());
        String orderType = normalizeOrderType(order.getOrderType());

        if (isOnlineOrderType(orderType)) {
            return finalAmount.subtract(moneyOrZero(order.getShippingFee()));
        }

        return finalAmount;
    }

    private String normalizeOrderType(Object rawType) {
        if (rawType == null) {
            return "";
        }

        return rawType.toString().trim().toUpperCase(Locale.ROOT);
    }

    private boolean isOnlineOrderType(String orderType) {
        return "ONLINE".equals(orderType);
    }

    private boolean isCounterOrderType(String orderType) {
        return "IN_STORE".equals(orderType) || "POS".equals(orderType);
    }

    private BigDecimal objectToMoney(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }

        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }

        return BigDecimal.ZERO;
    }

    private long objectToLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        return 0L;
    }

    private boolean containsWhitespace(String value) {
        return value != null && value.chars().anyMatch(Character::isWhitespace);
    }

    private String safeText(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }

        return value.trim();
    }

    private BigDecimal moneyOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long longOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private enum ChartGroupType {
        HOUR,
        DAY,
        MONTH,
        QUARTER
    }

    private record DateRange(
            ReportFilterType filterType,
            LocalDate fromDate,
            LocalDate toDate,
            LocalDateTime startDateTime,
            LocalDateTime endDateTimeExclusive,
            ChartGroupType chartGroupType
    ) {
    }

    private static final class RevenueBucket {

        private BigDecimal revenue = BigDecimal.ZERO;

        private long totalOrders = 0L;

        void addSale(BigDecimal amount) {
            this.revenue = this.revenue.add(amount == null ? BigDecimal.ZERO : amount);
            this.totalOrders++;
        }

        void addRefund(BigDecimal amount) {
            this.revenue = this.revenue.subtract(amount == null ? BigDecimal.ZERO : amount);
        }

        BigDecimal revenue() {
            return revenue;
        }

        long totalOrders() {
            return totalOrders;
        }
    }

    private String formatCapacity(Double value, String snapshotName) {
        if (value != null && value > 0) {
            String formatted = value % 1 == 0 ? String.valueOf(value.intValue()) : String.valueOf(value);
            return formatted + " ml";
        }
        if (StringUtils.hasText(snapshotName)) {
            return snapshotName.trim();
        }
        return "";
    }
}