package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.repository.entity.Trade;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;

public final class TradeSpecification {

    private TradeSpecification() {}

    public static Specification<Trade> tradeDateBetween(LocalDate from, LocalDate to) {
        return (Root<Trade> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (from == null && to == null) return cb.conjunction();
            if (from == null) return cb.lessThanOrEqualTo(root.get("tradeDate"), to);
            if (to == null) return cb.greaterThanOrEqualTo(root.get("tradeDate"), from);
            return cb.between(root.get("tradeDate"), from, to);
        };
    }

    public static Specification<Trade> hasStatus(String status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Trade> forCounterparty(Long counterpartyId) {
        return (root, query, cb) -> counterpartyId == null ? cb.conjunction()
                : cb.equal(root.get("counterparty").get("id"), counterpartyId);
    }

    public static Specification<Trade> refLike(String pattern) {
        return (root, query, cb) -> (pattern == null || pattern.isBlank()) ? cb.conjunction()
                : cb.like(root.get("tradeRef"), pattern + "%");
    }
}
