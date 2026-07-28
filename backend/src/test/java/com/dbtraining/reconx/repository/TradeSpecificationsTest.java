package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.repository.entity.Trade;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * TICKET-ADV056 — Unit tests for TradeSpecifications.
 *
 * These test each specification factory in isolation by mocking the
 * CriteriaBuilder and Root, verifying that:
 *   - null arguments produce conjunction() (no-op predicate)
 *   - non-null arguments produce the correct predicate call
 */
class TradeSpecificationsTest {

    private Root<Trade> root;
    private CriteriaQuery<?> query;
    private CriteriaBuilder cb;
    private Predicate conjunction;
    private Predicate realPredicate;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        root = mock(Root.class);
        query = mock(CriteriaQuery.class);
        cb = mock(CriteriaBuilder.class);
        conjunction = mock(Predicate.class);
        realPredicate = mock(Predicate.class);
        when(cb.conjunction()).thenReturn(conjunction);
    }

    // ---- hasStatus ----

    @Test
    @DisplayName("hasStatus(null) returns conjunction")
    void hasStatus_null_returnsConjunction() {
        Predicate result = TradeSpecifications.hasStatus(null).toPredicate(root, query, cb);
        assertThat(result).isSameAs(conjunction);
    }

    @Test
    @DisplayName("hasStatus(value) calls cb.equal on status field")
    @SuppressWarnings("unchecked")
    void hasStatus_nonNull_callsEqual() {
        Path<Object> statusPath = mock(Path.class);
        when(root.get("status")).thenReturn(statusPath);
        when(cb.equal(statusPath, "PENDING")).thenReturn(realPredicate);

        Predicate result = TradeSpecifications.hasStatus("PENDING").toPredicate(root, query, cb);

        assertThat(result).isSameAs(realPredicate);
        verify(cb).equal(statusPath, "PENDING");
    }

    // ---- tradeDateBetween ----

    @Test
    @DisplayName("tradeDateBetween(null, null) returns conjunction")
    void tradeDateBetween_bothNull_returnsConjunction() {
        Predicate result = TradeSpecifications.tradeDateBetween(null, null).toPredicate(root, query, cb);
        assertThat(result).isSameAs(conjunction);
    }

    @Test
    @DisplayName("tradeDateBetween(from, null) uses greaterThanOrEqualTo")
    @SuppressWarnings("unchecked")
    void tradeDateBetween_fromOnly_usesGte() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        Path<LocalDate> datePath = mock(Path.class);
        when(root.<LocalDate>get("tradeDate")).thenReturn(datePath);
        when(cb.greaterThanOrEqualTo(datePath, from)).thenReturn(realPredicate);

        Predicate result = TradeSpecifications.tradeDateBetween(from, null).toPredicate(root, query, cb);

        assertThat(result).isSameAs(realPredicate);
        verify(cb).greaterThanOrEqualTo(datePath, from);
    }

    @Test
    @DisplayName("tradeDateBetween(null, to) uses lessThanOrEqualTo")
    @SuppressWarnings("unchecked")
    void tradeDateBetween_toOnly_usesLte() {
        LocalDate to = LocalDate.of(2026, 12, 31);
        Path<LocalDate> datePath = mock(Path.class);
        when(root.<LocalDate>get("tradeDate")).thenReturn(datePath);
        when(cb.lessThanOrEqualTo(datePath, to)).thenReturn(realPredicate);

        Predicate result = TradeSpecifications.tradeDateBetween(null, to).toPredicate(root, query, cb);

        assertThat(result).isSameAs(realPredicate);
        verify(cb).lessThanOrEqualTo(datePath, to);
    }

    @Test
    @DisplayName("tradeDateBetween(from, to) uses between")
    @SuppressWarnings("unchecked")
    void tradeDateBetween_bothPresent_usesBetween() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);
        Path<LocalDate> datePath = mock(Path.class);
        when(root.<LocalDate>get("tradeDate")).thenReturn(datePath);
        when(cb.between(datePath, from, to)).thenReturn(realPredicate);

        Predicate result = TradeSpecifications.tradeDateBetween(from, to).toPredicate(root, query, cb);

        assertThat(result).isSameAs(realPredicate);
        verify(cb).between(datePath, from, to);
    }

    // ---- hasCounterparty ----

    @Test
    @DisplayName("hasCounterparty(null) returns conjunction")
    void hasCounterparty_null_returnsConjunction() {
        Predicate result = TradeSpecifications.hasCounterparty(null).toPredicate(root, query, cb);
        assertThat(result).isSameAs(conjunction);
    }

    @Test
    @DisplayName("hasCounterparty(id) navigates counterparty.id and calls equal")
    @SuppressWarnings("unchecked")
    void hasCounterparty_nonNull_callsEqual() {
        Path<Object> cpPath = mock(Path.class);
        Path<Object> idPath = mock(Path.class);
        when(root.get("counterparty")).thenReturn(cpPath);
        when(cpPath.get("id")).thenReturn(idPath);
        when(cb.equal(idPath, 42L)).thenReturn(realPredicate);

        Predicate result = TradeSpecifications.hasCounterparty(42L).toPredicate(root, query, cb);

        assertThat(result).isSameAs(realPredicate);
        verify(cb).equal(idPath, 42L);
    }
}
