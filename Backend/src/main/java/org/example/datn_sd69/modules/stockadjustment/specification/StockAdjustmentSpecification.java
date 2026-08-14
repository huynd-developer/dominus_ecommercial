package org.example.datn_sd69.modules.stockadjustment.specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.example.datn_sd69.entity.StockAdjustment;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.enums.StockAdjustmentStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class StockAdjustmentSpecification {

    private StockAdjustmentSpecification() {
    }

    public static Specification<StockAdjustment> build(
            String keyword,
            StockAdjustmentStatus status,
            Integer createdBy,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        return (root, query, cb) -> {

            List<Predicate> predicates =
                    new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {

                String pattern =
                        "%" + keyword.trim().toLowerCase() + "%";

                Join<StockAdjustment, User> creator =
                        root.join("createdBy", JoinType.LEFT);

                predicates.add(
                        cb.or(
                                cb.like(
                                        cb.lower(root.get("adjustmentNo")),
                                        pattern
                                ),
                                cb.like(
                                        cb.lower(creator.get("name")),
                                        pattern
                                ),
                                cb.like(
                                        cb.lower(
                                                cb.coalesce(
                                                        root.get("note"),
                                                        ""
                                                )
                                        ),
                                        pattern
                                )
                        )
                );
            }

            if (status != null) {
                predicates.add(
                        cb.equal(
                                root.get("status"),
                                status.getCode()
                        )
                );
            }

            if (createdBy != null) {
                predicates.add(
                        cb.equal(
                                root.get("createdBy").get("id"),
                                createdBy
                        )
                );
            }

            if (fromDate != null) {

                LocalDateTime from =
                        fromDate.atStartOfDay();

                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("createdAt"),
                                from
                        )
                );
            }

            if (toDate != null) {

                LocalDateTime toExclusive =
                        toDate.plusDays(1).atStartOfDay();

                predicates.add(
                        cb.lessThan(
                                root.get("createdAt"),
                                toExclusive
                        )
                );
            }

            return cb.and(
                    predicates.toArray(
                            new Predicate[0]
                    )
            );
        };
    }
}
