package org.example.datn_sd69.modules.report.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.modules.report.dto.BestSellingProductResponse;
import org.example.datn_sd69.modules.report.dto.ReportSummaryResponse;
import org.example.datn_sd69.modules.report.dto.RevenueChartResponse;
import org.example.datn_sd69.modules.report.enums.ReportFilterType;
import org.example.datn_sd69.modules.report.projection.BestSellingProductProjection;
import org.example.datn_sd69.modules.report.service.OwnerReportService;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
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
     * Chỉ tính doanh thu khi đơn đã HOÀN THÀNH.
     *
     * 0 - Chờ xác nhận
     * 1 - Đã xác nhận
     * 2 - Đang giao hàng
     * 3 - Hoàn thành
     * 4 - Đã hủy
     * 5 - Giao hàng thất bại
     *
     * Doanh thu thực tế không tính đơn đang chờ, đã xác nhận hoặc đang giao.
     */
    private static final Integer COMPLETED_STATUS = 3;

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final int MAX_CUSTOM_DAYS = 366;

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern POSITIVE_INTEGER_PATTERN = Pattern.compile("^\\d+$");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public ReportSummaryResponse getSummary(
            String filterType,
            String fromDate,
            String toDate
    ) {
        DateRange range = resolveDateRange(filterType, fromDate, toDate);

        BigDecimal totalRevenue = orderRepository.sumFinalAmountByStatusAndCreatedAtBetween(
                COMPLETED_STATUS,
                range.startDateTime(),
                range.endDateTimeExclusive()
        );

        Long totalOrders = orderRepository.countOrdersByStatusAndCreatedAtBetween(
                COMPLETED_STATUS,
                range.startDateTime(),
                range.endDateTimeExclusive()
        );

        Long totalProductsSold = orderItemRepository.sumSoldQuantityByCompletedOrders(
                COMPLETED_STATUS,
                range.startDateTime(),
                range.endDateTimeExclusive()
        );

        return new ReportSummaryResponse(
                range.filterType(),
                range.fromDate(),
                range.toDate(),
                moneyOrZero(totalRevenue),
                longOrZero(totalOrders),
                longOrZero(totalProductsSold)
        );
    }

    @Override
    public List<RevenueChartResponse> getRevenueChart(
            String filterType,
            String fromDate,
            String toDate
    ) {
        DateRange range = resolveDateRange(filterType, fromDate, toDate);

        List<Order> orders = orderRepository.findCompletedOrdersForChart(
                COMPLETED_STATUS,
                range.startDateTime(),
                range.endDateTimeExclusive()
        );

        LinkedHashMap<String, RevenueBucket> buckets = initChartBuckets(range);

        for (Order order : orders) {
            if (order == null || order.getCreatedAt() == null) {
                continue;
            }

            String label = buildChartLabel(order.getCreatedAt(), range.chartGroupType());

            buckets
                    .computeIfAbsent(label, key -> new RevenueBucket())
                    .add(order.getFinalAmount());
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
    public List<BestSellingProductResponse> getBestSellingProducts(
            String filterType,
            String fromDate,
            String toDate,
            String limit
    ) {
        DateRange range = resolveDateRange(filterType, fromDate, toDate);
        int safeLimit = parseLimit(limit);

        List<BestSellingProductProjection> projections = orderItemRepository.findBestSellingProducts(
                COMPLETED_STATUS,
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
            throw badRequest("filterType không hợp lệ. Chỉ nhận: DAY, WEEK, MONTH, YEAR, CUSTOM");
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
        };
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
        MONTH
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

        void add(BigDecimal amount) {
            this.revenue = this.revenue.add(amount == null ? BigDecimal.ZERO : amount);
            this.totalOrders++;
        }

        BigDecimal revenue() {
            return revenue;
        }

        long totalOrders() {
            return totalOrders;
        }
    }
}